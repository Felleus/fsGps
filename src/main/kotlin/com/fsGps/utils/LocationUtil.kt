package com.fsGps.utils

import org.bukkit.Location
import kotlin.math.sqrt

object LocationUtil {

    fun getDistance(location: Location, x: Double, z: Double): Double {
        val distanceX = location.x - x
        val distanceZ = location.z - z
        return sqrt(distanceX * distanceX + distanceZ * distanceZ)
    }
}
