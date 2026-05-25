package com.example

import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testVersionComparison() {
    val checker = com.example.util.GitHubUpdateChecker
    
    // Newer versions
    assertTrue(checker.isNewerVersion("1.0.0", "1.0.1"))
    assertTrue(checker.isNewerVersion("v1.0.0", "1.0.1"))
    assertTrue(checker.isNewerVersion("1.0.0", "v1.0.1"))
    assertTrue(checker.isNewerVersion("1.0", "1.0.1"))
    assertTrue(checker.isNewerVersion("v1.0.3", "v1.1"))
    assertTrue(checker.isNewerVersion("1.0.0", "2.0.0"))
    
    // Test cases with v. prefix (from tags like v.1.0.3)
    assertTrue(checker.isNewerVersion("1.0.2", "v.1.0.3"))
    assertTrue(checker.isNewerVersion("v.1.0.2", "v1.0.3"))
    assertTrue(checker.isNewerVersion("v.1.0.2", "v.1.0.3"))

    // Older or equal versions
    assertFalse(checker.isNewerVersion("1.0.1", "1.0.1"))
    assertFalse(checker.isNewerVersion("v1.0.2", "1.0.2"))
    assertFalse(checker.isNewerVersion("1.0.2", "v1.0.2"))
    assertFalse(checker.isNewerVersion("1.0.3", "1.0.2"))
    assertFalse(checker.isNewerVersion("2.0.0", "1.0.0"))
    assertFalse(checker.isNewerVersion("1.0.3", "v.1.0.3"))
    assertFalse(checker.isNewerVersion("v.1.0.3", "1.0.3"))
  }
}
