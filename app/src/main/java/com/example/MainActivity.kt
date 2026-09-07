package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.ZyronHomeScreen
import com.example.ui.ZyronViewModel
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.resolveZyronPalette

class MainActivity : ComponentActivity() {
  private val viewModel: ZyronViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val appConfig by viewModel.appConfig.collectAsStateWithLifecycle()
      val palette = resolveZyronPalette(appConfig.appTheme)
      MyApplicationTheme(palette = palette) {
        ZyronHomeScreen(viewModel = viewModel)
      }
    }
  }
}
