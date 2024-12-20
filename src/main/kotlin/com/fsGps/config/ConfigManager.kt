package com.fsGps.config

import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

class ConfigManager(private val plugin: JavaPlugin) {

    private val configFile = File(plugin.dataFolder, "config.yml")

    fun loadConfig(): YamlConfiguration {
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false)
        }
        return YamlConfiguration.loadConfiguration(configFile)
    }

    fun saveConfig(config: YamlConfiguration) {
        config.save(configFile)
    }
}
