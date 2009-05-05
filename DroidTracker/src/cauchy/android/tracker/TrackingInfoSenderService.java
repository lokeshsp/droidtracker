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

import java.net.URLEncoder;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.ConditionVariable;
import android.os.IBinder;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.telephony.gsm.SmsManager;
import android.util.Log;
import android.widget.Toast;

public class TrackingInfoSenderService extends Service implements Runnable {
    
    private final static int NUMBER_OF_PERIODS_FOR_PROVIDERS_TO_WARM_UP = 2;
    
    private IGeoDecoder geodecoder;
    private SmsManager sm;
    
    private LocationManager locationManager;
    private boolean gpsAvailable;
    private boolean firstTime = true;
    // private String initialAllowedLocationProviders = "";
    private Vector<Tracker> sessionOneOffTrackers = new Vector<Tracker>();
    private Vector<Tracker> sessionPeriodicTrackersToBeWarned = new Vector<Tracker>();
    private boolean locationTrackingOn = false;
    // private String autoSetAllowedLocationProviders = null;
    private ConditionVariable sendingCondition;
    private boolean waitingToSend = false;
    
    private final LocationListener locationChangesListener = new LocationListener() {
        
        private int gpsLastStatus = -1;
        
        public void onLocationChanged(Location location) {
            Log.d( IDroidTrackerConstants.CAUCHY_LOG,
                   "     ==> onLocationChanged received for: "
                           + ( ( location == null) ? "error null location"
                                   : location.getProvider()));
            if ( firstTime) {
                firstTime = false;
                sendPositionIfNeeded();
            }
            if ( location != null) {
                // Unlock the sending if GPS location is found or if network
                // location is on but
                // GPS is off and auto-on option is not set.
                if ( ( LocationManager.GPS_PROVIDER.equals( location.getProvider()) && gpsAvailable)
                        || ( gpsLastStatus == LocationProvider.OUT_OF_SERVICE)) { // &&
                    // !isProvidersAutoTurnOnOptionOn()))
                    // {
                    sendingCondition.open();
                }
            }
        }
        
        public void onProviderDisabled(String provider) {
            gpsAvailable = false;
            Log.d( IDroidTrackerConstants.CAUCHY_LOG,
                   "     ==> onProviderDisabled! ");
        }
        
        public void onProviderEnabled(String provider) {
            gpsAvailable = true;
            Log.d( IDroidTrackerConstants.CAUCHY_LOG,
                   "     ==> onProviderEnabled! ");
        }
        
        public void onStatusChanged(String provider, int status, Bundle extras) {
            Log.d( IDroidTrackerConstants.CAUCHY_LOG,
                   "     ==> onStatusChanged! ");
            if ( LocationManager.GPS_PROVIDER.equals( provider)) {
                gpsLastStatus = status;
                if ( status == LocationProvider.TEMPORARILY_UNAVAILABLE) {
                    gpsAvailable = false;
                    Log.d( IDroidTrackerConstants.CAUCHY_LOG,
                           "         ---> GPS status =  TEMPORARILY_UNAVAILABLE");
                } else if ( status == LocationProvider.OUT_OF_SERVICE) {
                    gpsAvailable = false;
                    Log.d( IDroidTrackerConstants.CAUCHY_LOG,
                           "         ---> GPS status =  OUT_OF_SERVICE");
                }
                if ( status == LocationProvider.AVAILABLE) {
                    gpsAvailable = true;
                    Log.d( IDroidTrackerConstants.CAUCHY_LOG,
                           "         ---> GPS status =  AVAILABLE");
                }
            }
        }
    };
    
    @Override
    public void onCreate() {
        Log.d( IDroidTrackerConstants.CAUCHY_LOG,
               "TrackingInfoSenderService onCreate...");
        
        locationManager = (LocationManager) getApplicationContext().getSystemService( Context.LOCATION_SERVICE);
        
        if ( geodecoder == null) {
            // geodecoder = new GeonamesWSGeodecoder();
            geodecoder = new AndroidBasedGeodecoder( getApplicationContext());
        }
        
        sendingCondition = new ConditionVariable( false);
        firstTime = true;
        triggerLocationListeners( getApplicationContext(), 60000L);
    }
    
