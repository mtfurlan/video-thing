package com.github.mtfurlan.videothing

import android.util.Log
import      java.io.BufferedReader
import      java.io.InputStreamReader

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

fun logProcess(process:Process) {
    var output: String = ""
    val inputStream =  BufferedReader(InputStreamReader(process.getInputStream()))
    while ( inputStream.readLine()?.also { output = it } != null) {
        Log.d("FOO", "Debug: " + output)
    }
    inputStream.close()
    process.waitFor()
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


        val dir = appContext.getExternalFilesDir("foo")
        val file = File(dir, "somefile.txt")
        file.printWriter().use { out ->
            out.println("test")
        }

        val rsync = copyFromAssets("rsync")
        rsync.setExecutable(true);
        //val sshKey = copyFromAssets("skynet_rsa")

        val binary = rsync.getAbsolutePath()
        //val rshArg = "-rsh 'ssh -p 22222 -i " + sshKey.getAbsolutePath() + "'"
        Log.e(TAG, binary.toString());
        //Log.e(TAG, rshArg.toString());

        val ls1 = ProcessBuilder("ls", "-l", binary)
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .start()
        logProcess(ls1);
        val ls2 = ProcessBuilder("ls", "-l", file.getAbsolutePath())
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .start()
        logProcess(ls2);
        //val process = ProcessBuilder(binary,
        //                        rshArg,
        //                        file.getAbsolutePath(),
        //                        "mark@space.i3detroit.org:/tmp/")
        //    .redirectOutput(ProcessBuilder.Redirect.INHERIT)
        //    .start()
        //logProcess(process);


        //deleting the file
        file.delete();
        System.out.println("file deleted");

    }
}
