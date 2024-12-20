package com.fsGps.manager

import com.fsGps.FsGps
import com.fsGps.model.GpsPoint
import com.fsGps.utils.LocationUtil
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import net.kyori.adventure.title.Title
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitRunnable
import java.lang.Math.toDegrees
import java.time.Duration
import kotlin.math.atan2
import kotlin.math.sqrt

class GpsManager(private val plugin: FsGps) {

    private val gpsPoints = mutableListOf<GpsPoint>()
    private val activeRoutes = mutableMapOf<Player, GpsPoint>()
    private val routeUpdateInterval = 20L

    fun loadConfig() {
        val config = plugin.config
        gpsPoints.clear()

        config.getConfigurationSection("gps_points")?.let { section ->
            for (key in section.getKeys(false)) {
                val pointConfig = section.getConfigurationSection(key)
                val x = pointConfig!!.getDouble("x")
                val z = pointConfig.getDouble("z")
                val category = pointConfig.getString("category")!!
                gpsPoints.add(GpsPoint(key, category, x, z))
            }
        }
    }

    fun saveConfig() {
        val config = plugin.config
        val gpsSection = config.createSection("gps_points")

        gpsPoints.forEach { point ->
            val pointConfig = gpsSection.createSection(point.name)
            pointConfig.set("x", point.x)
            pointConfig.set("z", point.z)
            pointConfig.set("category", point.category)
        }

        plugin.saveConfig()
    }

    fun createGpsPoint(player: Player, name: String, category: String) {
        val location = player.location
        val point = GpsPoint(name, category, location.x, location.z)
        gpsPoints.add(point)
        saveConfig()
        player.sendMessage("Точка $name создана в категории $category")
    }

    fun deleteGpsPoint(player: Player, name: String) {
        gpsPoints.removeIf { it.name == name }
        saveConfig()
        player.sendMessage("Точка $name удалена")
    }

    fun openGpsMenu(player: Player) {
        val inventory = Bukkit.createInventory(null, 54, Component.text("GPS Меню"))

        gpsPoints.forEach { point ->
            val item = ItemStack(Material.COMPASS)
            val meta = item.itemMeta!!

            meta.displayName(
                Component.text(point.name)
                    .color(TextColor.color(255, 255, 255))
                    .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false)
            )

            meta.lore(
                listOf(
                    Component.empty(),
                    Component.text("Категория: ")
                        .color(TextColor.color(255, 255, 0))
                        .append(Component.text(point.category)
                            .color(TextColor.color(255, 255, 255))
                            .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false)),
                    Component.text("Координаты: ")
                        .color(TextColor.color(255, 255, 0))
                        .append(Component.text("X=${point.x}, Z=${point.z}")
                            .color(TextColor.color(255, 255, 255))
                            .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false))
                )
            )


            item.itemMeta = meta
            inventory.addItem(item)
        }

        player.openInventory(inventory)
    }


    fun handleMenuClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        val item = event.currentItem ?: return

        // Проверяем название инвентаря через Adventure API
        if (event.view.title() != Component.text("GPS Меню")) return

        event.isCancelled = true

        // Получаем Component и преобразуем в строку
        val meta = item.itemMeta
        val pointNameComponent = meta?.displayName() ?: return
        val pointName = PlainTextComponentSerializer.plainText().serialize(pointNameComponent)

        val point = gpsPoints.find { it.name == pointName }

        if (point != null) {
            startNavigation(player, point.name)
            player.closeInventory()
            player.sendMessage("Навигация к точке ${point.name} началась!")
        } else {
            player.sendMessage("Ошибка: Точка $pointName не найдена.")
        }
    }

    fun showNearestGpsPoint(player: Player, category: String) {
        val nearestPoint = gpsPoints.filter { it.category == category }
            .minByOrNull { point -> LocationUtil.getDistance(player.location, point.x, point.z) }

        if (nearestPoint != null) {
            player.sendMessage("Ближайшая точка из категории $category: ${nearestPoint.name}")
            startNavigation(player, nearestPoint.name)
        } else {
            player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f)
            player.sendMessage("Точек в категории $category не найдено.")
        }
    }

    fun startNavigation(player: Player, name: String) {
        val destination = gpsPoints.find { it.name == name }
        if (destination != null) {
            activeRoutes[player] = destination
            player.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f)
            player.sendMessage("Навигация на точку $name началась!")
            startRouteUpdater()
        } else {
            player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f)
            player.sendMessage("Точка $name не найдена.")
        }
    }

    fun stopNavigation(player: Player) {
        activeRoutes.remove(player)
        player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 1.0f)
        player.sendMessage("Навигация остановлена.")
    }

    private fun startRouteUpdater() {
        object : BukkitRunnable() {
            override fun run() {
                activeRoutes.forEach { (player, destination) ->
                    if (player.isOnline) {
                        updateRoute(player, destination)
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, routeUpdateInterval)
    }

    private fun calculateRoute(player: Player, destination: GpsPoint) {
        val location = player.location
        val distanceX = location.x - destination.x
        val distanceZ = location.z - destination.z
        val distance = sqrt(distanceX * distanceX + distanceZ * distanceZ)

        if (distance <= 7) {
            activeRoutes.remove(player)
            player.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f)
            player.sendMessage(Component.text("Вы успешно достигли точки ${destination.name}!")
                .color(TextColor.color(0, 255, 0)))
        } else {
            val loc = player.location
            val destinationLoc = Location(player.world, destination.x, loc.y, destination.z)

            val eyeLocation = player.eyeLocation
            val lookDirection = eyeLocation.direction.clone().setY(0).normalize()

            val toTarget = destinationLoc.toVector().subtract(eyeLocation.toVector()).normalize()

            val dot = lookDirection.dot(toTarget)
            val det = lookDirection.x * toTarget.z - lookDirection.z * toTarget.x
            val angle = toDegrees(atan2(det, dot))

            val directionSymbol = when (angle) {
                in -22.5..22.5 -> "⬆"
                in 22.5..67.5 -> "⬈"
                in 67.5..112.5 -> "➡"
                in 112.5..157.5 -> "⬊"
                in 157.5..202.5, in -180.0..-157.5 -> "⬇"
                in -157.5..-112.5 -> "⬋"
                in -112.5..-67.5 -> "⬅"
                in -67.5..-22.5 -> "⬉"
                else -> "⬇"
            }

            player.showTitle(Title.title(
                Component.text(" $directionSymbol"),
                Component.text("До ${destination.name} осталось ${distance.toInt()} м."),
                Title.Times.times(Duration.ofSeconds(0), Duration.ofSeconds(2), Duration.ofSeconds(0))
            ))
        }
    }

    fun updateRoute(player: Player, destination: GpsPoint) {
        calculateRoute(player, destination)
    }

    fun updateAllRoutes() {
        activeRoutes.forEach { (player, destination) ->
            calculateRoute(player, destination)
        }
    }
}
