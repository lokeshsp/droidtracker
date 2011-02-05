package cauchy.android.tracker;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;

/**
 * Takes a picture, upload it to Picasa, then close. To be called like this:
 * @author olivier
 * 
 */
public class PictureTakerActivity extends Activity {
    
    private static final String LOG_TAG = "[CAUCHY_LOG]";
    
    private View mPreview;
    private SharedPreferences mPrefs;
    
    private PictureCallback pic_callback = new Camera.PictureCallback() {
        
        public void onPictureTaken(byte[] data, Camera camera) {
            Log.d( LOG_TAG, "raw_callback .onPictureTaken(), data = " + data);
            
            if ( data != null) {
                Log.d( LOG_TAG, "data.length = " + data.length);
                Bitmap myPic = BitmapFactory.decodeByteArray( data,
                                                              0,
                                                              data.length);
                try {
                    String picture_path = getApplication().getFilesDir()
                                                          .getAbsolutePath()
                            + IDroidTrackerConstants.TMP_PICTURE_FILE_NAME;
                    
                    File f = new File( picture_path);
                    if ( f.exists()) {
                        f.delete();
                    }
                    FileOutputStream buf = new FileOutputStream( picture_path);
                    
                    myPic.compress( Bitmap.CompressFormat.JPEG, 70, buf);
                    
                    SharedPreferences.Editor ed = mPrefs.edit();
                    ed.putBoolean( IDroidTrackerConstants.PICTURE_TO_UPLOAD,
                                   true);
                    ed.commit();
                    Intent sender_intent = new Intent( getApplicationContext(),
                                                       PictureUploaderService.class);
                    getApplicationContext().startService( sender_intent);
                    setResult( RESULT_OK);
                    finish();
                } catch ( Exception e) {
                    e.printStackTrace();
                    setResult( RESULT_CANCELED);
                    finish();
                }
            }
            
        }
    };
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState);
        
        Log.d( LOG_TAG, "PictureTaker onCreate!");
        
        // Hide the window title.
        requestWindowFeature( Window.FEATURE_NO_TITLE);
        
        mPrefs = getSharedPreferences( IDroidTrackerConstants.SHARED_PREFERENCES_KEY_MAIN,
                                       MODE_PRIVATE);
        
        // Create our Preview view and set it as the content of our activity.
        mPreview = new Preview( this);
        setContentView( mPreview);
    }
    
    private class Preview extends SurfaceView implements SurfaceHolder.Callback {
        SurfaceHolder mHolder;
        Camera mCamera;
        
        Preview(Context context) {
            super( context);
            
            // Install a SurfaceHolder.Callback so we get notified when the
            // underlying surface is created and destroyed.
            mHolder = getHolder();
            mHolder.addCallback( this);
            mHolder.setType( SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }
        
        public void surfaceCreated(SurfaceHolder holder) {
            // The Surface has been created, acquire the camera and tell it
            // where
            // to draw.
            try {
                mCamera = Camera.open();
                mCamera.setPreviewDisplay( holder);
            } catch ( Exception exception) {
                mCamera.release();
                mCamera = null;
                setResult( RESULT_CANCELED);
                finish();
            }
        }
        
        public void surfaceDestroyed(SurfaceHolder holder) {
            // Surface will be destroyed when we return, so stop the preview.
            // Because the CameraDevice object is not a shared resource, it's
            // very
            // important to release it when the activity is paused.
            mCamera.stopPreview();
            mCamera = null;
        }
        
        public void surfaceChanged(SurfaceHolder holder,
                                   int format,
                                   int w,
                                   int h) {
            // Now that the size is known, set up the camera parameters and
            // begin
            // the preview.
            Camera.Parameters parameters = mCamera.getParameters();
            
            try {
            	List<Camera.Size> supported_sizes = parameters.getSupportedPreviewSizes();
				if (!supported_sizes.isEmpty()) {
					Camera.Size s = null;
					Iterator<Camera.Size> it = supported_sizes.iterator();
					while (it.hasNext()) {
						Camera.Size size = (Camera.Size) it.next();
						if (size.width <= w) {
							s = size;
						} else {
							break;
						}
					}
					if (s == null) {
						s = supported_sizes.get(0);
					}
					parameters.setPreviewSize(s.width, s.height);
					mCamera.setParameters(parameters);
            	}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				Log.e(IDroidTrackerConstants.CAUCHY_LOG, "Picture Preview Params could not be saved: ", e);
			}
            mCamera.startPreview();
            
            // Wait 2s and take a picture
            Timer t = new Timer();
            t.schedule( new TimerTask() {
                
                @Override
                public void run() {
                    mCamera.takePicture( null, null, pic_callback);
                }
            }, 2000);
        }
        
    }
    
}