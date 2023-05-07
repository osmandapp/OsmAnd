package net.osmand.plus.mapcontextmenu.other;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.text.TextUtilsCompat;
import androidx.core.view.ViewCompat;

import net.osmand.LocationConvert;
import net.osmand.PlatformUtil;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.BaseMenuController;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

public class ShareMenu extends BaseMenuController {

	private static final Log log = PlatformUtil.getLog(ShareMenu.class);

	private static final String KEY_SHARE_MENU_LATLON = "key_share_menu_latlon";
	private static final String KEY_SHARE_MENU_POINT_TITLE = "key_share_menu_point_title";
	private static final String ZXING_BARCODE_SCANNER_COMPONENT = "com.google.zxing.client.android";
	private static final String ZXING_BARCODE_SCANNER_ACTIVITY = "com.google.zxing.client.android.ENCODE";

	private LatLon latLon;
	private String title;
	private String address;

	public enum ShareItem {
		MESSAGE(R.drawable.ic_action_message, R.string.shared_string_send),
		CLIPBOARD(R.drawable.ic_action_copy, R.string.shared_string_copy),
		ADDRESS(R.drawable.ic_action_street_name, R.string.copy_address),
		NAME(R.drawable.ic_action_copy, R.string.copy_poi_name),
		COORDINATES(R.drawable.ic_action_copy, R.string.copy_coordinates),
		GEO(R.drawable.ic_world_globe_dark, R.string.share_geo),
		QR_CODE(R.drawable.ic_action_qrcode, R.string.shared_string_qr_code);

		final int iconResourceId;
		final int titleResourceId;

		ShareItem(int iconResourceId, int titleResourceId) {
			this.iconResourceId = iconResourceId;
			this.titleResourceId = titleResourceId;
		}

		public int getIconResourceId() {
			return iconResourceId;
		}

		public int getTitleResourceId() {
			return titleResourceId;
		}
	}

	private ShareMenu(MapActivity mapActivity) {
		super(mapActivity);
	}

	public List<ShareItem> getItems() {
		List<ShareItem> list = new LinkedList<>();
		list.add(ShareItem.MESSAGE);
		list.add(ShareItem.CLIPBOARD);
		list.add(ShareItem.ADDRESS);
		list.add(ShareItem.NAME);
		list.add(ShareItem.COORDINATES);
		list.add(ShareItem.GEO);
		list.add(ShareItem.QR_CODE);
		return list;
	}

	public LatLon getLatLon() {
		return latLon;
	}

	public String getTitle() {
		return title;
	}

	public static void show(LatLon latLon, String title, String address, @NonNull MapActivity mapActivity) {
		ShareMenu menu = new ShareMenu(mapActivity);
		menu.latLon = latLon;
		menu.title = title;
		menu.address = address;

		ShareMenuFragment.showInstance(menu);
	}

