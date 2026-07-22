/*
 * Mojanq Launcher
 * Copyright (C) 2025 AltayHR and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/gpl-3.0.txt>.
 */

package com.movtery.zalithlauncher.utils.platform

import android.app.ActivityManager
import android.content.Context
import androidx.annotation.WorkerThread
import com.movtery.zalithlauncher.utils.device.Architecture
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.round

private const val BYTES_PER_MB = 1024L * 1024

private inline val Context.activityManager: ActivityManager
    get() = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

private fun getMemoryInfo(context: Context): ActivityManager.MemoryInfo {
    return ActivityManager.MemoryInfo().apply {
        context.activityManager.getMemoryInfo(this)
    }
}

/**
 * Sistem toplam belleğini döndürür (bayt cinsinden)
 */
@WorkerThread
fun getTotalMemory(context: Context) = getMemoryInfo(context).totalMem

/**
 * Kullanılan belleği döndürür (bayt cinsinden)
 */
@WorkerThread
fun getUsedMemory(context: Context): Long {
    val info = getMemoryInfo(context)
    return info.totalMem - info.availMem
}

/**
 * Mevcut boş belleği döndürür (bayt cinsinden)
 */
@WorkerThread
fun getFreeMemory(context: Context) = getMemoryInfo(context).availMem

/**
 * Ayarlar için maksimum bellek değerini hesaplar (sistem için alan bırakır)
 */
@WorkerThread
fun getMaxMemoryForSettings(context: Context): Int {
    val deviceRam = getTotalMemory(context).bytesToMB()
    val maxRam: Int = if (Architecture.is32BitsDevice || deviceRam < 2048) {
        min(1024.0, deviceRam).toInt()
    } else {
        (deviceRam - (if (deviceRam < 3064) 800 else 1024)).toInt()
    }
    return maxRam
}

/**
 * Swap/ZRAM dahil maksimum bellek değerini hesaplar.
 *
 * Modern Android cihazlarda ZRAM (sıkıştırılmış RAM diski) genellikle
 * toplam RAM'in %50-70'i kadar ek alan sağlar. Bu fonksiyon ZRAM'i de
 * hesaba katarak daha yüksek bellek limitine izin verir.
 *
 * Bu sayede fizik modu gibi çok fazla RAM gerektiren modlar çalışırken
 * uygulama çökmez - ZRAM üzerine taşma yaparak çalışmaya devam eder.
 */
@WorkerThread
fun getMaxMemoryWithSwap(context: Context): Int {
    val deviceRam = getTotalMemory(context).bytesToMB()
    if (Architecture.is32BitsDevice || deviceRam < 2048) {
        return min(1024.0, deviceRam).toInt()
    }
    // ZRAM genellikle toplam RAM'in %50'si kadar ek alan sağlar (muhafazakâr tahmin)
    val estimatedZram = deviceRam * 0.50
    val totalWithZram = deviceRam + estimatedZram
    // Sistem için daha fazla alan bırak (ZRAM kullanıldığında sistem ihtiyacı artar)
    val systemReserve: Double = if (deviceRam < 6144) 1500.0 else 2048.0
    val maxWithSwap = (totalWithZram - systemReserve).toInt()
    return maxOf(maxWithSwap, getMaxMemoryForSettings(context))
}

/**
 * Tahmini ZRAM/Swap boyutunu MB cinsinden döndürür
 */
@WorkerThread
fun getEstimatedSwapSize(context: Context): Int {
    val deviceRam = getTotalMemory(context).bytesToMB()
    return (deviceRam * 0.50).toInt()
}

/**
 * MB birimine çevirir
 */
fun Long.bytesToMB(decimals: Int = 2, roundDown: Boolean = false): Double {
    val megaBytes = this.toDouble() / BYTES_PER_MB
    return if (decimals == 0) {
        if (roundDown) floor(megaBytes) else round(megaBytes)
    } else {
        val roundingMode = if (roundDown) RoundingMode.DOWN else RoundingMode.HALF_UP
        BigDecimal(megaBytes).setScale(decimals, roundingMode).toDouble()
    }
}
