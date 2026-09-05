package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.domain.MinecraftLogAnalyzer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read app name from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Zyron AI", appName)
  }

  @Test
  fun `log sanitizer redacts sensitive credentials`() {
    val rawLogWithToken = "Connecting to session with accessToken='secret_token_1234567890' and password='my_password_xyz'"
    val sanitized = MinecraftLogAnalyzer.sanitizeLog(rawLogWithToken)
    assertFalse(sanitized.contains("secret_token_1234567890"))
    assertFalse(sanitized.contains("my_password_xyz"))
    assertTrue(sanitized.contains("[REDACTED_BY_ZYRON_SECURITY]"))
  }

  @Test
  fun `log analyzer detects java class version mismatch`() {
    val log = "Exception in thread main java.lang.UnsupportedClassVersionError: class file version 65.0"
    val result = MinecraftLogAnalyzer.analyzeLog(rawLog = log, mcVersion = "1.21")
    assertEquals("CONFIRMED INFORMATION", result.confidenceStatus)
    assertTrue(result.problem.contains("Java"))
    assertTrue(result.fixSteps.isNotEmpty())
  }
}

