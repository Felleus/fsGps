package com.fsGps.manager

import com.fsGps.FsGps
import com.fsGps.model.GpsPoint
import com.fsGps.utils.LocationUtil
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.title.Title
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.util.Vector
import java.lang.Math.toDegrees
import kotlin.math.atan2
import kotlin.math.sqrt
import java.lang.Math
import java.time.Duration

class GpsManager(private val plugin: FsGps) {

    private val gpsPoints = mutableListOf<GpsPoint>()
    private val activeRoutes = mutableMapOf<Player, GpsPoint>()
    private val configManager = plugin.config
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

    fun showNearestGpsPoint(player: Player, category: String) {
        val nearestPoint = gpsPoints.filter { it.category == category }
            .minByOrNull { point -> LocationUtil.getDistance(player.location, point.x, point.z) }

        if (nearestPoint != null) {
            player.sendMessage("Ближайшая точка из категории $category: ${nearestPoint.name}")
        } else {
            player.sendMessage("Точек в категории $category не найдено.")
        }
    }

    fun openGpsMenu(player: Player) {
        player.sendMessage("Меню GPS: ${gpsPoints.size} точек доступно.")
    }

    fun startNavigation(player: Player, name: String) {
        val destination = gpsPoints.find { it.name == name }
        if (destination != null) {
            activeRoutes[player] = destination
            player.sendMessage("Навигация на точку $name началась!")
            startRouteUpdater()
        } else {
            player.sendMessage("Точка $name не найдена.")
        }
    }

    fun stopNavigation(player: Player) {
        activeRoutes.remove(player)
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

        val title = Component.text("Навигация").color(TextColor.color(255, 0, 0))

        val routeTitle: Component = if (distance <= 7) {
            activeRoutes.remove(player)
            player.sendMessage(Component.text("Вы успешно достигли точки ${destination.name}!")
                .color(TextColor.color(0, 255, 0)))

            Component.text("Вы достигли места назначения").color(TextColor.color(0, 255, 0))
        } else {
            val loc = player.location
            val destinationLoc = Location(player.world, destination.x, loc.y, destination.z)

            val eyeLocation = player.eyeLocation
            val lookDirection = eyeLocation.direction.clone().setY(0).normalize()

            val toTarget = destinationLoc.toVector().subtract(eyeLocation.toVector()).normalize()

            val dot = lookDirection.dot(toTarget)
            val det = lookDirection.x * toTarget.z - lookDirection.z * toTarget.x
            val angle = toDegrees(atan2(det, dot))

            val direction = when (angle) {
                in -22.5..22.5 -> Component.text("⬆")
                in 22.5..67.5 -> Component.text("⬈")
                in 67.5..112.5 -> Component.text("➡")
                in 112.5..157.5 -> Component.text("⬊")
                in 157.5..202.5, in -180.0..-157.5 -> Component.text("⬇")
                in -157.5..-112.5 -> Component.text("⬋")
                in -112.5..-67.5 -> Component.text("⬅")
                in -67.5..-22.5 -> Component.text("⬉")
                else -> Component.text("⬇")
            }

            val directionColor = when (angle) {
                in -22.5..22.5 -> TextColor.color(0, 255, 0)
                in 22.5..67.5 -> TextColor.color(255, 255, 0)
                in 67.5..112.5 -> TextColor.color(255, 165, 0)
                in 112.5..157.5 -> TextColor.color(255, 69, 0)
                in 157.5..202.5 -> TextColor.color(255, 0, 0)
                in -157.5..-112.5 -> TextColor.color(255, 69, 0)
                in -112.5..-67.5 -> TextColor.color(255, 165, 0)
                in -67.5..-22.5 -> TextColor.color(255, 255, 0)
                else -> TextColor.color(255, 0, 0)
            }

            player.showTitle(Title.title(
                Component.text("До ${destination.name} осталось ${distance.toInt()} м."), Component.text("Направление: $direction"),
                Title.Times.times(Duration.ofSeconds(0), Duration.ofSeconds(2), Duration.ofSeconds(0))
            ));


            Component.text("До ${destination.name} осталось ${distance.toInt()} м.")
                .color(TextColor.color(255, 255, 255))
                .append(direction)
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
