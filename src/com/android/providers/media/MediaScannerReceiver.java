/* //device/content/providers/media/src/com/android/providers/media/MediaScannerReceiver.java
**
** Copyright 2007, The Android Open Source Project
**
** Licensed under the Apache License, Version 2.0 (the "License"); 
** you may not use this file except in compliance with the License. 
** You may obtain a copy of the License at 
**
**     http://www.apache.org/licenses/LICENSE-2.0 
**
** Unless required by applicable law or agreed to in writing, software 
** distributed under the License is distributed on an "AS IS" BASIS, 
** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
** See the License for the specific language governing permissions and 
** limitations under the License.
*/

package com.android.providers.media;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.os.SystemProperties;


public class MediaScannerReceiver extends BroadcastReceiver {
    private final static String TAG = "MediaScannerReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        final Uri uri = intent.getData();
		String packageName = intent.getStringExtra("package");
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            // Scan both internal and external storage
            scan(context, MediaProvider.INTERNAL_VOLUME);
            scan(context, MediaProvider.EXTERNAL_VOLUME);

        }else if((VolumeInfo.ACTION_VOLUME_STATE_CHANGED.equals(action))){
		if(("true".equals(SystemProperties.get("ro.udisk.visible")))){
			String id = intent.getStringExtra(VolumeInfo.EXTRA_VOLUME_ID);
			int state = intent.getIntExtra(VolumeInfo.EXTRA_VOLUME_STATE,-1);
			if(VolumeInfo.STATE_MOUNTED == state)
			{
				Log.d(TAG,"----MediaScanner get volume mounted,start scan---");
				/*StorageManager mStorageManager = context.getSystemService(StorageManager.class);
				VolumeInfo vol = mStorageManager.findVolumeById(id);
				scanFile(context, vol.getPath().getPath());*/
				scan(context, MediaProvider.EXTERNAL_VOLUME);
			}
	        }
	}else if(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE.equals(action) && "Documents".equals(packageName)){
		scan(context, MediaProvider.EXTERNAL_VOLUME);
	}else {
            if (uri != null && uri.getScheme() != null && uri.getScheme().equals("file")) {
                // handle intents related to external storage
                String path = uri.getPath();
                String externalStoragePath = Environment.getExternalStorageDirectory().getPath();
                String legacyPath = Environment.getLegacyExternalStorageDirectory().getPath();

                try {
                    path = new File(path).getCanonicalPath();
                } catch (IOException e) {
                    Log.e(TAG, "couldn't canonicalize " + path);
                    return;
                }
                if (path.startsWith(legacyPath)) {
                    path = externalStoragePath + path.substring(legacyPath.length());
                }

                if (Intent.ACTION_MEDIA_MOUNTED.equals(action)) {
                    // scan whenever any volume is mounted
                    scan(context, MediaProvider.EXTERNAL_VOLUME);
                } else if (Intent.ACTION_MEDIA_SCANNER_SCAN_FILE.equals(action) &&
                        path != null && path.startsWith(externalStoragePath + "/")) {
                    scanFile(context, path);
                } else if ( Intent.ACTION_MEDIA_SCANNER_SCAN_FILE.equals(action) &&
                        "RockExplorer".equals(packageName)) {
                    scan(context, MediaProvider.INTERNAL_VOLUME);
                    scan(context, MediaProvider.EXTERNAL_VOLUME);
                }
            }
        }
    }

    private void scan(Context context, String volume) {
        Bundle args = new Bundle();
        args.putString("volume", volume);
        context.startService(
                new Intent(context, MediaScannerService.class).putExtras(args));
    }    

    private void scanFile(Context context, String path) {
        Bundle args = new Bundle();
        args.putString("filepath", path);
        context.startService(
                new Intent(context, MediaScannerService.class).putExtras(args));
    }    
}
