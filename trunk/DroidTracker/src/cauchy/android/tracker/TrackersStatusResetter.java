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
import android.util.Log;

/**
 * Reset the status of all trackers to "non tracking" at phone startup.
 * We could also use this class to startup the service and continue tracking
 * but I prefer to stop all trackings if the phone is switched off.
 * @author olivier
 *
 */
public class TrackersStatusResetter extends BroadcastReceiver {
    
    @Override
    public void onReceive(Context context, Intent intent) {
        
        Log.i( IDroidTrackerConstants.CAUCHY_LOG,
               "TrackersStatusResetter onReceive, resetting the status of all trackers to \"non tracking\"...");
        TrackersDataManager dataManager = new TrackersDataManager( context);
        dataManager.stopAllTracking();
        dataManager.close();
    }
}
