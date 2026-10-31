package com.example

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.data.PlaybackResumeHelper
import com.example.player.PipController
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun appName_and_pipRemoteActions_areConfiguredProperly() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("negai", appName)

        val pipParams = PipController.buildPipParams(
            context = context,
            isPlayerActive = true,
            isPlaying = true,
            videoWidth = 1920,
            videoHeight = 1080,
            autoEnterEnabled = true,
            seekStepSeconds = 15
        )
        assertEquals(3, pipParams.actions.size)
        assertTrue(pipParams.actions[0].title.toString().contains("15"))
        assertTrue(pipParams.actions[2].title.toString().contains("15"))
    }

    @Test
    fun lastPlayedFileExistenceCheck_detectsExistingAndDeletedLocalVideo() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val tempVideo = File(context.cacheDir, "episode_01_test.mp4")
        tempVideo.writeBytes(byteArrayOf(0x00, 0x00, 0x00, 0x18))
        val fileUri = Uri.fromFile(tempVideo).toString()

        assertTrue(PlaybackResumeHelper.isMediaFileExists(context, fileUri))

        assertTrue(tempVideo.delete())
        assertFalse(PlaybackResumeHelper.isMediaFileExists(context, fileUri))
    }
}
