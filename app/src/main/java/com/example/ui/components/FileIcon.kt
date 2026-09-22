package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.FileItem
import com.example.model.FileType
import com.example.ui.theme.*

@Composable
fun FileIcon(
  item: FileItem,
  modifier: Modifier = Modifier,
  size: Dp = 44.dp,
  iconSize: Dp = 26.dp,
) {
  val (bgColor, iconColor, iconVector) = when (item.fileType) {
    FileType.FOLDER -> Triple(
      FolderAmber.copy(alpha = 0.18f),
      FolderAmber,
      if (item.childCount > 0) Icons.Default.Folder else Icons.Default.FolderOpen
    )
    FileType.ARCHIVE -> Triple(
      ArchiveBlue.copy(alpha = 0.18f),
      ArchiveBlue,
      Icons.Default.Archive
    )
    FileType.APK -> Triple(
      ApkGreen.copy(alpha = 0.18f),
      ApkGreen,
      Icons.Default.Android
    )
    FileType.IMAGE -> Triple(
      ImagePurple.copy(alpha = 0.18f),
      ImagePurple,
      Icons.Default.Image
    )
    FileType.AUDIO -> Triple(
      AudioCyan.copy(alpha = 0.18f),
      AudioCyan,
      Icons.Default.Audiotrack
    )
    FileType.VIDEO -> Triple(
      VideoCoral.copy(alpha = 0.18f),
      VideoCoral,
      Icons.Default.Movie
    )
    FileType.DOCUMENT -> Triple(
      DocumentIndigo.copy(alpha = 0.18f),
      DocumentIndigo,
      Icons.Default.Description
    )
    FileType.CODE -> Triple(
      CodeTeal.copy(alpha = 0.18f),
      CodeTeal,
      Icons.Default.Code
    )
    FileType.BINARY -> Triple(
      BinaryGray.copy(alpha = 0.18f),
      BinaryGray,
      Icons.Default.Memory
    )
    FileType.UNKNOWN -> Triple(
      Color.White.copy(alpha = 0.08f),
      Color.LightGray,
      Icons.AutoMirrored.Filled.InsertDriveFile
    )
  }

  Box(
    modifier = modifier
      .size(size)
      .clip(RoundedCornerShape(10.dp))
      .background(bgColor),
    contentAlignment = Alignment.Center
  ) {
    Icon(
      imageVector = iconVector,
      contentDescription = item.name,
      tint = iconColor,
      modifier = Modifier.size(iconSize)
    )
  }
}
