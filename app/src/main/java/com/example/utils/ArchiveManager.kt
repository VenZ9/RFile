package com.example.utils

import com.example.model.FileItem
import com.example.model.FileType
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

object ArchiveManager {

  data class VirtualArchiveFolder(
    val path: String,
    val items: List<FileItem>,
  )

  /**
   * Reads all entries in a ZIP file and generates a structured list of FileItems
   * representing a specific folder within the archive (or root if internalFolder is "").
   */
  fun listArchiveContents(
    archiveFile: File,
    internalFolder: String = "",
  ): List<FileItem> {
    val normalizedInternal = internalFolder.trim('/').let { if (it.isEmpty()) "" else "$it/" }
    val results = mutableMapOf<String, FileItem>()

    if (archiveFile.extension.equals("tar", ignoreCase = true)) {
      return listTarArchiveContents(archiveFile, normalizedInternal)
    }

    try {
      ZipFile(archiveFile).use { zip ->
        val entries = zip.entries()
        while (entries.hasMoreElements()) {
          val entry = entries.nextElement()
          val entryName = entry.name.trimStart('/')

          // Check if this entry is within the requested internal folder
          if (normalizedInternal.isNotEmpty() && !entryName.startsWith(normalizedInternal)) {
            continue
          }

          // Relative path inside this folder level
          val subPath = if (normalizedInternal.isEmpty()) entryName else entryName.removePrefix(normalizedInternal)
          if (subPath.isEmpty()) continue

          val segments = subPath.split('/')
          val firstName = segments[0]
          val isDir = entry.isDirectory || segments.size > 1

          if (!results.containsKey(firstName)) {
            val itemPath = if (normalizedInternal.isEmpty()) firstName else "$normalizedInternal$firstName"
            val ext = if (isDir) "" else File(firstName).extension
            val type = if (isDir) FileType.FOLDER else FileUtils.determineFileTypeFromExtension(ext, isDir)

            results[firstName] = FileItem(
              name = firstName,
              path = itemPath,
              size = if (isDir) 0L else entry.size.coerceAtLeast(0L),
              lastModified = entry.time.coerceAtLeast(archiveFile.lastModified()),
              isDirectory = isDir,
              isArchive = false,
              extension = ext,
              fileType = type,
              permissions = if (isDir) "drwxr-xr-x" else "-rw-r--r--",
              childCount = 0,
              isArchiveVirtualEntry = true,
              parentArchiveFilePath = archiveFile.absolutePath,
              compressedSize = if (isDir) 0L else entry.compressedSize.coerceAtLeast(0L),
            )
          }
        }
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }

    return results.values.sortedWith(
      compareBy<FileItem> { !it.isDirectory }
        .thenBy { it.name.lowercase() }
    )
  }

  /**
   * Tests the archive integrity by reading all stream bytes and validating headers.
   */
  fun testArchive(archiveFile: File): Pair<Boolean, String> {
    return try {
      var count = 0
      var totalSize = 0L
      ZipFile(archiveFile).use { zip ->
        val entries = zip.entries()
        val buffer = ByteArray(8192)
        while (entries.hasMoreElements()) {
          val entry = entries.nextElement()
          count++
          totalSize += entry.size.coerceAtLeast(0L)
          if (!entry.isDirectory) {
            zip.getInputStream(entry).use { stream ->
              while (stream.read(buffer) != -1) {
                // read through to verify CRC
              }
            }
          }
        }
      }
      Pair(true, "Archive verified OK!\nEntries checked: $count\nUncompressed payload: ${FileUtils.formatFileSize(totalSize)}")
    } catch (e: Exception) {
      Pair(false, "Integrity check failed: ${e.message}")
    }
  }

  /**
   * Extracts an entire ZIP archive or single entry into outputDir.
   */
  fun extractZip(
    archiveFile: File,
    outputDir: File,
    specificEntryName: String? = null,
    onProgress: (Float, String) -> Unit = { _, _ -> },
  ): Boolean {
    if (!outputDir.exists()) outputDir.mkdirs()

    return try {
      ZipFile(archiveFile).use { zip ->
        val entriesList = mutableListOf<ZipEntry>()
        val enumEntries = zip.entries()
        while (enumEntries.hasMoreElements()) {
          val e = enumEntries.nextElement()
          if (specificEntryName == null || e.name == specificEntryName || e.name.startsWith("$specificEntryName/")) {
            entriesList.add(e)
          }
        }

        val total = entriesList.size.coerceAtLeast(1)
        var processed = 0
        val buffer = ByteArray(8192)

        for (entry in entriesList) {
          processed++
          onProgress(processed.toFloat() / total.toFloat(), entry.name)

          // Normalize path to prevent Zip Slip vulnerability
          val targetFile = File(outputDir, entry.name)
          if (!targetFile.canonicalPath.startsWith(outputDir.canonicalPath)) {
            continue
          }

          if (entry.isDirectory) {
            targetFile.mkdirs()
          } else {
            targetFile.parentFile?.mkdirs()
            zip.getInputStream(entry).use { input ->
              FileOutputStream(targetFile).use { output ->
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                  output.write(buffer, 0, bytesRead)
                }
              }
            }
          }
        }
      }
      true
    } catch (e: Exception) {
      e.printStackTrace()
      false
    }
  }

