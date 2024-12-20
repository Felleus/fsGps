package com.fsGps.command

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.Default
import co.aikar.commands.annotation.Subcommand
import com.fsGps.manager.GpsManager
import org.bukkit.entity.Player

@CommandAlias("gps")
class GpsCommand(private val gpsManager: GpsManager) : BaseCommand() {

    @Subcommand("create")
    fun onCreate(player: Player, name: String, category: String) {
        gpsManager.createGpsPoint(player, name, category)
        player.sendMessage("Точка $name создана в категории $category")
    }

    @Subcommand("delete")
    fun onDelete(player: Player, name: String) {
        gpsManager.deleteGpsPoint(player, name)
        player.sendMessage("Точка $name удалена")
    }

    @Subcommand("near")
    fun onNear(player: Player, category: String) {
        gpsManager.showNearestGpsPoint(player, category)
    }

    @Subcommand("start")
    fun onStart(player: Player, name: String) {
        gpsManager.startNavigation(player, name)
    }

    @Subcommand("stop")
    fun onStop(player: Player) {
        gpsManager.stopNavigation(player)
    }

    @Default
    fun onList(player: Player) {
        gpsManager.openGpsMenu(player)
    }
}
