package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.*
import com.example.ui.theme.*
import com.example.utils.FileUtils
import com.example.utils.HexViewerHelper
import java.io.File

// ==========================================
// CREATE FOLDER DIALOG
// ==========================================
@Composable
fun CreateFolderDialog(
  onDismiss: () -> Unit,
  onConfirm: (String) -> Unit,
) {
  var name by remember { mutableStateOf("New Folder") }

  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Icon(Icons.Default.CreateNewFolder, contentDescription = null, tint = EmeraldPrimary)
    },
    title = { Text("Create New Folder") },
    text = {
      OutlinedTextField(
        value = name,
        onValueChange = { name = it },
        label = { Text("Folder Name") },
        singleLine = true,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("create_folder_input")
      )
    },
    confirmButton = {
      Button(
        onClick = { if (name.isNotBlank()) onConfirm(name.trim()) },
        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color(0xFF003820)),
        modifier = Modifier.testTag("confirm_create_folder")
      ) {
        Text("Create", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    }
  )
}

// ==========================================
// CREATE FILE DIALOG
// ==========================================
@Composable
fun CreateFileDialog(
  onDismiss: () -> Unit,
  onConfirm: (String, String) -> Unit,
) {
  var name by remember { mutableStateOf("untitled.txt") }
  var initialContent by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Icon(Icons.Default.NoteAdd, contentDescription = null, tint = EmeraldPrimary)
    },
    title = { Text("Create New File") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("File Name (with extension)") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("create_file_input")
        )
        OutlinedTextField(
          value = initialContent,
          onValueChange = { initialContent = it },
          label = { Text("Initial Content (Optional)") },
          maxLines = 3,
          modifier = Modifier.fillMaxWidth()
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { if (name.isNotBlank()) onConfirm(name.trim(), initialContent) },
        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color(0xFF003820)),
        modifier = Modifier.testTag("confirm_create_file")
      ) {
        Text("Create", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    }
  )
}

// ==========================================
// COMPRESS DIALOG (ZArchiver Style)
// ==========================================
@Composable
fun CompressDialog(
  itemsToCompress: List<FileItem>,
  onDismiss: () -> Unit,
  onConfirm: (String, ArchiveType, CompressionLevel) -> Unit,
) {
  val defaultName = if (itemsToCompress.size == 1) {
    itemsToCompress.first().name.substringBeforeLast('.')
  } else {
    "archive_${System.currentTimeMillis() / 1000}"
  }

  var archiveName by remember { mutableStateOf(defaultName) }
  var selectedType by remember { mutableStateOf(ArchiveType.ZIP) }
  var selectedLevel by remember { mutableStateOf(CompressionLevel.NORMAL) }

  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Icon(Icons.Default.Archive, contentDescription = null, tint = EmeraldPrimary)
    },
    title = { Text("Compress Archive") },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Text(
          text = "Compressing ${itemsToCompress.size} item(s)",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.primary
        )

        OutlinedTextField(
          value = archiveName,
          onValueChange = { archiveName = it },
          label = { Text("Archive Name") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("archive_name_input")
        )

        // Archive format selection
        Text("Archive Format:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          ArchiveType.values().forEach { type ->
            FilterChip(
              selected = selectedType == type,
              onClick = { selectedType = type },
              label = { Text(type.name, fontSize = 11.sp) },
              modifier = Modifier.weight(1f)
            )
          }
        }

        // Compression level
        Text("Compression Level:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          CompressionLevel.values().forEach { level ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .clickable { selectedLevel = level }
                .padding(vertical = 4.dp, horizontal = 6.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              RadioButton(
                selected = selectedLevel == level,
                onClick = { selectedLevel = level }
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(level.displayName, style = MaterialTheme.typography.bodySmall)
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (archiveName.isNotBlank()) {
            onConfirm(archiveName.trim(), selectedType, selectedLevel)
          }
        },
        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color(0xFF003820)),
        modifier = Modifier.testTag("confirm_compress")
      ) {
        Text("Compress", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    }
  )
}

