package cauchy.android.tracker;

import java.io.InputStream;

import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.util.Log;

public class ContactAccessorNewApi extends ContactAccessor {

	@Override
	public Intent getContactPickerIntent() {
		Intent i = new Intent(Intent.ACTION_PICK, Contacts.CONTENT_URI);
		i.setType( ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE);
		return i;
	}

	@Override
	public Intent getAddContactIntent() {
		//Intent i = new Intent(Contacts.Intents.Insert.ACTION, Contacts.CONTENT_URI);
		Intent i = new Intent(ContactsContract.Intents.Insert.ACTION, Contacts.CONTENT_URI);
		return i;
	}

	@Override
	public String getNameColumn(boolean new_contact) {
		//return ContactsContract.Contacts.DISPLAY_NAME;
		if ( new_contact) {
			return ContactsContract.Contacts.DISPLAY_NAME;
		}
		return ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME;
	}
	
	@Override
	public String getNumberColumn(boolean new_contact) {
		return ContactsContract.CommonDataKinds.Phone.NUMBER;
	}

	@Override
	public String getPersonIdColumn(boolean new_contact) {
		//return ContactsContract.CommonDataKinds.Phone.CONTACT_ID;
		if ( new_contact) {
			return ContactsContract.Contacts._ID;
		}
		return ContactsContract.Contacts._ID;
	}

	@Override
	public String getContactPhoneNumber(Context ctx, long contactId) {
		Uri contact_uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
		contact_uri = Uri.parse( contact_uri.toString() + "/" + contactId);
		Cursor phones = ctx.getContentResolver().query(
				contact_uri,
				null,
				null,
				null, null);
		while (phones.moveToNext()) {
			String phoneNumber = phones
					.getString(phones
							.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
			if (phoneNumber != null) {
				return phoneNumber;
			}
		}
		phones.close();
		return "";
	}
	
	@Override
	public Drawable getContactImage(Context ctx, long tracker_id) {   	
		Uri uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, tracker_id);
		Bitmap bitmap;
		    InputStream input = ContactsContract.Contacts.openContactPhotoInputStream(ctx.getContentResolver(), uri);
		    if (input == null) {
		    	return null;
		     }
		    bitmap = BitmapFactory.decodeStream(input);

        return new BitmapDrawable(bitmap);
    }
	
	@Override
	public String getContactEmail(Activity activity, long tracker_id) {
		// Get Contact Id
		Uri contact_uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
		contact_uri = Uri.parse( contact_uri.toString() + "/" + tracker_id);
		Cursor cur = activity.getContentResolver().query( 
				contact_uri, 
				null,
				null,
				null,
				null);
		Log.d( IDroidTrackerConstants.CAUCHY_LOG, "-> getContactEmail cur.getCount() = " + cur.getCount());
		if ( cur.getCount() < 1) {
			return "";
		}
		cur.moveToFirst();
		String id = cur.getString(cur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID));
		
		Log.d( IDroidTrackerConstants.CAUCHY_LOG, "-> tracker_id / id = " + tracker_id + " / " + id);
		
		// Use it to get Email
		String[] projection = new String[] {
				ContactsContract.CommonDataKinds.Email.CONTACT_ID,
				ContactsContract.CommonDataKinds.Email.DATA };
		
		
		Cursor emailCur = activity.getContentResolver().query( 
				ContactsContract.CommonDataKinds.Email.CONTENT_URI, 
				projection,
				ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?", 
				new String[]{""+id},
				null);
		Log.d( IDroidTrackerConstants.CAUCHY_LOG, "-> getContactEmail for" + tracker_id + " cur size = " + emailCur.getCount());
		try {
			while (emailCur.moveToNext()) {
				String mail = emailCur.getString(
						emailCur.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA));
				if (mail != null) {
					return mail;
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			emailCur.close();
		}
		return "";
	}

	@Override
	public String getContactName(Context ctx, long contactId) {
		// Get Contact Id
		Uri contact_uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
		contact_uri = Uri.parse( contact_uri.toString() + "/" + contactId);
		Cursor cur = ctx.getContentResolver().query( 
				contact_uri, 
				null,
				null,
				null,
				null);
		Log.d( IDroidTrackerConstants.CAUCHY_LOG, "-> getContactEmail cur.getCount() = " + cur.getCount());
		if ( cur.getCount() < 1) {
			return "";
		}
		cur.moveToFirst();
		String result = cur.getString(cur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
		cur.close();
		
		return result;
		
//		// You know have the number so now query it like this
//		Cursor cur = ctx.getContentResolver().query(
//				ContactsContract.Contacts.CONTENT_URI, null,
//				ContactsContract.Contacts._ID + " = " + contactId, null, null);
//		while (cur.moveToNext()) {
//			String name = cur.getString(cur
//					.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
//			if (name != null) {
//				return name;
//			}
//		}
//		cur.close();
//		return "";
	}
}
