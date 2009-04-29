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


/**
 * @author Olivier Bonal
 *
 */
public class Tracker {

	public long id;
	public String name;
	public String number;
	public long tracking_state;
    public String email;
    public long tracking_period;
    public long tracking_countdown;
    public String tracking_format;
    public long tracker_type;
    public long lostphone_tracking_state;
    
    private static String periodMsgPrefix;
    private static String periodMsgSuffix = "mn";

	@Override
	public String toString() {
	  return name +
	    ((tracking_state == TrackersDataManager.STATE_TRACKING && tracking_period != -1)?getPeriodInfoString():"");
	}

	
	public static void setPeriodPassPhraseMessageElements( String prefix, String suffix) {
	    periodMsgPrefix = prefix;
	    periodMsgSuffix = suffix;
	}
	
    /**
     * TODO: find a way to get this text from resources
     * @return
     */
    private String getPeriodInfoString() {
        return " (" + periodMsgPrefix + " " + (tracking_period/60000) + " " + periodMsgSuffix + ")";
    }

	/**
	 * @return
	 */
	public boolean isTracking() {
		return tracking_state == 1;
	}
	
	public boolean isTemporaryLostPhoneTracker() {
	    return tracker_type == TrackersDataManager.TYPE_TEMPORARY_FOR_LOSTPHONE_MODE;
	}
	
	public boolean isLostPhoneTrackingActive() {
	    return lostphone_tracking_state == TrackersDataManager.LOST_PHONE_TRACKING_ON;
	}
	
	@Override
	public boolean equals(Object o) {
	    if ( !(o instanceof Tracker) || o == null) {
	        return false;
	    }
	    Tracker t2 = (Tracker)o;
	    if ( number == null) {
	        return (t2.number == null) && id == t2.id;
	    }
	    return number.equals(t2.number) && id == t2.id;
	}
	
	@Override
	public int hashCode() {
	    if ( number == null) {
	        return -1;
	    }
	    return number.hashCode();
	}
	
}
