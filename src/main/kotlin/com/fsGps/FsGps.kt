package com.fsGps

import net.kyori.adventure.platform.bukkit.BukkitAudiences
import co.aikar.commands.PaperCommandManager
import com.fsGps.command.GpsCommand
import com.fsGps.listener.GpsMenuListener
import com.fsGps.manager.GpsManager
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable

class FsGps : JavaPlugin() {

    private lateinit var gpsManager: GpsManager
    private var adventure: BukkitAudiences? = null

    override fun onEnable() {
        gpsManager = GpsManager(this)
        gpsManager.loadConfig()
        adventure = BukkitAudiences.create(this)

        val commandManager = PaperCommandManager(this)
        commandManager.registerCommand(GpsCommand(gpsManager))
        server.pluginManager.registerEvents(GpsMenuListener(gpsManager), this)

        object : BukkitRunnable() {
            override fun run() {
                gpsManager.updateAllRoutes()
            }
        }.runTaskTimer(this, 0L, 5L) // Каждые 20 тиков (1 секунду)
    }

    override fun onDisable() {
        adventure?.close()
        adventure = null
        gpsManager.saveConfig()
    }

    fun adventure(): BukkitAudiences = adventure ?: throw IllegalStateException("Adventure is not initialized")
}
