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
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;

public class NotificationDisplayerActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState);
        Bundle bundleExtra = getIntent().getExtras();
        CharSequence title = bundleExtra.getCharSequence( IDroidTrackerConstants.KEY_NOTIFICATION_TITLE);
        CharSequence msg = bundleExtra.getCharSequence( IDroidTrackerConstants.KEY_NOTIFICATION_MSG);
        OnClickListener ok_listener = new OnClickListener() {
            
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
            
        };
        new AlertDialog.Builder( this).setTitle( title)
                                      .setIcon( R.drawable.icon)
                                      .setMessage( msg)
                                      .setPositiveButton( getText( R.string.ok_button_label),
                                                          ok_listener)
                                      .show();
    }
}
