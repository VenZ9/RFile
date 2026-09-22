package com.example.utils

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.example.model.ApkInfo
import java.io.File

object ApkInspectorHelper {

  fun parseApk(context: Context, file: File): ApkInfo? {
    if (!file.exists() || !file.name.endsWith(".apk", ignoreCase = true)) return null

    return try {
      val pm = context.packageManager
      val flags = PackageManager.GET_PERMISSIONS or PackageManager.GET_ACTIVITIES
      val packageInfo = pm.getPackageArchiveInfo(file.absolutePath, flags) ?: return null

      val appInfo = packageInfo.applicationInfo ?: return null
      appInfo.sourceDir = file.absolutePath
      appInfo.publicSourceDir = file.absolutePath

      val appLabel = pm.getApplicationLabel(appInfo).toString()
      val packageName = packageInfo.packageName ?: "Unknown"
      val versionName = packageInfo.versionName ?: "1.0"
      val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        packageInfo.longVersionCode
      } else {
        @Suppress("DEPRECATION")
        packageInfo.versionCode.toLong()
      }

      val minSdk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        appInfo.minSdkVersion
      } else {
        21
      }
      val targetSdk = appInfo.targetSdkVersion

      val permissions = packageInfo.requestedPermissions?.map { perm ->
        perm.substringAfterLast('.')
      } ?: emptyList()

      ApkInfo(
        appName = appLabel.ifBlank { file.nameWithoutExtension },
        packageName = packageName,
        versionName = versionName,
        versionCode = versionCode,
        minSdk = minSdk,
        targetSdk = targetSdk,
        permissions = permissions,
        fileSize = file.length(),
      )
    } catch (e: Exception) {
      e.printStackTrace()
      null
    }
  }
}
