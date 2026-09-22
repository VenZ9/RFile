package com.example.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.FileItem
import com.example.utils.FileUtils

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileListItem(
  item: FileItem,
  isSelected: Boolean,
  isMultiSelectMode: Boolean,
  onItemClick: () -> Unit,
  onItemLongClick: () -> Unit,
  onSelectToggle: () -> Unit,
  onMoreClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val backgroundColor = if (isSelected) {
    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
  } else {
    Color.Transparent
  }

  Row(
    modifier = modifier
      .fillMaxWidth()
      .background(backgroundColor)
      .combinedClickable(
        onClick = {
          if (isMultiSelectMode) {
            onSelectToggle()
          } else {
            onItemClick()
          }
        },
        onLongClick = onItemLongClick
      )
      .padding(horizontal = 12.dp, vertical = 8.dp)
      .testTag("file_item_${item.name}"),
    verticalAlignment = Alignment.CenterVertically
  ) {
    // Selection Checkbox or Icon
    if (isMultiSelectMode) {
      Checkbox(
        checked = isSelected,
        onCheckedChange = { onSelectToggle() },
        modifier = Modifier.padding(end = 6.dp)
      )
    }

    FileIcon(item = item)

    Spacer(modifier = Modifier.width(12.dp))

    // Name and details
    Column(
      modifier = Modifier.weight(1f)
    ) {
      Text(
        text = item.name,
        style = MaterialTheme.typography.bodyMedium.copy(
          fontWeight = if (item.isDirectory) FontWeight.SemiBold else FontWeight.Normal,
          fontSize = 14.sp
        ),
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )

      Spacer(modifier = Modifier.height(2.dp))

      Row(
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Size or Item Count
        val sizeText = if (item.isDirectory) {
          if (item.childCount > 0) "${item.childCount} items" else "Folder"
        } else {
          FileUtils.formatFileSize(item.size)
        }

        Text(
          text = sizeText,
          style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
          color = MaterialTheme.colorScheme.primary,
          fontWeight = FontWeight.Medium
        )

        Text(
          text = " • ",
          style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
          color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )

        // Date
        Text(
          text = FileUtils.formatDate(item.lastModified),
          style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Unix Permissions flag like ZArchiver
        if (!item.isArchiveVirtualEntry) {
          Text(
            text = " • ",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
          )
          Text(
            text = item.permissions,
            style = MaterialTheme.typography.bodySmall.copy(
              fontSize = 10.sp,
              fontFamily = FontFamily.Monospace
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
          )
        }
      }
    }

    // 3-dots action menu
    IconButton(
      onClick = onMoreClick,
      modifier = Modifier
        .size(36.dp)
        .testTag("more_${item.name}")
    ) {
      Icon(
        imageVector = Icons.Default.MoreVert,
        contentDescription = "Options",
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(18.dp)
      )
    }
  }
}
