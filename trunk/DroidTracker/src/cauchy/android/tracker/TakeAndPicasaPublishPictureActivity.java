package cauchy.android.tracker;

import java.io.ByteArrayOutputStream;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Context;
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
 * Takes a picture, upload it to Picasa, then close.
 * To be called like this:
 *       Intent i = new Intent( this, TakeAndPicasaPublishPictureActivity.class);
 *       i.putExtra( TakeAndPicasaPublishPictureActivity.KEY_PICASA_LOGIN, username);
 *       i.putExtra( TakeAndPicasaPublishPictureActivity.KEY_PICASA_PASSWORD, passwd);
 *       i.putExtra( TakeAndPicasaPublishPictureActivity.KEY_PICASA_DEST_ALBUM_NAME, albumname);
 *       startActivity( i);
 * 
 * @author olivier
 * 
 */
public class TakeAndPicasaPublishPictureActivity extends Activity {
    
    private static final String LOG_TAG = "[CAUCHY_LOG]";
    
    public static final String KEY_PICASA_LOGIN = "PICASA_LOGIN";
    public static final String KEY_PICASA_PASSWORD = "PICASA_PASSWORD";
    public static final String KEY_PICASA_DEST_ALBUM_NAME = "PICASA_DEST_ALBUM_NAME";
    
    private View mPreview;
    private String picasaLogin;
    private String picasaPasswd;
    private String picasaDestinationAlbumName;
    
    private PictureCallback pic_callback = new Camera.PictureCallback() {
        
        public void onPictureTaken(byte[] data, Camera camera) {
            Log.d( LOG_TAG, "raw_callback .onPictureTaken(), data = " + data);
            
            if ( data != null) {
                Log.d( LOG_TAG, "data.length = " + data.length);
                Bitmap myPic = BitmapFactory.decodeByteArray( data,
                                                              0,
                                                              data.length);
                try {
                    final PicasaWSUtils pws = new PicasaWSUtils( picasaLogin,
                                                                 picasaPasswd);
                    ByteArrayOutputStream buf = new ByteArrayOutputStream();
                    myPic.compress( Bitmap.CompressFormat.JPEG, 70, buf);
                    
                    final byte[] jpegByteArray = buf.toByteArray();
                    buf.close();
                    camera.stopPreview();
                    
                    Runnable picasa_updater = new Runnable() {
                        public void run() {
                            pws.addPicture( jpegByteArray,
                                            picasaDestinationAlbumName);
                            setResult( RESULT_OK);
                            finish();
                        }
                    };
                    Thread picasa_updater_thread = new Thread( picasa_updater);
                    picasa_updater_thread.start();
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
        
        // Hide the window title.
        requestWindowFeature( Window.FEATURE_NO_TITLE);
        
        // Create our Preview view and set it as the content of our activity.
        mPreview = new Preview( this);
        setContentView( mPreview);
        
        Bundle extras = getIntent().getExtras();
        picasaLogin = extras.getString( KEY_PICASA_LOGIN);
        picasaPasswd = extras.getString( KEY_PICASA_PASSWORD);
        picasaDestinationAlbumName = extras.getString( KEY_PICASA_DEST_ALBUM_NAME);
        
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
            mCamera = Camera.open();
            try {
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
            parameters.setPreviewSize( w, h);
            mCamera.setParameters( parameters);
            mCamera.startPreview();
            
            // Wait 1s and take a picture
            Timer t = new Timer();
            t.schedule( new TimerTask() {
                
                @Override
                public void run() {
                    mCamera.takePicture( null, null, pic_callback);
                }
            }, 1000);
        }
        
    }
    
}