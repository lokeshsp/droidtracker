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
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.widget.TextView;

public class AboutActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState);
        setContentView( R.layout.about);
        
        TextView versionField = (TextView)findViewById( R.id.about_version_label);
        if ( versionField != null) {
	        try {
				String curVersion = getPackageManager().getPackageInfo("cauchy.android.tracker", 0).versionName;
				versionField.setText( getText(R.string.about_version) + curVersion);
	        } catch (NameNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
        }
    }
}
