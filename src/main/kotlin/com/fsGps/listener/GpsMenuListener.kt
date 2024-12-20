package com.fsGps.listener

import com.fsGps.manager.GpsManager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent

class GpsMenuListener(private val gpsManager: GpsManager) : Listener {

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        gpsManager.handleMenuClick(event)
    }
}
