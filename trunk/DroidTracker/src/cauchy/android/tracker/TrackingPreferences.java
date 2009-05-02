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

import android.app.AlertDialog;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceChangeListener;
import android.util.Log;

public class TrackingPreferences extends PreferenceActivity {
    
    private EditTextPreference smsPassphraseLostphonePref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState);
        
        setPreferenceScreen( createPreferenceHierarchy());
        smsPassphraseLostphonePref.setDependency( IDroidTrackerConstants.PREFERENCE_KEY_ACTIVATE_LOST_PHONE_MODE);
    }
    
    private PreferenceScreen createPreferenceHierarchy() {
        // Root
        final PreferenceScreen root = getPreferenceManager().createPreferenceScreen( this);
        
        // SMS Preferences
        PreferenceCategory sms_category = new PreferenceCategory( this);
        sms_category.setTitle( R.string.preferences_sms_category_label);
        root.addPreference( sms_category);
        
        // Start Pass Phrase
        EditTextPreference sms_passphrase_start_pref = new EditTextPreference( this);
        sms_passphrase_start_pref.setDialogTitle( R.string.preferences_sms_start_passphrase_label);
        sms_passphrase_start_pref.setKey( IDroidTrackerConstants.PREFERENCE_KEY_SMS_START_PASSPHRASE);
        sms_passphrase_start_pref.setDefaultValue( getString( R.string.preferences_sms_start_passphrase_default));
        sms_passphrase_start_pref.setTitle( R.string.preferences_sms_start_passphrase_label);
        sms_passphrase_start_pref.setSummary( R.string.preferences_sms_start_passphrase_summary);
        sms_category.addPreference( sms_passphrase_start_pref);
        
        // Stop Pass Phrase
        EditTextPreference sms_passphrase_stop_pref = new EditTextPreference( this);
        sms_passphrase_stop_pref.setDialogTitle( R.string.preferences_sms_stop_passphrase_label);
        sms_passphrase_stop_pref.setKey( IDroidTrackerConstants.PREFERENCE_KEY_SMS_STOP_PASSPHRASE);
        sms_passphrase_stop_pref.setDefaultValue( getString( R.string.preferences_sms_stop_passphrase_default));
        sms_passphrase_stop_pref.setTitle( R.string.preferences_sms_stop_passphrase_label);
        sms_passphrase_stop_pref.setSummary( R.string.preferences_sms_stop_passphrase_summary);
        sms_category.addPreference( sms_passphrase_stop_pref);
        
        // Period Setting Pass Phrase
        EditTextPreference sms_passphrase_period_pref = new EditTextPreference( this);
        sms_passphrase_period_pref.setDialogTitle( R.string.preferences_sms_period_passphrase_label);
        sms_passphrase_period_pref.setKey( IDroidTrackerConstants.PREFERENCE_KEY_SMS_PERIOD_PASSPHRASE);
        sms_passphrase_period_pref.setDefaultValue( getString( R.string.preferences_sms_period_passphrase_default));
        sms_passphrase_period_pref.setTitle( R.string.preferences_sms_period_passphrase_label);
        sms_passphrase_period_pref.setSummary( R.string.preferences_sms_period_passphrase_summary);
        OnPreferenceChangeListener pref_change_listener = new OnPreferenceChangeListener() {
            
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if ( newValue == null ||
                     !newValue.toString().contains( IDroidTrackerConstants.PERIOD_PASSPHRASE_SEPARATOR) ||
                     newValue.toString().endsWith( IDroidTrackerConstants.PERIOD_PASSPHRASE_SEPARATOR)) {
                    Log.e( IDroidTrackerConstants.CAUCHY_LOG, "Invalid value: " + newValue);
                    new AlertDialog.Builder( root.getContext())
                    .setTitle( getText( R.string.preferences_sms_period_passphrase_error_dialog_title))
                    .setMessage(getText( R.string.preferences_sms_period_passphrase_error_dialog_msg))
                    .setPositiveButton( getText( R.string.ok_button_label), null)
                    .show();

                    return false;
                } else {
                    Log.i( IDroidTrackerConstants.CAUCHY_LOG, "Good value: " + newValue);
                    return true;
                }
            }
        };
        sms_passphrase_period_pref.setOnPreferenceChangeListener(pref_change_listener);
        
        sms_category.addPreference( sms_passphrase_period_pref);
        
        
        // Lost/Stolen Phone mode
        //////////
        /*
         * The Preferences screenPref serves as a screen break (similar to page
         * break in word processing). Like for other preference types, we assign
         * a key here so that it is able to save and restore its instance state.
         */
        // Screen preference
        PreferenceScreen screenPref = getPreferenceManager().createPreferenceScreen(this);
        screenPref.setKey("screen_preference");
        screenPref.setTitle(R.string.lostphone_mode_label);
        screenPref.setSummary(R.string.lostphone_mode_summary);
        sms_category.addPreference(screenPref);
        
        
        CheckBoxPreference lost_phone_mode_activation_pref = new CheckBoxPreference( this);
        lost_phone_mode_activation_pref.setKey( IDroidTrackerConstants.PREFERENCE_KEY_ACTIVATE_LOST_PHONE_MODE);
        lost_phone_mode_activation_pref.setTitle( R.string.activate_lostphone_mode_label);
        lost_phone_mode_activation_pref.setDefaultValue( false);
        lost_phone_mode_activation_pref.setSummary( R.string.activate_lostphone_mode_summary);
        screenPref.addPreference(lost_phone_mode_activation_pref);
        
        smsPassphraseLostphonePref = new EditTextPreference( this);
        smsPassphraseLostphonePref.setDialogTitle( R.string.preferences_sms_lostphone_passphrase_label);
        smsPassphraseLostphonePref.setKey( IDroidTrackerConstants.PREFERENCE_KEY_SMS_LOSTPHONE_PASSPHRASE);
        smsPassphraseLostphonePref.setDefaultValue( getString( R.string.preferences_sms_lostphone_passphrase_default));
        smsPassphraseLostphonePref.setTitle( R.string.preferences_sms_lostphone_passphrase_label);
        smsPassphraseLostphonePref.setSummary( R.string.preferences_sms_lostphone_passphrase_summary);
        TypedArray a = obtainStyledAttributes(R.styleable.TogglePrefAttrs);
        smsPassphraseLostphonePref.setLayoutResource(
                                            a.getResourceId(R.styleable.TogglePrefAttrs_android_preferenceLayoutChild,
                                                    0));
        screenPref.addPreference( smsPassphraseLostphonePref);
        a.recycle();
        
        
        
        // Ask Confirmation for trakcing to start
        CheckBoxPreference confirmation_requested_pref = new CheckBoxPreference( this);
        confirmation_requested_pref.setKey( IDroidTrackerConstants.PREFERENCE_KEY_TRACKING_CONFIRMATION_REQUESTED);
        confirmation_requested_pref.setTitle( R.string.tracking_confirmation_requested_label);
        confirmation_requested_pref.setDefaultValue( true);
        confirmation_requested_pref.setSummary( R.string.tracking_confirmation_requested_summary);
        sms_category.addPreference(confirmation_requested_pref);
        
        
        // Sending Preferences
        PreferenceCategory sending_category = new PreferenceCategory( this);
        sending_category.setTitle( R.string.preferences_sending_category_label);
        root.addPreference( sending_category);
        
        // Notify from sent info
        CheckBoxPreference display_sent_info_pref = new CheckBoxPreference( this);
        display_sent_info_pref.setKey( IDroidTrackerConstants.PREFERENCE_KEY_DISPLAY_SENT_INFO);
        display_sent_info_pref.setTitle( R.string.preferences_display_info_when_sending_label);
        display_sent_info_pref.setDefaultValue( true);
        display_sent_info_pref.setSummary( R.string.preferences_display_info_when_sending_summary);
        sending_category.addPreference(display_sent_info_pref);
        
        // Auto Turn Network Location Providers On
        CheckBoxPreference display_settings_if_single_provider_pref = new CheckBoxPreference( this);
        display_settings_if_single_provider_pref.setKey( IDroidTrackerConstants.PREFERENCE_KEY_DISPLAY_SETTINGS_FOR_SINGLE_PROVIDER);
        display_settings_if_single_provider_pref.setTitle( R.string.preferences_display_settings_if_single_provider_label);
        display_settings_if_single_provider_pref.setDefaultValue( true);
        display_settings_if_single_provider_pref.setSummary( R.string.preferences_display_settings_if_single_provider_summary);
        sending_category.addPreference(display_settings_if_single_provider_pref);
        
        return root;
    }
}