	public void share(ShareItem item) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity == null) {
			return;
		}
		StringBuilder sb = new StringBuilder();
		if (!Algorithms.isEmpty(title)) {
			sb.append(title).append("\n");
		}
		if (!Algorithms.isEmpty(address) && !address.equals(title) && !address.equals(mapActivity.getString(R.string.no_address_found))) {
			sb.append(address).append("\n");
		}
		sb.append(mapActivity.getString(R.string.shared_string_location)).append(": ");
		if (TextUtilsCompat.getLayoutDirectionFromLocale(Locale.getDefault()) == ViewCompat.LAYOUT_DIRECTION_RTL) {
			sb.append("\n");
		}

		String geoUrl = "";
		String httpUrl = "";
		try {
			String lat = LocationConvert.convertLatitude(latLon.getLatitude(), LocationConvert.FORMAT_DEGREES, false);
			String lon = LocationConvert.convertLongitude(latLon.getLongitude(), LocationConvert.FORMAT_DEGREES, false);
			lat = lat.substring(0, lat.length() - 1);
			lon = lon.substring(0, lon.length() - 1);
			int zoom = mapActivity.getMapView().getZoom();
			geoUrl = MapUtils.buildGeoUrl(lat, lon, zoom);
			httpUrl = "https://osmand.net/map?pin=" + lat + "," + lon + "#" + zoom + "/" + lat + "/" + lon;
		} catch (RuntimeException e) {
			log.error("Failed to convert coordinates", e);
		}
		if (!Algorithms.isEmpty(geoUrl) && !Algorithms.isEmpty(httpUrl)) {
			sb.append(geoUrl).append("\n").append(httpUrl);
		}
		String sms = sb.toString();
		switch (item) {
			case MESSAGE:
				sendMessage(mapActivity, sms);
				break;
			case CLIPBOARD:
				copyToClipboardWithToast(mapActivity, sms, Toast.LENGTH_LONG);
				break;
			case ADDRESS:
				if (!Algorithms.isEmpty(address)) {
					copyToClipboardWithToast(mapActivity, address, Toast.LENGTH_LONG);
				} else {
					Toast.makeText(mapActivity,
							R.string.no_address_found,
							Toast.LENGTH_LONG).show();
				}
				break;
			case NAME:
				if (!Algorithms.isEmpty(title)) {
					copyToClipboardWithToast(mapActivity, title, Toast.LENGTH_LONG);
				} else {
					Toast.makeText(mapActivity,
							R.string.toast_empty_name_error,
							Toast.LENGTH_LONG).show();
				}
				break;
			case COORDINATES:
				OsmandSettings st = ((OsmandApplication) mapActivity.getApplicationContext()).getSettings();
				int f = st.COORDINATES_FORMAT.get();
				String coordinates = OsmAndFormatter.getFormattedCoordinates(latLon.getLatitude(), latLon.getLongitude(), f);
				copyToClipboardWithToast(mapActivity, coordinates, Toast.LENGTH_LONG);
				break;
			case GEO:
				if (!Algorithms.isEmpty(geoUrl)) {
					Intent mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(geoUrl));
					AndroidUtils.startActivityIfSafe(mapActivity, mapIntent);
				}
				break;
			case QR_CODE:
				Bundle bundle = new Bundle();
				bundle.putFloat("LAT", (float) latLon.getLatitude());
				bundle.putFloat("LONG", (float) latLon.getLongitude());
				sendQRCode(mapActivity, "LOCATION_TYPE", bundle, null);
				break;
		}
	}

	public void saveMenu(Bundle bundle) {
		bundle.putSerializable(KEY_SHARE_MENU_LATLON, latLon);
		bundle.putString(KEY_SHARE_MENU_POINT_TITLE, title);
	}

	public static ShareMenu restoreMenu(Bundle bundle, MapActivity mapActivity) {
		ShareMenu menu = new ShareMenu(mapActivity);
		menu.title = bundle.getString(KEY_SHARE_MENU_POINT_TITLE);
		menu.latLon = AndroidUtils.getSerializable(bundle, KEY_SHARE_MENU_LATLON, LatLon.class);
		return menu;
	}

	public static void sendSms(Activity activity, String sms) {
		Intent sendIntent = new Intent(Intent.ACTION_VIEW);
		sendIntent.putExtra("sms_body", sms);
		sendIntent.setType("vnd.android-dir/mms-sms");
		AndroidUtils.startActivityIfSafe(activity, sendIntent);
	}

	public static void sendEmail(Activity activity, String email) {
		Intent intent = new Intent(Intent.ACTION_SEND);
		intent.setType("vnd.android.cursor.dir/email");
		intent.putExtra(Intent.EXTRA_SUBJECT, "Location");
		intent.putExtra(Intent.EXTRA_TEXT, Html.fromHtml(email));
		intent.setType("text/html");
		Intent chooserIntent = Intent.createChooser(intent, activity.getString(R.string.send_location));
		AndroidUtils.startActivityIfSafe(activity, intent, chooserIntent);
	}

	public static void sendMessage(Activity activity, String text) {
		Intent intent = new Intent(Intent.ACTION_SEND);
		intent.setAction(Intent.ACTION_SEND);
		intent.putExtra(Intent.EXTRA_TEXT, text);
		intent.setType("text/plain");
		Intent chooserIntent = Intent.createChooser(intent, activity.getString(R.string.send_location));
		AndroidUtils.startActivityIfSafe(activity, intent, chooserIntent);
	}

	public static void sendQRCode(Activity activity, String encodeType, Bundle encodeData, String strEncodeData) {
		Intent intent = new Intent();
		intent.addCategory(Intent.CATEGORY_DEFAULT);
		intent.setAction(ZXING_BARCODE_SCANNER_ACTIVITY);
		intent.putExtra("ENCODE_TYPE", encodeType);
		if (strEncodeData != null) {
			intent.putExtra("ENCODE_DATA", strEncodeData);
		} else {
			intent.putExtra("ENCODE_DATA", encodeData);
		}
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);

		if (!AndroidUtils.startActivityIfSafe(activity, intent)) {
			if (Version.isMarketEnabled()) {
				AlertDialog.Builder builder = new AlertDialog.Builder(activity);
				builder.setMessage(activity.getString(R.string.zxing_barcode_scanner_not_found));
				builder.setPositiveButton(activity.getString(R.string.shared_string_yes), (dialog, which) -> {
					OsmandApplication app = ((OsmandApplication) activity.getApplication());
					Uri uri = Uri.parse(Version.getUrlWithUtmRef(app, ZXING_BARCODE_SCANNER_COMPONENT));
					Intent viewIntent = new Intent(Intent.ACTION_VIEW, uri);
					AndroidUtils.startActivityIfSafe(activity, viewIntent);
				});
				builder.setNegativeButton(activity.getString(R.string.shared_string_no), null);
				builder.show();
			} else {
				Toast.makeText(activity, R.string.zxing_barcode_scanner_not_found, Toast.LENGTH_LONG).show();
			}
		}
	}

	public static void copyToClipboardWithToast(@NonNull Context context, @NonNull String text, int duration) {
		copyToClipboard(context, text, true, duration);
	}

	/**
	 * @return true if text was copied
	 */
	public static boolean copyToClipboard(@NonNull Context context, @NonNull String text) {
		return copyToClipboard(context, text, false, -1);
	}

	/**
	 * @return true if text was copied
	 */
	private static boolean copyToClipboard(@NonNull Context context, @NonNull String text,
										   boolean showToast, int duration) {
		Object object = context.getSystemService(Activity.CLIPBOARD_SERVICE);
		if (object instanceof ClipboardManager) {
			ClipboardManager clipboardManager = (ClipboardManager) object;
			clipboardManager.setPrimaryClip(ClipData.newPlainText("", text));
			if (showToast) {
				String toastMessage = context.getString(R.string.copied_to_clipboard) + ":\n" + text;
				Toast.makeText(context, toastMessage, duration).show();
			}
			return true;
		}
		return false;
	}
}