// ==========================================
// EXTRACT DIALOG
// ==========================================
@Composable
fun ExtractDialog(
  archiveItem: FileItem,
  onDismiss: () -> Unit,
  onConfirm: (Boolean) -> Unit, // true = extract to folder, false = extract here
) {
  var extractToFolder by remember { mutableStateOf(true) }

  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Icon(Icons.Default.Unarchive, contentDescription = null, tint = EmeraldPrimary)
    },
    title = { Text("Extract Archive") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = archiveItem.name,
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = "Size: ${FileUtils.formatFileSize(archiveItem.size)}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(4.dp))

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable { extractToFolder = true }
            .padding(4.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          RadioButton(selected = extractToFolder, onClick = { extractToFolder = true })
          Spacer(modifier = Modifier.width(6.dp))
          Text("Extract to ${archiveItem.name.substringBeforeLast('.')}/", style = MaterialTheme.typography.bodySmall)
        }

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable { extractToFolder = false }
            .padding(4.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          RadioButton(selected = !extractToFolder, onClick = { extractToFolder = false })
          Spacer(modifier = Modifier.width(6.dp))
          Text("Extract here (Current directory)", style = MaterialTheme.typography.bodySmall)
        }
      }
    },
    confirmButton = {
      Button(
        onClick = { onConfirm(extractToFolder) },
        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color(0xFF003820)),
        modifier = Modifier.testTag("confirm_extract")
      ) {
        Text("Extract", fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    }
  )
}

// ==========================================
// PROPERTIES DIALOG
// ==========================================
@Composable
fun PropertiesDialog(
  item: FileItem,
  onDismiss: () -> Unit,
  onCalculateHash: () -> Unit,
  onInspectHex: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Icon(Icons.Default.Info, contentDescription = null, tint = EmeraldPrimary)
    },
    title = { Text("Properties") },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        PropertyRow("Name", item.name)
        PropertyRow("Type", item.fileType.name)
        PropertyRow("Path", item.path)
        if (!item.isDirectory) {
          PropertyRow("Size", "${FileUtils.formatFileSize(item.size)} (${item.size} bytes)")
        } else {
          PropertyRow("Items", "${item.childCount} entries")
        }
        PropertyRow("Modified", FileUtils.formatDate(item.lastModified))
        PropertyRow("Permissions", item.permissions)

        if (!item.isDirectory) {
          Spacer(modifier = Modifier.height(8.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            FilledTonalButton(
              onClick = onCalculateHash,
              modifier = Modifier.weight(1f)
            ) {
              Icon(Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Checksum", fontSize = 11.sp)
            }

            FilledTonalButton(
              onClick = onInspectHex,
              modifier = Modifier.weight(1f)
            ) {
              Icon(Icons.Default.Memory, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Hex View", fontSize = 11.sp)
            }
          }
        }
      }
    },
    confirmButton = {
      Button(onClick = onDismiss) { Text("Close") }
    }
  )
}

@Composable
private fun PropertyRow(label: String, value: String) {
  Column(modifier = Modifier.fillMaxWidth()) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.primary
    )
    Text(
      text = value,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurface,
      fontFamily = if (label == "Permissions" || label == "Path") FontFamily.Monospace else FontFamily.Default
    )
    HorizontalDivider(modifier = Modifier.padding(top = 4.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
  }
}

// ==========================================
// HASH / CHECKSUM RESULT DIALOG
// ==========================================
@Composable
fun HashResultDialog(
  hashResult: HashResult,
  onDismiss: () -> Unit,
) {
  val context = LocalContext.current

  fun copyToClipboard(text: String, label: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText(label, text))
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Icon(Icons.Default.Fingerprint, contentDescription = null, tint = EmeraldPrimary)
    },
    title = { Text("Checksum / Hashes") },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Text(hashResult.fileName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)

        HashItemRow("MD5", hashResult.md5) { copyToClipboard(hashResult.md5, "MD5") }
        HashItemRow("SHA-1", hashResult.sha1) { copyToClipboard(hashResult.sha1, "SHA-1") }
        HashItemRow("SHA-256", hashResult.sha256) { copyToClipboard(hashResult.sha256, "SHA-256") }
      }
    },
    confirmButton = {
      Button(onClick = onDismiss) { Text("Done") }
    }
  )
}

@Composable
private fun HashItemRow(algorithm: String, hash: String, onCopy: () -> Unit) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
  ) {
    Column(modifier = Modifier.padding(10.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(algorithm, style = MaterialTheme.typography.labelMedium, color = EmeraldPrimary, fontWeight = FontWeight.Bold)
        IconButton(onClick = onCopy, modifier = Modifier.size(28.dp)) {
          Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
        }
      }
      Text(
        text = hash,
        style = MaterialTheme.typography.bodySmall.copy(
          fontFamily = FontFamily.Monospace,
          fontSize = 11.sp
        ),
        color = MaterialTheme.colorScheme.onSurface
      )
    }
  }
}