    @Override
    public void onDestroy() {
        Log.i( IDroidTrackerConstants.CAUCHY_LOG,
               "TrackingInfoSenderService onDestroy...");
        super.onDestroy();
        
        stopLocationRequests();
        
        // Stops all tracking in case onDestroy is not called by me but by a
        // phone event.
        final TrackersDataManager dataManager = new TrackersDataManager( this);
        dataManager.stopAllTracking();
        dataManager.close();
    }
    
    @Override
    public void onStart(Intent intent, int startId) {
        Log.d( IDroidTrackerConstants.CAUCHY_LOG,
               "TrackingInfoSenderService started: " + startId);
        sm = SmsManager.getDefault();
        
        if ( !firstTime) {
            sendPositionIfNeeded();
        }
    }
    
    private void sendPositionIfNeeded() {
        
        Thread thr = new Thread( null,
                                 TrackingInfoSenderService.this,
                                 "Tracking Info Sender Thread");
        thr.start();
    }
    
    public void run() {
        Looper.prepare();
        
        checkForNotificationNeeds();
        
        if ( ( sessionOneOffTrackers == null || sessionOneOffTrackers.isEmpty())
                && ( sessionPeriodicTrackersToBeWarned == null || sessionPeriodicTrackersToBeWarned.isEmpty())) {
            Log.i( IDroidTrackerConstants.CAUCHY_LOG,
                   "sendPositionIfNeeded(): Nothing to do");
            return;
        }
        
        if ( waitingToSend) {
            Log.i( IDroidTrackerConstants.CAUCHY_LOG,
                   "sendPositionIfNeeded(): Already blocked on send, return as previous send will do the job once unblocked, we don't want SMS send twice.");
            return;
        }
        
        waitingToSend = true;
        
        Log.i( IDroidTrackerConstants.CAUCHY_LOG,
               "sendPositionIfNeeded(): sending is needed, will block until GPS location is found or till it times out.");
        // open will be true if the condition was opened, false if the call
        // returns because of the timeout.
        boolean opened = sendingCondition.block( NUMBER_OF_PERIODS_FOR_PROVIDERS_TO_WARM_UP
                * IDroidTrackerConstants.TRACKING_CHECK_PERIOD_MS);
        if ( opened) {
            Log.i( IDroidTrackerConstants.CAUCHY_LOG,
                   "sendPosition is unblocked because location was received.");
        } else {
            Log.i( IDroidTrackerConstants.CAUCHY_LOG,
                   "sendPosition is unblocked because of timeout waiting for GPS location.");
        }
        
        waitingToSend = false;
        
        final TrackersDataManager dataManager = new TrackersDataManager( this);
        LocationMessage locationMessage = getCurrentLocationMessage( getApplicationContext());
        Log.i( IDroidTrackerConstants.CAUCHY_LOG,
               "     \n\n===================> sendPosition() run():\n"
                       + locationMessage.getMessage());
        if ( sessionOneOffTrackers != null && !sessionOneOffTrackers.isEmpty()) {
            Iterator<Tracker> it = sessionOneOffTrackers.iterator();
            while ( it.hasNext()) {
                Tracker tracker = (Tracker) it.next();
                if ( tracker != null && tracker.isTracking()) {
                    // Send Info
                    sendToTracker( locationMessage, tracker);
                    // Remove as tracker once info has been sent once
                    dataManager.stopTracking( tracker.id);
                }
            }
        }
        sessionOneOffTrackers.clear();
        
        if ( sessionPeriodicTrackersToBeWarned != null
                && !sessionPeriodicTrackersToBeWarned.isEmpty()) {
            Iterator<Tracker> it = sessionPeriodicTrackersToBeWarned.iterator();
            while ( it.hasNext()) {
                Tracker tracker = (Tracker) it.next();
                if ( tracker != null && tracker.isTracking()) {
                    boolean success = sendToTracker( locationMessage, tracker);
                    if ( success) {
                        dataManager.resetCountDownForTrackerId( tracker.id);
                    } else {
                        // Remove as tracker if problem (exception) on last send
                        dataManager.stopTracking( tracker.id);
                    }
                }
            }
        }
        sessionPeriodicTrackersToBeWarned.clear();
        
        dataManager.close();
        
        if ( !isNextTrackingSoon()) {
            stopLocationRequests();
        }
        
        Looper.loop();
    }
    
