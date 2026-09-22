package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmeraldPrimary

@Composable
fun ArchiveBanner(
  archiveName: String,
  onExtractAll: () -> Unit,
  onExitArchive: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val shape = RoundedCornerShape(10.dp)

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 12.dp, vertical = 6.dp)
      .clip(shape)
      .border(1.dp, EmeraldPrimary.copy(alpha = 0.5f), shape),
    color = MaterialTheme.colorScheme.surfaceVariant,
    tonalElevation = 4.dp
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(
        imageVector = Icons.Default.Archive,
        contentDescription = "Archive Mode",
        tint = EmeraldPrimary,
        modifier = Modifier.size(24.dp)
      )

      Spacer(modifier = Modifier.width(10.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "Browsing: $archiveName",
          style = MaterialTheme.typography.bodyMedium.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
          ),
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1
        )
        Text(
          text = "In-Archive Inspector • Virtual View",
          style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
          color = EmeraldPrimary
        )
      }

      // Extract All button
      FilledTonalButton(
        onClick = onExtractAll,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        modifier = Modifier.height(32.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
          containerColor = EmeraldPrimary.copy(alpha = 0.2f),
          contentColor = EmeraldPrimary
        )
      ) {
        Icon(
          imageVector = Icons.Default.Unarchive,
          contentDescription = null,
          modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = "Extract", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
      }

      Spacer(modifier = Modifier.width(6.dp))

      // Exit button
      IconButton(
        onClick = onExitArchive,
        modifier = Modifier.size(32.dp)
      ) {
        Icon(
          imageVector = Icons.Default.Close,
          contentDescription = "Exit Archive",
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(18.dp)
        )
      }
    }
  }
}
