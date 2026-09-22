package com.example.model

import java.io.File

enum class FileType {
  FOLDER,
  ARCHIVE,
  APK,
  IMAGE,
  AUDIO,
  VIDEO,
  DOCUMENT,
  CODE,
  BINARY,
  UNKNOWN,
}

enum class ArchiveType(val extension: String, val displayName: String) {
  ZIP(".zip", "ZIP Archive"),
  TAR(".tar", "TAR Tarball"),
  GZ(".gz", "GZ Compressed"),
}

enum class CompressionLevel(val level: Int, val displayName: String) {
  STORE(0, "Store (No Compression)"),
  FAST(1, "Fastest"),
  NORMAL(6, "Normal (Deflate)"),
  MAXIMUM(9, "Ultra / Maximum"),
}

enum class SortOption(val title: String) {
  NAME_ASC("Name (A to Z)"),
  NAME_DESC("Name (Z to A)"),
  DATE_DESC("Date (Newest first)"),
  DATE_ASC("Date (Oldest first)"),
  SIZE_DESC("Size (Largest first)"),
  SIZE_ASC("Size (Smallest first)"),
  TYPE("Type (Grouped)"),
}

enum class ViewMode {
  LIST,
  GRID,
}

data class FileItem(
  val name: String,
  val path: String,
  val size: Long = 0L,
  val lastModified: Long = 0L,
  val isDirectory: Boolean = false,
  val isArchive: Boolean = false,
  val extension: String = "",
  val fileType: FileType = FileType.UNKNOWN,
  val permissions: String = "-rw-rw-r--",
  val childCount: Int = 0,
  val isArchiveVirtualEntry: Boolean = false,
  val parentArchiveFilePath: String? = null,
  val compressedSize: Long = 0L,
) {
  fun toFile(): File = File(path)
}

data class ApkInfo(
  val appName: String,
  val packageName: String,
  val versionName: String,
  val versionCode: Long,
  val minSdk: Int,
  val targetSdk: Int,
  val permissions: List<String>,
  val fileSize: Long,
)

data class HashResult(
  val fileName: String,
  val filePath: String,
  val fileSize: Long,
  val md5: String,
  val sha1: String,
  val sha256: String,
)

data class StorageBreakdown(
  val totalBytes: Long = 0L,
  val freeBytes: Long = 0L,
  val usedBytes: Long = 0L,
  val archiveBytes: Long = 0L,
  val apkBytes: Long = 0L,
  val imageBytes: Long = 0L,
  val audioBytes: Long = 0L,
  val videoBytes: Long = 0L,
  val docBytes: Long = 0L,
  val otherBytes: Long = 0L,
)

enum class ClipboardOperation {
  COPY,
  CUT,
}

data class ClipboardState(
  val items: List<FileItem>,
  val operation: ClipboardOperation,
)