    private void checkForNotificationNeeds() {
        Log.i( IDroidTrackerConstants.CAUCHY_LOG, "checkForNotificationNeeds()");
        boolean trackingToComeSoon = false;
        final TrackersDataManager dataManager = new TrackersDataManager( TrackingInfoSenderService.this);
        List<Tracker> trackers_list = dataManager.fetchTrackers( true);
        if ( trackers_list != null && !trackers_list.isEmpty()) {
            Iterator<Tracker> it = trackers_list.iterator();
            while ( it.hasNext()) {
                Tracker tracker = (Tracker) it.next();
                if ( tracker != null && tracker.isTracking()) {
                    
                    Log.i( IDroidTrackerConstants.CAUCHY_LOG, "tracker = "
                            + tracker);
                    
                    // First check for one-off trackers
                    if ( tracker.tracking_period == -1) {
                        // If it is is already inside it probably means we are
                        // still waiting for an answer from GPS
                        // when answer will be received, we don't want to send 2
                        // SMS
                        if ( !sessionOneOffTrackers.contains( tracker)) {
                            sessionOneOffTrackers.add( tracker);
                        }
                        trackingToComeSoon = true;
                        continue;
                    }
                    
                    // Then check for periodic ones
                    long decreased_countdown = tracker.tracking_countdown
                            - IDroidTrackerConstants.TRACKING_CHECK_PERIOD_MS;
                    dataManager.setCountDownForTrackerId( tracker.id,
                                                          decreased_countdown);
                    if ( decreased_countdown <= 0) {
                        // If it is is already inside it probably means we are
                        // still waiting for an answer from GPS
                        // when answer will be received, we don't want to send 2
                        // SMS
                        if ( !sessionPeriodicTrackersToBeWarned.contains( tracker)) {
                            sessionPeriodicTrackersToBeWarned.add( tracker);
                        }
                    }
                    if ( decreased_countdown <= NUMBER_OF_PERIODS_FOR_PROVIDERS_TO_WARM_UP
                            * IDroidTrackerConstants.TRACKING_CHECK_PERIOD_MS) {
                        trackingToComeSoon = true;
                    }
                }
            }
        }
        dataManager.close();
        
        if ( trackingToComeSoon && !locationTrackingOn) {
            triggerLocationListeners( getApplicationContext(), 60000L);
        }
        
    }
    
    private boolean isNextTrackingSoon() {
        final TrackersDataManager dataManager = new TrackersDataManager( TrackingInfoSenderService.this);
        List<Tracker> trackers_list = dataManager.fetchTrackers( true);
        dataManager.close();
        if ( trackers_list != null && !trackers_list.isEmpty()) {
            Iterator<Tracker> it = trackers_list.iterator();
            while ( it.hasNext()) {
                Tracker tracker = (Tracker) it.next();
                if ( tracker != null && tracker.isTracking()) {
                    
                    // First check for one-off trackers
                    if ( tracker.tracking_period == -1) {
                        return true;
                    }
                    
                    // Then check for periodic ones
                    if ( tracker.tracking_countdown <= NUMBER_OF_PERIODS_FOR_PROVIDERS_TO_WARM_UP
                            * IDroidTrackerConstants.TRACKING_CHECK_PERIOD_MS) {
                        return true;
                    }
                }
            }
        } else {
            // No trackers left, stop self.
            Log.i( IDroidTrackerConstants.CAUCHY_LOG,
                   " ******* No trackers left, stop self! *******");
            stopSelf();
        }
        return false;
    }
    
