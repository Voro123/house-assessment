package com.voro.houseassessment.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.baidu.mapapi.CoordType
import com.baidu.mapapi.SDKInitializer
import java.security.MessageDigest

object BaiduMapRuntime {
    @Volatile
    private var initializedApiKey: String? = null

    @Synchronized
    fun initialize(context: Context, apiKey: String): Result<Unit> {
        val normalizedKey = apiKey.trim()
        if (normalizedKey.isBlank()) return Result.failure(IllegalArgumentException("百度地图 AK 不能为空"))

        if (initializedApiKey == normalizedKey) return Result.success(Unit)
        if (initializedApiKey != null && initializedApiKey != normalizedKey) {
            return Result.failure(IllegalStateException("百度地图 AK 已在本次进程初始化。修改 AK 后请彻底关闭 App 再重新打开。"))
        }

        return runCatching {
            val appContext = context.applicationContext
            // Baidu requires privacy consent to be declared before any map SDK initialization.
            SDKInitializer.setAgreePrivacy(appContext, true)
            // The official SDK supports setting AK dynamically before initialize().
            SDKInitializer.setApiKey(normalizedKey)
            SDKInitializer.initialize(appContext)
            // Mainland China uses GCJ02 when globally declared; overseas map output remains WGS84.
            SDKInitializer.setCoordType(CoordType.GCJ02)
            initializedApiKey = normalizedKey
        }
    }
}

@Suppress("DEPRECATION")
fun getAppSigningSha1(context: Context): String {
    val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
    } else {
        context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
    }

    val certificateBytes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val signingInfo = packageInfo.signingInfo ?: return "无法读取签名"
        val signatures = if (signingInfo.hasMultipleSigners()) {
            signingInfo.apkContentsSigners
        } else {
            signingInfo.signingCertificateHistory
        }
        signatures.firstOrNull()?.toByteArray()
    } else {
        packageInfo.signatures?.firstOrNull()?.toByteArray()
    } ?: return "无法读取签名"

    val digest = MessageDigest.getInstance("SHA-1").digest(certificateBytes)
    return digest.joinToString(":") { byte -> "%02X".format(byte.toInt() and 0xFF) }
}
