package com.example.data

import android.content.Context
import android.os.Environment
import com.example.model.ArchiveType
import com.example.model.FileItem
import com.example.model.FileType
import com.example.model.StorageBreakdown
import com.example.utils.ArchiveManager
import com.example.utils.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class StorageRepository(private val context: Context) {

  // Primary roots
  val deviceStorageRoot: File
    get() {
      val ext = Environment.getExternalStorageDirectory()
      return if (ext != null && ext.canRead()) ext else context.filesDir
    }

  val appSandboxRoot: File
    get() = context.getExternalFilesDir(null) ?: context.filesDir

  /**
   * Initializes realistic sample files and test archives on first run
   * so user can immediately test all ZArchiver & R File features.
   */
  suspend fun initializeSampleEnvironmentIfNeeded() = withContext(Dispatchers.IO) {
    try {
      val base = appSandboxRoot
      val flagFile = File(base, ".rfile_initialized")
      if (flagFile.exists()) return@withContext

      // Create realistic folder structure
      val downloadDir = File(base, "Download").apply { mkdirs() }
      val docsDir = File(base, "Documents").apply { mkdirs() }
      val archivesDir = File(base, "Archives").apply { mkdirs() }
      val dcimDir = File(base, "DCIM/Camera").apply { mkdirs() }
      val codeDir = File(base, "Code").apply { mkdirs() }
      val androidDataDir = File(base, "Android/data/com.rfile.app/files").apply { mkdirs() }

      // 1. Documents: Text & JSON config
      File(docsDir, "readme_rfile.txt").writeText(
        """
        ==================================================
        * R FILE - Next-Gen Archive & File Manager *
        ==================================================
        Inspired by ZArchiver with advanced power-user capabilities:
        - View inside archives (.zip, .tar, .gz) without extraction
        - High-speed multi-level compression & extraction
        - Built-in Text/Code Viewer & Editor with line numbers
        - Built-in Hex / Binary Inspector
        - Checksum & Hash Generator (MD5, SHA-1, SHA-256)
        - APK Manifest & Permissions Inspector
        - Storage Analyzer with visual breakdown
        - Multi-select, Batch Operations & Floating Clipboard
        ==================================================
        """.trimIndent()
      )

      File(docsDir, "server_config.json").writeText(
        """
        {
          "app": "R File",
          "version": "1.0.0",
          "compression": {
            "default_level": 6,
            "enable_integrity_test": true
          },
          "storage": {
            "root": "/storage/emulated/0",
            "show_hidden": false
          }
        }
        """.trimIndent()
      )

      // 2. Code files
      File(codeDir, "ArchiveWorker.kt").writeText(
        "package com.rfile.core\n\nclass ArchiveWorker {\n    fun compress(files: List<String>, output: String) {\n        println(\"Compressing archive payload...\")\n    }\n}\n"
      )

      File(codeDir, "deploy.sh").writeText(
        """
        #!/bin/bash
        echo "Building R File archive package..."
        tar -czvf release_v1.0.tar.gz ./app
        echo "Done!"
        """.trimIndent()
      )

      // 3. Android Data file (binary for Hex Viewer demo)
      val saveGameFile = File(androidDataDir, "game_state.dat")
      saveGameFile.outputStream().use { fos ->
        // Write header and some identifiable bytes for hex view
        val magic = byteArrayOf(0x52, 0x46, 0x49, 0x4C, 0x45, 0x01, 0x00, 0x00) // "RFILE\1\0\0"
        fos.write(magic)
        for (i in 0 until 128) {
          fos.write((i * 7 + 13) and 0xFF)
        }
      }

      // 4. Create sample files to pack into demo archives
      val tempDir = File(base, ".temp_builder").apply { mkdirs() }
      val modReadme = File(tempDir, "mod_info.txt").apply {
        writeText("Shadow Knight Mod v2.4\nCreated by R File Team\nCompatibility: Android 10+")
      }
      val modConfig = File(tempDir, "settings.ini").apply {
        writeText("[Graphics]\nFPS=120\nResolution=High\nAntiAliasing=4x\n[Audio]\nMasterVolume=100")
      }
      val subDir = File(tempDir, "assets").apply { mkdirs() }
      File(subDir, "texture_map.bin").apply {
        writeBytes(ByteArray(256) { (it and 0xFF).toByte() })
      }

      // Create real valid demo ZIP archive in Download/
      val zipOutput = File(downloadDir, "game_mod_v2.4.zip")
      ArchiveManager.createZipArchive(
        listOf(modReadme, modConfig, subDir),
        zipOutput,
        compressionLevel = 6
      )

      // Create real valid demo TAR archive in Archives/
      val tarOutput = File(archivesDir, "system_backup.tar")
      ArchiveManager.createTarArchive(
        listOf(modReadme, modConfig, subDir),
        tarOutput
      )

      // Clean temp dir
      tempDir.deleteRecursively()

      // 5. Create a mock APK for APK inspector demonstration
      val sampleApk = File(downloadDir, "sample_utility.apk")
      sampleApk.writeBytes(
        // Write a minimal zip with dummy AndroidManifest for file representation
        zipOutput.readBytes()
      )

      flagFile.createNewFile()
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  suspend fun listFiles(currentPath: String): List<FileItem> = withContext(Dispatchers.IO) {
    val dir = File(currentPath)
    if (!dir.exists() || !dir.canRead()) {
      return@withContext emptyList()
    }

    val files = dir.listFiles() ?: return@withContext emptyList()
    return@withContext files.map { file ->
      FileUtils.toFileItem(file)
    }
  }

  suspend fun calculateStorageBreakdown(): StorageBreakdown = withContext(Dispatchers.IO) {
    val base = appSandboxRoot
    var totalSize = 0L
    var archiveSize = 0L
    var apkSize = 0L
    var imageSize = 0L
    var audioSize = 0L
    var videoSize = 0L
    var docSize = 0L
    var otherSize = 0L

    fun scan(dir: File, depth: Int = 0) {
      if (depth > 6) return
      val list = dir.listFiles() ?: return
      for (f in list) {
        if (f.isDirectory) {
          scan(f, depth + 1)
        } else {
          val len = f.length()
          totalSize += len
          when (FileUtils.determineFileType(f)) {
            FileType.ARCHIVE -> archiveSize += len
            FileType.APK -> apkSize += len
            FileType.IMAGE -> imageSize += len
            FileType.AUDIO -> audioSize += len
            FileType.VIDEO -> videoSize += len
            FileType.DOCUMENT -> docSize += len
            else -> otherSize += len
          }
        }
      }
    }

    scan(base)

    val statTotal = base.totalSpace.coerceAtLeast(totalSize)
    val statFree = base.freeSpace

    StorageBreakdown(
      totalBytes = statTotal,
      freeBytes = statFree,
      usedBytes = totalSize,
      archiveBytes = archiveSize,
      apkBytes = apkSize,
      imageBytes = imageSize,
      audioBytes = audioSize,
      videoBytes = videoSize,
      docBytes = docSize,
      otherBytes = otherSize,
    )
  }

  suspend fun createDirectory(parentPath: String, dirName: String): Boolean = withContext(Dispatchers.IO) {
    val parent = File(parentPath)
    if (!parent.exists()) parent.mkdirs()
    val newDir = File(parent, dirName.trim())
    newDir.mkdir()
  }

  suspend fun createFile(parentPath: String, fileName: String, initialContent: String = ""): Boolean = withContext(Dispatchers.IO) {
    val parent = File(parentPath)
    if (!parent.exists()) parent.mkdirs()
    val newFile = File(parent, fileName.trim())
    if (newFile.createNewFile()) {
      if (initialContent.isNotEmpty()) {
        newFile.writeText(initialContent)
      }
      true
    } else false
  }

  suspend fun deleteItems(items: List<FileItem>): Int = withContext(Dispatchers.IO) {
    var deleted = 0
    for (item in items) {
      val f = File(item.path)
      if (f.exists()) {
        if (f.deleteRecursively()) deleted++
      }
    }
    deleted
  }

  suspend fun renameItem(oldPath: String, newName: String): Boolean = withContext(Dispatchers.IO) {
    val old = File(oldPath)
    if (!old.exists()) return@withContext false
    val newFile = File(old.parentFile, newName.trim())
    old.renameTo(newFile)
  }

  suspend fun pasteItems(
    items: List<FileItem>,
    destinationDir: String,
    isCut: Boolean,
  ): Int = withContext(Dispatchers.IO) {
    val dest = File(destinationDir)
    if (!dest.exists()) dest.mkdirs()
    var count = 0

    for (item in items) {
      val src = File(item.path)
      if (!src.exists()) continue
      val target = FileUtils.getSafeTargetFile(dest, src.name)

      if (isCut) {
        if (FileUtils.move(src, target)) count++
      } else {
        FileUtils.copyRecursively(src, target)
        count++
      }
    }
    count
  }
}