  /**
   * Creates a ZIP archive with the given compression level (0 = STORE, 9 = MAXIMUM).
   */
  fun createZipArchive(
    filesToCompress: List<File>,
    destinationZip: File,
    compressionLevel: Int = 6,
    onProgress: (Float, String) -> Unit = { _, _ -> },
  ): Boolean {
    destinationZip.parentFile?.mkdirs()

    return try {
      val allEntries = mutableListOf<Pair<File, String>>()
      for (file in filesToCompress) {
        collectFilesForArchive(file, "", allEntries)
      }

      val total = allEntries.size.coerceAtLeast(1)
      var processed = 0
      val buffer = ByteArray(8192)

      FileOutputStream(destinationZip).use { fos ->
        BufferedOutputStream(fos).use { bos ->
          ZipOutputStream(bos).use { zos ->
            zos.setLevel(compressionLevel)

            for ((file, entryRelativePath) in allEntries) {
              processed++
              onProgress(processed.toFloat() / total.toFloat(), file.name)

              if (file.isDirectory) {
                val dirEntry = ZipEntry(if (entryRelativePath.endsWith("/")) entryRelativePath else "$entryRelativePath/")
                dirEntry.time = file.lastModified()
                zos.putNextEntry(dirEntry)
                zos.closeEntry()
              } else {
                val fileEntry = ZipEntry(entryRelativePath)
                fileEntry.time = file.lastModified()
                fileEntry.size = file.length()
                zos.putNextEntry(fileEntry)

                FileInputStream(file).use { fis ->
                  BufferedInputStream(fis).use { bis ->
                    var bytes: Int
                    while (bis.read(buffer).also { bytes = it } != -1) {
                      zos.write(buffer, 0, bytes)
                    }
                  }
                }
                zos.closeEntry()
              }
            }
          }
        }
      }
      true
    } catch (e: Exception) {
      e.printStackTrace()
      false
    }
  }

  private fun collectFilesForArchive(
    file: File,
    basePath: String,
    outList: MutableList<Pair<File, String>>,
  ) {
    val relativePath = if (basePath.isEmpty()) file.name else "$basePath/${file.name}"
    outList.add(Pair(file, relativePath))

    if (file.isDirectory) {
      file.listFiles()?.forEach { child ->
        collectFilesForArchive(child, relativePath, outList)
      }
    }
  }