// ==========================================
// APK INSPECTOR DIALOG
// ==========================================
@Composable
fun ApkInspectorDialog(
  apkInfo: ApkInfo,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Icon(Icons.Default.Android, contentDescription = null, tint = ApkGreen)
    },
    title = { Text("APK Package Inspector") },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Text(
          text = apkInfo.appName,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold
        )

        PropertyRow("Package ID", apkInfo.packageName)
        PropertyRow("Version", "${apkInfo.versionName} (Code: ${apkInfo.versionCode})")
        PropertyRow("SDK Target", "Min SDK: ${apkInfo.minSdk} • Target SDK: ${apkInfo.targetSdk}")
        PropertyRow("Package Size", FileUtils.formatFileSize(apkInfo.fileSize))

        Text(
          text = "Requested Permissions (${apkInfo.permissions.size}):",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
          color = EmeraldPrimary,
          modifier = Modifier.padding(top = 4.dp)
        )

        if (apkInfo.permissions.isEmpty()) {
          Text("No special permissions declared", style = MaterialTheme.typography.bodySmall)
        } else {
          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            apkInfo.permissions.take(20).forEach { perm ->
              Text(
                text = "• $perm",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            if (apkInfo.permissions.size > 20) {
              Text("...and ${apkInfo.permissions.size - 20} more", style = MaterialTheme.typography.labelSmall)
            }
          }
        }
      }
    },
    confirmButton = {
      Button(onClick = onDismiss) { Text("Close") }
    }
  )
}

// ==========================================
// HEX VIEWER DIALOG (Power-User Feature)
// ==========================================
@Composable
fun HexViewerDialog(
  item: FileItem,
  onDismiss: () -> Unit,
) {
  val hexData = remember(item.path) {
    HexViewerHelper.loadHexDump(File(item.path), maxBytes = 4096)
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      modifier = Modifier
        .fillMaxSize()
        .padding(16.dp),
      shape = RoundedCornerShape(16.dp),
      color = SlateBackground,
      tonalElevation = 8.dp
    ) {
      Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text("Hex Inspector", style = MaterialTheme.typography.titleMedium, color = EmeraldPrimary, fontWeight = FontWeight.Bold)
            Text(item.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
          IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface)
          }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = SlateOutline)

        // Hex Table Column Header
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .background(SlateSurfaceVariant)
            .padding(horizontal = 6.dp, vertical = 4.dp)
        ) {
          Text("OFFSET", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace), color = EmeraldPrimary, modifier = Modifier.width(70.dp))
          Text("HEX DUMP (16 BYTES)", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace), color = CyanAccent, modifier = Modifier.weight(1f))
          Text("ASCII", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace), color = FolderAmber, modifier = Modifier.width(80.dp))
        }

        // Rows
        LazyColumn(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .padding(top = 4.dp)
        ) {
          items(hexData.first) { row ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 1.dp)
            ) {
              Text(
                text = row.offset,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp),
                color = EmeraldPrimary,
                modifier = Modifier.width(70.dp)
              )
              Text(
                text = row.hexPart,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
              )
              Text(
                text = row.asciiPart,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp),
                color = FolderAmber,
                modifier = Modifier.width(80.dp)
              )
            }
          }
        }

        Text(
          text = "Showing first 4KB of ${FileUtils.formatFileSize(hexData.second)}",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(top = 8.dp)
        )
      }
    }
  }
}

// ==========================================
// IN-APP TEXT / CODE EDITOR
// ==========================================
@Composable
fun TextEditorDialog(
  item: FileItem,
  initialContent: String,
  onDismiss: () -> Unit,
  onSave: (String) -> Unit,
) {
  var text by remember { mutableStateOf(initialContent) }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      modifier = Modifier
        .fillMaxSize()
        .padding(12.dp),
      shape = RoundedCornerShape(16.dp),
      color = SlateBackground,
      tonalElevation = 8.dp
    ) {
      Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Toolbar
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = item.name,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = EmeraldPrimary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = "${text.lines().size} lines • ${text.length} chars",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
              onClick = { onSave(text) },
              colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color(0xFF003820)),
              modifier = Modifier.testTag("save_text_editor")
            ) {
              Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Save", fontWeight = FontWeight.Bold)
            }

            IconButton(onClick = onDismiss) {
              Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface)
            }
          }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = SlateOutline)

        // Editor
        OutlinedTextField(
          value = text,
          onValueChange = { text = it },
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .testTag("text_editor_input"),
          textStyle = MaterialTheme.typography.bodyMedium.copy(
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface
          ),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = EmeraldPrimary,
            unfocusedBorderColor = SlateOutline
          )
        )
      }
    }
  }
}

