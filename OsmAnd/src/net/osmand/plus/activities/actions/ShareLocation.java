package net.osmand.plus.activities.actions;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.MapActivityActions;
import net.osmand.util.MapUtils;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

public class ShareLocation extends OsmAndAction {
	
	public ShareLocation(MapActivity mapActivity) {
		super(mapActivity);
	}
	
	@Override
	public int getDialogID() {
		return OsmAndDialogs.DIALOG_SHARE_LOCATION;
	}
	
	@Override
	public void run() {
    	super.showDialog();
	}
	
	public Dialog createDialog(Activity activity, final Bundle args) {
		mapActivity = (MapActivity) activity;
		AlertDialog.Builder builder = new Builder(mapActivity);
		builder.setTitle(R.string.send_location_way_choose_title);
		builder.setItems(new String[]{
				"Email", "SMS", "Clipboard", "geo:", "QR-Code"
		}, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				final double latitude = args.getDouble(MapActivityActions.KEY_LATITUDE);
				final double longitude = args.getDouble(MapActivityActions.KEY_LONGITUDE);
				final int zoom = args.getInt(MapActivityActions.KEY_ZOOM);
				try {
					final String shortOsmUrl = MapUtils.buildShortOsmUrl(latitude, longitude, zoom);
					final String appLink = "http://download.osmand.net/go?lat=" + ((float) latitude) + "&lon=" + ((float) longitude) + "&z=" + zoom;
					String sms = mapActivity.getString(R.string.send_location_sms_pattern, shortOsmUrl, appLink);
					if (which == 0) {
						sendEmail(shortOsmUrl, appLink);
					} else if (which == 1) {
						sendSms(sms);
					} else if (which == 2) {
						sendToClipboard(sms);
					} else if (which == 3) {
						sendGeoActivity(latitude, longitude, zoom);
					} else if (which == 4) {
						sendQRCode(latitude, longitude);
					}
				} catch (RuntimeException e) {
					Toast.makeText(mapActivity, R.string.input_output_error, Toast.LENGTH_SHORT).show();
				}				
			}

			
		});
    	return builder.create();
    }
	


	private void sendEmail(final String shortOsmUrl, final String appLink) {
		String email = mapActivity.getString(R.string.send_location_email_pattern, shortOsmUrl, appLink);
		ShareDialog.sendEmail(mapActivity, email, getString(R.string.send_location));
	}

	private void sendSms(String sms) {
		ShareDialog.sendSms(mapActivity, sms);
	}

	private void sendToClipboard(String sms) {
		ShareDialog.sendToClipboard(mapActivity, sms);
		
	}

	private void sendGeoActivity(final double latitude, final double longitude, final int zoom) {
		final String simpleGeo = "geo:"+((float) latitude)+","+((float)longitude) +"?z="+zoom;
		Uri location = Uri.parse(simpleGeo);
		Intent mapIntent = new Intent(Intent.ACTION_VIEW, location);
		mapActivity.startActivity(mapIntent);
	}

	private void sendQRCode(final double latitude, final double longitude) {
		Bundle bundle = new Bundle();
		bundle.putFloat("LAT", (float) latitude);
		bundle.putFloat("LONG", (float) longitude);
		ShareDialog.sendQRCode(mapActivity, "LOCATION_TYPE", bundle, null);
	}


}
