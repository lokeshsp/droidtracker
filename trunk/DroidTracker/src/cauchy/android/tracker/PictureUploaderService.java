/**
 * Copyright 2009 Olivier Bonal <olivier.bonal@gmail.com>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * 
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 */

package cauchy.android.tracker;

import java.io.FileInputStream;
import java.io.IOException;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

public class PictureUploaderService extends Service {
    
    private SharedPreferences mPrefs;
    
    @Override
    public void onCreate() {
        Log.i( IDroidTrackerConstants.CAUCHY_LOG,
               "PictureUploaderService onCreate...");
        
        mPrefs = getSharedPreferences( IDroidTrackerConstants.SHARED_PREFERENCES_KEY_MAIN,
                                       MODE_PRIVATE);
    }
    
    @Override
    public void onDestroy() {
        Log.i( IDroidTrackerConstants.CAUCHY_LOG,
               "PictureUploaderService onDestroy...");
    }
    
    @Override
    public void onStart(Intent intent, int startId) {
        checkForPicturesToUpload();
    }
    
    private void checkForPicturesToUpload() {
        boolean upload_picture = shouldUploadPicture();
        if ( upload_picture) {
            
            Runnable picture_uploader = new Runnable() {
                public void run() {
                    // Reset flag
                    SharedPreferences.Editor ed = mPrefs.edit();
                    ed.putBoolean( IDroidTrackerConstants.PICTURE_TO_UPLOAD,
                                   false);
                    // Start by sleeping 10s to wait for picture to be taken
                    try {
                        Thread.sleep( 10000);
                    } catch ( InterruptedException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    }
                    ed.commit();
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences( PictureUploaderService.this);
                    String picasaLogin = prefs.getString( IDroidTrackerConstants.PREFERENCE_KEY_PICASA_LOGIN,
                                                          null);
                    String picasaPasswd = prefs.getString( IDroidTrackerConstants.PREFERENCE_KEY_PICASA_PASSWD,
                                                           null);
                    String albumname = prefs.getString( IDroidTrackerConstants.PREFERENCE_KEY_PICASA_ALBUM_NAME,
                                                        null);
                    if ( picasaLogin != null && picasaLogin.length() > 0
                            && picasaPasswd != null
                            && picasaPasswd.length() > 0 && albumname != null
                            && albumname.length() > 0) {
                        
                    }
                    final PicasaWSUtils pws = new PicasaWSUtils( picasaLogin,
                                                                 picasaPasswd);
                    String picture_path = getApplication().getFilesDir()
                                                          .getAbsolutePath()
                            + IDroidTrackerConstants.TMP_PICTURE_FILE_NAME;
                    try {
                        FileInputStream fis = new FileInputStream( picture_path);
                        byte[] jpegByteArray = new byte[fis.available()];
                        fis.read( jpegByteArray);
                        fis.close();
                        pws.addPicture( jpegByteArray, albumname);
                        stopSelf();
                    } catch ( IOException e) {
                        e.printStackTrace();
                    }
                }
            };
            Thread picture_uploader_thread = new Thread( picture_uploader);
            picture_uploader_thread.start();
        }
        
    }
    
    private boolean shouldUploadPicture() {
        return mPrefs.getBoolean( IDroidTrackerConstants.PICTURE_TO_UPLOAD,
                                  false);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
