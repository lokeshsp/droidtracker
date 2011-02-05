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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.telephony.gsm.SmsMessage;
import android.util.Log;

public class SMSReceiver extends BroadcastReceiver {
    
    /**
     * Handle Tracking messages.
     * 
     * @Override
     */
    public void onReceive(Context context, Intent intent) {
        
        Bundle bundle = intent.getExtras();
        Log.d( IDroidTrackerConstants.CAUCHY_LOG, "SMS RECEIVED!");
        if ( bundle == null) {
            return;
        }
        Object[] pdusObj = (Object[]) bundle.get( "pdus");
        for ( int pdu_index = 0; pdu_index < pdusObj.length; pdu_index++) {
            SmsMessage msg = SmsMessage.createFromPdu( (byte[]) pdusObj[pdu_index]);
            final String msg_body = msg.getMessageBody();
            if (msg_body == null) {
                return;
            }
            Log.d( IDroidTrackerConstants.CAUCHY_LOG, "SMS RECEIVED! Body: "
                    + msg_body);
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences( context);
            String sms_start_passphrase = prefs.getString( IDroidTrackerConstants.PREFERENCE_KEY_SMS_START_PASSPHRASE,
                                                           context.getString( R.string.preferences_sms_start_passphrase_default));
            String sms_stop_passphrase = prefs.getString( IDroidTrackerConstants.PREFERENCE_KEY_SMS_STOP_PASSPHRASE,
                                                          context.getString( R.string.preferences_sms_stop_passphrase_default));
            String sms_lostphone_passphrase = prefs.getString( IDroidTrackerConstants.PREFERENCE_KEY_SMS_LOSTPHONE_PASSPHRASE,
                                                               context.getString( R.string.preferences_sms_lostphone_passphrase_default));
            boolean lost_phone_mode_activated = prefs.getBoolean( IDroidTrackerConstants.PREFERENCE_KEY_ACTIVATE_LOST_PHONE_MODE,
                                                                  false);
            final String lower_cased_body = msg_body.toLowerCase();
            Log.d( IDroidTrackerConstants.CAUCHY_LOG,
                   "              LowerCased Body: " + lower_cased_body);
            final boolean contains_and_should_use_lostphone_passphrase = lost_phone_mode_activated
                    && lower_cased_body.contains( sms_lostphone_passphrase.toLowerCase());
            if ( !lower_cased_body.contains( sms_start_passphrase.toLowerCase())
                    && !lower_cased_body.contains( sms_stop_passphrase.toLowerCase())
                    && !contains_and_should_use_lostphone_passphrase) {
                return;
            }
            
            final String msg_to = msg.getOriginatingAddress();
//            final String msg_display_to = msg.getDisplayOriginatingAddress();
            
            TrackersDataManager dataManager = new TrackersDataManager( context);
            Tracker tracker = dataManager.fetchTrackerByNumberUsingCallerIDMinMatch( msg_to);
            Log.i( IDroidTrackerConstants.CAUCHY_LOG,
                   "-------> Look up tracker by number using CallerIDMinMatch ("
                           + msg_to + "), result: " + tracker);
            
            if ( tracker == null) {
                
                if ( contains_and_should_use_lostphone_passphrase) {
                    // If in lost/stolen phone mode, auto create tracker if
                    // needed
                    dataManager.addTemporaryTracker( msg_to);
                    tracker = dataManager.fetchTrackerByNumberUsingCallerIDMinMatch( msg_to);
                    Log.d( IDroidTrackerConstants.CAUCHY_LOG,
                           "Temporary Tracker created: " + tracker + " / id= "
                                   + tracker.id);
                } else {
                    Log.d( IDroidTrackerConstants.CAUCHY_LOG,
                           "Tracking request from unauthorized contact refused: "
                                   + msg_to);
                    dataManager.close();
                    return;
                }
            }
            dataManager.close();
            
            Intent i = new Intent( IDroidTrackerConstants.HANDLE_SMS_ACTION,
                                   null,
                                   context,
                                   TrackingRequestHandler.class);
            i.putExtra( IDroidTrackerConstants.KEY_MSG_TO, msg_to);
//            i.putExtra( IDroidTrackerConstants.KEY_MSG_DISPLAY_TO,
//                        msg_display_to);
            i.putExtra( IDroidTrackerConstants.KEY_MSG_BODY, msg_body);
            i.putExtra( IDroidTrackerConstants.KEY_TRACKER_ID, tracker.id);
            i.putExtra( IDroidTrackerConstants.KEY_LOSTPHONE_TRACKING,
                        contains_and_should_use_lostphone_passphrase);
            i.setFlags( Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity( i);
        }
    }
}
