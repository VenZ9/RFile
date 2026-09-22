package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmeraldPrimary
import java.io.File

@Composable
fun BreadcrumbBar(
  currentPath: String,
  inArchiveMode: Boolean,
  archiveInternalPath: String,
  activeArchiveName: String?,
  onNavigateTo: (String) -> Unit,
  onNavigateUp: () -> Unit,
  onOpenStorageBookmarks: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val scrollState = rememberScrollState()

  LaunchedEffect(currentPath, archiveInternalPath) {
    scrollState.animateScrollTo(scrollState.maxValue)
  }

  Surface(
    modifier = modifier.fillMaxWidth(),
    color = MaterialTheme.colorScheme.surfaceVariant,
    tonalElevation = 2.dp
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 8.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Up One Level Button
      IconButton(
        onClick = onNavigateUp,
        modifier = Modifier
          .size(36.dp)
          .testTag("nav_up_button")
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowBack,
          contentDescription = "Navigate Up",
          tint = EmeraldPrimary,
          modifier = Modifier.size(20.dp)
        )
      }

      // Storage selector / bookmarks icon
      IconButton(
        onClick = onOpenStorageBookmarks,
        modifier = Modifier
          .size(36.dp)
          .testTag("storage_bookmarks_button")
      ) {
        Icon(
          imageVector = Icons.Default.FolderSpecial,
          contentDescription = "Storage Bookmarks",
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(20.dp)
        )
      }

      Spacer(modifier = Modifier.width(4.dp))

      // Scrollable path crumbs
      Row(
        modifier = Modifier
          .weight(1f)
          .horizontalScroll(scrollState),
        verticalAlignment = Alignment.CenterVertically
      ) {
        if (inArchiveMode) {
          // Inside Archive breadcrumbs
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(6.dp))
              .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f))
              .padding(horizontal = 8.dp, vertical = 4.dp)
          ) {
            Text(
              text = activeArchiveName ?: "Archive",
              color = MaterialTheme.colorScheme.secondary,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold
            )
          }

          val segments = archiveInternalPath.split('/').filter { it.isNotBlank() }
          var cumulative = ""
          for (seg in segments) {
            cumulative = if (cumulative.isEmpty()) seg else "$cumulative/$seg"
            val target = cumulative
            Text(
              text = " / ",
              color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
              fontSize = 12.sp
            )
            Text(
              text = seg,
              color = MaterialTheme.colorScheme.onSurface,
              fontSize = 12.sp,
              fontWeight = FontWeight.Medium,
              modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .clickable { onNavigateTo(target) }
                .padding(horizontal = 4.dp, vertical = 2.dp)
            )
          }
        } else {
          // Standard filesystem path breadcrumbs
          val pathParts = currentPath.split(File.separatorChar).filter { it.isNotBlank() }

          // Root icon
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(4.dp))
              .clickable { onNavigateTo(File.separator) }
              .padding(horizontal = 4.dp, vertical = 2.dp)
          ) {
            Text(
              text = "/",
              color = EmeraldPrimary,
              fontWeight = FontWeight.Bold,
              fontSize = 14.sp
            )
          }

          var accumulatedPath = ""
          for (part in pathParts) {
            accumulatedPath += File.separator + part
            val thisPath = accumulatedPath
            Text(
              text = " > ",
              color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
              fontSize = 11.sp
            )
            Text(
              text = part,
              color = if (thisPath == currentPath) EmeraldPrimary else MaterialTheme.colorScheme.onSurface,
              fontSize = 12.sp,
              fontWeight = if (thisPath == currentPath) FontWeight.SemiBold else FontWeight.Normal,
              modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .clickable { onNavigateTo(thisPath) }
                .padding(horizontal = 4.dp, vertical = 2.dp)
            )
          }
        }
      }
    }
  }
}
