package com.example.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.FileItem
import com.example.utils.FileUtils

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileGridItem(
  item: FileItem,
  isSelected: Boolean,
  isMultiSelectMode: Boolean,
  onItemClick: () -> Unit,
  onItemLongClick: () -> Unit,
  onSelectToggle: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val shape = RoundedCornerShape(12.dp)
  val bgColor = if (isSelected) {
    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
  } else {
    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
  }

  Box(
    modifier = modifier
      .padding(4.dp)
      .clip(shape)
      .background(bgColor)
      .border(
        width = if (isSelected) 1.5.dp else 1.dp,
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
        shape = shape
      )
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
      .padding(10.dp)
      .testTag("file_grid_${item.name}")
  ) {
    if (isMultiSelectMode) {
      Checkbox(
        checked = isSelected,
        onCheckedChange = { onSelectToggle() },
        modifier = Modifier
          .align(Alignment.TopEnd)
          .size(24.dp)
      )
    }

    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(top = if (isMultiSelectMode) 12.dp else 0.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      FileIcon(
        item = item,
        size = 52.dp,
        iconSize = 32.dp
      )

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = item.name,
        style = MaterialTheme.typography.bodySmall.copy(
          fontWeight = if (item.isDirectory) FontWeight.SemiBold else FontWeight.Normal,
          fontSize = 12.sp
        ),
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center
      )

      Spacer(modifier = Modifier.height(2.dp))

      val sizeText = if (item.isDirectory) {
        if (item.childCount > 0) "${item.childCount} items" else "Folder"
      } else {
        FileUtils.formatFileSize(item.size)
      }

      Text(
        text = sizeText,
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
        color = MaterialTheme.colorScheme.primary,
        maxLines = 1
      )
    }
  }
}