    private void stopLocationRequests() {
        
        Log.d( IDroidTrackerConstants.CAUCHY_LOG,
               " --------> Turning off location change listeners.");
        
        // Remove location change listeners
        locationManager.removeUpdates( locationChangesListener);
        
        // // Put Location Provider's states back to where they were when
        // tracking
        // // started
        // resetLocationProvidersToOriginalStateIfNeeded();
        
        locationTrackingOn = false;
    }
    
    private boolean sendToTracker(LocationMessage locationMessage,
                                  Tracker tracker) {
        Log.i( IDroidTrackerConstants.CAUCHY_LOG,
               "TrackingInfoSenderService sending to tracker: " + tracker
                       + ", format = " + tracker.tracking_format);
        // SMS Format
        if ( IDroidTrackerConstants.FORMAT_SMS.equals( tracker.tracking_format)) {
            if ( tracker.number != null) {
                
                try {
                    // sm.sendTextMessage( tracker.number,
                    // null,
                    // locationMessage,
                    // null,
                    // null);
                    
                    String msg = locationMessage.getMessage();
                    
                    if ( locationMessage.getShortLocationURLString() != null) {
                        msg = msg + "\n"
                                + locationMessage.getShortLocationURLString();
                    }
                    // + "("
                    // + locationMessage.getLatitude() + ","
                    // + locationMessage.getLongitude() + ")";
                    
                    final ArrayList<String> divided_message = sm.divideMessage( msg);
                    
                    Log.i( IDroidTrackerConstants.CAUCHY_LOG, "Number of SMS: "
                            + divided_message.size());
                    
                    sm.sendMultipartTextMessage( tracker.number,
                                                 null,
                                                 divided_message,
                                                 null,
                                                 null);
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences( this);
                    boolean display_sent_info = prefs.getBoolean( IDroidTrackerConstants.PREFERENCE_KEY_DISPLAY_SENT_INFO,
                                                                  true);
                    if ( display_sent_info
                            && !tracker.isLostPhoneTrackingActive()) {
                        final String sent_msg_notification_title = getString( R.string.sent_notication_title_prefix)
                                + " " + tracker.name;
                        showNotification( sent_msg_notification_title,
                                          msg,
                                          sent_msg_notification_title);
                    }
                    
                    // Take Picture if set in preferences
                    // TODO: Create Preferences for that + add location and
                    // possibly comments to picture in picasa using sent message
                    if ( tracker.isLostPhoneTrackingActive()) {
                        boolean send_picture_to_picasa = prefs.getBoolean( IDroidTrackerConstants.PREFERENCE_KEY_SEND_PICTURE_TO_PICASA,
                                                                           false);
                        if ( send_picture_to_picasa) {
                            String username = prefs.getString( IDroidTrackerConstants.PREFERENCE_KEY_PICASA_LOGIN,
                                                               null);
                            String passwd = prefs.getString( IDroidTrackerConstants.PREFERENCE_KEY_PICASA_PASSWD,
                                                             null);
                            String albumname = prefs.getString( IDroidTrackerConstants.PREFERENCE_KEY_PICASA_ALBUM_NAME,
                                                                null);
                            
                            if ( username != null &&
                                 username.length() > 0 &&
                                 passwd != null &&
                                 passwd.length() > 0 &&
                                 albumname != null &&
                                 albumname.length() > 0
                                 ) {
                                
                                Intent i = new Intent( this,
                                                       TakeAndPicasaPublishPictureActivity.class);
                                i.putExtra( TakeAndPicasaPublishPictureActivity.KEY_PICASA_LOGIN,
                                            username);
                                i.putExtra( TakeAndPicasaPublishPictureActivity.KEY_PICASA_PASSWORD,
                                            passwd);
                                i.putExtra( TakeAndPicasaPublishPictureActivity.KEY_PICASA_DEST_ALBUM_NAME,
                                            albumname);
                                i.setFlags( Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity( i);
                            }
                        }
                    }
                } catch ( Exception e) {
                    Log.e( IDroidTrackerConstants.CAUCHY_LOG, "     ==> msg: "
                            + locationMessage.getMessage() + " error "
                            + e.getMessage());
                    return false;
                }
            }
        } else if ( IDroidTrackerConstants.FORMAT_MAIL.equals( tracker.tracking_format)) {
            // Mail Format
            Intent msg_intent = new Intent( Intent.ACTION_SEND);
            String[] recipients = { tracker.email };
            
            msg_intent.putExtra( Intent.EXTRA_EMAIL, recipients);
            // msg.putExtra(Intent.EXTRA_CC, carbonCopies);
            
            String msg = getApplicationContext().getString( R.string.result_prefix)
                    + locationMessage.getMessage();
            
            if ( locationMessage.getLongLocationURLString() != null) {
                msg = msg
                        + getApplicationContext().getString( R.string.result_map_url)
                        + locationMessage.getLongLocationURLString();
            }
            
            msg_intent.putExtra( Intent.EXTRA_TEXT, msg);
            msg_intent.putExtra( Intent.EXTRA_SUBJECT,
                                 getString( R.string.mail_subject));
            msg_intent.setType( "message/rfc822");
            Intent chooser_intent = Intent.createChooser( msg_intent,
                                                          getString( R.string.chooser_title));
            chooser_intent.setFlags( Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity( chooser_intent);
        } else if ( DroidTrackerUtils.isTwitter( tracker)) {
            Log.i( IDroidTrackerConstants.CAUCHY_LOG, "SENDING TO TWITTER");
            // final String twitter_msg = getString(
            // R.string.twitter_msg_header) + "" +
            // locationMessage.getShortLocationURLString();
            final String twitter_msg = locationMessage.getTwitterMessage();
            Intent twitter_intent = DroidTrackerUtils.getTweetIntent( getApplicationContext(),
                                                                      twitter_msg);
            twitter_intent.setFlags( Intent.FLAG_ACTIVITY_NEW_TASK);
            Log.i( IDroidTrackerConstants.CAUCHY_LOG, "Twitter intent = "
                    + twitter_intent);
            if ( twitter_intent != null) {
                try {
                    startActivity( twitter_intent);
                } catch ( ActivityNotFoundException e) {
                    // Twidroid is not installed
                    Log.e( IDroidTrackerConstants.CAUCHY_LOG,
                           "Error: Trying to send location to twitter even though twidroid is not installed: "
                                   + e.getMessage());
                    // TODO: Dialog instead of toast in order to get a
                    // onResume() on DroidTracker
                    Toast t = Toast.makeText( getApplicationContext(),
                                              "Cannot Send to Twitter: Twidroid is not installed!",
                                              Toast.LENGTH_SHORT);
                    t.show();
                    return false;
                }
            }
        }
        return true;
    }
    
    protected void showNotification(String notification_title,
                                    String notification_message,
                                    String notification_ticker_text) {
        
        // look up the notification manager service
        NotificationManager nm = (NotificationManager) getSystemService( NOTIFICATION_SERVICE);
        
        // The PendingIntent to launch our activity if the user selects this
        // notification
        Intent notification_displayer_intent = new Intent( IDroidTrackerConstants.DISPLAY_NOTIFICATION_ACTION,
                                                           null,
                                                           this,
                                                           NotificationDisplayerActivity.class);
        notification_displayer_intent.putExtra( IDroidTrackerConstants.KEY_NOTIFICATION_TITLE,
                                                notification_title);
        notification_displayer_intent.putExtra( IDroidTrackerConstants.KEY_NOTIFICATION_MSG,
                                                notification_message);
        notification_displayer_intent.setFlags( Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent contentIntent = PendingIntent.getActivity( this,
                                                                 0,
                                                                 notification_displayer_intent,
                                                                 PendingIntent.FLAG_CANCEL_CURRENT);
        
        // construct the Notification object.
        Notification notif = new Notification( R.drawable.icon,
                                               notification_ticker_text,
                                               System.currentTimeMillis());
        
        // Set the info for the views that show in the notification panel.
        notif.setLatestEventInfo( this,
                                  notification_title,
                                  notification_message,
                                  contentIntent);
        
        notif.flags = Notification.FLAG_AUTO_CANCEL;
        
        // after a 100ms delay, vibrate for 250ms, pause for 100 ms and
        // then vibrate for 500ms.
        notif.vibrate = new long[] { 100, 250, 100, 500 };
        
        nm.notify( IDroidTrackerConstants.SENT_NOTIFICATION_ID, notif);
    }
    
    private LocationMessage getLocationMessage(Location location) {
        return new LocationMessage( location);
    }
    
    private LocationMessage getCurrentLocationMessage(final Context context) {
        
        // Criteria criteria = new Criteria();
        // criteria.setAccuracy(Criteria.ACCURACY_FINE);
        // criteria.setAltitudeRequired(false);
        // criteria.setBearingRequired(false);
        // criteria.setCostAllowed(true);
        // criteria.setPowerRequirement(Criteria.NO_REQUIREMENT);
        //
        // String best_provider = locationManager.getBestProvider(criteria,
        // true);
        // Location best_last_location = locationManager.getLastKnownLocation(
        // best_provider);
        //
        // return getLocationMessage( best_last_location);
        
        Location last_location_gps = null;
        Location last_location_network = null;
        try {
            if ( locationManager.isProviderEnabled( LocationManager.GPS_PROVIDER)) {
                last_location_gps = locationManager.getLastKnownLocation( LocationManager.GPS_PROVIDER);
            }
            
            if ( locationManager.isProviderEnabled( LocationManager.NETWORK_PROVIDER)) {
                last_location_network = locationManager.getLastKnownLocation( LocationManager.NETWORK_PROVIDER);
            }
        } catch ( Exception e) {
            Log.e( IDroidTrackerConstants.CAUCHY_LOG,
                   "Exception while getting locations: " + e.getMessage());
            
        }
        LocationMessage result = null;
        if ( gpsAvailable
                && last_location_gps != null
                && ( last_location_network == null || last_location_network.getTime()
                        - last_location_gps.getTime() < ( NUMBER_OF_PERIODS_FOR_PROVIDERS_TO_WARM_UP * IDroidTrackerConstants.TRACKING_CHECK_PERIOD_MS))) {
            result = getLocationMessage( last_location_gps);
        }
        
        if ( result == null) {
            result = getLocationMessage( last_location_network);
        }
        
        return result;
        
    }
    
    private void triggerLocationListeners(final Context context,
                                          final long period) {
        
        Runnable location_listeners_creator = new Runnable() {
            public void run() {
                Looper.prepare();
                Log.d( IDroidTrackerConstants.CAUCHY_LOG,
                       " --------> Turning on location change listeners.");
                // // Turn on location providers if they're off and the auto
                // // turn-on is set in preferences
                // turnLocationProvidersOnIfNeeded( getApplicationContext());
                gpsAvailable = locationManager.isProviderEnabled( LocationManager.GPS_PROVIDER);
                try {
                    locationManager.requestLocationUpdates( LocationManager.GPS_PROVIDER,
                                                            period,
                                                            0,
                                                            locationChangesListener);
                    
                    locationManager.requestLocationUpdates( LocationManager.NETWORK_PROVIDER,
                                                            period,
                                                            0,
                                                            locationChangesListener);
                } catch ( Exception e) {
                    Log.e( IDroidTrackerConstants.CAUCHY_LOG,
                           "Exception while setting location listeners: "
                                   + e.getMessage());
                }
                locationTrackingOn = true;
                sendingCondition.close();
                Looper.loop();
            }
        };
        Thread t = new Thread( location_listeners_creator,
                               "Location Listeners Init Thread");
        t.start();
    }
    
    // /**
    // * Turn on location providers if they're off and the auto turn-on is set
    // in
    // * preferences.
    // *
    // * @param context
    // */
    // private void turnLocationProvidersOnIfNeeded(final Context context) {
    // boolean auto_turn_providers_on = isProvidersAutoTurnOnOptionOn();
    // Log.i( IDroidTrackerConstants.CAUCHY_LOG, "auto_turn_providers_on = "
    // + auto_turn_providers_on);
    // if ( auto_turn_providers_on) {
    // initialAllowedLocationProviders = Settings.System.getString(
    // context.getContentResolver(),
    // Settings.System.LOCATION_PROVIDERS_ALLOWED);
    // Log.d( IDroidTrackerConstants.CAUCHY_LOG,
    // "TrackingInfoSenderService.turnLocationProvidersOnIfNeeded() active_location_providers = "
    // + initialAllowedLocationProviders);
    // if ( initialAllowedLocationProviders == null
    // || initialAllowedLocationProviders.length() == 0) {
    // Log.d( IDroidTrackerConstants.CAUCHY_LOG,
    // "                                    No location providers! Activating them! ");
    // setAllowedLocationProviders( context,
    // LocationManager.NETWORK_PROVIDER
    // + ","
    // + LocationManager.GPS_PROVIDER);
    // } else {
    // StringBuffer new_providers = new StringBuffer(
    // initialAllowedLocationProviders);
    // if ( !initialAllowedLocationProviders.contains(
    // LocationManager.NETWORK_PROVIDER)) {
    // new_providers.append( ","
    // + LocationManager.NETWORK_PROVIDER);
    // }
    // if ( !initialAllowedLocationProviders.contains(
    // LocationManager.GPS_PROVIDER)) {
    // new_providers.append( "," + LocationManager.GPS_PROVIDER);
    // }
    // if ( new_providers.length() > initialAllowedLocationProviders.length()) {
    // setAllowedLocationProviders( context,
    // new_providers.toString());
    // }
    // }
    // }
    // }
    
    // /**
    // * @return
    // */
    // private boolean isProvidersAutoTurnOnOptionOn() {
    // SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(
    // this);
    // boolean auto_turn_providers_on = prefs.getBoolean(
    // IDroidTrackerConstants.PREFERENCE_KEY_AUTO_TURN_PROVIDERS_ON,
    // true);
    // return auto_turn_providers_on;
    // }
    
    // /**
    // * Put Location Provider's states back to where they were when service
    // * started.
    // */
    // private void resetLocationProvidersToOriginalStateIfNeeded() {
    // boolean auto_turn_providers_on = isProvidersAutoTurnOnOptionOn();
    // if ( auto_turn_providers_on) {
    // String currently_allowed_providers = Settings.System.getString(
    // getApplicationContext().getContentResolver(),
    // Settings.System.LOCATION_PROVIDERS_ALLOWED);
    // // Set back initial allowed location providers only if the user
    // // has not changed them since as we don't want to mess them up if
    // // so.
    // if ( autoSetAllowedLocationProviders != null
    // && autoSetAllowedLocationProviders.equals( currently_allowed_providers))
    // {
    // setAllowedLocationProviders( getApplicationContext(),
    // initialAllowedLocationProviders);
    // }
    // }
    // }
    
    // /**
    // * @param context
    // * @param allowed_location_providers
    // */
    // private void setAllowedLocationProviders(final Context context,
    // String allowed_location_providers) {
    // boolean auto_turn_providers_on = isProvidersAutoTurnOnOptionOn();
    // if ( !auto_turn_providers_on) {
    // return;
    // }
    // Log.d( IDroidTrackerConstants.CAUCHY_LOG,
    // "Setting allowed location providers to: "
    // + allowed_location_providers);
    // Settings.System.putString( context.getContentResolver(),
    // Settings.System.LOCATION_PROVIDERS_ALLOWED,
    // allowed_location_providers);
    // autoSetAllowedLocationProviders = allowed_location_providers;
    // Intent intent = new Intent(
    // "android.intent.action.ACTION_PROVIDER_CHANGED");
    // context.sendBroadcast( intent);
    // }
    
    @Override
    public IBinder onBind(Intent arg0) {
        // TODO Auto-generated method stub
        return null;
    }
    
    private class LocationMessage {
        private String message;
        private String twitterMessage;
        private String latitude;
        private String longitude;
        private String longLocationURLString;
        private String shortLocationURLString;
        
        public LocationMessage(Location location) {
            if ( location == null) {
                message = getString( R.string.location_unknown_msg);
                twitterMessage = getString( R.string.location_unknown_msg);
            } else {
                
                final String location_provider = location.getProvider();
                latitude = String.valueOf( location.getLatitude());
                longitude = String.valueOf( location.getLongitude());
                
                String addressFromGeoCode = geodecoder.getAddressFromGeoCode( latitude,
                                                                              longitude);
                String url_encoded_address_part = "";
                
                Date result_date = new Date( location.getTime());
                DateFormat f = DateFormat.getDateTimeInstance( DateFormat.SHORT,
                                                               DateFormat.SHORT);
                
                StringBuffer result = new StringBuffer();
                result.append( getApplicationContext().getString( R.string.result_source));
                result.append( " ");
                result.append( location_provider);
                result.append( " ");
                result.append( getApplicationContext().getString( R.string.result_when));
                result.append( " ");
                result.append( f.format( result_date));
                
                StringBuffer twitter_result = new StringBuffer();
                twitter_result.append( getApplicationContext().getString( R.string.result_when));
                twitter_result.append( " ");
                twitter_result.append( f.format( result_date));
                twitter_result.append( ", ");
                twitter_result.append( getString( R.string.twitter_msg_header));
                
                if ( addressFromGeoCode == null) {
                    result.append( "\n"
                            + getString( R.string.location_no_translation_msg));
                    
                    twitter_result.append( " ");
                    twitter_result.append( getString( R.string.location_no_translation_msg));
                } else {
                    
                    result.append( getApplicationContext().getString( R.string.result_where));
                    result.append( addressFromGeoCode);
                    
                    result.append( getApplicationContext().getString( R.string.result_geocode));
                    
                    url_encoded_address_part = "+("
                            + getURLEncodedAddress( addressFromGeoCode) + ")";
                    
                    twitter_result.append( " ");
                    twitter_result.append( addressFromGeoCode);
                }
                message = result.toString();
                twitterMessage = twitter_result.toString();
                
                // Get Google Map URLs
                this.longLocationURLString = "http://maps.google.com/maps?f=q&source=s_q&hl=en&geocode=&q="
                        + latitude
                        + ","
                        + longitude
                        + url_encoded_address_part
                        + "&sll="
                        + latitude
                        + ","
                        + longitude
                        + "&g="
                        + latitude
                        + ","
                        + longitude
                        + url_encoded_address_part
                        + "&ie=UTF8&t=h&z=16&iwloc=addr";
                this.shortLocationURLString = "http://maps.google.com/maps?q="
                        + latitude + "," + longitude;
            }
        }
        
        private String getURLEncodedAddress(String addressFromGeoCode) {
            addressFromGeoCode.replace( " ", "+");
            return URLEncoder.encode( addressFromGeoCode);
        }
        
        public String getMessage() {
            return message;
        }
        
        public String getTwitterMessage() {
            return twitterMessage;
        }
        
        public String getLatitude() {
            return latitude;
        }
        
        public String getLongitude() {
            return longitude;
        }
        
        public String getLongLocationURLString() {
            return longLocationURLString;
        }
        
        public String getShortLocationURLString() {
            return shortLocationURLString;
        }
    }
}