  /**
   * Lightweight TAR reader: TAR format has 512-byte header blocks.
   */
  private fun listTarArchiveContents(tarFile: File, normalizedInternal: String): List<FileItem> {
    val results = mutableMapOf<String, FileItem>()
    try {
      FileInputStream(tarFile).use { fis ->
        val header = ByteArray(512)
        while (fis.read(header) == 512) {
          // Check for empty block (end of tar)
          if (header.all { it == 0.toByte() }) break

          // Name is in first 100 bytes
          val nameBytes = header.copyOfRange(0, 100)
          val name = String(nameBytes).trim('\u0000', ' ')
          if (name.isBlank()) break

          // Size is at offset 124, 12 bytes octal
          val sizeBytes = header.copyOfRange(124, 136)
          val sizeStr = String(sizeBytes).trim('\u0000', ' ')
          val size = try { sizeStr.toLong(8) } catch (_: Exception) { 0L }

          // Type flag at offset 156 ('5' for directory)
          val typeFlag = header[156].toInt().toChar()
          val isDir = typeFlag == '5' || name.endsWith("/")

          val entryName = name.trimStart('/')
          if (normalizedInternal.isNotEmpty() && !entryName.startsWith(normalizedInternal)) {
            // Skip data bytes
            skipTarData(fis, size)
            continue
          }

          val subPath = if (normalizedInternal.isEmpty()) entryName else entryName.removePrefix(normalizedInternal)
          if (subPath.isNotEmpty()) {
            val segments = subPath.split('/')
            val firstName = segments[0]
            val entryIsDir = isDir || segments.size > 1

            if (!results.containsKey(firstName)) {
              val ext = if (entryIsDir) "" else File(firstName).extension
              val type = if (entryIsDir) FileType.FOLDER else FileUtils.determineFileTypeFromExtension(ext, entryIsDir)

              results[firstName] = FileItem(
                name = firstName,
                path = if (normalizedInternal.isEmpty()) firstName else "$normalizedInternal$firstName",
                size = if (entryIsDir) 0L else size,
                lastModified = tarFile.lastModified(),
                isDirectory = entryIsDir,
                isArchive = false,
                extension = ext,
                fileType = type,
                permissions = if (entryIsDir) "drwxr-xr-x" else "-rw-r--r--",
                childCount = 0,
                isArchiveVirtualEntry = true,
                parentArchiveFilePath = tarFile.absolutePath,
                compressedSize = size,
              )
            }
          }

          // Skip tar data blocks (rounded up to 512)
          skipTarData(fis, size)
        }
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
    return results.values.sortedWith(
      compareBy<FileItem> { !it.isDirectory }
        .thenBy { it.name.lowercase() }
    )
  }

  private fun skipTarData(stream: FileInputStream, size: Long) {
    if (size <= 0) return
    val remainder = size % 512
    val padding = if (remainder > 0) 512 - remainder else 0
    val totalToSkip = size + padding
    var skipped = 0L
    while (skipped < totalToSkip) {
      val s = stream.skip(totalToSkip - skipped)
      if (s <= 0) break
      skipped += s
    }
  }

  /**
   * Creates a standard TAR archive.
   */
  fun createTarArchive(
    filesToCompress: List<File>,
    destinationTar: File,
    onProgress: (Float, String) -> Unit = { _, _ -> },
  ): Boolean {
    destinationTar.parentFile?.mkdirs()
    return try {
      val allEntries = mutableListOf<Pair<File, String>>()
      for (file in filesToCompress) {
        collectFilesForArchive(file, "", allEntries)
      }

      val total = allEntries.size.coerceAtLeast(1)
      var processed = 0
      val buffer = ByteArray(8192)

      FileOutputStream(destinationTar).use { fos ->
        BufferedOutputStream(fos).use { bos ->
          for ((file, entryRelativePath) in allEntries) {
            processed++
            onProgress(processed.toFloat() / total.toFloat(), file.name)

            val header = ByteArray(512)
            val nameBytes = entryRelativePath.toByteArray(Charsets.US_ASCII)
            System.arraycopy(nameBytes, 0, header, 0, minOf(nameBytes.size, 99))

            // Mode
            val modeStr = (if (file.isDirectory) "0000755\u0000" else "0000644\u0000").toByteArray(Charsets.US_ASCII)
            System.arraycopy(modeStr, 0, header, 100, modeStr.size)

            // Size (octal)
            val size = if (file.isDirectory) 0L else file.length()
            val sizeStr = String.format("%011o\u0000", size).toByteArray(Charsets.US_ASCII)
            System.arraycopy(sizeStr, 0, header, 124, minOf(sizeStr.size, 12))

            // MTime (octal)
            val mtimeStr = String.format("%011o\u0000", file.lastModified() / 1000).toByteArray(Charsets.US_ASCII)
            System.arraycopy(mtimeStr, 0, header, 136, minOf(mtimeStr.size, 12))

            // Typeflag
            header[156] = if (file.isDirectory) '5'.code.toByte() else '0'.code.toByte()

            // Magic "ustar "
            val magic = "ustar  \u0000".toByteArray(Charsets.US_ASCII)
            System.arraycopy(magic, 0, header, 257, minOf(magic.size, 8))

            // Checksum calculation: place 8 spaces at 148..155
            for (i in 148..155) header[i] = ' '.code.toByte()
            var chk = 0L
            for (b in header) chk += (b.toInt() and 0xFF)
            val chkStr = String.format("%06o\u0000 ", chk).toByteArray(Charsets.US_ASCII)
            System.arraycopy(chkStr, 0, header, 148, minOf(chkStr.size, 8))

            bos.write(header)

            if (!file.isDirectory) {
              FileInputStream(file).use { fis ->
                var bytesRead: Int
                while (fis.read(buffer).also { bytesRead = it } != -1) {
                  bos.write(buffer, 0, bytesRead)
                }
              }
              // Tar pad to 512
              val remainder = (size % 512).toInt()
              if (remainder > 0) {
                bos.write(ByteArray(512 - remainder))
              }
            }
          }
          // Write two 512-byte zero blocks at end of tar
          bos.write(ByteArray(1024))
        }
      }
      true
    } catch (e: Exception) {
      e.printStackTrace()
      false
    }
  }
}
