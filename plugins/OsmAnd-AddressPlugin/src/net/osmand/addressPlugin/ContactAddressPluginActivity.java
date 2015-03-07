package net.osmand.addressPlugin;

import java.util.StringTokenizer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;

public class ContactAddressPluginActivity extends Activity {
	private static final String OSMAND_COMPONENT = "net.osmand"; //$NON-NLS-1$
	private static final String OSMAND_COMPONENT_PLUS = "net.osmand.plus"; //$NON-NLS-1$
	private static final String OSMAND_ACTIVITY = "net.osmand.plus.activities.search.GeoIntentActivity"; //$NON-NLS-1$
	private static final int CONTACT_PICKER_RESULT = 1001;  
	//content://com.android.contacts/data/5
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		final Intent intent = getIntent();
		if (intent != null) {

			Cursor cur=getContentResolver().query(intent.getData(),null,null,null,null);
			if (cur.moveToFirst() == false)
			{
			   //no rows empty cursor
			   return;
			}

			final String street = cur.getString(cur.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS));
			StringTokenizer token=new StringTokenizer(street, ",");
			boolean latlong=false;
			String latitude=null;
			String longitude=null;
			if(token.countTokens()==2){
				String temp=token.nextToken();
				if(temp.startsWith("loc:")){
					temp=temp.substring(4);
				}
				 if (temp.trim().matches("((-|\\+)?[0-9]+(\\.[0-9]+)?)+")) {  
			            latitude=temp.trim();
			        } 
				 temp=token.nextToken();
				 if(temp.trim().matches("((-|\\+)?[0-9]+(\\.[0-9]+)?)+")){
					 longitude= temp.trim();
				 } 
				 if(latitude!=null && longitude!=null){
					 latlong=true;
				 }
			}
			String data="geo:";
			if(latlong){
				data=data+latitude+","+longitude;
			}else{
				data=data+"0,0?q="+street;
			}

			//The cursor is already positioned at the begining of the cursor
			//let's access a few columns

			//let's now see how we can loop through a cursor
			startOSMAND(intent, data);
			/*while(cur.moveToNext())
			{
			   //cursor moved successfully
			   //access fields
			}*/

		}
		 finish();
	}


    public void startOSMAND(Intent original,String data){
    	Intent intentPlus = new Intent();
		intentPlus.setComponent(new ComponentName(OSMAND_COMPONENT_PLUS, OSMAND_ACTIVITY));
		intentPlus.setFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
		intentPlus.setData(Uri.parse(data));
		intentPlus.setAction(original.getAction());
		ResolveInfo resolved = getPackageManager().resolveActivity(intentPlus, PackageManager.MATCH_DEFAULT_ONLY);
		if(resolved != null) {
			stopService(intentPlus);
			startActivity(intentPlus);
		} else {
			Intent intentNormal = new Intent();
			intentNormal.setFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
			intentNormal.setData(original.getData());
			intentNormal.setComponent(new ComponentName(OSMAND_COMPONENT, OSMAND_ACTIVITY));
			resolved = getPackageManager().resolveActivity(intentNormal, PackageManager.MATCH_DEFAULT_ONLY);
			if (resolved != null) {
				stopService(intentNormal);
				startActivity(intentNormal);
			} else {
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setMessage(getString(R.string.osmand_app_not_found));
				builder.setPositiveButton(getString(R.string.shared_string_yes), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=pname:" + OSMAND_COMPONENT_PLUS));
						try {
							stopService(intent);
							startActivity(intent);
						} catch (ActivityNotFoundException e) {
						}
					}
				});
				builder.setNegativeButton(getString(R.string.default_buttons_no), null);
				builder.show();
			}
		}

    }
}