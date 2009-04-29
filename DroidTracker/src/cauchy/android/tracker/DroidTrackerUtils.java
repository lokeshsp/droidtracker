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

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

public class DroidTrackerUtils {
    /**
     * Indicates whether the specified action can be used as an intent. This
     * method queries the package manager for installed packages that can
     * respond to an intent with the specified action. If no suitable package is
     * found, this method returns false.
     * 
     * @param context
     *            The application's environment.
     * @param action
     *            The Intent action to check for availability.
     * 
     * @return True if an Intent with the specified action can be sent and
     *         responded to, false otherwise.
     */
    public static boolean isIntentAvailable(Context context, String action) {
        final PackageManager packageManager = context.getPackageManager();
        final Intent intent = new Intent( action);
        List<ResolveInfo> list = packageManager.queryIntentActivities( intent,
                                                                       PackageManager.MATCH_DEFAULT_ONLY);
        return list.size() > 0;
    }
    
    public static boolean isTwitterClientInstalled(Context context) {
        return isIntentAvailable( context, "com.twidroid.SendTweet");
    }
    
    public static boolean isTwitter(Tracker tracker) {
        return tracker != null
                && tracker.tracker_type == TrackersDataManager.TYPE_OTHER_APPLICATION
                && IDroidTrackerConstants.FORMAT_TWITTER.equals( tracker.tracking_format);
    }
    
    public static Intent getTweetIntent(Context context, String msg) {
        if ( isTwitterClientInstalled( context)) {
            Intent intent = new Intent( "com.twidroid.SendTweet");
            intent.putExtra( "com.twidroid.extra.MESSAGE", msg);
            return intent;
        }
        return null;
    }
    
}
