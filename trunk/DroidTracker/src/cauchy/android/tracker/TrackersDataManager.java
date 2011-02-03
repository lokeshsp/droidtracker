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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.telephony.PhoneNumberUtils;
import android.util.Log;

public class TrackersDataManager {
    public static final int STATE_NOT_TRACKING = 0;
    public static final int STATE_TRACKING = 1;
    
    public static final int TYPE_NORMAL = 0;
    public static final int TYPE_TEMPORARY_FOR_LOSTPHONE_MODE = 1;
    public static final int TYPE_OTHER_APPLICATION = 2;
    
    public static final int LOST_PHONE_TRACKING_OFF = 0;
    public static final int LOST_PHONE_TRACKING_ON = 1;
    
    private static final String DATABASE_NAME = "trackersdb";
    
    private static final String DATABASE_TABLE = "trackersdata";
    
    private static final int DATABASE_VERSION = 5;
    
    private static final String DATABASE_CREATE = "create table "
            + DATABASE_TABLE + " (tracker_id integer primary key, "
            + "tracker_name text not null, " + "tracker_number text key, "
            + "tracker_email text not null, " + "tracking_state integer, "
            + "tracking_period integer, " + "tracking_countdown integer, "
            + "tracking_format text not null, "
            + "number_lookup text not null, " + "tracker_type integer, "
            + "lostphone_tracking_state integer" + ");";
    
    private SQLiteDatabase db;
    private DatabaseHelper dbHelper;
    
    private static class DatabaseHelper extends SQLiteOpenHelper {
        
        DatabaseHelper(Context context) {
            super( context, DATABASE_NAME, null, DATABASE_VERSION);
        }
        
