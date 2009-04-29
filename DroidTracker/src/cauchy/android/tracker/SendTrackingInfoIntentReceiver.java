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

import java.util.List;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class SendTrackingInfoIntentReceiver extends BroadcastReceiver {
    
    @Override
    public void onReceive(Context context, Intent intent) {
        
        Log.d( IDroidTrackerConstants.CAUCHY_LOG,
               "SendTrackingInfoIntentReceiver onReceiveIntent");
        TrackersDataManager dataManager = new TrackersDataManager( context);
        List<Tracker> trackers_list = dataManager.fetchTrackers( true);
        dataManager.close();
        if ( trackers_list == null || trackers_list.isEmpty()) {
            Intent sender_intent = new Intent( context,
                                               TrackingInfoSenderService.class);
            Log.i( IDroidTrackerConstants.CAUCHY_LOG,
                   " --> STOPPING SERVICE AND ALARM");
            context.stopService( sender_intent);
            AlarmManager am = (AlarmManager) context.getSystemService( Activity.ALARM_SERVICE);
            am.cancel( PendingIntent.getBroadcast( context,
                                                   0,
                                                   intent,
                                                   PendingIntent.FLAG_CANCEL_CURRENT));
        } else {
            Intent sender_intent = new Intent( context,
                                               TrackingInfoSenderService.class);
            sender_intent.putExtras( intent.getExtras());
            context.startService( sender_intent);
        }
        
    }
}
