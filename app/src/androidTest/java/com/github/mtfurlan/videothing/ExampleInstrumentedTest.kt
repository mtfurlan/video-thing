package com.github.mtfurlan.videothing

import android.content.Context
import android.os.Build
import android.util.Log

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert
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

    fun installBinaryFromAssets(context: Context, filename: String): Int {
        // try to grab matching executable for a ABI supported by this device
        var src: InputStream? = null
        for (abi in Build.SUPPORTED_ABIS) {
            src = try {
                context.assets.open("$abi/$filename")
            } catch (e: IOException) {
                // no need to close src here
                continue
            }
        }
        if (src == null) {
            Log.e( TAG, "Could not find supported rsync binary for ABI: " + Arrays.toString(Build.SUPPORTED_ABIS))
            return -1
        }
        val outFile = File(context.getFilesDir(), filename)

        val dst = outFile.outputStream();

        try {
            src.copyTo(dst);
            src.close()
            dst.close()
        } catch (e: IOException) {
            Log.e(TAG, "Error copying file: $e")
            return -1;
        }

        try {
            outFile.setExecutable(true)
        } catch (e: SecurityException) {
            Log.e(TAG, "Error setting executable flag: $e")
            return -1
        }
        return 0
    }

    class CommandResult{
        var code:Int=0
        var output:String=""
    }
    fun runCommand(command: ArrayList<String>):CommandResult {
        val pb = ProcessBuilder(command)
        pb.redirectErrorStream(true)
        val process = pb.start()

        // stdout/stderr are combined
        val reader = BufferedReader(InputStreamReader(process.inputStream))

        // Wait for the command to finish.
        process.waitFor()

        val ret = CommandResult();
        ret.code = process.exitValue();
        ret.output = reader.use { it.readText() }
        Log.e(TAG, "ERROR: ${ret.code}");

        return ret;
    }

    /**
     * convert openssh key to dropbear and save to context.getFilesDir()/tmp_key
     *
     * Note: caller is responsible for deleting the file
     * @returns File of converted key
     **/
    fun makeTmpDropbearKey(context:Context, sshKeyPath:String): File {
        val dropbearKey = File(context.getFilesDir(), "tmp_key");
        val dropbearKeyPath = dropbearKey.getAbsolutePath();
        val dropbearConvertPath = File(context.getFilesDir(), "dropbearconvert").getAbsolutePath();

        val convertKeyCommand = arrayListOf<String>()
        convertKeyCommand.add(dropbearConvertPath);
        convertKeyCommand.add("openssh");
        convertKeyCommand.add("dropbear");
        convertKeyCommand.add(sshKeyPath);
        convertKeyCommand.add(dropbearKeyPath);

        val convertKeyResult = runCommand(convertKeyCommand);
        if(convertKeyResult.code != 0) {
            Log.e(TAG, "failed to convert ssh key, dropbearconvert returned ${convertKeyResult.code}");
            Log.e(TAG, convertKeyResult.output);
            throw Exception("failed to convert ssh key, dropbearconvert returned ${convertKeyResult.code}");
        }

        if (!dropbearKey.exists()) {
            throw Exception("Failed to convert dropbear key");
        }

        return dropbearKey;
    }

    fun installBinaries(context:Context) {
        installBinaryFromAssets(context, "rsync");
        installBinaryFromAssets(context, "ssh");
        installBinaryFromAssets(context, "dropbearconvert");
    }

    @Test
    fun actuallyRsyncAFile() {
        // Context of the app under test.
        val context = InstrumentationRegistry.getInstrumentation().targetContext;

        val file = File(context.getFilesDir(), "testFile")
        file.printWriter().use { out ->
            out.println("test")
        }


        val rsyncPath = File(context.getFilesDir(), "rsync").getAbsolutePath()
        val sshPath = File(context.getFilesDir(), "ssh").getAbsolutePath();


        val sshKeyPath = copyFromAssets("skynet_ed25519").getAbsolutePath();

        val dropbearKey = makeTmpDropbearKey(context, sshKeyPath);

        val rsyncCommand = arrayListOf<String>()
        rsyncCommand.add(rsyncPath);
        //dropbear doesn't do UserKnownHostsFile so we just live with errors
        // -y is dropbear for -oStrictHostKeyChecking=no
        rsyncCommand.add("--rsh");
        rsyncCommand.add("$sshPath -p 22222 -i ${dropbearKey.getAbsolutePath()} -y");
        rsyncCommand.add(file.getAbsolutePath());
        rsyncCommand.add("mtfurlan@space.i3detroit.org:/tmp/");

        Log.d(TAG, "rsync exec: " + rsyncCommand.toString());

        val rsyncResult = runCommand(rsyncCommand);
        if(rsyncResult.code != 0) {
            Log.e(TAG, "rsync failed: ${rsyncResult.code}");
            Log.e(TAG, rsyncResult.output);
            Assert.fail("rsync failed: ${rsyncResult.code}");
        }


        //cleanup
        dropbearKey.delete();
        file.delete();

    }
}
