package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ClipboardOperation
import com.example.model.ClipboardState
import com.example.ui.theme.EmeraldPrimary

@Composable
fun ClipboardBar(
  clipboardState: ClipboardState,
  onPaste: () -> Unit,
  onClear: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val shape = RoundedCornerShape(16.dp)
  val isCut = clipboardState.operation == ClipboardOperation.CUT

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 8.dp)
      .clip(shape)
      .border(1.dp, EmeraldPrimary.copy(alpha = 0.5f), shape),
    color = MaterialTheme.colorScheme.surfaceVariant,
    tonalElevation = 6.dp
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(
        imageVector = if (isCut) Icons.Default.ContentCut else Icons.Default.ContentCopy,
        contentDescription = null,
        tint = EmeraldPrimary,
        modifier = Modifier.size(20.dp)
      )

      Spacer(modifier = Modifier.width(10.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "${clipboardState.items.size} item(s) selected",
          style = MaterialTheme.typography.bodyMedium.copy(
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp
          ),
          color = MaterialTheme.colorScheme.onSurface
        )
        Text(
          text = if (isCut) "Ready to move (cut)" else "Ready to duplicate (copy)",
          style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      // Paste Here button
      Button(
        onClick = onPaste,
        colors = ButtonDefaults.buttonColors(
          containerColor = EmeraldPrimary,
          contentColor = Color(0xFF003820)
        ),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        modifier = Modifier
          .height(36.dp)
          .testTag("paste_button")
      ) {
        Icon(
          imageVector = Icons.Default.ContentPaste,
          contentDescription = null,
          modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = "Paste Here", fontSize = 12.sp, fontWeight = FontWeight.Bold)
      }

      Spacer(modifier = Modifier.width(6.dp))

      IconButton(
        onClick = onClear,
        modifier = Modifier.size(36.dp)
      ) {
        Icon(
          imageVector = Icons.Default.Close,
          contentDescription = "Cancel",
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(18.dp)
        )
      }
    }
  }
}
