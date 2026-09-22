package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.FileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileManagerScreen(
  viewModel: FileViewModel,
  modifier: Modifier = Modifier,
) {
  val currentPath by viewModel.currentPath.collectAsState()
  val inArchiveMode by viewModel.inArchiveMode.collectAsState()
  val activeArchiveFile by viewModel.activeArchiveFile.collectAsState()
  val archiveInternalPath by viewModel.archiveInternalPath.collectAsState()
  val rawFiles by viewModel.files.collectAsState()
  val isLoading by viewModel.isLoading.collectAsState()
  val selectedFiles by viewModel.selectedFiles.collectAsState()
  val isMultiSelectMode by viewModel.isMultiSelectMode.collectAsState()
  val clipboard by viewModel.clipboard.collectAsState()
  val sortOption by viewModel.sortOption.collectAsState()
  val viewMode by viewModel.viewMode.collectAsState()
  val searchQuery by viewModel.searchQuery.collectAsState()
  val categoryFilter by viewModel.categoryFilter.collectAsState()
  val statusMessage by viewModel.statusMessage.collectAsState()
  val storageBreakdown by viewModel.storageBreakdown.collectAsState()

  // Dialog states from VM
  val createFolderDialog by viewModel.createFolderDialog.collectAsState()
  val createFileDialog by viewModel.createFileDialog.collectAsState()
  val compressDialogTarget by viewModel.compressDialogTarget.collectAsState()
  val extractDialogTarget by viewModel.extractDialogTarget.collectAsState()
  val propertiesItem by viewModel.propertiesItem.collectAsState()
  val hashResult by viewModel.hashResult.collectAsState()
  val apkInfo by viewModel.apkInfo.collectAsState()
  val hexViewerTarget by viewModel.hexViewerTarget.collectAsState()
  val textEditorTarget by viewModel.textEditorTarget.collectAsState()
  val textEditorContent by viewModel.textEditorContent.collectAsState()
  val storageAnalyzerVisible by viewModel.storageAnalyzerVisible.collectAsState()
  val deleteConfirmItems by viewModel.deleteConfirmItems.collectAsState()
  val renameTarget by viewModel.renameTarget.collectAsState()
  val fileActionSheetItem by viewModel.fileActionSheetItem.collectAsState()
  val archiveTestResult by viewModel.archiveTestResult.collectAsState()
  val archiveProgress by viewModel.archiveProgress.collectAsState()
  val archiveProgressMessage by viewModel.archiveProgressMessage.collectAsState()

  // Local UI states
  var isSearchActive by remember { mutableStateOf(false) }
  var sortMenuExpanded by remember { mutableStateOf(false) }
  var isSpeedDialOpen by remember { mutableStateOf(false) }
  var showBookmarksSheet by remember { mutableStateOf(false) }
  var renameInputText by remember { mutableStateOf("") }

  LaunchedEffect(renameTarget) {
    if (renameTarget != null) {
      renameInputText = renameTarget!!.name
    }
  }

  val snackbarHostState = remember { SnackbarHostState() }
  LaunchedEffect(statusMessage) {
    statusMessage?.let {
      snackbarHostState.showSnackbar(it)
      viewModel.clearStatusMessage()
    }
  }

  // Filtered & Sorted items
  val displayedFiles = remember(rawFiles, searchQuery, categoryFilter, sortOption) {
    var list = rawFiles

    // Category Filter
    if (categoryFilter != null) {
      list = list.filter { it.fileType == categoryFilter || it.isDirectory }
    }

    // Search Query
    if (searchQuery.isNotBlank()) {
      list = list.filter { it.name.contains(searchQuery.trim(), ignoreCase = true) }
    }

    // Sort: Folders always first (like ZArchiver), then sorted by criterion
    val comparator = when (sortOption) {
      SortOption.NAME_ASC -> compareBy<FileItem> { !it.isDirectory }.thenBy { it.name.lowercase() }
      SortOption.NAME_DESC -> compareBy<FileItem> { !it.isDirectory }.thenByDescending { it.name.lowercase() }
      SortOption.DATE_DESC -> compareBy<FileItem> { !it.isDirectory }.thenByDescending { it.lastModified }
      SortOption.DATE_ASC -> compareBy<FileItem> { !it.isDirectory }.thenBy { it.lastModified }
      SortOption.SIZE_DESC -> compareBy<FileItem> { !it.isDirectory }.thenByDescending { it.size }
      SortOption.SIZE_ASC -> compareBy<FileItem> { !it.isDirectory }.thenBy { it.size }
      SortOption.TYPE -> compareBy<FileItem> { !it.isDirectory }.thenBy { it.fileType.name }.thenBy { it.name.lowercase() }
    }
    list.sortedWith(comparator)
  }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = SlateBackground,
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
      TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = SlateSurface,
          titleContentColor = MaterialTheme.colorScheme.onSurface,
          actionIconContentColor = MaterialTheme.colorScheme.onSurface
        ),
        title = {
          if (isSearchActive) {
            TextField(
              value = searchQuery,
              onValueChange = { viewModel.setSearchQuery(it) },
              placeholder = { Text("Search files...", fontSize = 14.sp) },
              singleLine = true,
              colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = EmeraldPrimary,
                unfocusedIndicatorColor = Color.Transparent
              ),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("search_field")
            )
          } else {
            Column {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  text = "R File",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  color = EmeraldPrimary
                )
                if (isMultiSelectMode) {
                  Text(
                    text = " (${selectedFiles.size} selected)",
                    style = MaterialTheme.typography.titleSmall,
                    color = CyanAccent
                  )
                }
              }
              Text(
                text = "${displayedFiles.size} items • ${sortOption.title.substringBefore(' ')}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        },
        actions = {
          // Search toggle
          IconButton(onClick = {
            isSearchActive = !isSearchActive
            if (!isSearchActive) viewModel.setSearchQuery("")
          }) {
            Icon(
              imageVector = if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
              contentDescription = "Search"
            )
          }

          // Multi-select toggle
          IconButton(onClick = {
            if (isMultiSelectMode) {
              viewModel.clearSelection()
            } else {
              viewModel.selectAll()
            }
          }) {
            Icon(
              imageVector = if (isMultiSelectMode) Icons.Default.Deselect else Icons.Default.Checklist,
              contentDescription = "Select Mode",
              tint = if (isMultiSelectMode) EmeraldPrimary else MaterialTheme.colorScheme.onSurface
            )
          }

          // View mode toggle
          IconButton(onClick = { viewModel.toggleViewMode() }) {
            Icon(
              imageVector = if (viewMode == ViewMode.LIST) Icons.Default.GridView else Icons.Default.ViewList,
              contentDescription = "View Mode"
            )
          }

          // Sort Menu
          Box {
            IconButton(onClick = { sortMenuExpanded = true }) {
              Icon(imageVector = Icons.Default.Sort, contentDescription = "Sort")
            }
            DropdownMenu(
              expanded = sortMenuExpanded,
              onDismissRequest = { sortMenuExpanded = false },
              modifier = Modifier.background(SlateSurface)
            ) {
              SortOption.values().forEach { opt ->
                DropdownMenuItem(
                  text = {
                    Text(
                      text = opt.title,
                      color = if (sortOption == opt) EmeraldPrimary else MaterialTheme.colorScheme.onSurface,
                      fontWeight = if (sortOption == opt) FontWeight.Bold else FontWeight.Normal
                    )
                  },
                  onClick = {
                    viewModel.setSortOption(opt)
                    sortMenuExpanded = false
                  }
                )
              }
            }
          }

          // Storage Analyzer button
          IconButton(onClick = { viewModel.showStorageAnalyzer() }) {
            Icon(
              imageVector = Icons.Default.PieChart,
              contentDescription = "Storage Analyzer",
              tint = CyanAccent
            )
          }
        }
      )
    },
    floatingActionButton = {
      if (isMultiSelectMode && selectedFiles.isNotEmpty()) {
        // Multi-select batch action bar
        ExtendedFloatingActionButton(
          onClick = {
            val selectedItems = rawFiles.filter { selectedFiles.contains(it.path) }
            viewModel.promptCompress(selectedItems)
          },
          containerColor = EmeraldPrimary,
          contentColor = Color(0xFF003820),
          icon = { Icon(Icons.Default.Archive, contentDescription = null) },
          text = { Text("Compress (${selectedFiles.size})", fontWeight = FontWeight.Bold) },
          modifier = Modifier.testTag("batch_compress_fab")
        )
      } else {
        // Speed Dial Expanding FAB
        Column(
          horizontalAlignment = Alignment.End,
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          AnimatedVisibility(
            visible = isSpeedDialOpen,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 }
          ) {
            Column(
              horizontalAlignment = Alignment.End,
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              // Create Archive
              SmallFloatingActionButton(
                onClick = {
                  isSpeedDialOpen = false
                  viewModel.promptCompress(displayedFiles.take(1))
                },
                containerColor = ArchiveBlue,
                contentColor = Color.White
              ) {
                Icon(Icons.Default.Archive, contentDescription = "New Archive")
              }

              // Create File
              SmallFloatingActionButton(
                onClick = {
                  isSpeedDialOpen = false
                  viewModel.openCreateFileDialog()
                },
                containerColor = CyanAccent,
                contentColor = Color(0xFF003648)
              ) {
                Icon(Icons.Default.NoteAdd, contentDescription = "New File")
              }

              // Create Folder
              SmallFloatingActionButton(
                onClick = {
                  isSpeedDialOpen = false
                  viewModel.openCreateFolderDialog()
                },
                containerColor = FolderAmber,
                contentColor = Color(0xFF452B00)
              ) {
                Icon(Icons.Default.CreateNewFolder, contentDescription = "New Folder")
              }
            }
          }

          // Main Plus FAB
          FloatingActionButton(
            onClick = { isSpeedDialOpen = !isSpeedDialOpen },
            containerColor = EmeraldPrimary,
            contentColor = Color(0xFF003820),
            shape = CircleShape,
            modifier = Modifier.testTag("main_fab")
          ) {
            Icon(
              imageVector = if (isSpeedDialOpen) Icons.Default.Close else Icons.Default.Add,
              contentDescription = "Add"
            )
          }
        }
      }
    }
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      // Breadcrumbs Path Bar
      BreadcrumbBar(
        currentPath = currentPath,
        inArchiveMode = inArchiveMode,
        archiveInternalPath = archiveInternalPath,
        activeArchiveName = activeArchiveFile?.name,
        onNavigateTo = { path ->
          if (inArchiveMode) {
            // inside archive navigation
            viewModel.navigateTo(path)
          } else {
            viewModel.navigateTo(path)
          }
        },
        onNavigateUp = { viewModel.navigateUp() },
        onOpenStorageBookmarks = { showBookmarksSheet = true }
      )

      // In-Archive Banner if browsing inside an archive
      if (inArchiveMode && activeArchiveFile != null) {
        ArchiveBanner(
          archiveName = activeArchiveFile!!.name,
          onExtractAll = {
            viewModel.promptExtract(
              FileItem(
                name = activeArchiveFile!!.name,
                path = activeArchiveFile!!.absolutePath,
                size = activeArchiveFile!!.length(),
                isArchive = true,
                isDirectory = false
              )
            )
          },
          onExitArchive = { viewModel.exitArchiveMode() }
        )
      }

      // Quick Category Filter Bar
      QuickFilterBar(
        selectedFilter = categoryFilter,
        onSelectFilter = { viewModel.setCategoryFilter(it) }
      )

      // Multi-select control strip if active
      if (isMultiSelectMode) {
        Surface(
          color = SlateSurfaceVariant,
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "${selectedFiles.size} of ${displayedFiles.size} selected",
              style = MaterialTheme.typography.bodySmall,
              color = EmeraldPrimary,
              fontWeight = FontWeight.Bold
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              TextButton(onClick = { viewModel.selectAll() }) {
                Text("All", fontSize = 12.sp)
              }
              TextButton(onClick = { viewModel.invertSelection() }) {
                Text("Invert", fontSize = 12.sp)
              }
              TextButton(onClick = { viewModel.copySelected() }) {
                Text("Copy", fontSize = 12.sp)
              }
              TextButton(onClick = { viewModel.cutSelected() }) {
                Text("Cut", fontSize = 12.sp)
              }
              TextButton(onClick = {
                val toDelete = rawFiles.filter { selectedFiles.contains(it.path) }
                viewModel.promptDelete(toDelete)
              }) {
                Text("Delete", color = VideoCoral, fontSize = 12.sp)
              }
            }
          }
        }
      }

      // Content Area: File List / Grid or Loading
      Box(modifier = Modifier.weight(1f)) {
        if (isLoading) {
          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = EmeraldPrimary)
          }
        } else if (displayedFiles.isEmpty()) {
          // Empty State
          Column(
            modifier = Modifier
              .fillMaxSize()
              .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
          ) {
            Icon(
              imageVector = Icons.Default.FolderOpen,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
              modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
              text = if (searchQuery.isNotBlank()) "No files match \"$searchQuery\"" else "Folder is empty",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "Use the + button to create a file, folder, or archive",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
          }
        } else {
          // Display items
          if (viewMode == ViewMode.LIST) {
            LazyColumn(
              modifier = Modifier
                .fillMaxSize()
                .testTag("file_list")
            ) {
              items(
                items = displayedFiles,
                key = { it.path }
              ) { item ->
                FileListItem(
                  item = item,
                  isSelected = selectedFiles.contains(item.path),
                  isMultiSelectMode = isMultiSelectMode,
                  onItemClick = {
                    handleItemClick(item, viewModel)
                  },
                  onItemLongClick = {
                    viewModel.openActionSheet(item)
                  },
                  onSelectToggle = {
                    viewModel.toggleSelectFile(item.path)
                  },
                  onMoreClick = {
                    viewModel.openActionSheet(item)
                  }
                )
                HorizontalDivider(
                  modifier = Modifier.padding(start = 68.dp),
                  color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
                )
              }
            }
          } else {
            LazyVerticalGrid(
              columns = GridCells.Adaptive(minSize = 100.dp),
              modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
                .testTag("file_grid"),
              contentPadding = PaddingValues(bottom = 60.dp)
            ) {
              items(
                items = displayedFiles,
                key = { it.path }
              ) { item ->
                FileGridItem(
                  item = item,
                  isSelected = selectedFiles.contains(item.path),
                  isMultiSelectMode = isMultiSelectMode,
                  onItemClick = {
                    handleItemClick(item, viewModel)
                  },
                  onItemLongClick = {
                    viewModel.openActionSheet(item)
                  },
                  onSelectToggle = {
                    viewModel.toggleSelectFile(item.path)
                  }
                )
              }
            }
          }
        }

        // Floating Clipboard bar if items are ready to paste
        clipboard?.let { clip ->
          Box(
            modifier = Modifier
              .align(Alignment.BottomCenter)
              .navigationBarsPadding()
          ) {
            ClipboardBar(
              clipboardState = clip,
              onPaste = { viewModel.pasteClipboard() },
              onClear = { viewModel.clearClipboard() }
            )
          }
        }
      }
    }
  }

  // ==========================================
  // DIALOGS & OVERLAYS
  // ==========================================

  if (createFolderDialog) {
    CreateFolderDialog(
      onDismiss = { viewModel.closeCreateFolderDialog() },
      onConfirm = { name -> viewModel.createFolder(name) }
    )
  }

  if (createFileDialog) {
    CreateFileDialog(
      onDismiss = { viewModel.closeCreateFileDialog() },
      onConfirm = { name, content -> viewModel.createFile(name, content) }
    )
  }

  compressDialogTarget?.let { targets ->
    CompressDialog(
      itemsToCompress = targets,
      onDismiss = { viewModel.dismissCompressDialog() },
      onConfirm = { name, type, level ->
        viewModel.executeCompress(name, type, level)
      }
    )
  }

  extractDialogTarget?.let { archiveItem ->
    ExtractDialog(
      archiveItem = archiveItem,
      onDismiss = { viewModel.dismissExtractDialog() },
      onConfirm = { extractIntoFolder ->
        viewModel.executeExtract(archiveItem, extractIntoFolder)
      }
    )
  }

  propertiesItem?.let { item ->
    PropertiesDialog(
      item = item,
      onDismiss = { viewModel.dismissProperties() },
      onCalculateHash = {
        viewModel.dismissProperties()
        viewModel.calculateHash(item)
      },
      onInspectHex = {
        viewModel.dismissProperties()
        viewModel.openHexViewer(item)
      }
    )
  }

  hashResult?.let { hashes ->
    HashResultDialog(
      hashResult = hashes,
      onDismiss = { viewModel.dismissHashDialog() }
    )
  }

  apkInfo?.let { apk ->
    ApkInspectorDialog(
      apkInfo = apk,
      onDismiss = { viewModel.dismissApkInfo() }
    )
  }

  hexViewerTarget?.let { hexItem ->
    HexViewerDialog(
      item = hexItem,
      onDismiss = { viewModel.dismissHexViewer() }
    )
  }

  textEditorTarget?.let { target ->
    TextEditorDialog(
      item = target,
      initialContent = textEditorContent,
      onDismiss = { viewModel.dismissTextEditor() },
      onSave = { newContent -> viewModel.saveTextEditor(newContent) }
    )
  }

  if (storageAnalyzerVisible) {
    StorageAnalyzerDialog(
      breakdown = storageBreakdown,
      onDismiss = { viewModel.dismissStorageAnalyzer() }
    )
  }

  deleteConfirmItems?.let { itemsToDelete ->
    AlertDialog(
      onDismissRequest = { viewModel.dismissDelete() },
      icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = VideoCoral) },
      title = { Text("Delete Confirmation") },
      text = {
        Text("Are you sure you want to permanently delete ${itemsToDelete.size} item(s)?\nThis cannot be undone.")
      },
      confirmButton = {
        Button(
          onClick = { viewModel.confirmDelete() },
          colors = ButtonDefaults.buttonColors(containerColor = VideoCoral, contentColor = Color.White),
          modifier = Modifier.testTag("confirm_delete_button")
        ) {
          Text("Delete", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { viewModel.dismissDelete() }) { Text("Cancel") }
      }
    )
  }

  renameTarget?.let { item ->
    AlertDialog(
      onDismissRequest = { viewModel.dismissRename() },
      icon = { Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null, tint = EmeraldPrimary) },
      title = { Text("Rename") },
      text = {
        OutlinedTextField(
          value = renameInputText,
          onValueChange = { renameInputText = it },
          label = { Text("New Name") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("rename_input")
        )
      },
      confirmButton = {
        Button(
          onClick = {
            if (renameInputText.isNotBlank()) {
              viewModel.confirmRename(renameInputText.trim())
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color(0xFF003820)),
          modifier = Modifier.testTag("confirm_rename_button")
        ) {
          Text("Rename", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { viewModel.dismissRename() }) { Text("Cancel") }
      }
    )
  }

  fileActionSheetItem?.let { actionItem ->
    FileActionSheet(
      item = actionItem,
      onDismiss = { viewModel.dismissActionSheet() },
      onOpen = {
        if (actionItem.isArchive) {
          viewModel.openArchive(actionItem)
        } else if (actionItem.isDirectory) {
          viewModel.navigateTo(actionItem.path)
        }
      },
      onExtract = { viewModel.promptExtract(actionItem) },
      onTestArchive = { viewModel.testArchive(actionItem) },
      onCompress = { viewModel.promptCompress(listOf(actionItem)) },
      onCut = {
        viewModel.toggleSelectFile(actionItem.path)
        viewModel.cutSelected()
      },
      onCopy = {
        viewModel.toggleSelectFile(actionItem.path)
        viewModel.copySelected()
      },
      onRename = { viewModel.promptRename(actionItem) },
      onDelete = { viewModel.promptDelete(listOf(actionItem)) },
      onInspectHex = { viewModel.openHexViewer(actionItem) },
      onInspectApk = { viewModel.inspectApk(actionItem) },
      onCalculateHash = { viewModel.calculateHash(actionItem) },
      onEditText = { viewModel.openTextEditor(actionItem) },
      onProperties = { viewModel.showProperties(actionItem) }
    )
  }

  archiveTestResult?.let { testResult ->
    AlertDialog(
      onDismissRequest = { viewModel.dismissArchiveTestResult() },
      icon = { Icon(Icons.Default.Verified, contentDescription = null, tint = EmeraldPrimary) },
      title = { Text("Archive Integrity Test") },
      text = {
        Text(
          text = testResult,
          style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
        )
      },
      confirmButton = {
        Button(onClick = { viewModel.dismissArchiveTestResult() }) { Text("OK") }
      }
    )
  }

  // Compression / Extraction Progress Overlay
  archiveProgress?.let { progress ->
    Dialog(onDismissRequest = { /* Prevent dismiss while processing */ }) {
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = SlateSurface,
        tonalElevation = 8.dp,
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp)
      ) {
        Column(
          modifier = Modifier.padding(20.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          CircularProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            color = EmeraldPrimary,
            modifier = Modifier.size(56.dp)
          )
          Text(
            text = "${(progress * 100).toInt()}%",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = EmeraldPrimary
          )
          Text(
            text = archiveProgressMessage ?: "Processing archive...",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2
          )
        }
      }
    }
  }

  if (showBookmarksSheet) {
    StorageBookmarksSheet(
      onDismiss = { showBookmarksSheet = false },
      onSelectDeviceStorage = { viewModel.navigateToDeviceStorage() },
      onSelectSandbox = { viewModel.navigateToSandbox() },
      onSelectDownloads = { viewModel.navigateToDownload() },
      onSelectDocuments = { viewModel.navigateToDocuments() },
      onSelectArchives = { viewModel.navigateToArchives() }
    )
  }
}

private fun handleItemClick(item: FileItem, viewModel: FileViewModel) {
  if (item.isArchiveVirtualEntry) {
    if (item.isDirectory) {
      viewModel.navigateInsideArchiveFolder(item)
    } else {
      // In-archive file viewer
      viewModel.openActionSheet(item)
    }
  } else if (item.isArchive) {
    // ZArchiver feature: clicking an archive opens it directly to browse virtual contents!
    viewModel.openArchive(item)
  } else if (item.isDirectory) {
    viewModel.navigateTo(item.path)
  } else if (item.fileType == FileType.APK) {
    viewModel.inspectApk(item)
  } else if (item.fileType == FileType.CODE || item.fileType == FileType.DOCUMENT) {
    viewModel.openTextEditor(item)
  } else if (item.fileType == FileType.BINARY) {
    viewModel.openHexViewer(item)
  } else {
    viewModel.openActionSheet(item)
  }
}
