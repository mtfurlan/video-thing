package com.github.mtfurlan.videothing

import android.content.Context
import android.os.Build
import android.util.Log

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
import java.io.BufferedReader
import java.io.DataOutputStream


import java.io.File;
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.util.*

@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
fun copyFromAssets(assetFile:String):File {
    val appContext = InstrumentationRegistry.getInstrumentation().targetContext;
    val outFile = File(appContext.getFilesDir(), assetFile)
    //val outFile = File("/sdcard/Android/data/com.github.mtfurlan.videothing/", assetFile)
    if (!outFile.exists()) {
        appContext.assets.open(assetFile).copyTo(outFile.outputStream())
    }
    return outFile;
}

fun copyExecutable(context: Context, filename: String): Int {
    // copy and overwrite
    val file = context.getFileStreamPath(filename)
    var src: InputStream? = null

    // try to grab matching executable for a ABI supported by this device
    for (abi in Build.SUPPORTED_ABIS) {
        src = try {
            context.assets.open("$abi/$filename")
        } catch (e: IOException) {
            // no need to close src here
            Log.d(TAG, "$abi is not supported")
            continue
        }
    }
    if (src == null) {
        Log.e(
            TAG,
            "Could not find supported rsync binary for ABI: " + Arrays.toString(Build.SUPPORTED_ABIS)
        )
        return -1
    }
    Log.d(TAG, "Found appropriate rsync binary: $src")
    var dst: OutputStream? = null
    try {
        dst = DataOutputStream(context.openFileOutput(filename, Context.MODE_PRIVATE))
        val data = ByteArray(4096)
        var count: Int
        while (src.read(data).also { count = it } != -1) {
            dst.write(data, 0, count)
        }
    } catch (e: IOException) {
        Log.e(TAG, "Error copying executable: $e")
        return -1
    }
    try {
        src.close()
        dst.close()
    } catch (e: IOException) {
        Log.e(TAG, "Error closing input or output stream: $e")
    }
    val f = File(context.filesDir, filename)
    try {
        f.setExecutable(true)
    } catch (e: SecurityException) {
        Log.e(TAG, "Error setting executable flag: $e")
        return -1
    }
    return 0
}

@Throws(IOException::class, InterruptedException::class)
fun runCommand(context: Context, command: ArrayList<String>) {
    val pb = ProcessBuilder(command)
    pb.directory(context.filesDir)
    pb.redirectErrorStream(true)
    val process = pb.start()

    /*
     * GET STDOUT/STDERR
     */
    var temp = ""
    val reader = BufferedReader(InputStreamReader(process.inputStream))

    /* Read STDOUT & STDERR */while (reader.readLine().also { temp = it } != null) {
        Log.v(
            TAG, """
     $temp
     
     """.trimIndent()
        )
    }
    reader.close()

    // Wait for the command to finish.
    process.waitFor()

    // Show message how it ended.
    val errno = process.exitValue()
    if (errno != 0) {
        Log.v(TAG, "\nSync FAILED (error code $errno).\n")
    } else {
        Log.v(TAG, "\nSync complete.\n")
    }
}

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