// ==========================================
// STORAGE ANALYZER DIALOG
// ==========================================
@Composable
fun StorageAnalyzerDialog(
  breakdown: StorageBreakdown,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Icon(Icons.Default.PieChart, contentDescription = null, tint = EmeraldPrimary)
    },
    title = { Text("Storage Space Analyzer") },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        // Bar
        val usedFraction = if (breakdown.totalBytes > 0) {
          breakdown.usedBytes.toFloat() / breakdown.totalBytes.toFloat()
        } else 0f

        Column {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text("Used: ${FileUtils.formatFileSize(breakdown.usedBytes)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            Text("Total: ${FileUtils.formatFileSize(breakdown.totalBytes)}", style = MaterialTheme.typography.bodySmall)
          }

          Spacer(modifier = Modifier.height(6.dp))

          LinearProgressIndicator(
            progress = { usedFraction.coerceIn(0f, 1f) },
            modifier = Modifier
              .fillMaxWidth()
              .height(10.dp)
              .clip(RoundedCornerShape(5.dp)),
            color = EmeraldPrimary,
            trackColor = SlateSurfaceVariant,
          )
        }

        Text("Category Breakdown:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = EmeraldPrimary)

        StorageCategoryItem("Archives (.zip, .tar, .7z)", breakdown.archiveBytes, ArchiveBlue)
        StorageCategoryItem("APKs & Apps", breakdown.apkBytes, ApkGreen)
        StorageCategoryItem("Images & Photos", breakdown.imageBytes, ImagePurple)
        StorageCategoryItem("Audio & Music", breakdown.audioBytes, AudioCyan)
        StorageCategoryItem("Video & Movies", breakdown.videoBytes, VideoCoral)
        StorageCategoryItem("Documents & Code", breakdown.docBytes, DocumentIndigo)
        StorageCategoryItem("Other Files", breakdown.otherBytes, BinaryGray)
      }
    },
    confirmButton = {
      Button(onClick = onDismiss) { Text("Close") }
    }
  )
}

@Composable
private fun StorageCategoryItem(name: String, bytes: Long, color: Color) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Box(
        modifier = Modifier
          .size(10.dp)
          .clip(CircleShape)
          .background(color)
      )
      Spacer(modifier = Modifier.width(8.dp))
      Text(name, style = MaterialTheme.typography.bodySmall)
    }
    Text(FileUtils.formatFileSize(bytes), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
  }
}

