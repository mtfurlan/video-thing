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
    //val outFile = File("/sdcard/Android/data/com.github.mtfurlan.videothing/", assetFile)
    if (!outFile.exists()) {
        appContext.assets.open(assetFile).copyTo(outFile.outputStream())
    }
    return outFile;
}

@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    private val TAG = "RSYNC TEST"
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext;

        val file = File(appContext.getFilesDir(), "testFile")
        file.printWriter().use { out ->
            out.println("test")
        }

        copyExecutable(appContext, "rsync");

        val rsyncPath = File(appContext.getFilesDir(), "rsync").getAbsolutePath()
        Log.d(TAG, "rsyncPath: " + rsyncPath);


        val sshKey = copyFromAssets("skynet_rsa")


        val command = arrayListOf<String>()
        command.add(rsyncPath);

        command.add("--rsh 'ssh -p 22222 -i " + sshKey.getAbsolutePath() + "'");
        command.add(file.getAbsolutePath());
        command.add("mark@space.i3detroit.org:/tmp/");
        command.add("--help");

        Log.d(TAG, "rsync exec: " + command.toString());

        runCommand(appContext, command);


        //deleting the file
        file.delete();
        System.out.println("file deleted");

    }
}
