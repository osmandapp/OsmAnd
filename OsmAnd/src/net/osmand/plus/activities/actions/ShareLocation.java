package net.osmand.plus.activities.actions;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.AlertDialog.Builder;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.text.ClipboardManager;
import android.text.Html;
import android.widget.Toast;
import net.osmand.access.AccessibleAlertBuilder;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.MapActivityActions;
import net.osmand.util.MapUtils;

public class ShareLocation extends OsmAndAction {
	
	private static final String ZXING_BARCODE_SCANNER_COMPONENT = "com.google.zxing.client.android"; //$NON-NLS-1$
	private static final String ZXING_BARCODE_SCANNER_ACTIVITY = "com.google.zxing.client.android.ENCODE"; //$NON-NLS-1$


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
		Intent intent = new Intent(Intent.ACTION_SEND);
		intent.setType("vnd.android.cursor.dir/email"); //$NON-NLS-1$
		intent.putExtra(Intent.EXTRA_SUBJECT, "Location"); //$NON-NLS-1$
		intent.putExtra(Intent.EXTRA_TEXT, Html.fromHtml(email));
		intent.setType("text/html");
		mapActivity.startActivity(Intent.createChooser(intent, getString(R.string.send_location)));
	}

	private void sendSms(String sms) {
		Intent sendIntent = new Intent(Intent.ACTION_VIEW);
		sendIntent.putExtra("sms_body", sms); 
		sendIntent.setType("vnd.android-dir/mms-sms");
		mapActivity.startActivity(sendIntent);
	}

	private void sendToClipboard(String sms) {
		ClipboardManager clipboard = (ClipboardManager) mapActivity.getSystemService(Activity.CLIPBOARD_SERVICE);
		clipboard.setText(sms);
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
		Intent intent = new Intent();
		intent.addCategory(Intent.CATEGORY_DEFAULT);
		intent.setAction(ZXING_BARCODE_SCANNER_ACTIVITY);
		ResolveInfo resolved = mapActivity.getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);

		if (resolved != null) {
			intent.putExtra("ENCODE_TYPE", "LOCATION_TYPE");
			intent.putExtra("ENCODE_DATA", bundle);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
			mapActivity.startActivity(intent);
		} else {
			if (Version.isMarketEnabled(mapActivity.getMyApplication())) {
				AlertDialog.Builder builder = new AccessibleAlertBuilder(mapActivity);
				builder.setMessage(getString(R.string.zxing_barcode_scanner_not_found));
				builder.setPositiveButton(getString(R.string.default_buttons_yes), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(Version.marketPrefix(mapActivity.getMyApplication()) 
								+ ZXING_BARCODE_SCANNER_COMPONENT));
						try {
							mapActivity.startActivity(intent);
						} catch (ActivityNotFoundException e) {
						}
					}
				});
				builder.setNegativeButton(getString(R.string.default_buttons_no), null);
				builder.show();
			} else {
				Toast.makeText(mapActivity, R.string.zxing_barcode_scanner_not_found, Toast.LENGTH_LONG).show();
			}
		}
	}


}