// ==========================================
// FILE ACTION SHEET (Bottom Sheet for single file)
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileActionSheet(
  item: FileItem,
  onDismiss: () -> Unit,
  onOpen: () -> Unit,
  onExtract: () -> Unit,
  onTestArchive: () -> Unit,
  onCompress: () -> Unit,
  onCut: () -> Unit,
  onCopy: () -> Unit,
  onRename: () -> Unit,
  onDelete: () -> Unit,
  onInspectHex: () -> Unit,
  onInspectApk: () -> Unit,
  onCalculateHash: () -> Unit,
  onEditText: () -> Unit,
  onProperties: () -> Unit,
) {
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    containerColor = SlateSurface
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 24.dp)
        .verticalScroll(rememberScrollState())
    ) {
      // Header
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        FileIcon(item = item, size = 48.dp, iconSize = 28.dp)
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = item.name,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Text(
            text = if (item.isDirectory) "${item.childCount} items" else FileUtils.formatFileSize(item.size),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = SlateOutline)

      // Actions
      if (item.isArchive) {
        ActionSheetItem(Icons.Default.FolderOpen, "View Inside Archive (No Extract)", EmeraldPrimary) {
          onDismiss()
          onOpen()
        }
        ActionSheetItem(Icons.Default.Unarchive, "Extract Archive...", EmeraldPrimary) {
          onDismiss()
          onExtract()
        }
        ActionSheetItem(Icons.Default.Verified, "Test Archive Integrity", CyanAccent) {
          onDismiss()
          onTestArchive()
        }
      }

      if (item.fileType == FileType.APK) {
        ActionSheetItem(Icons.Default.Android, "Inspect APK Details & Manifest", ApkGreen) {
          onDismiss()
          onInspectApk()
        }
      }

      if (item.fileType == FileType.CODE || item.fileType == FileType.DOCUMENT || (!item.isDirectory && !item.isArchive)) {
        ActionSheetItem(Icons.Default.EditNote, "Edit in Text/Code Editor", CodeTeal) {
          onDismiss()
          onEditText()
        }
      }

      if (!item.isDirectory) {
        ActionSheetItem(Icons.Default.Memory, "Inspect Hex & Binary", CyanAccent) {
          onDismiss()
          onInspectHex()
        }
        ActionSheetItem(Icons.Default.Fingerprint, "Calculate Checksum (MD5/SHA)", EmeraldPrimary) {
          onDismiss()
          onCalculateHash()
        }
      }

      ActionSheetItem(Icons.Default.Archive, "Compress into Archive...", ArchiveBlue) {
        onDismiss()
        onCompress()
      }

      ActionSheetItem(Icons.Default.ContentCut, "Cut (Move)", MaterialTheme.colorScheme.onSurface) {
        onDismiss()
        onCut()
      }

      ActionSheetItem(Icons.Default.ContentCopy, "Copy", MaterialTheme.colorScheme.onSurface) {
        onDismiss()
        onCopy()
      }

      ActionSheetItem(Icons.Default.DriveFileRenameOutline, "Rename", MaterialTheme.colorScheme.onSurface) {
        onDismiss()
        onRename()
      }

      ActionSheetItem(Icons.Default.Info, "Properties", MaterialTheme.colorScheme.onSurface) {
        onDismiss()
        onProperties()
      }

      ActionSheetItem(Icons.Default.Delete, "Delete", VideoCoral) {
        onDismiss()
        onDelete()
      }
    }
  }
}

@Composable
private fun ActionSheetItem(
  icon: ImageVector,
  title: String,
  tint: Color,
  onClick: () -> Unit,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(horizontal = 20.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(imageVector = icon, contentDescription = title, tint = tint, modifier = Modifier.size(22.dp))
    Spacer(modifier = Modifier.width(16.dp))
    Text(
      text = title,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurface
    )
  }
}

// ==========================================
// STORAGE BOOKMARKS SHEET
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageBookmarksSheet(
  onDismiss: () -> Unit,
  onSelectDeviceStorage: () -> Unit,
  onSelectSandbox: () -> Unit,
  onSelectDownloads: () -> Unit,
  onSelectDocuments: () -> Unit,
  onSelectArchives: () -> Unit,
) {
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    containerColor = SlateSurface
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 24.dp)
    ) {
      Text(
        text = "Storage Locations & Bookmarks",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = EmeraldPrimary,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
      )

      HorizontalDivider(color = SlateOutline)

      BookmarkRow(Icons.Default.PhoneAndroid, "Internal Shared Storage", "/storage/emulated/0", EmeraldPrimary) {
        onDismiss()
        onSelectDeviceStorage()
      }

      BookmarkRow(Icons.Default.FolderZip, "R File Sandbox & Sample Drive", "Interactive App Storage", FolderAmber) {
        onDismiss()
        onSelectSandbox()
      }

      BookmarkRow(Icons.Default.Download, "Download Folder", "Quick Access to Downloads", CyanAccent) {
        onDismiss()
        onSelectDownloads()
      }

      BookmarkRow(Icons.Default.Article, "Documents Folder", "Configs, text & documents", DocumentIndigo) {
        onDismiss()
        onSelectDocuments()
      }

      BookmarkRow(Icons.Default.Archive, "Archives Folder", "Compressed files (.zip, .tar)", ArchiveBlue) {
        onDismiss()
        onSelectArchives()
      }
    }
  }
}

@Composable
private fun BookmarkRow(
  icon: ImageVector,
  title: String,
  subtitle: String,
  tint: Color,
  onClick: () -> Unit,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(horizontal = 20.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Box(
      modifier = Modifier
        .size(40.dp)
        .clip(RoundedCornerShape(10.dp))
        .background(tint.copy(alpha = 0.15f)),
      contentAlignment = Alignment.Center
    ) {
      Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
    }

    Spacer(modifier = Modifier.width(14.dp))

    Column(modifier = Modifier.weight(1f)) {
      Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
      Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }

    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), modifier = Modifier.size(16.dp))
  }
}
