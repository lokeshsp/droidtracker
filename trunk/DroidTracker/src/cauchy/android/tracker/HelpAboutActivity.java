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

import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.widget.TabHost;

public class HelpAboutActivity extends TabActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState);
        
        // Request Custom Title and set Theme
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setTheme( R.style.DroidTrackerTheme);
        
        TabHost host = getTabHost();
        Intent help_intent = new Intent( IDroidTrackerConstants.HELP_ACTION,
                                         null,
                                         this,
                                         HelpActivity.class);
        Intent about_intent = new Intent( IDroidTrackerConstants.ABOUT_ACTION,
                                          null,
                                          this,
                                          AboutActivity.class);
        Intent faq_intent = new Intent( IDroidTrackerConstants.FAQ_ACTION,
                                          null,
                                          this,
                                          FAQActivity.class);
        host.addTab( host.newTabSpec( IDroidTrackerConstants.ABOUT_TAG)
                         .setIndicator( getText( R.string.about_label),
                                        getResources().getDrawable( android.R.drawable.ic_dialog_info))
                         .setContent( about_intent));
        host.addTab( host.newTabSpec( IDroidTrackerConstants.HELP_TAG)
                         .setIndicator( getText( R.string.help_label),
                                        getResources().getDrawable(  android.R.drawable.ic_dialog_alert))
                         .setContent( help_intent));
        host.addTab( host.newTabSpec( IDroidTrackerConstants.FAQ_TAG)
                     .setIndicator( getText( R.string.faq_label),
                                    getResources().getDrawable(  android.R.drawable.ic_dialog_email))
                     .setContent( faq_intent));
        
        // Set Custom Title
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.window_title);
    }
}