        @Override
        public void onCreate(SQLiteDatabase db) {
            
            db.execSQL( DATABASE_CREATE);
        }
        
        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w( IDroidTrackerConstants.CAUCHY_LOG,
                   "Upgrading database from version " + oldVersion + " to "
                           + newVersion + ", which will destroy all old data");
            db.execSQL( "DROP TABLE IF EXISTS " + DATABASE_TABLE);
            onCreate( db);
        }
    }
    
    public TrackersDataManager(Context ctx) {
        dbHelper = new DatabaseHelper( ctx);
        db = dbHelper.getWritableDatabase();
    }
    
    public void close() {
        db.close();
    }
    
    public boolean isClosed() {
        return !db.isOpen();
    }
    
    public void addTracker(long l_id, String name, String number, String email) {
        Log.i( IDroidTrackerConstants.CAUCHY_LOG, "Adding Tracker " + l_id
                + "/" + name + "/" + number + " / " + email);
        ContentValues initialValues = new ContentValues();
        initialValues.put( "tracker_id", l_id);
        initialValues.put( "tracker_name", name);
        initialValues.put( "tracker_number",
                           PhoneNumberUtils.stripSeparators( number));
        initialValues.put( "tracker_email", email);
        initialValues.put( "tracking_state", STATE_NOT_TRACKING);
        initialValues.put( "tracking_period", 0);
        initialValues.put( "tracking_countdown", 0);
        initialValues.put( "tracking_format", IDroidTrackerConstants.FORMAT_SMS);
        initialValues.put( "number_lookup",
                           PhoneNumberUtils.getStrippedReversed( number));
        initialValues.put( "tracker_type", TYPE_NORMAL);
        initialValues.put( "lostphone_tracking_state", LOST_PHONE_TRACKING_OFF);
        db.insert( DATABASE_TABLE, null, initialValues);
    }
    
    public void addTemporaryTracker(String number) {
        Log.i( IDroidTrackerConstants.CAUCHY_LOG, "Adding TemporaryTracker: "
                + number);
        ContentValues initialValues = new ContentValues();
        initialValues.put( "tracker_id", getNewTemporaryTrackerID());
        initialValues.put( "tracker_name", number);
        initialValues.put( "tracker_number",
                           PhoneNumberUtils.stripSeparators( number));
        initialValues.put( "tracker_email", "");
        initialValues.put( "tracking_state", STATE_NOT_TRACKING);
        initialValues.put( "tracking_period", 0);
        initialValues.put( "tracking_countdown", 0);
        initialValues.put( "tracking_format", IDroidTrackerConstants.FORMAT_SMS);
        initialValues.put( "number_lookup",
                           PhoneNumberUtils.getStrippedReversed( number));
        initialValues.put( "tracker_type", TYPE_TEMPORARY_FOR_LOSTPHONE_MODE);
        initialValues.put( "lostphone_tracking_state", LOST_PHONE_TRACKING_OFF);
        db.insert( DATABASE_TABLE, null, initialValues);
    }
    
    public void addTwitterTracker() {
        Log.i( IDroidTrackerConstants.CAUCHY_LOG, "Adding Twitter Tracker");
        ContentValues initialValues = new ContentValues();
        initialValues.put( "tracker_id", getNewTemporaryTrackerID());
        initialValues.put( "tracker_name",
                           IDroidTrackerConstants.TWITTER_TRACKER_NAME);
        initialValues.put( "tracker_number", "0");
        initialValues.put( "tracker_email", "");
        initialValues.put( "tracking_state", STATE_NOT_TRACKING);
        initialValues.put( "tracking_period", 0);
        initialValues.put( "tracking_countdown", 0);
        initialValues.put( "tracking_format",
                           IDroidTrackerConstants.FORMAT_TWITTER);
        initialValues.put( "number_lookup", "0");
        initialValues.put( "tracker_type", TYPE_OTHER_APPLICATION);
        initialValues.put( "lostphone_tracking_state", LOST_PHONE_TRACKING_OFF);
        db.insert( DATABASE_TABLE, null, initialValues);
    }
    
    private long getNewTemporaryTrackerID() {
        Log.d( IDroidTrackerConstants.CAUCHY_LOG,
               "getNewTemporaryTrackerID start.");
        int result = -1;
        while ( fetchTrackerById( result) != null) {
            result--;
        }
        Log.d( IDroidTrackerConstants.CAUCHY_LOG,
               "getNewTemporaryTrackerID returns: " + result);
        return result;
    }
    
    public void updateTracker(long id, String name, String number, String email) {
        ContentValues args = new ContentValues();
        args.put( "tracker_name", name);
        args.put( "tracker_number", number);
        args.put( "tracker_email", email);
        db.update( DATABASE_TABLE, args, "tracker_id=" + id, null);
    }
    
    public void deleteTracker(long id) {
        db.delete( DATABASE_TABLE, "tracker_id=" + id, null);
    }
    
    public List<Tracker> fetchTrackers(boolean tracking_only) {
        ArrayList<Tracker> ret = new ArrayList<Tracker>();
        try {
            Cursor c = db.query( DATABASE_TABLE, new String[] { "tracker_id",
                    "tracker_name", "tracker_number", "tracker_email",
                    "tracking_state", "tracking_period", "tracking_countdown",
                    "tracking_format", "number_lookup", "tracker_type",
                    "lostphone_tracking_state" }, null, null, null, null, null);
            int numRows = c.getCount();
            c.moveToFirst();
            for ( int i = 0; i < numRows; ++i) {
                Tracker tracker = new Tracker();
                tracker.id = c.getLong( 0);
                tracker.name = c.getString( 1);
                tracker.number = c.getString( 2);
                tracker.email = c.getString( 3);
                tracker.tracking_state = c.getLong( 4);
                tracker.tracking_period = c.getLong( 5);
                tracker.tracking_countdown = c.getLong( 6);
                tracker.tracking_format = c.getString( 7);
                tracker.tracker_type = c.getLong( 9);
                tracker.lostphone_tracking_state = c.getLong( 10);
                if ( !tracking_only || tracker.tracking_state == STATE_TRACKING) {
                    ret.add( tracker);
                }
                c.moveToNext();
            }
            c.close();
        } catch ( SQLException e) {
            Log.println( Log.ERROR,
                         IDroidTrackerConstants.CAUCHY_LOG,
                         "Error fetching all trackers: " + e.toString());
        }
        return ret;
    }
    
    public Tracker fetchTrackerById(long id) {
        return fetchTracker( "tracker_id=" + id);
    }
    
    public Tracker fetchTrackerByName(String name) {
        return fetchTracker( "tracker_name=\"" + name + "\"");
    }
    
    public Tracker fetchTrackerByNumber(String number) {
        return fetchTracker( "tracker_number=\""
                + PhoneNumberUtils.stripSeparators( number) + "\"");
    }
    
    public Tracker fetchTrackerByNumberUsingCallerIDMinMatch(String number) {
        List<Tracker> matching_trackers = fetchTrackers( "number_lookup LIKE \""
                + PhoneNumberUtils.toCallerIDMinMatch( number) + "%\"");
        if ( matching_trackers == null || matching_trackers.isEmpty()) {
            return null;
        } else if ( matching_trackers.size() == 1) {
            // Only one match, returning it
            Log.d( IDroidTrackerConstants.CAUCHY_LOG,
                   "Only one match, returning it: " + matching_trackers);
            return matching_trackers.get( 0);
        } else {
            // Several matches, we look for the best one
            Log.d( IDroidTrackerConstants.CAUCHY_LOG,
                   "Several matches, we look for the best one: "
                           + matching_trackers);
            Tracker match = null;
            Iterator<Tracker> it = matching_trackers.iterator();
            while ( it.hasNext()) {
                Tracker tracker = (Tracker) it.next();
                if ( PhoneNumberUtils.compare( number, tracker.number)) {
                    return tracker;
                }
            }
            return match;
        }
    }
    
    private List<Tracker> fetchTrackers(String criterion) {
        ArrayList<Tracker> matching_trackers = new ArrayList<Tracker>();
        // Tracker tracker = new Tracker();
        Cursor c = db.query( DATABASE_TABLE, new String[] { "tracker_id",
                "tracker_name", "tracker_number", "tracker_email",
                "tracking_state", "tracking_period", "tracking_countdown",
                "tracking_format", "number_lookup", "tracker_type",
                "lostphone_tracking_state" }, criterion, null, null, null, null);
        int numRows = c.getCount();
        Tracker tracker = null;
        c.moveToFirst();
        for ( int i = 0; i < numRows; ++i) {
            tracker = new Tracker();
            tracker.id = c.getLong( 0);
            tracker.name = c.getString( 1);
            tracker.number = c.getString( 2);
            tracker.email = c.getString( 3);
            tracker.tracking_state = c.getLong( 4);
            tracker.tracking_period = c.getLong( 5);
            tracker.tracking_countdown = c.getLong( 6);
            tracker.tracking_format = c.getString( 7);
            tracker.tracker_type = c.getLong( 9);
            tracker.lostphone_tracking_state = c.getLong( 10);
            matching_trackers.add( tracker);
            c.moveToNext();
        }
        c.close();
        return matching_trackers;
    }
    
    private Tracker fetchTracker(String criterion) {
        Tracker tracker = new Tracker();
        Cursor c = db.query( DATABASE_TABLE, new String[] { "tracker_id",
                "tracker_name", "tracker_number", "tracker_email",
                "tracking_state", "tracking_period", "tracking_countdown",
                "tracking_format", "number_lookup", "tracker_type",
                "lostphone_tracking_state" }, criterion, null, null, null, null);
        if ( c.getCount() > 0) {
            c.moveToFirst();
            tracker.id = c.getLong( 0);
            tracker.name = c.getString( 1);
            tracker.number = c.getString( 2);
            tracker.email = c.getString( 3);
            tracker.tracking_state = c.getLong( 4);
            tracker.tracking_period = c.getLong( 5);
            tracker.tracking_countdown = c.getLong( 6);
            tracker.tracking_format = c.getString( 7);
            tracker.tracker_type = c.getLong( 9);
            tracker.lostphone_tracking_state = c.getLong( 10);
            c.close();
            return tracker;
        }
        c.close();
        return null;
    }
    
    public void startTracking(long id,
                              long period,
                              String format,
                              boolean is_lost_phone_mode) {
        Log.i( IDroidTrackerConstants.CAUCHY_LOG, "-------> START TRACKING:");
        Log.i( IDroidTrackerConstants.CAUCHY_LOG, "         id     = " + id);
        Log.i( IDroidTrackerConstants.CAUCHY_LOG, "         period = " + period);
        Log.i( IDroidTrackerConstants.CAUCHY_LOG, "         format = " + format);
        // MAIL and TWITTER format are available for one time tracking only:
        if ( period != -1
                && ( IDroidTrackerConstants.FORMAT_MAIL.equals( format) || IDroidTrackerConstants.FORMAT_TWITTER.equals( format))) {
            Log.i( IDroidTrackerConstants.CAUCHY_LOG,
                   "         FORCING FORMAT TO SMS !!!= ");
            format = IDroidTrackerConstants.FORMAT_SMS;
        }
        
        int lost_phone_mode = ( is_lost_phone_mode) ? LOST_PHONE_TRACKING_ON
                : LOST_PHONE_TRACKING_OFF;
        updateTrackersState( id, STATE_TRACKING, period, format);
        updateLostPhoneModeState( id, lost_phone_mode);
    }
    
    public void stopTracking(long id) {
        Log.i( IDroidTrackerConstants.CAUCHY_LOG,
               "-------> STOP TRACKING for id:" + id);
        // Check if this was a lost phone tracking only tracker. If so
        // delete this temporary tracker
        Tracker t = fetchTrackerById( id);
        if ( t != null) {
            updateTrackersState( id, STATE_NOT_TRACKING, 0, t.tracking_format);
            if ( t.isTemporaryLostPhoneTracker()) {
                deleteTracker( id);
            }
        }
    }
    
    public void setCountDownForTrackerId(long id, long new_countdown_value) {
        ContentValues args = new ContentValues();
        // Removes the tracking check period from the countdown (in ms)
        args.put( "tracking_countdown", new_countdown_value);
        db.update( DATABASE_TABLE, args, "tracker_id=" + id, null);
    }
    
    public void resetCountDownForTrackerId(long id) {
        Tracker t = fetchTrackerById( id);
        ContentValues args = new ContentValues();
        args.put( "tracking_countdown", t.tracking_period);
        db.update( DATABASE_TABLE, args, "tracker_id=" + id, null);
    }
    
    private void updateTrackersState(long id,
                                     long tracking_state,
                                     long tracking_period,
                                     String tracking_format) {
        ContentValues args = new ContentValues();
        args.put( "tracking_state", tracking_state);
        args.put( "tracking_format", tracking_format);
        args.put( "tracking_period", tracking_period);
        // First time coutdown is set to one minute in order for the info to be
        // send straight awayn on first time.
        args.put( "tracking_countdown",
                  IDroidTrackerConstants.TRACKING_CHECK_PERIOD_MS);
        db.update( DATABASE_TABLE, args, "tracker_id=" + id, null);
    }
    
    private void updateLostPhoneModeState(long id, int lost_phone_mode) {
        ContentValues args = new ContentValues();
        args.put( "lostphone_tracking_state", lost_phone_mode);
        db.update( DATABASE_TABLE, args, "tracker_id=" + id, null);
    }
    
    public void stopAllTracking() {
        List<Tracker> trackers = fetchTrackers( true);
        Iterator<Tracker> it = trackers.iterator();
        while ( it.hasNext()) {
            Tracker tracker = (Tracker) it.next();
            stopTracking( tracker.id);
        }
        
    }
    
}
