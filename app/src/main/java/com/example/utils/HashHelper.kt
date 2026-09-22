package com.example.utils

import com.example.model.HashResult
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

object HashHelper {

  fun calculateHashes(file: File): HashResult {
    val md5Digest = MessageDigest.getInstance("MD5")
    val sha1Digest = MessageDigest.getInstance("SHA-1")
    val sha256Digest = MessageDigest.getInstance("SHA-256")

    val buffer = ByteArray(8192)
    FileInputStream(file).use { fis ->
      var bytesRead: Int
      while (fis.read(buffer).also { bytesRead = it } != -1) {
        md5Digest.update(buffer, 0, bytesRead)
        sha1Digest.update(buffer, 0, bytesRead)
        sha256Digest.update(buffer, 0, bytesRead)
      }
    }

    return HashResult(
      fileName = file.name,
      filePath = file.absolutePath,
      fileSize = file.length(),
      md5 = bytesToHex(md5Digest.digest()),
      sha1 = bytesToHex(sha1Digest.digest()),
      sha256 = bytesToHex(sha256Digest.digest()),
    )
  }

  private fun bytesToHex(bytes: ByteArray): String {
    val sb = StringBuilder()
    for (b in bytes) {
      sb.append(String.format("%02x", b))
    }
    return sb.toString()
  }
}
