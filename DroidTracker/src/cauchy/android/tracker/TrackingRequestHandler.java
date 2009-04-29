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

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * @author obonal
 */
public class TrackingRequestHandler extends Activity implements
                                                    IDroidTrackerConstants {
    
    private static final int DIALOG_TRACKING_REQUEST_CONFIRMATION = 0;
    
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate( icicle);
        
        String action = getIntent().getAction();
        if ( !IDroidTrackerConstants.HANDLE_SMS_ACTION.equals( action)) {
            Log.d( IDroidTrackerConstants.CAUCHY_LOG,
                   " Invalid action for TrackingRequestHandler: " + action);
            setResult( RESULT_CANCELED);
            finish();
        }
        
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences( this);
        boolean confirmation_requested = prefs.getBoolean( IDroidTrackerConstants.PREFERENCE_KEY_TRACKING_CONFIRMATION_REQUESTED,
                                                           true);
        
        Bundle extras = getIntent().getExtras();
        final long tracker_id = extras.getLong( KEY_TRACKER_ID);
        final String msg_to = extras.getString( KEY_MSG_TO);
        final String msg_display_to = extras.getString( KEY_MSG_DISPLAY_TO);
        final String msg_body_lowercased = extras.getString( KEY_MSG_BODY)
                                                 .toLowerCase();
        
        final TrackersDataManager dataManager = new TrackersDataManager( this);
        final Tracker tracker = dataManager.fetchTrackerById( tracker_id);
        
        final boolean is_lost_phone_tracking = extras.getBoolean( IDroidTrackerConstants.KEY_LOSTPHONE_TRACKING);
        
        String sms_stop_passphrase = prefs.getString( IDroidTrackerConstants.PREFERENCE_KEY_SMS_STOP_PASSPHRASE,
                                                      getString( R.string.preferences_sms_stop_passphrase_default));
        if ( tracker.isTracking()) {
            Log.d( IDroidTrackerConstants.CAUCHY_LOG,
                   "Request from already tracking tracker, look for stop passphase: "
                           + sms_stop_passphrase);
            if ( msg_body_lowercased.contains( sms_stop_passphrase.toLowerCase())) {
                dataManager.stopTracking( tracker_id);
            }
            dataManager.close();
            finish();
        } else {
            dataManager.close();
            // If tracker is not tracking anymore but the sentence we receive
            // contains the stop pass phrase, we don't want to start tracking
            // as it is a stop message.
            if ( msg_body_lowercased.contains( sms_stop_passphrase.toLowerCase())) {
                finish();
            }
            if ( confirmation_requested && !is_lost_phone_tracking) {
                showDialog( DIALOG_TRACKING_REQUEST_CONFIRMATION);
            } else {
                startTrackingMessageHandling( tracker_id,
                                              msg_to,
                                              msg_display_to,
                                              msg_body_lowercased,
                                              tracker,
                                              is_lost_phone_tracking);
                
            }
        }
    }
    
    @Override
    protected Dialog onCreateDialog(int id) {
        Bundle extras = getIntent().getExtras();
        final long tracker_id = extras.getLong( KEY_TRACKER_ID);
        final String msg_to = extras.getString( KEY_MSG_TO);
        final String msg_display_to = extras.getString( KEY_MSG_DISPLAY_TO);
        final String msg_body_lowercased = extras.getString( KEY_MSG_BODY)
                                                 .toLowerCase();
        final TrackersDataManager dataManager = new TrackersDataManager( this);
        final Tracker tracker = dataManager.fetchTrackerById( tracker_id);
        dataManager.close();
        
        switch ( id) {
            case DIALOG_TRACKING_REQUEST_CONFIRMATION:
                return new AlertDialog.Builder( TrackingRequestHandler.this).setIcon( R.drawable.start_tracking)
                                                                            .setTitle( R.string.tracking_request_msg_title)
                                                                            .setMessage( getString( R.string.tracking_request_msg_body)
                                                                                    + " "
                                                                                    + tracker.name
                                                                                    + "?")
                                                                            .setPositiveButton( R.string.tracking_request_msg_button_accept,
                                                                                                new DialogInterface.OnClickListener() {
                                                                                                    public void onClick(DialogInterface dialog,
                                                                                                                        int whichButton) {
                                                                                                        
                                                                                                        startTrackingMessageHandling( tracker_id,
                                                                                                                                      msg_to,
                                                                                                                                      msg_display_to,
                                                                                                                                      msg_body_lowercased,
                                                                                                                                      tracker,
                                                                                                                                      false);
                                                                                                        
                                                                                                    }
                                                                                                })
                                                                            .setNegativeButton( R.string.tracking_request_msg_button_refuse,
                                                                                                new DialogInterface.OnClickListener() {
                                                                                                    public void onClick(DialogInterface dialog,
                                                                                                                        int whichButton) {
                                                                                                        // SmsManager.getDefault().sendTextMessage(
                                                                                                        // tracker.number,
                                                                                                        // null,
                                                                                                        // getText(
                                                                                                        // R.string.tracking_refused_msg).toString(),
                                                                                                        // null,
                                                                                                        // null);
                                                                                                        setResult( RESULT_CANCELED);
                                                                                                        finish();
                                                                                                    }
                                                                                                })
                                                                            .create();
        }
        return super.onCreateDialog( id);
    }
    
    private void handleMessage(Context context,
                               long tracker_id,
                               String msg_to,
                               String msg_display_to,
                               String msg_body_lowercased,
                               boolean is_lost_phone_tracking) {
        
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences( context);
        String sms_start_passphrase = prefs.getString( IDroidTrackerConstants.PREFERENCE_KEY_SMS_START_PASSPHRASE,
                                                       context.getString( R.string.preferences_sms_start_passphrase_default));
        String sms_lostphone_passphrase = prefs.getString( IDroidTrackerConstants.PREFERENCE_KEY_SMS_LOSTPHONE_PASSPHRASE,
                                                           context.getString( R.string.preferences_sms_lostphone_passphrase_default));
        Log.i( IDroidTrackerConstants.CAUCHY_LOG,
               "handleMessage, start pass phrase  : " + sms_start_passphrase);
        Log.i( IDroidTrackerConstants.CAUCHY_LOG,
               "               msb_body_lowercased: " + msg_body_lowercased);
        if ( ( !is_lost_phone_tracking && msg_body_lowercased.contains( sms_start_passphrase.toLowerCase()))
                || ( is_lost_phone_tracking && msg_body_lowercased.contains( sms_lostphone_passphrase.toLowerCase()))) {
            Log.i( IDroidTrackerConstants.CAUCHY_LOG, sms_start_passphrase);
            long ms_period;
            
            String sms_period_passphrase = prefs.getString( IDroidTrackerConstants.PREFERENCE_KEY_SMS_PERIOD_PASSPHRASE,
                                                            context.getString( R.string.preferences_sms_period_passphrase_default));
            
            int period_passphrase_separator_index = sms_period_passphrase.indexOf( IDroidTrackerConstants.PERIOD_PASSPHRASE_SEPARATOR);
            
            String period_prefix = null;
            String period_suffix = null;
            
            int period_prefix_index = -1;
            
            if ( period_passphrase_separator_index != -1) {
                
                try {
                    period_prefix = sms_period_passphrase.substring( 0,
                                                                     period_passphrase_separator_index)
                                                         .toLowerCase();
                    period_suffix = sms_period_passphrase.substring( period_passphrase_separator_index
                                                                             + IDroidTrackerConstants.PERIOD_PASSPHRASE_SEPARATOR.length(),
                                                                     sms_period_passphrase.length())
                                                         .toLowerCase();
                    Log.i( IDroidTrackerConstants.CAUCHY_LOG,
                           "handleMessage, period_prefix = [" + period_prefix
                                   + "]");
                    Log.i( IDroidTrackerConstants.CAUCHY_LOG,
                           "handleMessage, period_suffix = [" + period_suffix
                                   + "]");
                    period_prefix_index = msg_body_lowercased.indexOf( period_prefix);
                } catch ( Exception e) {
                    period_prefix_index = -1;
                }
            }
            
            if ( period_prefix_index != -1) {
                
                String body_from_after_prefix = msg_body_lowercased.substring( period_prefix_index
                        + period_prefix.length());
                String[] tokens = body_from_after_prefix.split( "[ ]");
                for ( int i = 0; i < tokens.length; i++) {
                    String token = tokens[i];
                    Log.i( IDroidTrackerConstants.CAUCHY_LOG, "tokens[" + i
                            + "] = " + token);
                }
                if ( tokens.length >= 3) {
                    String period_string = tokens[1];
                    try {
                        Float period = Float.valueOf( period_string);
                        if ( Math.floor( period) != period) {
                            Log.w( IDroidTrackerConstants.CAUCHY_LOG,
                                   "Invalid Period: "
                                           + getText( R.string.invalid_period_dialog_message));
                            ms_period = -1;
                        } else {
                            ms_period = (long) ( period * 60000);
                        }
                    } catch ( Exception e) {
                        Log.println( Log.ERROR,
                                     IDroidTrackerConstants.CAUCHY_LOG,
                                     " Exception on Parsing Period: "
                                             + period_string
                                             + " - Default one time tracking will be used.");
                        ms_period = -1;
                    }
                } else {
                    // One Time Tracking
                    ms_period = -1;
                }
            } else {
                // One Time Tracking
                ms_period = -1;
            }
            
            // If tracking request comes from SMS, reply is sent by SMS
            // hence the forced format.
            startSendingTrackingInfo( this,
                                      tracker_id,
                                      msg_to,
                                      ms_period,
                                      IDroidTrackerConstants.FORMAT_SMS,
                                      is_lost_phone_tracking);
        }
    }
    
    /**
     * @param tracker_id
     * @param msg_to
     * @param msg_display_to
     * @param msg_body_lowercased
     * @param tracker
     */
    private void startTrackingMessageHandling(final long tracker_id,
                                              final String msg_to,
                                              final String msg_display_to,
                                              final String msg_body_lowercased,
                                              final Tracker tracker,
                                              final boolean is_lost_phone_tracking) {
        handleMessage( TrackingRequestHandler.this,
                       tracker_id,
                       msg_to,
                       msg_display_to,
                       msg_body_lowercased,
                       is_lost_phone_tracking);
        setResult( RESULT_OK);
        finish();
    }
    
    public static void startSendingTrackingInfo(final Activity parent_activity,
                                                long tracker_id,
                                                final String to,
                                                final long ms_period,
                                                final String format,
                                                boolean is_lost_phone_tracking) {
        // //////////////
//        String active_location_providers = Settings.System.getString( parent_activity.getContentResolver(),
//                                                                      Settings.System.LOCATION_PROVIDERS_ALLOWED);
//        Log.d( IDroidTrackerConstants.CAUCHY_LOG,
//               "TrackingInfoSenderService.onStart() active_location_providers = "
//                       + active_location_providers);
//        if ( active_location_providers == null
//                || active_location_providers.length() == 0) {
//            Log.d( IDroidTrackerConstants.CAUCHY_LOG,
//                   "                                    No location providers! Activating them! ");
//            Settings.System.putString( parent_activity.getContentResolver(),
//                                       Settings.System.LOCATION_PROVIDERS_ALLOWED,
//                                       "network,gps");
//            // Intent intent = new
//            // Intent("android.intent.action.ACTION_PROVIDER_CHANGED");
//            // parent_activity.sendBroadcast(intent);
//            LocationManager locationManager = (LocationManager) parent_activity.getSystemService( Context.LOCATION_SERVICE);
//            try {
//                Method m = locationManager.getClass()
//                                          .getMethod( "updateProviders",
//                                                      new Class[] {});
//                m.setAccessible( true);
//                m.invoke( locationManager, new Object[] {});
//            } catch ( Exception e) {
//                Log.e( IDroidTrackerConstants.CAUCHY_LOG,
//                       "Couldn't call updateProviders",
//                       e);
//            }
//        } else {
//            Log.d( IDroidTrackerConstants.CAUCHY_LOG,
//                   "                                    Location providers found! Keeping as is... ");
//        }
        // //////////////
        
        Log.d( CAUCHY_LOG, "startSendingTrackingInfo to " + to + " every "
                + ms_period + " ms.");
        final TrackersDataManager dataManager = new TrackersDataManager( parent_activity);
        dataManager.startTracking( tracker_id,
                                   ms_period,
                                   format,
                                   is_lost_phone_tracking);
        dataManager.close();
        Intent sender_intent = new Intent( parent_activity,
                                           SendTrackingInfoIntentReceiver.class);
        sender_intent.putExtra( KEY_MSG_TO, to);
        
        PendingIntent send_alarm_intent = PendingIntent.getBroadcast( parent_activity,
                                                                      0,
                                                                      sender_intent,
                                                                      PendingIntent.FLAG_CANCEL_CURRENT);
        
        long firstTime = SystemClock.elapsedRealtime();
        
        // Schedule the alarm!
        AlarmManager am = (AlarmManager) parent_activity.getSystemService( ALARM_SERVICE);
        
        am.setRepeating( AlarmManager.ELAPSED_REALTIME_WAKEUP,
                         firstTime,
                         IDroidTrackerConstants.TRACKING_CHECK_PERIOD_MS,// ms_period,
                         send_alarm_intent);
        
    }
    
}
