package com.example.utils

import com.example.model.FileItem
import com.example.model.FileType
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FileUtils {

  private val archiveExtensions = setOf("zip", "tar", "gz", "tgz", "bz2", "xz", "7z", "rar", "iso", "jar")
  private val apkExtensions = setOf("apk", "xapk", "apks", "aab")
  private val imageExtensions = setOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "svg")
  private val audioExtensions = setOf("mp3", "wav", "ogg", "flac", "m4a", "aac", "wma", "opus")
  private val videoExtensions = setOf("mp4", "mkv", "avi", "mov", "webm", "flv", "3gp")
  private val documentExtensions = setOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "epub", "csv")
  private val codeExtensions = setOf(
    "txt", "json", "xml", "kt", "java", "py", "c", "cpp", "h", "html",
    "css", "js", "ts", "md", "sh", "yml", "yaml", "ini", "cfg", "properties",
    "gradle", "kts", "sql", "log", "conf", "env"
  )
  private val binaryExtensions = setOf("so", "bin", "dat", "dex", "o", "class", "dll", "exe")

  fun formatFileSize(size: Long): String {
    if (size <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, units.size - 1)
    val value = size / Math.pow(1024.0, digitGroups.toDouble())
    return DecimalFormat("#,##0.#").format(value) + " " + units[digitGroups]
  }

  fun formatDate(timestamp: Long): String {
    if (timestamp <= 0L) return "Unknown"
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
  }

  fun determineFileType(file: File): FileType {
    if (file.isDirectory) return FileType.FOLDER
    val ext = file.extension.lowercase(Locale.ROOT)
    return when {
      ext in archiveExtensions -> FileType.ARCHIVE
      ext in apkExtensions -> FileType.APK
      ext in imageExtensions -> FileType.IMAGE
      ext in audioExtensions -> FileType.AUDIO
      ext in videoExtensions -> FileType.VIDEO
      ext in documentExtensions -> FileType.DOCUMENT
      ext in codeExtensions -> FileType.CODE
      ext in binaryExtensions -> FileType.BINARY
      else -> FileType.UNKNOWN
    }
  }

  fun determineFileTypeFromExtension(extension: String, isDir: Boolean): FileType {
    if (isDir) return FileType.FOLDER
    val ext = extension.lowercase(Locale.ROOT)
    return when {
      ext in archiveExtensions -> FileType.ARCHIVE
      ext in apkExtensions -> FileType.APK
      ext in imageExtensions -> FileType.IMAGE
      ext in audioExtensions -> FileType.AUDIO
      ext in videoExtensions -> FileType.VIDEO
      ext in documentExtensions -> FileType.DOCUMENT
      ext in codeExtensions -> FileType.CODE
      ext in binaryExtensions -> FileType.BINARY
      else -> FileType.UNKNOWN
    }
  }

  fun isArchiveFile(file: File): Boolean {
    if (file.isDirectory) return false
    val ext = file.extension.lowercase(Locale.ROOT)
    return ext in archiveExtensions
  }

  fun getFilePermissions(file: File): String {
    val sb = StringBuilder()
    sb.append(if (file.isDirectory) "d" else "-")
    sb.append(if (file.canRead()) "r" else "-")
    sb.append(if (file.canWrite()) "w" else "-")
    sb.append(if (file.canExecute()) "x" else "-")
    sb.append("r--r--")
    return sb.toString()
  }

  fun toFileItem(file: File): FileItem {
    val isDir = file.isDirectory
    val ext = file.extension.lowercase(Locale.ROOT)
    val type = determineFileType(file)
    val children = if (isDir) (file.listFiles()?.size ?: 0) else 0
    return FileItem(
      name = file.name,
      path = file.absolutePath,
      size = if (isDir) 0L else file.length(),
      lastModified = file.lastModified(),
      isDirectory = isDir,
      isArchive = isArchiveFile(file),
      extension = ext,
      fileType = type,
      permissions = getFilePermissions(file),
      childCount = children,
    )
  }

  fun copyRecursively(src: File, dest: File) {
    if (src.isDirectory) {
      if (!dest.exists()) dest.mkdirs()
      src.listFiles()?.forEach { child ->
        copyRecursively(child, File(dest, child.name))
      }
    } else {
      dest.parentFile?.mkdirs()
      FileInputStream(src).use { input ->
        FileOutputStream(dest).use { output ->
          input.copyTo(output)
        }
      }
    }
  }

  fun move(src: File, dest: File): Boolean {
    if (src.renameTo(dest)) return true
    copyRecursively(src, dest)
    src.deleteRecursively()
    return true
  }

  fun getSafeTargetFile(parent: File, targetName: String): File {
    var candidate = File(parent, targetName)
    if (!candidate.exists()) return candidate

    val baseName = candidate.nameWithoutExtension
    val ext = candidate.extension
    var counter = 1
    while (candidate.exists()) {
      val newName = if (ext.isNotEmpty()) "$baseName ($counter).$ext" else "$baseName ($counter)"
      candidate = File(parent, newName)
      counter++
    }
    return candidate
  }
}
