package com.example.utils

import java.io.File
import java.io.FileInputStream

object HexViewerHelper {

  data class HexRow(
    val offset: String,
    val hexPart: String,
    val asciiPart: String,
  )

  /**
   * Reads up to maxBytes (e.g. 64KB) from a file and formats it into hex rows for power-user inspection.
   */
  fun loadHexDump(file: File, offsetStart: Long = 0L, maxBytes: Int = 32768): Pair<List<HexRow>, Long> {
    val rows = mutableListOf<HexRow>()
    val fileSize = file.length()
    if (!file.exists() || file.isDirectory) return Pair(emptyList(), fileSize)

    FileInputStream(file).use { fis ->
      if (offsetStart > 0) {
        fis.skip(offsetStart)
      }

      val buffer = ByteArray(16)
      var currentOffset = offsetStart
      var totalRead = 0

      while (totalRead < maxBytes) {
        val toRead = minOf(buffer.size, maxBytes - totalRead)
        val read = fis.read(buffer, 0, toRead)
        if (read == -1) break

        val offsetStr = String.format("%08X", currentOffset)

        // Hex representation
        val hexBuilder = StringBuilder()
        for (i in 0 until 16) {
          if (i == 8) hexBuilder.append(" ")
          if (i < read) {
            hexBuilder.append(String.format("%02X ", buffer[i]))
          } else {
            hexBuilder.append("   ")
          }
        }

        // ASCII representation
        val asciiBuilder = StringBuilder()
        for (i in 0 until read) {
          val b = buffer[i].toInt() and 0xFF
          if (b in 32..126) {
            asciiBuilder.append(b.toChar())
          } else {
            asciiBuilder.append('.')
          }
        }

        rows.add(HexRow(offsetStr, hexBuilder.toString(), asciiBuilder.toString()))

        currentOffset += read
        totalRead += read
      }
    }

    return Pair(rows, fileSize)
  }
}
