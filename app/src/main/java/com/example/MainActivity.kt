package com.example

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.FileManagerScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.FileViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        val viewModel: FileViewModel = viewModel()

        // Storage permission launcher for Android 11+ and legacy
        val permissionLauncher = rememberLauncherForActivityResult(
          ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
          if (permissions.values.any { it }) {
            viewModel.refreshCurrentDirectory()
          }
        }

        LaunchedEffect(Unit) {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
              try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                  data = Uri.parse("package:$packageName")
                }
                // Do not crash if action isn't available
              } catch (_: Exception) {}
            }
          } else {
            permissionLauncher.launch(
              arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
              )
            )
          }
        }

        val inArchiveMode by viewModel.inArchiveMode.collectAsState()
        val currentPath by viewModel.currentPath.collectAsState()

        // Handle Back button: Navigate up in folders or out of archive
        BackHandler {
          val handled = viewModel.navigateUp()
          if (!handled) {
            finish()
          }
        }

        Surface(modifier = Modifier.fillMaxSize()) {
          FileManagerScreen(viewModel = viewModel)
        }
      }
    }
  }
}

