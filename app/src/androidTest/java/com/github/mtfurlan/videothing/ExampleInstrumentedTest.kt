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
    private val TAG = "RSYNC_TEST"

    fun copyFromAssets(assetFile:String):File {
        val context = InstrumentationRegistry.getInstrumentation().targetContext;
        val outFile = File(context.getFilesDir(), assetFile)
        //val outFile = File("/sdcard/Android/data/com.github.mtfurlan.videothing/", assetFile)
        if (!outFile.exists()) {
            context.assets.open(assetFile).copyTo(outFile.outputStream())
        }
        return outFile;
    }

    fun copyExecutable(context: Context, filename: String): Int {
        //TODO: can simplify a lot from copyFromAssets
        // copy and overwrite
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
        var dst: OutputStream
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
        val reader = BufferedReader(InputStreamReader(process.inputStream))
        var temp:String?
        while (reader.readLine().also { temp = it } != null) {
            Log.v(TAG, "output: $temp")
        }
        reader.close()

        // Wait for the command to finish.
        process.waitFor()

        // Show message how it ended.
        val errno = process.exitValue()
        if (errno != 0) {
            Log.v(TAG, "Sync FAILED (error code $errno).")
        } else {
            Log.v(TAG, "Sync complete.")
        }
    }

    @Test
    fun useAppContext() {
        // Context of the app under test.
        val context = InstrumentationRegistry.getInstrumentation().targetContext;

        val file = File(context.getFilesDir(), "testFile")
        file.printWriter().use { out ->
            out.println("test")
        }

        copyExecutable(context, "rsync");
        copyExecutable(context, "ssh");
        copyExecutable(context, "dropbearconvert");

        val rsyncPath = File(context.getFilesDir(), "rsync").getAbsolutePath()
        val sshPath = File(context.getFilesDir(), "ssh").getAbsolutePath();
        val dropbearConvertPath = File(context.getFilesDir(), "dropbearconvert").getAbsolutePath();


        val sshKeyPath = copyFromAssets("skynet_ed25519").getAbsolutePath();
        val dropbearKey = File(context.getFilesDir(), "tmp_key");
        val dropbearKeyPath = dropbearKey.getAbsolutePath();

        val convertKeyCommand = arrayListOf<String>()
        convertKeyCommand.add(dropbearConvertPath);
        convertKeyCommand.add("openssh");
        convertKeyCommand.add("dropbear");
        convertKeyCommand.add(sshKeyPath);
        convertKeyCommand.add(dropbearKeyPath);

        Log.d(TAG, "dropbearconvert exec: " + convertKeyCommand.toString());
        runCommand(context, convertKeyCommand);

        if (!dropbearKey.exists()) {
            Log.e(TAG, "Failed to convert dropbear key");
        }

        val rsyncCommand = arrayListOf<String>()
        rsyncCommand.add(rsyncPath);
        //dropbear doesn't do UserKnownHostsFile so we just live with errors
        // -y is dropbear for -oStrictHostKeyChecking=no
        rsyncCommand.add("--rsh");
        rsyncCommand.add("$sshPath -p 22222 -i $dropbearKeyPath -y");
        rsyncCommand.add(file.getAbsolutePath());
        rsyncCommand.add("mtfurlan@space.i3detroit.org:/tmp/");

        Log.d(TAG, "rsync exec: " + rsyncCommand.toString());

        runCommand(context, rsyncCommand);


        //deleting the file
        file.delete();
        System.out.println("file deleted");

    }
}
