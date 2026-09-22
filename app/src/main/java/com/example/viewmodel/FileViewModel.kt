package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.StorageRepository
import com.example.model.*
import com.example.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class FileViewModel(application: Application) : AndroidViewModel(application) {

  private val repository = StorageRepository(application)

  // Navigation & Browsing
  private val _currentPath = MutableStateFlow("")
  val currentPath: StateFlow<String> = _currentPath.asStateFlow()

  private val _inArchiveMode = MutableStateFlow(false)
  val inArchiveMode: StateFlow<Boolean> = _inArchiveMode.asStateFlow()

  private val _activeArchiveFile = MutableStateFlow<File?>(null)
  val activeArchiveFile: StateFlow<File?> = _activeArchiveFile.asStateFlow()

  private val _archiveInternalPath = MutableStateFlow("")
  val archiveInternalPath: StateFlow<String> = _archiveInternalPath.asStateFlow()

  // File list & filtered view
  private val _files = MutableStateFlow<List<FileItem>>(emptyList())
  val files: StateFlow<List<FileItem>> = _files.asStateFlow()

  private val _isLoading = MutableStateFlow(false)
  val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

  // Selection & Clipboard
  private val _selectedFiles = MutableStateFlow<Set<String>>(emptySet())
  val selectedFiles: StateFlow<Set<String>> = _selectedFiles.asStateFlow()

  private val _isMultiSelectMode = MutableStateFlow(false)
  val isMultiSelectMode: StateFlow<Boolean> = _isMultiSelectMode.asStateFlow()

  private val _clipboard = MutableStateFlow<ClipboardState?>(null)
  val clipboard: StateFlow<ClipboardState?> = _clipboard.asStateFlow()

  // View & Sort Settings
  private val _sortOption = MutableStateFlow(SortOption.NAME_ASC)
  val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

  private val _viewMode = MutableStateFlow(ViewMode.LIST)
  val viewMode: StateFlow<ViewMode> = _viewMode.asStateFlow()

  private val _searchQuery = MutableStateFlow("")
  val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

  private val _categoryFilter = MutableStateFlow<FileType?>(null)
  val categoryFilter: StateFlow<FileType?> = _categoryFilter.asStateFlow()

  // Feedback & Storage Stats
  private val _statusMessage = MutableStateFlow<String?>(null)
  val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

  private val _storageBreakdown = MutableStateFlow(StorageBreakdown())
  val storageBreakdown: StateFlow<StorageBreakdown> = _storageBreakdown.asStateFlow()

  // Progress Indicators
  private val _archiveProgress = MutableStateFlow<Float?>(null)
  val archiveProgress: StateFlow<Float?> = _archiveProgress.asStateFlow()

  private val _archiveProgressMessage = MutableStateFlow<String?>(null)
  val archiveProgressMessage: StateFlow<String?> = _archiveProgressMessage.asStateFlow()

  // Dialog & Sheet States
  private val _createFolderDialog = MutableStateFlow(false)
  val createFolderDialog: StateFlow<Boolean> = _createFolderDialog.asStateFlow()

  private val _createFileDialog = MutableStateFlow(false)
  val createFileDialog: StateFlow<Boolean> = _createFileDialog.asStateFlow()

  private val _compressDialogTarget = MutableStateFlow<List<FileItem>?>(null)
  val compressDialogTarget: StateFlow<List<FileItem>?> = _compressDialogTarget.asStateFlow()

  private val _extractDialogTarget = MutableStateFlow<FileItem?>(null)
  val extractDialogTarget: StateFlow<FileItem?> = _extractDialogTarget.asStateFlow()

  private val _propertiesItem = MutableStateFlow<FileItem?>(null)
  val propertiesItem: StateFlow<FileItem?> = _propertiesItem.asStateFlow()

  private val _hashResult = MutableStateFlow<HashResult?>(null)
  val hashResult: StateFlow<HashResult?> = _hashResult.asStateFlow()

  private val _apkInfo = MutableStateFlow<ApkInfo?>(null)
  val apkInfo: StateFlow<ApkInfo?> = _apkInfo.asStateFlow()

  private val _hexViewerTarget = MutableStateFlow<FileItem?>(null)
  val hexViewerTarget: StateFlow<FileItem?> = _hexViewerTarget.asStateFlow()

  private val _textEditorTarget = MutableStateFlow<FileItem?>(null)
  val textEditorTarget: StateFlow<FileItem?> = _textEditorTarget.asStateFlow()

  private val _textEditorContent = MutableStateFlow("")
  val textEditorContent: StateFlow<String> = _textEditorContent.asStateFlow()

  private val _storageAnalyzerVisible = MutableStateFlow(false)
  val storageAnalyzerVisible: StateFlow<Boolean> = _storageAnalyzerVisible.asStateFlow()

  private val _deleteConfirmItems = MutableStateFlow<List<FileItem>?>(null)
  val deleteConfirmItems: StateFlow<List<FileItem>?> = _deleteConfirmItems.asStateFlow()

  private val _renameTarget = MutableStateFlow<FileItem?>(null)
  val renameTarget: StateFlow<FileItem?> = _renameTarget.asStateFlow()

  private val _fileActionSheetItem = MutableStateFlow<FileItem?>(null)
  val fileActionSheetItem: StateFlow<FileItem?> = _fileActionSheetItem.asStateFlow()

  private val _archiveTestResult = MutableStateFlow<String?>(null)
  val archiveTestResult: StateFlow<String?> = _archiveTestResult.asStateFlow()

  init {
    viewModelScope.launch {
      repository.initializeSampleEnvironmentIfNeeded()
      // Default to app Sandbox Root which contains sample folders and files
      val initialPath = repository.appSandboxRoot.absolutePath
      _currentPath.value = initialPath
      refreshCurrentDirectory()
      loadStorageBreakdown()
    }
  }

  fun refreshCurrentDirectory() {
    viewModelScope.launch {
      _isLoading.value = true
      try {
        if (_inArchiveMode.value && _activeArchiveFile.value != null) {
          val archive = _activeArchiveFile.value!!
          val list = ArchiveManager.listArchiveContents(archive, _archiveInternalPath.value)
          _files.value = list
        } else {
          val path = _currentPath.value
          val rawList = repository.listFiles(path)
          _files.value = rawList
        }
      } catch (e: Exception) {
        _statusMessage.value = "Error reading directory: ${e.message}"
      } finally {
        _isLoading.value = false
      }
    }
  }

  fun navigateTo(path: String) {
    if (_inArchiveMode.value) {
      exitArchiveMode()
    }
    _currentPath.value = path
    clearSelection()
    refreshCurrentDirectory()
  }

  fun navigateUp(): Boolean {
    if (_inArchiveMode.value) {
      val internal = _archiveInternalPath.value.trimEnd('/')
      if (internal.isEmpty()) {
        exitArchiveMode()
        return true
      } else {
        val parentInternal = internal.substringBeforeLast('/', "")
        _archiveInternalPath.value = parentInternal
        refreshCurrentDirectory()
        return true
      }
    }

    val current = File(_currentPath.value)
    val parent = current.parentFile
    if (parent != null && parent.canRead()) {
      _currentPath.value = parent.absolutePath
      clearSelection()
      refreshCurrentDirectory()
      return true
    }
    return false
  }

  fun openArchive(fileItem: FileItem) {
    val file = File(fileItem.path)
    if (!file.exists()) return
    _inArchiveMode.value = true
    _activeArchiveFile.value = file
    _archiveInternalPath.value = ""
    clearSelection()
    refreshCurrentDirectory()
  }

  fun navigateInsideArchiveFolder(virtualFolder: FileItem) {
    _archiveInternalPath.value = virtualFolder.path
    clearSelection()
    refreshCurrentDirectory()
  }

  fun exitArchiveMode() {
    _inArchiveMode.value = false
    _activeArchiveFile.value = null
    _archiveInternalPath.value = ""
    clearSelection()
    refreshCurrentDirectory()
  }

  fun toggleViewMode() {
    _viewMode.value = if (_viewMode.value == ViewMode.LIST) ViewMode.GRID else ViewMode.LIST
  }

  fun setSortOption(option: SortOption) {
    _sortOption.value = option
  }

  fun setSearchQuery(query: String) {
    _searchQuery.value = query
  }

  fun setCategoryFilter(filter: FileType?) {
    _categoryFilter.value = filter
  }

  fun toggleSelectFile(path: String) {
    val current = _selectedFiles.value.toMutableSet()
    if (current.contains(path)) {
      current.remove(path)
    } else {
      current.add(path)
    }
    _selectedFiles.value = current
    if (current.isEmpty()) {
      _isMultiSelectMode.value = false
    } else {
      _isMultiSelectMode.value = true
    }
  }

  fun selectAll() {
    _selectedFiles.value = _files.value.map { it.path }.toSet()
    _isMultiSelectMode.value = true
  }

  fun clearSelection() {
    _selectedFiles.value = emptySet()
    _isMultiSelectMode.value = false
  }

  fun invertSelection() {
    val all = _files.value.map { it.path }.toSet()
    _selectedFiles.value = all.minus(_selectedFiles.value)
    _isMultiSelectMode.value = _selectedFiles.value.isNotEmpty()
  }

  // Clipboard operations
  fun copySelected() {
    val items = _files.value.filter { _selectedFiles.value.contains(it.path) }
    if (items.isNotEmpty()) {
      _clipboard.value = ClipboardState(items, ClipboardOperation.COPY)
      _statusMessage.value = "${items.size} item(s) copied to clipboard"
      clearSelection()
    }
  }

  fun cutSelected() {
    val items = _files.value.filter { _selectedFiles.value.contains(it.path) }
    if (items.isNotEmpty()) {
      _clipboard.value = ClipboardState(items, ClipboardOperation.CUT)
      _statusMessage.value = "${items.size} item(s) cut to clipboard"
      clearSelection()
    }
  }

  fun clearClipboard() {
    _clipboard.value = null
    _statusMessage.value = "Clipboard cleared"
  }

  fun pasteClipboard() {
    val clip = _clipboard.value ?: return
    viewModelScope.launch {
      _isLoading.value = true
      val count = repository.pasteItems(
        items = clip.items,
        destinationDir = _currentPath.value,
        isCut = clip.operation == ClipboardOperation.CUT
      )
      if (clip.operation == ClipboardOperation.CUT) {
        _clipboard.value = null
      }
      _statusMessage.value = "Pasted $count item(s)"
      refreshCurrentDirectory()
      loadStorageBreakdown()
    }
  }

  // Create & Edit Actions
  fun openCreateFolderDialog() { _createFolderDialog.value = true }
  fun closeCreateFolderDialog() { _createFolderDialog.value = false }

  fun createFolder(name: String) {
    viewModelScope.launch {
      if (repository.createDirectory(_currentPath.value, name)) {
        _statusMessage.value = "Folder created: $name"
        refreshCurrentDirectory()
      } else {
        _statusMessage.value = "Failed to create folder"
      }
      closeCreateFolderDialog()
    }
  }

  fun openCreateFileDialog() { _createFileDialog.value = true }
  fun closeCreateFileDialog() { _createFileDialog.value = false }

  fun createFile(name: String, content: String = "") {
    viewModelScope.launch {
      if (repository.createFile(_currentPath.value, name, content)) {
        _statusMessage.value = "File created: $name"
        refreshCurrentDirectory()
      } else {
        _statusMessage.value = "Failed to create file"
      }
      closeCreateFileDialog()
    }
  }

  fun promptDelete(items: List<FileItem>) {
    _deleteConfirmItems.value = items
  }

  fun confirmDelete() {
    val targets = _deleteConfirmItems.value ?: return
    viewModelScope.launch {
      _isLoading.value = true
      val count = repository.deleteItems(targets)
      _statusMessage.value = "Deleted $count item(s)"
      _deleteConfirmItems.value = null
      clearSelection()
      refreshCurrentDirectory()
      loadStorageBreakdown()
    }
  }

  fun dismissDelete() {
    _deleteConfirmItems.value = null
  }

  fun promptRename(item: FileItem) {
    _renameTarget.value = item
  }

  fun confirmRename(newName: String) {
    val target = _renameTarget.value ?: return
    viewModelScope.launch {
      if (repository.renameItem(target.path, newName)) {
        _statusMessage.value = "Renamed to $newName"
        refreshCurrentDirectory()
      } else {
        _statusMessage.value = "Failed to rename"
      }
      _renameTarget.value = null
    }
  }

  fun dismissRename() {
    _renameTarget.value = null
  }

  // Compression & Extraction
  fun promptCompress(items: List<FileItem>) {
    _compressDialogTarget.value = items
  }

  fun dismissCompressDialog() {
    _compressDialogTarget.value = null
  }

  fun executeCompress(
    archiveName: String,
    archiveType: ArchiveType,
    level: CompressionLevel,
  ) {
    val items = _compressDialogTarget.value ?: return
    _compressDialogTarget.value = null
    viewModelScope.launch(Dispatchers.IO) {
      _archiveProgress.value = 0.01f
      _archiveProgressMessage.value = "Compressing to ${archiveName}${archiveType.extension}..."

      val filesToCompress = items.map { File(it.path) }
      val fullTargetName = if (archiveName.endsWith(archiveType.extension)) archiveName else "$archiveName${archiveType.extension}"
      val targetFile = File(_currentPath.value, fullTargetName)

      val success = when (archiveType) {
        ArchiveType.ZIP -> ArchiveManager.createZipArchive(
          filesToCompress = filesToCompress,
          destinationZip = targetFile,
          compressionLevel = level.level,
          onProgress = { p, name ->
            _archiveProgress.value = p
            _archiveProgressMessage.value = "Packing: $name"
          }
        )
        ArchiveType.TAR -> ArchiveManager.createTarArchive(
          filesToCompress = filesToCompress,
          destinationTar = targetFile,
          onProgress = { p, name ->
            _archiveProgress.value = p
            _archiveProgressMessage.value = "Packing: $name"
          }
        )
        ArchiveType.GZ -> ArchiveManager.createZipArchive(
          filesToCompress = filesToCompress,
          destinationZip = targetFile,
          compressionLevel = level.level,
          onProgress = { p, name ->
            _archiveProgress.value = p
            _archiveProgressMessage.value = "Compressing: $name"
          }
        )
      }

      _archiveProgress.value = null
      _archiveProgressMessage.value = null
      _statusMessage.value = if (success) "Created archive: $fullTargetName" else "Compression failed"
      clearSelection()
      refreshCurrentDirectory()
      loadStorageBreakdown()
    }
  }

  fun promptExtract(archiveItem: FileItem) {
    _extractDialogTarget.value = archiveItem
  }

  fun dismissExtractDialog() {
    _extractDialogTarget.value = null
  }

  fun executeExtract(targetItem: FileItem, extractIntoFolder: Boolean) {
    _extractDialogTarget.value = null
    viewModelScope.launch(Dispatchers.IO) {
      val archiveFile = File(targetItem.path)
      val destFolder = if (extractIntoFolder) {
        val folderName = archiveFile.nameWithoutExtension
        File(archiveFile.parentFile ?: File(_currentPath.value), folderName)
      } else {
        archiveFile.parentFile ?: File(_currentPath.value)
      }

      _archiveProgress.value = 0.01f
      _archiveProgressMessage.value = "Extracting ${archiveFile.name}..."

      val success = ArchiveManager.extractZip(
        archiveFile = archiveFile,
        outputDir = destFolder,
        onProgress = { p, name ->
          _archiveProgress.value = p
          _archiveProgressMessage.value = "Unpacking: $name"
        }
      )

      _archiveProgress.value = null
      _archiveProgressMessage.value = null
      _statusMessage.value = if (success) "Extracted to ${destFolder.name}/" else "Extraction failed"
      refreshCurrentDirectory()
      loadStorageBreakdown()
    }
  }

  fun testArchive(archiveItem: FileItem) {
    viewModelScope.launch(Dispatchers.IO) {
      _isLoading.value = true
      val result = ArchiveManager.testArchive(File(archiveItem.path))
      _isLoading.value = false
      _archiveTestResult.value = result.second
    }
  }

  fun dismissArchiveTestResult() {
    _archiveTestResult.value = null
  }

  // Advanced Tools: Hash, Hex Viewer, APK Inspector, Text Editor
  fun showProperties(item: FileItem) {
    _propertiesItem.value = item
  }

  fun dismissProperties() {
    _propertiesItem.value = null
  }

  fun calculateHash(item: FileItem) {
    viewModelScope.launch(Dispatchers.IO) {
      _isLoading.value = true
      val res = HashHelper.calculateHashes(File(item.path))
      _isLoading.value = false
      _hashResult.value = res
    }
  }

  fun dismissHashDialog() {
    _hashResult.value = null
  }

  fun inspectApk(item: FileItem) {
    viewModelScope.launch(Dispatchers.IO) {
      val info = ApkInspectorHelper.parseApk(getApplication(), File(item.path))
      if (info != null) {
        _apkInfo.value = info
      } else {
        _statusMessage.value = "Could not parse APK package info"
      }
    }
  }

  fun dismissApkInfo() {
    _apkInfo.value = null
  }

  fun openHexViewer(item: FileItem) {
    _hexViewerTarget.value = item
  }

  fun dismissHexViewer() {
    _hexViewerTarget.value = null
  }

  fun openTextEditor(item: FileItem) {
    viewModelScope.launch(Dispatchers.IO) {
      val f = File(item.path)
      val content = try {
        if (f.length() > 500_000) {
          f.bufferedReader().use { it.readText().take(500_000) }
        } else {
          f.readText()
        }
      } catch (e: Exception) {
        "Error reading file: ${e.message}"
      }
      _textEditorContent.value = content
      _textEditorTarget.value = item
    }
  }

  fun saveTextEditor(newContent: String) {
    val target = _textEditorTarget.value ?: return
    viewModelScope.launch(Dispatchers.IO) {
      try {
        File(target.path).writeText(newContent)
        _statusMessage.value = "Saved ${target.name}"
        _textEditorTarget.value = null
        refreshCurrentDirectory()
      } catch (e: Exception) {
        _statusMessage.value = "Failed to save: ${e.message}"
      }
    }
  }

  fun dismissTextEditor() {
    _textEditorTarget.value = null
  }

  fun showStorageAnalyzer() {
    loadStorageBreakdown()
    _storageAnalyzerVisible.value = true
  }

  fun dismissStorageAnalyzer() {
    _storageAnalyzerVisible.value = false
  }

  private fun loadStorageBreakdown() {
    viewModelScope.launch {
      _storageBreakdown.value = repository.calculateStorageBreakdown()
    }
  }

  fun openActionSheet(item: FileItem) {
    _fileActionSheetItem.value = item
  }

  fun dismissActionSheet() {
    _fileActionSheetItem.value = null
  }

  fun clearStatusMessage() {
    _statusMessage.value = null
  }

  // Quick Bookmarks Navigation
  fun navigateToDeviceStorage() {
    navigateTo(repository.deviceStorageRoot.absolutePath)
  }

  fun navigateToSandbox() {
    navigateTo(repository.appSandboxRoot.absolutePath)
  }

  fun navigateToDownload() {
    val d = File(repository.appSandboxRoot, "Download")
    if (!d.exists()) d.mkdirs()
    navigateTo(d.absolutePath)
  }

  fun navigateToDocuments() {
    val d = File(repository.appSandboxRoot, "Documents")
    if (!d.exists()) d.mkdirs()
    navigateTo(d.absolutePath)
  }

  fun navigateToArchives() {
    val d = File(repository.appSandboxRoot, "Archives")
    if (!d.exists()) d.mkdirs()
    navigateTo(d.absolutePath)
  }
}
