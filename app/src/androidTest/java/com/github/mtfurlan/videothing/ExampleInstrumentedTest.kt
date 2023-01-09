package com.github.mtfurlan.videothing

import android.util.Log
import      java.io.BufferedReader
import      java.io.InputStreamReader


import com.github.pgreze.process.process
import com.github.pgreze.process.Redirect
import com.github.pgreze.process.unwrap
import kotlinx.coroutines.runBlocking

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*


import java.io.File;


fun copyFromAssets(assetFile:String):File {
    val appContext = InstrumentationRegistry.getInstrumentation().targetContext;
    val outFile = File(appContext.getFilesDir(), assetFile)
    if (!outFile.exists()) {
        appContext.assets.open(assetFile).copyTo(outFile.outputStream())
    }
    return outFile;
}

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    private val TAG = "RSYNC TEST"
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext;
        assertEquals("com.github.mtfurlan.videothing", appContext.packageName);


        val file = File(appContext.getFilesDir(), "testFile")
        file.printWriter().use { out ->
            out.println("test")
        }

        val rsync = copyFromAssets("rsync")
        rsync.setExecutable(true);
        val sshKey = copyFromAssets("skynet_rsa")

        val binary = rsync.getAbsolutePath()
        val rshArg = "--rsh 'ssh -p 22222 -i " + sshKey.getAbsolutePath() + "'"
        Log.e(TAG, binary.toString());
        Log.e(TAG, rshArg.toString());

        runBlocking {
            val ls1 = process("ls", "-l", binary, stdout = Redirect.CAPTURE).unwrap()
            Log.d(TAG, "Success:\n${ls1.joinToString("\n")}")

            val ls2 = process("ls", "-l", file.getAbsolutePath(), stdout = Redirect.CAPTURE).unwrap()
            Log.d(TAG, "Success:\n${ls2.joinToString("\n")}")

            val rs1 = process(binary, "-h", stdout = Redirect.CAPTURE).unwrap()
            Log.d(TAG, "Success:\n${rs1.joinToString("\n")}")
        }

        //deleting the file
        file.delete();
        System.out.println("file deleted");

    }
}
