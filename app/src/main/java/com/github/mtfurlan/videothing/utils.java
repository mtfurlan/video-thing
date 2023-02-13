package com.github.mtfurlan.videothing;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import androidx.annotation.LayoutRes;
import com.google.android.material.snackbar.Snackbar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.json.*;

public class utils {
    private static final String TAG = "Syncopoli";

    public int copyExecutable(Context context, String filename) {
        // copy and overwrite

        File file = context.getFileStreamPath(filename);


        InputStream src = null;

        // try to grab matching executable for a ABI supported by this device
        for (String abi : Build.SUPPORTED_ABIS) {
            try {
                src = context.getAssets().open(abi + '/' + filename);
            } catch (IOException e) {
                // no need to close src here
                Log.d(TAG, abi + " is not supported");
                continue;
            }
        }

        if (src == null) {
            Log.e(TAG, "Could not find supported rsync binary for ABI: " + Arrays.toString(Build.SUPPORTED_ABIS));
            return -1;
        }

        Log.d(TAG, "Found appropriate rsync binary: " + src);

        OutputStream dst = null;
        try {
            dst = new DataOutputStream(context.openFileOutput(filename, Context.MODE_PRIVATE));

            byte data[] = new byte[4096];
            int count;

            while ((count = src.read(data)) != -1) {
                dst.write(data, 0, count);
            }
        } catch (IOException e) {
            Log.e(TAG, "Error copying executable: " + e.toString());
            return -1;
        }

        try {
            src.close();
            dst.close();
        } catch  (IOException e) {
            Log.e(TAG, "Error closing input or output stream: " + e.toString());
        }

        File f = new File(context.getFilesDir(), filename);
        try {
            f.setExecutable(true);
        } catch (SecurityException e) {
            Log.e(TAG, "Error setting executable flag: " + e.toString());
            return -1;
        }

        return 0;
    }

    public void runCommand(Context context, ArrayList<String> command) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(context.getFilesDir());
        pb.redirectErrorStream(true);

        Process process = pb.start();

        /*
         * GET STDOUT/STDERR
         */

        String temp = "";
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

        /* Read STDOUT & STDERR */
        while ((temp = reader.readLine()) != null) {
            Log.v(TAG, temp + "\n");
        }
        reader.close();

        // Wait for the command to finish.
        process.waitFor();

        // Show message how it ended.
        int errno = process.exitValue();
        if (errno != 0) {
            Log.v(TAG, "\nSync FAILED (error code " + errno + ").\n");
        } else {
            Log.v(TAG, "\nSync complete.\n");
        }
    }
}
