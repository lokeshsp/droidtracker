package cauchy.android.tracker;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Build;

public abstract class ContactAccessor {

	private static ContactAccessor sInstance;

    public static ContactAccessor getInstance() {
        if (sInstance == null) {
            int sdkVersion = Integer.parseInt(Build.VERSION.SDK);
            if (sdkVersion < Build.VERSION_CODES.ECLAIR) {
            	sInstance = new ContactAccessorOldApi();
            } else {
            	sInstance = new ContactAccessorNewApi();
            }
        }
        return sInstance;
    }

    public abstract Intent getContactPickerIntent();
    public abstract Intent getAddContactIntent();
    
    public String[] getProjection(boolean new_contact) {
    	
		String[] projection;
		if ( new_contact) {
			projection = new String[] {
	                getPersonIdColumn(new_contact),
	                getNameColumn(new_contact) };
		} else {
			projection = new String[] {
	                getPersonIdColumn(new_contact),
	                getNameColumn(new_contact),
	                getNumberColumn(new_contact) };
		}
		return projection;
	}

	public abstract String getNameColumn(boolean new_contact);
	public abstract String getNumberColumn(boolean new_contact);
	public abstract String getPersonIdColumn(boolean new_contact);
	
	public abstract String getContactPhoneNumber(Context ctx, long contact_id);
	public abstract String getContactName(Context ctx, long contact_id);
	public abstract Drawable getContactImage(Context ctx, long tracker_id);
	public abstract String getContactEmail(Activity activity, long tracker_id);
	public abstract long getPersonIdFromPhoneId(ContentResolver content_resolver, long tracker_id);
	
}
