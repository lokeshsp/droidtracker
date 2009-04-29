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
import android.location.Address;
import android.location.Geocoder;
import android.util.Log;

public class AndroidBasedGeodecoder implements IGeoDecoder {
    
    private Geocoder coder;
    
    
    public AndroidBasedGeodecoder( Context ctx) {
        this.coder = new Geocoder( ctx);
    }
    
    public String getAddressFromGeoCode(String lat, String lng) {
        try {
            Log.println( Log.DEBUG, IDroidTrackerConstants.CAUCHY_LOG, "=============> AndroidBasedGeodecoder getAddressFromGeoCode");
            Log.println( Log.DEBUG, IDroidTrackerConstants.CAUCHY_LOG, "               lat = " + lat);
            Log.println( Log.DEBUG, IDroidTrackerConstants.CAUCHY_LOG, "               lng = " + lng);
            final double double_lat = new Double(lat).doubleValue();
            final double double_lng = new Double(lng).doubleValue();
            Log.println( Log.DEBUG, IDroidTrackerConstants.CAUCHY_LOG, "               double_lat = " + double_lat);
            Log.println( Log.DEBUG, IDroidTrackerConstants.CAUCHY_LOG, "               double_lng = " + double_lng);
            List<Address> addresses = coder.getFromLocation( double_lat, double_lng, 1);
            Address address = addresses.get( 0);
            StringBuffer result = new StringBuffer();
            int maxAddressLineIndex = address.getMaxAddressLineIndex();
            for ( int i = 0; i < maxAddressLineIndex; i++) {
                result.append( address.getAddressLine(i));
                if ( i < maxAddressLineIndex - 1) {
                    result.append( "\n");
                }
            }
            Log.println( Log.DEBUG, IDroidTrackerConstants.CAUCHY_LOG, "               result = " + result.toString());
            return result.toString();
        } catch ( Exception e) {
            Log.println( Log.ERROR, IDroidTrackerConstants.CAUCHY_LOG, "=============> AndroidBasedGeodecoder getAddressFromGeoCode EXCEPTION " + e.getMessage());
            return null;
        }
    }
    
}
