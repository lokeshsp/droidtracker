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

import java.io.ByteArrayInputStream;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PictureDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Contacts;
import android.provider.Contacts.People;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.AdapterView.OnItemSelectedListener;

public class DroidTracker extends ListActivity implements
                                              IDroidTrackerConstants {
    
    private static final int MENU_NEW_TRACKER_ID = Menu.FIRST;
    private static final int MENU_STOP_ALL_TRACKING_ID = MENU_NEW_TRACKER_ID + 1;
    private static final int MENU_SETTINGS = MENU_STOP_ALL_TRACKING_ID + 1;
    private static final int MENU_ABOUT = MENU_SETTINGS + 1;
    
    private static final int PICK_CONTACT_REQUEST = 0;
    private static final int CREATE_AND_PICK_CONTACT_REQUEST = 1;
    
    private static final int DIALOG_STOP_TRACKING = 0;
    private static final int DIALOG_IDLE_TRACKER_MENU = 1;
    private static final int DIALOG_IDLE_TWITTER_TRACKER_MENU = 2;
    private static final int DIALOG_START_TRACKING = 3;
    private static final int DIALOG_NEW_TRACKER = 4;
    
    private boolean showTrackingOnlyMode = false;
    
    private TrackersDataManager dataManager;
    private long selectedTrackerId = -1;
    private SharedPreferences mPrefs;
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate( icicle);
        setContentView( R.layout.main);
        Spinner spinner = (Spinner) this.findViewById( R.id.mode_spinner);
        
        ArrayAdapter<String> spinner_adapter = new ArrayAdapter<String>( this,
                                                                         android.R.layout.simple_spinner_item);
        spinner_adapter.setDropDownViewResource( android.R.layout.simple_spinner_dropdown_item);
        
        spinner_adapter.add( getString( R.string.all_trackers_label));
        spinner_adapter.add( getString( R.string.tracking_only_label));
        spinner.setAdapter( spinner_adapter);
        
        Tracker.setPeriodPassPhraseMessageElements( getString( R.string.tracker_periodicity_prefix),
                                                    getString( R.string.tracker_periodicity_suffix));
        
        dataManager = new TrackersDataManager( this);
        fillTrackersList();
        
        OnItemSelectedListener l = new OnItemSelectedListener() {
            
            @SuppressWarnings("unchecked")
            public void onNothingSelected(AdapterView arg0) {
                showTrackingOnlyMode = false;
            }
            
            @SuppressWarnings("unchecked")
            public void onItemSelected(AdapterView parent,
                                       View v,
                                       int position,
                                       long id) {
                showTrackingOnlyMode = ( position == 1);
                fillTrackersList();
            }
        };
        spinner.setOnItemSelectedListener( l);
        
        mPrefs = getSharedPreferences( IDroidTrackerConstants.SHARED_PREFERENCES_KEY_MAIN, MODE_PRIVATE);
        selectedTrackerId = mPrefs.getLong( "selectedTrackerId", -1);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        dataManager.close();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        fillTrackersList();
    }
    
    /**
     * Update trackers list from db.
     */
    private void fillTrackersList() {
        Tracker twitter_tracker = dataManager.fetchTrackerByName( IDroidTrackerConstants.TWITTER_TRACKER_NAME);
        if ( DroidTrackerUtils.isTwitterClientInstalled( getApplicationContext())) {
            if ( twitter_tracker == null) {
                dataManager.addTwitterTracker();
            }
        } else {
            if ( twitter_tracker != null
                    && DroidTrackerUtils.isTwitter( twitter_tracker)) {
                dataManager.deleteTracker( twitter_tracker.id);
            }
        }
        List<Tracker> all_trackers = dataManager.fetchTrackers( showTrackingOnlyMode);
        // ArrayAdapter<Tracker> adapter = new ArrayAdapter<Tracker>( this,
        // R.layout.contacts,
        // all_trackers);
        TrackersAdapter adapter = new TrackersAdapter( this,
                                                       R.layout.contacts,
                                                       all_trackers);
        setListAdapter( adapter);
    }
    
    private class TrackersAdapter extends ArrayAdapter<Tracker> {
        
        private List<Tracker> trackers;
        
        public TrackersAdapter(Context context,
                               int textViewResourceId,
                               List<Tracker> items) {
            super( context, textViewResourceId, items);
            this.trackers = items;
        }
        
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if ( v == null) {
                LayoutInflater vi = (LayoutInflater) getSystemService( Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate( R.layout.contacts, null);
            }
            
            Tracker t = trackers.get( position);
            if ( t != null) {
                ImageView iv = (ImageView) v.findViewById( R.id.trackericon);
                if ( iv != null) {
                    if ( DroidTrackerUtils.isTwitter( t)) {
                        iv.setImageResource( R.drawable.twitter_small);
                    } else {
                        final Drawable contactImage = getContactImage( t.id);
                        if ( contactImage != null) {
                            iv.setImageDrawable( contactImage);
                        } else {
                            iv.setImageResource( R.drawable.user_small);
                            // iv.setImageResource( R.drawable.empty_small);
                        }
                    }
                }
                TextView tv = (TextView) v.findViewById( R.id.contact);
                if ( tv != null) {
                    tv.setText( t.toString());
                }
            }
            
            return v;
        }
    }
    
    private Drawable getContactImage(long tracker_id) {
        Uri mContacts = Contacts.Photos.CONTENT_URI;
        
        String[] picture_projection = new String[] {
                Contacts.PhotosColumns.PERSON_ID, Contacts.PhotosColumns.DATA };
        
        Cursor c = this.managedQuery( mContacts,
                                      picture_projection,
                                      Contacts.PhotosColumns.PERSON_ID + "=\'"
                                              + tracker_id + "\'",
                                      null,
                                      Contacts.PhotosColumns.PERSON_ID + " ASC");
        int picture_col = c.getColumnIndex( Contacts.PhotosColumns.DATA);
        int person_col = c.getColumnIndex( Contacts.PhotosColumns.PERSON_ID);
        System.out.println( "DroidTracker.getContactImage() picture_col/person_col/c.getCount() = "
                + picture_col + " / " + person_col + "/" + c.getCount());
        if ( c.getCount() > 0) {
            c.moveToFirst();
            try {
                byte[] test = c.getBlob( picture_col);
                ByteArrayInputStream is = new ByteArrayInputStream( test);
                return PictureDrawable.createFromStream( is, "contact_pic");
            } catch ( Exception e) {
                System.err.println( "DroidTracker.getContactImage(), error = "
                        + e.getMessage());
            }
        }
        return null;
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean result = super.onCreateOptionsMenu( menu);
        menu.add( Menu.NONE,
                  MENU_NEW_TRACKER_ID,
                  Menu.NONE,
                  R.string.new_tracker_menu).setIcon( R.drawable.new_tracker);
        menu.add( Menu.NONE,
                  MENU_STOP_ALL_TRACKING_ID,
                  Menu.NONE,
                  R.string.stop_tracking_menu)
            .setIcon( R.drawable.stop_tracking);
        menu.add( Menu.NONE, MENU_SETTINGS, Menu.NONE, R.string.settings_menu)
            .setIcon( R.drawable.settings);
        menu.add( Menu.NONE, MENU_ABOUT, Menu.NONE, R.string.about_menu)
            .setIcon( R.drawable.about);
        return result;
    }
    
    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        final Tracker selected_tracker = ( getSelectedItemPosition() < 0) ? null
                : (Tracker) getListAdapter().getItem( getSelectedItemPosition());
        // changed from removed item.getId()
        switch ( item.getItemId()) {
            case MENU_NEW_TRACKER_ID:
                showDialog( DIALOG_NEW_TRACKER);
                break;
            case MENU_STOP_ALL_TRACKING_ID:
                if ( selected_tracker != null) {
                    dataManager.stopAllTracking();
                    fillTrackersList();
                }
                break;
            case MENU_SETTINGS:
                Intent settings_intent = new Intent( IDroidTrackerConstants.PREFERENCES_ACTION,
                                                     null,
                                                     this,
                                                     TrackingPreferences.class);
                startActivity( settings_intent);
                break;
            case MENU_ABOUT:
                Intent about_intent = new Intent( IDroidTrackerConstants.HELP_ABOUT_ACTION,
                                                  null,
                                                  this,
                                                  HelpAboutActivity.class);
                startActivity( about_intent);
                break;
            default:
                break;
        }
        return super.onMenuItemSelected( featureId, item);
    }
    
    protected void onListItemClick(ListView listview, View view, int i, long l1) {
        super.onListItemClick( listview, view, i, l1);
        final Tracker selected_tracker = (Tracker) getListAdapter().getItem( i);
        if ( selected_tracker != null) {
            this.selectedTrackerId = selected_tracker.id;
            // Updates Tracker to make sure having the latest state
            // TODO: Try using Id only to define trackers
            final Tracker updated_tracker = dataManager.fetchTrackerById( selectedTrackerId);
            if ( updated_tracker.tracking_state == TrackersDataManager.STATE_TRACKING) {
                showDialog( DIALOG_STOP_TRACKING);
            } else if ( DroidTrackerUtils.isTwitter( updated_tracker)) {
                showDialog( DIALOG_IDLE_TWITTER_TRACKER_MENU);
            } else {
                showDialog( DIALOG_IDLE_TRACKER_MENU);
            }
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        
        SharedPreferences.Editor ed = mPrefs.edit();
        ed.putLong( "selectedTrackerId", selectedTrackerId);
        ed.commit();
    }
    
    @Override
    protected Dialog onCreateDialog(int id) {
        final AlertDialog.Builder alert_builder = new AlertDialog.Builder( DroidTracker.this);
        switch ( id) {
            case DIALOG_STOP_TRACKING:
                // alert_builder.setIcon( R.drawable.stop_tracking);
                setDialogIconFromSelectedTracker( alert_builder);
                alert_builder.setTitle( R.string.stop_tracking_dialog_body);
                alert_builder.setPositiveButton( R.string.ok_button_label,
                                                 new DialogInterface.OnClickListener() {
                                                     public void onClick(DialogInterface dialog,
                                                                         int whichButton) {
                                                         final Tracker selectedTracker = dataManager.fetchTrackerById( selectedTrackerId);
                                                         if ( selectedTracker != null) {
                                                             dataManager.stopTracking( selectedTracker.id);
                                                             fillTrackersList();
                                                         }
                                                     }
                                                 });
                alert_builder.setNegativeButton( R.string.cancel_button_label,
                                                 new DialogInterface.OnClickListener() {
                                                     public void onClick(DialogInterface dialog,
                                                                         int whichButton) {
                                                     }
                                                 });
                return alert_builder.create();
            case DIALOG_IDLE_TWITTER_TRACKER_MENU:
                alert_builder.setIcon( R.drawable.twitter);
                alert_builder.setTitle( R.string.idle_twitter_tracker_dialog_title);
                alert_builder.setItems( R.array.twitter_tracker_actions_dialog_items,
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog,
                                                                int which) {
                                                final Tracker selectedTracker = dataManager.fetchTrackerById( selectedTrackerId);
                                                switch ( which) {
                                                    case 0:
                                                        if ( selectedTracker != null) {
                                                            // TrackingRequestHandler.startSendingTrackingInfo(
                                                            // DroidTracker.this,
                                                            // selectedTracker.id,
                                                            // selectedTracker.number,
                                                            // -1,
                                                            // IDroidTrackerConstants.FORMAT_TWITTER,
                                                            // false);
                                                            startSendingTrackingInfo( selectedTracker.id,
                                                                                      selectedTracker.number,
                                                                                      -1,
                                                                                      IDroidTrackerConstants.FORMAT_TWITTER,
                                                                                      false);
                                                        }
                                                        break;
                                                    default:
                                                        break;
                                                }
                                            }
                                        });
                return alert_builder.create();
            case DIALOG_IDLE_TRACKER_MENU:
                setDialogIconFromSelectedTracker( alert_builder);
                alert_builder.setTitle( getText( R.string.idle_tracker_dialog_title)
                        + " " + getSelectedTrackerName());
                alert_builder.setItems( R.array.tracker_actions_dialog_items,
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog,
                                                                int which) {
                                                
                                                final Tracker selectedTracker = dataManager.fetchTrackerById( selectedTrackerId);
                                                switch ( which) {
                                                    case 0:
                                                        if ( selectedTracker != null) {
                                                            showDialog( DIALOG_START_TRACKING);
                                                        }
                                                        break;
                                                    case 1:
                                                        if ( selectedTracker != null) {
                                                            dataManager.deleteTracker( selectedTracker.id);
                                                            fillTrackersList();
                                                        }
                                                        break;
                                                    case 2:
                                                        if ( selectedTracker != null) {
                                                            updateTrackerFromID( selectedTracker.id);
                                                        }
                                                        break;
                                                    default:
                                                        break;
                                                }
                                            }
                                        });
                return alert_builder.create();
            case DIALOG_START_TRACKING:
                LayoutInflater factory = LayoutInflater.from( this);
                final View textEntryView = factory.inflate( R.layout.tracking_params_input,
                                                            null);
                final TextView period_prefix = (TextView) textEntryView.findViewById( R.id.period_prefix);
                final TextView ms_period_view = (TextView) textEntryView.findViewById( R.id.ms_period);
                final TextView period_suffix = (TextView) textEntryView.findViewById( R.id.period_suffix);
                final Spinner spinner = (Spinner) textEntryView.findViewById( R.id.period_spinner);
                OnItemSelectedListener l = new OnItemSelectedListener() {
                    
                    @SuppressWarnings("unchecked")
                    public void onNothingSelected(AdapterView arg0) {
                    }
                    
                    @SuppressWarnings("unchecked")
                    public void onItemSelected(AdapterView parent,
                                               View v,
                                               int position,
                                               long id) {
                        int visibility = ( position < 2) ? TextView.GONE
                                : TextView.VISIBLE;
                        period_prefix.setVisibility( visibility);
                        ms_period_view.setVisibility( visibility);
                        period_suffix.setVisibility( visibility);
                    }
                };
                spinner.setOnItemSelectedListener( l);
                
                setDialogIconFromSelectedTracker( alert_builder);
                alert_builder.setTitle( getText( R.string.tracking_confirmation_text)
                        + " " + getSelectedTrackerName() + "?");
                
                alert_builder.setView( textEntryView);
                alert_builder.setPositiveButton( R.string.confirm_tracking,
                                                 new DialogInterface.OnClickListener() {
                                                     public void onClick(DialogInterface dialog,
                                                                         int whichButton) {
                                                         String period_string = null;
                                                         long ms_period;
                                                         String tracking_format;
                                                         Log.d( IDroidTrackerConstants.CAUCHY_LOG,
                                                                "Spinner Chosen Index = "
                                                                        + spinner.getSelectedItemPosition());
                                                         switch ( spinner.getSelectedItemPosition()) {
                                                             case 0:
                                                                 ms_period = -1;
                                                                 tracking_format = IDroidTrackerConstants.FORMAT_SMS;
                                                                 break;
                                                             case 1:
                                                                 ms_period = -1;
                                                                 tracking_format = IDroidTrackerConstants.FORMAT_MAIL;
                                                                 break;
                                                             case 2:
                                                                 float period;
                                                                 try {
                                                                     period_string = ms_period_view.getText()
                                                                                                   .toString()
                                                                                                   .trim();
                                                                     period = Float.valueOf( period_string);
                                                                     if ( Math.floor( period) != period) {
                                                                         alert_builder.setTitle( getText( R.string.invalid_period_dialog_title))
                                                                                      .setMessage( getText( R.string.invalid_period_dialog_message))
                                                                                      .setPositiveButton( getText( R.string.ok_button_label),
                                                                                                          null)
                                                                                      .show();
                                                                         // Invalid
                                                                         // period
                                                                         // was
                                                                         // input:
                                                                         // Tracking
                                                                         // is
                                                                         // cancelled
                                                                         return;
                                                                     } else {
                                                                         ms_period = (long) ( period * 60000);
                                                                     }
                                                                 } catch ( Exception e) {
                                                                     String error_text = " Problem Parsing Period: "
                                                                             + period_string
                                                                             + " - Tracking Cancelled.";
                                                                     Log.println( Log.ERROR,
                                                                                  IDroidTrackerConstants.CAUCHY_LOG,
                                                                                  error_text);
                                                                     alert_builder.setTitle( getText( R.string.invalid_period_dialog_title))
                                                                                  .setMessage( getText( R.string.invalid_period_dialog_message))
                                                                                  .setPositiveButton( getText( R.string.ok_button_label),
                                                                                                      null)
                                                                                  .show();
                                                                     // Invalid
                                                                     // period
                                                                     // was
                                                                     // input:
                                                                     // Tracking
                                                                     // is
                                                                     // cancelled
                                                                     return;
                                                                 }
                                                                 tracking_format = IDroidTrackerConstants.FORMAT_SMS;
                                                                 break;
                                                             default:
                                                                 ms_period = -1;
                                                                 tracking_format = IDroidTrackerConstants.FORMAT_SMS;
                                                                 break;
                                                         }
                                                         
                                                         final Tracker selectedTracker = dataManager.fetchTrackerById( selectedTrackerId);
                                                         if ( selectedTracker != null) {
                                                             startSendingTrackingInfo( selectedTracker.id,
                                                                                       selectedTracker.number,
                                                                                       ms_period,
                                                                                       tracking_format,
                                                                                       false);
                                                         }
                                                     }
                                                 });
                alert_builder.setNegativeButton( R.string.cancel_tracking,
                                                 new DialogInterface.OnClickListener() {
                                                     public void onClick(DialogInterface dialog,
                                                                         int whichButton) {
                                                         // Does
                                                         // nothing
                                                         // on
                                                         // cancel
                                                     }
                                                 });
                return alert_builder.create();
            case DIALOG_NEW_TRACKER:
                alert_builder.setTitle( getText( R.string.new_tracker_menu));
                alert_builder.setItems( R.array.new_tracker_menu_options,
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog,
                                                                int which) {
                                                
                                                switch ( which) {
                                                    case 0:
                                                        Intent i = new Intent( Intent.ACTION_PICK,
                                                                             Uri.parse( "content://contacts/people/"));
                                                        i.setType( Contacts.Phones.CONTENT_TYPE);
                                                        startActivityForResult( i, PICK_CONTACT_REQUEST);
                                                        break;
                                                    case 1:
                                                        Intent add_contact_intent = new Intent( Contacts.Intents.Insert.ACTION,
                                                                                                Uri.parse( "content://contacts/people/"));
                                                        startActivityForResult( add_contact_intent, CREATE_AND_PICK_CONTACT_REQUEST);
                                                        // TODO: Show Contact Creation Dialog
                                                        break;
                                                    default:
                                                        break;
                                                }
                                            }
                                        });
                return alert_builder.create();
        }
        return null;
    }
    
    private void setDialogIconFromSelectedTracker(final AlertDialog.Builder alert_builder) {
        final Tracker selectedTracker = dataManager.fetchTrackerById( selectedTrackerId);
        if ( DroidTrackerUtils.isTwitter( selectedTracker)) {
            alert_builder.setIcon( R.drawable.twitter);
        } else {
            Drawable contact_pic = ( selectedTracker == null) ? null
                    : getContactImage( selectedTracker.id);
            if ( contact_pic != null) {
                alert_builder.setIcon( contact_pic);
            } else {
                alert_builder.setIcon( R.drawable.user_small);
            }
        }
    }
    
    private String getSelectedTrackerName() {
        final Tracker selectedTracker = dataManager.fetchTrackerById( selectedTrackerId);
        if ( selectedTracker != null) {
            return selectedTracker.name;
        } else {
            return getString( R.string.idle_tracker_dialog_title_default_tracker_name);
        }
    }
    
    private void startSendingTrackingInfo(long id,
                                          String number,
                                          long ms_period,
                                          String format,
                                          boolean b) {
        Intent i = new Intent( IDroidTrackerConstants.HANDLE_SMS_ACTION,
                               null,
                               getApplicationContext(),
                               TrackingRequestHandler.class);
        // i.putExtra( IDroidTrackerConstants.KEY_MSG_TO, msg_to);
        i.putExtra( IDroidTrackerConstants.KEY_MSG_DISPLAY_TO, number);
        // i.putExtra( IDroidTrackerConstants.KEY_MSG_BODY, msg_body);
        i.putExtra( IDroidTrackerConstants.KEY_TRACKER_ID, id);
        i.putExtra( IDroidTrackerConstants.KEY_LOSTPHONE_TRACKING, false);
        i.putExtra( IDroidTrackerConstants.KEY_IS_MANUAL_START, true);
        i.putExtra( IDroidTrackerConstants.KEY_PERIOD, ms_period);
        i.putExtra( IDroidTrackerConstants.KEY_FORMAT, format);
        
        // i.setFlags( Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity( i);
        
        // TODO start with result and do the fill on result
        fillTrackersList();
        
    }
    
    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        super.onPrepareDialog( id, dialog);
        final Tracker selectedTracker = dataManager.fetchTrackerById( selectedTrackerId);
        switch ( id) {
            case DIALOG_START_TRACKING:
                dialog.setTitle( getText( R.string.tracking_confirmation_text)
                        + " " + getSelectedTrackerName() + "?");
                setDialogTitleFromSelectedTracker( dialog, selectedTracker);
                
                break;
            case DIALOG_IDLE_TRACKER_MENU:
                dialog.setTitle( getText( R.string.idle_tracker_dialog_title)
                        + " " + getSelectedTrackerName());
                setDialogTitleFromSelectedTracker( dialog, selectedTracker);
                
                break;
            case DIALOG_STOP_TRACKING:
                setDialogTitleFromSelectedTracker( dialog, selectedTracker);
                break;
            default:
                break;
        }
    }
    
    private void setDialogTitleFromSelectedTracker(Dialog dialog,
                                                   final Tracker selectedTracker) {
        if ( dialog instanceof AlertDialog) {
            Drawable contact_pic = ( selectedTracker == null) ? null
                    : getContactImage( selectedTracker.id);
            if ( contact_pic != null) {
                ( (AlertDialog) dialog).setIcon( contact_pic);
            } else {
                ( (AlertDialog) dialog).setIcon( R.drawable.user_small);
            }
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode,
                                    int resultCode,
                                    Intent source_intent) {
        // String data , Bundle extras) {
        if ( requestCode == PICK_CONTACT_REQUEST) {
            if ( resultCode == RESULT_OK) {
                // Log.d( CAUCHY_LOG, "Contact Picked data   : " + data);
                addTrackerFromContactIntentResult( source_intent);
                fillTrackersList();
            }
        } else if ( requestCode == CREATE_AND_PICK_CONTACT_REQUEST) {
            if ( resultCode == RESULT_OK) {
                //Log.d( IDroidTrackerConstants.CAUCHY_LOG," contact = " + source_intent.getDataString());
                addTrackerFromContactIntentResult( source_intent);
                fillTrackersList();
            } else {
                Log.d( IDroidTrackerConstants.CAUCHY_LOG," Contact Creation Cancelled!");
            }
        }
    }

    private void addTrackerFromContactIntentResult(Intent source_intent) {
        // Request the selected record.
        String[] projection = new String[] {
                android.provider.BaseColumns._ID,
                android.provider.Contacts.PeopleColumns.NAME,
                android.provider.Contacts.PhonesColumns.NUMBER };
        
        Cursor cur;
        cur = managedQuery( Uri.parse( source_intent.getDataString()),
                            projection, // Which columns to return.
                            null,
                            null, // WHERE clause.
                            People.NAME + " ASC"); // Sort order.
        
        Log.d( CAUCHY_LOG, "Query Contact result URI =  "
                + Uri.parse( source_intent.getDataString()));
        
        // Log.d( CAUCHY_LOG, "Query Contact, # of results: " +
        // cur.count());
        
        int id_col = cur.getColumnIndex( android.provider.BaseColumns._ID);
        int name_col = cur.getColumnIndex( android.provider.Contacts.PeopleColumns.NAME);
        int number_col = cur.getColumnIndex( android.provider.Contacts.PhonesColumns.NUMBER);
        
        while ( cur.moveToNext()) {
            long tracker_id = cur.getLong( id_col);
            String tracker_name = cur.getString( name_col);
            String tracker_number = cur.getString( number_col);
            String tracker_email = getContactEmail( tracker_id);
            
            dataManager.addTracker( tracker_id,
                                    tracker_name,
                                    tracker_number,
                                    tracker_email);
        }
    }

    private String getContactEmail(long tracker_id) {
        Uri mContacts = Contacts.ContactMethods.CONTENT_URI;
        
        String[] mail_projection = new String[] {
                Contacts.ContactMethods.PERSON_ID,
                Contacts.ContactMethods.KIND,
                Contacts.ContactMethods.DATA };
        
        Cursor c = this.managedQuery( mContacts,
                                      mail_projection,
                                      Contacts.ContactMethods.PERSON_ID
                                              + "=\'"
                                              + tracker_id
                                              + "\'",
                                      null,
                                      Contacts.ContactMethods.PERSON_ID
                                              + " ASC");
        int email_col = c.getColumnIndex( Contacts.ContactMethods.DATA);
        c.moveToFirst();
        String tracker_email;
        try {
            tracker_email = c.getString( email_col);
        } catch ( Exception e) {
            tracker_email = "";
        }
        return tracker_email;
    }
    
    public void updateTrackerFromID(long id) {
        Uri contact_uri = Contacts.People.CONTENT_URI;
        
        // Request the selected record.
        // String[] projection = new String[] {
        // android.provider.BaseColumns._ID,
        // android.provider.Contacts.PeopleColumns.NAME,
        // android.provider.Contacts.PhonesColumns.NUMBER };
        String[] projection = new String[] {
                android.provider.Contacts.People._ID,
                android.provider.Contacts.PeopleColumns.NAME,
                android.provider.Contacts.PhonesColumns.NUMBER };
        
        // Cursor cur = this.managedQuery( contact_uri,
        // projection,
        // People._ID + "=\'" + id + "\'",
        // null,
        // People.NAME + " ASC");
        
        Cursor cur = managedQuery( Uri.parse( contact_uri.toString() + "/" + id),
                                   projection, // Which columns to return.
                                   null,
                                   null, // WHERE clause.
                                   People.NAME + " ASC"); // Sort order.
        
        int id_col = cur.getColumnIndex( android.provider.BaseColumns._ID);
        int name_col = cur.getColumnIndex( android.provider.Contacts.PeopleColumns.NAME);
        int number_col = cur.getColumnIndex( android.provider.Contacts.PhonesColumns.NUMBER);
        
        while ( cur.moveToNext()) {
            long tracker_id = cur.getLong( id_col);
            String tracker_name = cur.getString( name_col);
            String tracker_number = cur.getString( number_col);
            Uri mContacts = Contacts.ContactMethods.CONTENT_URI;
            
            String[] mail_projection = new String[] {
                    Contacts.ContactMethods.PERSON_ID,
                    Contacts.ContactMethods.KIND, Contacts.ContactMethods.DATA };
            
            Cursor c = this.managedQuery( mContacts,
                                          mail_projection,
                                          Contacts.ContactMethods.PERSON_ID
                                                  + "=\'"
                                                  + cur.getLong( id_col) + "\'",
                                          null,
                                          Contacts.ContactMethods.PERSON_ID
                                                  + " ASC");
            int email_col = c.getColumnIndex( Contacts.ContactMethods.DATA);
            c.moveToFirst();
            String tracker_email;
            try {
                tracker_email = c.getString( email_col);
            } catch ( Exception e) {
                tracker_email = "";
            }
            
            dataManager.updateTracker( tracker_id,
                                       tracker_name,
                                       tracker_number,
                                       tracker_email);
        }
        
        fillTrackersList();
        
    }
}