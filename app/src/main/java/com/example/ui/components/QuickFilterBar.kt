package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.FileType
import com.example.ui.theme.EmeraldPrimary

data class CategoryFilterItem(
  val type: FileType?,
  val label: String,
  val icon: ImageVector,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickFilterBar(
  selectedFilter: FileType?,
  onSelectFilter: (FileType?) -> Unit,
  modifier: Modifier = Modifier,
) {
  val filters = listOf(
    CategoryFilterItem(null, "All Files", Icons.Default.AllInclusive),
    CategoryFilterItem(FileType.ARCHIVE, "Archives", Icons.Default.Archive),
    CategoryFilterItem(FileType.APK, "APKs", Icons.Default.Android),
    CategoryFilterItem(FileType.DOCUMENT, "Documents", Icons.Default.Description),
    CategoryFilterItem(FileType.CODE, "Code & Scripts", Icons.Default.Code),
    CategoryFilterItem(FileType.IMAGE, "Images", Icons.Default.Image),
    CategoryFilterItem(FileType.AUDIO, "Audio", Icons.Default.Audiotrack),
    CategoryFilterItem(FileType.VIDEO, "Video", Icons.Default.Movie),
    CategoryFilterItem(FileType.BINARY, "Binary & ROM", Icons.Default.Memory),
  )

  val scrollState = rememberScrollState()

  Row(
    modifier = modifier
      .fillMaxWidth()
      .horizontalScroll(scrollState)
      .padding(horizontal = 12.dp, vertical = 4.dp),
    horizontalArrangement = Arrangement.spacedBy(6.dp)
  ) {
    for (f in filters) {
      val isSelected = selectedFilter == f.type
      FilterChip(
        selected = isSelected,
        onClick = { onSelectFilter(if (isSelected) null else f.type) },
        label = { Text(text = f.label, fontSize = 11.sp) },
        leadingIcon = {
          Icon(
            imageVector = f.icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
          )
        },
        colors = FilterChipDefaults.filterChipColors(
          selectedContainerColor = EmeraldPrimary.copy(alpha = 0.18f),
          selectedLabelColor = EmeraldPrimary,
          containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = FilterChipDefaults.filterChipBorder(
          enabled = true,
          selected = isSelected,
          borderColor = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
          borderWidth = 1.dp
        ),
        modifier = Modifier.height(30.dp)
      )
    }
  }
}
