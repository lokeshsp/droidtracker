package cauchy.android.tracker;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Log;

public class ContactAccessorOldApi extends ContactAccessor {

	@Override
	public Intent getContactPickerIntent() {
		Intent i = new Intent(Intent.ACTION_PICK, android.provider.Contacts.People.CONTENT_URI);
		i.setType( android.provider.Contacts.Phones.CONTENT_TYPE);
		return i;
	}

	@Override
	public Intent getAddContactIntent() {
		Intent i = new Intent(android.provider.Contacts.Intents.Insert.ACTION, android.provider.Contacts.People.CONTENT_URI);
		return i;
	}

	@Override
	public String getNameColumn(boolean new_contact) {
		//return People.NAME;
		if ( new_contact) {
			return android.provider.Contacts.PeopleColumns.NAME;
		}
 		return android.provider.Contacts.PeopleColumns.NAME;
	}

	@Override
	public String getNumberColumn(boolean new_contact) {
		if ( new_contact) {
			return android.provider.Contacts.PhonesColumns.NUMBER;
		}
		return android.provider.Contacts.PhonesColumns.NUMBER;
	}

	@Override
	public String getPersonIdColumn(boolean new_contact) {
		if (new_contact) {
			return android.provider.BaseColumns._ID;
		}
		return android.provider.Contacts.Phones.PERSON_ID;
	}
	
	@Override
	public String getContactPhoneNumber(Context ctx, long contactId) {
		// You now have the number so now query it like this
		Cursor phones = ctx.getContentResolver().query( 
				android.provider.Contacts.Phones.CONTENT_URI, 
			      null, 
			      android.provider.Contacts.Phones.PERSON_ID +" = "+ contactId, 
			      null, null); 
			    while (phones.moveToNext()) { 
			     String phoneNumber = phones.getString( 
			       phones.getColumnIndex( 
			    		   android.provider.Contacts.Phones.NUMBER));
			     if (phoneNumber != null) {
			    	 return phoneNumber;
			     }
			    } 
			    phones.close(); 
		return "";
	}

	@Override
	public Drawable getContactImage(Context ctx, long tracker_id) {
    	Uri person = ContentUris.withAppendedId(android.provider.Contacts.People.CONTENT_URI,tracker_id);
     
        Bitmap bitmap = android.provider.Contacts.People.loadContactPhoto(ctx, person, R.drawable.user_small, null);
    	
        return new BitmapDrawable(bitmap);
    }

	@Override
	public String getContactEmail(Activity activity, long tracker_id) {
		Log.d( IDroidTrackerConstants.CAUCHY_LOG, "-> getContactEmail for" + tracker_id);
        Uri mContacts = android.provider.Contacts.ContactMethods.CONTENT_URI;
        
        String[] mail_projection = new String[] {
        		android.provider.Contacts.ContactMethods.PERSON_ID,
        		android.provider.Contacts.ContactMethods.KIND,
        		android.provider.Contacts.ContactMethods.DATA };
        
        Cursor c = activity.managedQuery( mContacts,
                                      mail_projection,
                                      //null,
                                      android.provider.Contacts.ContactMethods.PERSON_ID
                                              + "=\'"
                                              + tracker_id
                                              + "\'",
                                      null,
                                      android.provider.Contacts.ContactMethods.PERSON_ID
                                              + " ASC");
        int email_col = c.getColumnIndex( android.provider.Contacts.ContactMethods.DATA);
        
        int person_col = c.getColumnIndex( android.provider.Contacts.ContactMethods.PERSON_ID);
        int kind_col = c.getColumnIndex( android.provider.Contacts.ContactMethods.KIND);
//        c.moveToFirst();
        String tracker_email = "";
        while ( c.moveToNext() && (tracker_email == null || tracker_email.length() == 0)) { 
            try {
                tracker_email = c.getString( email_col);
            } catch ( Exception e) {
                tracker_email = "";
            }
            Log.d( IDroidTrackerConstants.CAUCHY_LOG, "  -> tracker_email = " + tracker_email);
            Log.d( IDroidTrackerConstants.CAUCHY_LOG, "  -> person_col = " + c.getString( person_col));
            Log.d( IDroidTrackerConstants.CAUCHY_LOG, "  -> kind_col = " + c.getString( kind_col));
        }
        c.close();
        return tracker_email;
	}

	@Override
	public String getContactName(Context ctx, long contactId) {
		Uri contact_uri = android.provider.Contacts.Phones.CONTENT_URI;
		String[] projection = new String[] {
              android.provider.Contacts.Phones.PERSON_ID,
              android.provider.Contacts.Phones.NAME };
		
		// You now have the number so now query it like this
		Cursor cur = ctx.getContentResolver().query(
				//Uri.parse( contact_uri.toString() + "/" + contactId),
				contact_uri,
				projection,
				android.provider.Contacts.Phones.PERSON_ID
                + "=\'"
                + contactId
                + "\'",
				null,
				null);
		try {
			while (cur.moveToNext()) {
				String name = cur
						.getString(cur
								.getColumnIndex(android.provider.Contacts.Phones.NAME));
				if (name != null) {
					return name;
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			cur.close();
		}
		return "";
	}

	@Override
	public long getPersonIdFromPhoneId(ContentResolver contentResolver,
									   long trackerId) {
		return trackerId;
	}
	
}
