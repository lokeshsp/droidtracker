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

public interface IDroidTrackerConstants {

    public static final String CAUCHY_LOG = "[DroidTracker]";
    
    public static final long TRACKING_CHECK_PERIOD_MS = 60 * 1000; // 1 minute
    
    public static final String KEY_MSG_TO = "msg_to";
    public static final String KEY_MSG_DISPLAY_TO = "msg_display_to";
    public static final String KEY_MSG_BODY = "msg_body";
    public static final String KEY_TRACKER_ID = "tracker_id";
    public static final String KEY_LOSTPHONE_TRACKING = "lostphone_tracking";
    public static final String KEY_IS_MANUAL_START = "manual_start";
    public static final String KEY_PERIOD = "tracking_period";
    public static final String KEY_FORMAT = "tracking_format";

    public static final String KEY_NOTIFICATION_TITLE = "KEY_NOTIFICATION_TITLE";
    public static final String KEY_NOTIFICATION_MSG = "KEY_NOTIFICATION_MSG";
    
    public static final String HANDLE_SMS_ACTION = "HANDLE_SMS_ACTION";
    public static final String START_TRACKING_ACTION = "START_TRACKING_ACTION";
    
    public static final String PREFERENCES_ACTION = "PREFERENCES_ACTION";
    public static final String HELP_ABOUT_ACTION = "HELP_ABOUT_ACTION";
    public static final String HELP_ACTION = "HELP_ACTION";
    public static final String ABOUT_ACTION = "ABOUT_ACTION";
    public static final String FAQ_ACTION = "FAQ_ACTION";
    public static final String DISPLAY_NOTIFICATION_ACTION = "DISPLAY_NOTIFI_ACTION";
    
    public static final String FORMAT_SMS = "SMS";
    public static final String FORMAT_MAIL = "MAIL";
    public static final String FORMAT_TWITTER = "TWITTER";

    public static final String PREFERENCE_KEY_SMS_START_PASSPHRASE = "PREFERENCE_KEY_SMS_START_PASSPHRASE";
    public static final String PREFERENCE_KEY_SMS_STOP_PASSPHRASE = "PREFERENCE_KEY_SMS_STOP_PASSPHRASE";
    public static final String PREFERENCE_KEY_ACTIVATE_LOST_PHONE_MODE = "PREFERENCE_KEY_ACTIVATE_LOST_PHONE_MODE";
    public static final String PREFERENCE_KEY_SMS_LOSTPHONE_PASSPHRASE = "PREFERENCE_KEY_SMS_LOSTPHONE_PASSPHRASE";
    public static final String PREFERENCE_KEY_TRACKING_CONFIRMATION_REQUESTED = "PREFERENCE_KEY_TRACKING_CONFIRMATION_REQUESTED";
    public static final String PREFERENCE_KEY_DISPLAY_SENT_INFO = "PREFERENCE_KEY_DISPLAY_SENT_INFO";
    public static final String PREFERENCE_KEY_SMS_PERIOD_PASSPHRASE = "PREFERENCE_KEY_SMS_PERIOD_PASSPHRASE";
    public static final String PREFERENCE_KEY_DISPLAY_SETTINGS_FOR_SINGLE_PROVIDER = "PREFERENCE_KEY_DISPLAY_SETTINGS_FOR_SINGLE_PROVIDER";
    public static final String PREFERENCE_KEY_SEND_PICTURE_TO_PICASA = "PREFERENCE_KEY_SEND_PICTURE_TO_PICASA";
    public static final String PREFERENCE_KEY_PICASA_LOGIN = "PREFERENCE_KEY_PICASA_LOGIN";
    public static final String PREFERENCE_KEY_PICASA_PASSWD = "PREFERENCE_KEY_PICASA_PASSWD";
    public static final String PREFERENCE_KEY_PICASA_ALBUM_NAME = "PREFERENCE_KEY_PICASA_ALBUM_NAME";

    public static final String PERIOD_PASSPHRASE_SEPARATOR = " {x} ";

    public static final int SENT_NOTIFICATION_ID = 0;
    

    public static final String ABOUT_TAG = "ABOUT_TAG";
    public static final String HELP_TAG = "HELP_TAG";
    public static final String FAQ_TAG = "FAQ_TAG";

    public static final String TWITTER_TRACKER_NAME = "Twitter";

    public static final String TMP_PICTURE_FILE_NAME = "droidtracker_snap.jpg";

    public static final String PICTURE_TO_UPLOAD = "PICTURE_TO_UPLOAD";

    public static final String SHARED_PREFERENCES_KEY_MAIN = "DroidTrackerPrefs";



    
}
