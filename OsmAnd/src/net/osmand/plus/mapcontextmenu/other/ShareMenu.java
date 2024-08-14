package net.osmand.plus.mapcontextmenu.other;

import static net.osmand.plus.mapcontextmenu.other.ShareSheetReceiver.KEY_SHARE_ACTION_ID;
import static net.osmand.plus.mapcontextmenu.other.ShareSheetReceiver.KEY_SHARE_ADDRESS;
import static net.osmand.plus.mapcontextmenu.other.ShareSheetReceiver.KEY_SHARE_COORDINATES;
import static net.osmand.plus.mapcontextmenu.other.ShareSheetReceiver.KEY_SHARE_GEOURL;
import static net.osmand.plus.mapcontextmenu.other.ShareSheetReceiver.KEY_SHARE_SMS;
import static net.osmand.plus.mapcontextmenu.other.ShareSheetReceiver.KEY_SHARE_TITLE;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.service.chooser.ChooserAction;
import android.text.Html;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.text.TextUtilsCompat;
import androidx.core.view.ViewCompat;

import net.osmand.LocationConvert;
import net.osmand.PlatformUtil;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
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

	private LatLon latLon;
	private String title;
	private String address;
	private String coordinates;
	private String geoUrl;
	private String sms;

	private ShareMenu(MapActivity mapActivity) {
		super(mapActivity);
	}

	@NonNull
	public List<ShareItem> getItems() {
		List<ShareItem> list = new LinkedList<>();
		list.add(ShareItem.MESSAGE);
		list.add(ShareItem.CLIPBOARD);
		list.add(ShareItem.ADDRESS);
		list.add(ShareItem.NAME);
		list.add(ShareItem.COORDINATES);
		list.add(ShareItem.GEO);
		return list;
	}

	public LatLon getLatLon() {
		return latLon;
	}

	public String getTitle() {
		return title;
	}

	public static void show(LatLon latLon, String title, String address, @NonNull MapActivity activity) {
		ShareMenu menu = new ShareMenu(activity);
		menu.latLon = latLon;
		menu.title = title;
		menu.address = address;

		if (Build.VERSION.SDK_INT >= 34) {
			showNativeShareDialog(menu, activity);
		} else {
			ShareMenuFragment.showInstance(activity.getSupportFragmentManager(), menu);
		}
	}

	public void share(@NonNull ShareItem item) {
		MapActivity activity = getMapActivity();
		if (activity != null) {
			setupSharingFields(activity);
			startAction(activity, item, sms, address, title, coordinates, geoUrl);
		}
	}

	@RequiresApi(api = 34)
	private static void showNativeShareDialog(@NonNull ShareMenu menu, @NonNull MapActivity activity) {
		menu.setupSharingFields(activity);

		OsmandApplication app = activity.getMyApplication();
		List<ShareItem> items = ShareItem.getNativeShareItems();
		ChooserAction[] actions = new ChooserAction[items.size()];

		Intent intent = new Intent(app, ShareSheetReceiver.class);
		intent.putExtra(KEY_SHARE_SMS, menu.sms);
		intent.putExtra(KEY_SHARE_ADDRESS, menu.address);
		intent.putExtra(KEY_SHARE_TITLE, menu.title);
		intent.putExtra(KEY_SHARE_COORDINATES, menu.coordinates);
		intent.putExtra(KEY_SHARE_GEOURL, menu.geoUrl);

		for (int i = 0; i < items.size(); i++) {
			ShareItem item = items.get(i);
			intent.putExtra(KEY_SHARE_ACTION_ID, item.ordinal());

			PendingIntent pendingIntent = PendingIntent.getBroadcast(app, item.ordinal(),
					intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

			actions[i] = new ChooserAction.Builder(Icon.createWithResource(app, item.getIconId()),
					activity.getString(item.getTitleId()), pendingIntent).build();
		}

		Intent sendIntent = new Intent(Intent.ACTION_SEND);
		sendIntent.setType("text/plain");
		sendIntent.putExtra(Intent.EXTRA_TEXT, menu.sms);
		sendIntent.setAction(Intent.ACTION_SEND);

		Intent shareIntent = Intent.createChooser(sendIntent, null);
		shareIntent.putExtra(Intent.EXTRA_CHOOSER_CUSTOM_ACTIONS, actions);
		AndroidUtils.startActivityIfSafe(activity.getMyApplication(), sendIntent, shareIntent);
	}

	private void setupSharingFields(@NonNull MapActivity activity) {
		StringBuilder builder = new StringBuilder();
		if (!Algorithms.isEmpty(title)) {
			builder.append(title).append("\n");
		}
		if (!Algorithms.isEmpty(address) && !address.equals(title) && !address.equals(activity.getString(R.string.no_address_found))) {
			builder.append(address).append("\n");
		}
		builder.append(activity.getString(R.string.shared_string_location)).append(": ");
		if (TextUtilsCompat.getLayoutDirectionFromLocale(Locale.getDefault()) == ViewCompat.LAYOUT_DIRECTION_RTL) {
			builder.append("\n");
		}

		geoUrl = "";
		String httpUrl = "";
		try {
			String lat = LocationConvert.convertLatitude(latLon.getLatitude(), LocationConvert.FORMAT_DEGREES, false);
			String lon = LocationConvert.convertLongitude(latLon.getLongitude(), LocationConvert.FORMAT_DEGREES, false);
			lat = lat.substring(0, lat.length() - 1);
			lon = lon.substring(0, lon.length() - 1);
			int zoom = activity.getMapView().getZoom();
			geoUrl = MapUtils.buildGeoUrl(lat, lon, zoom);
			httpUrl = "https://osmand.net/map?pin=" + lat + "," + lon + "#" + zoom + "/" + lat + "/" + lon;
		} catch (RuntimeException e) {
			log.error("Failed to convert coordinates", e);
		}
		if (!Algorithms.isEmpty(geoUrl) && !Algorithms.isEmpty(httpUrl)) {
			builder.append(geoUrl).append("\n").append(httpUrl);
		}
		sms = builder.toString();

		OsmandSettings settings = ((OsmandApplication) activity.getApplicationContext()).getSettings();
		int format = settings.COORDINATES_FORMAT.get();
		coordinates = OsmAndFormatter.getFormattedCoordinates(latLon.getLatitude(), latLon.getLongitude(), format);
	}

	public static void startAction(@NonNull Context context, @NonNull ShareItem item,
	                               @NonNull String sms, @NonNull String address, @NonNull String title,
	                               @NonNull String coordinates, @NonNull String geoUrl) {
		switch (item) {
			case MESSAGE:
				sendMessage(context, sms);
				break;
			case CLIPBOARD:
				copyToClipboardWithToast(context, sms, Toast.LENGTH_LONG);
				break;
			case ADDRESS:
				if (!Algorithms.isEmpty(address)) {
					copyToClipboardWithToast(context, address, Toast.LENGTH_LONG);
				} else {
					Toast.makeText(context, R.string.no_address_found, Toast.LENGTH_LONG).show();
				}
				break;
			case NAME:
				if (!Algorithms.isEmpty(title)) {
					copyToClipboardWithToast(context, title, Toast.LENGTH_LONG);
				} else {
					Toast.makeText(context, R.string.toast_empty_name_error, Toast.LENGTH_LONG).show();
				}
				break;
			case COORDINATES:
				copyToClipboardWithToast(context, coordinates, Toast.LENGTH_LONG);
				break;
			case GEO:
				if (!Algorithms.isEmpty(geoUrl)) {
					Intent mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(geoUrl));
					AndroidUtils.startActivityIfSafe(context, mapIntent);
				}
				break;
		}
	}

	public void saveMenu(@NonNull Bundle bundle) {
		bundle.putSerializable(KEY_SHARE_MENU_LATLON, latLon);
		bundle.putString(KEY_SHARE_MENU_POINT_TITLE, title);
	}

	@NonNull
	public static ShareMenu restoreMenu(@NonNull Bundle bundle, @NonNull MapActivity activity) {
		ShareMenu menu = new ShareMenu(activity);
		menu.title = bundle.getString(KEY_SHARE_MENU_POINT_TITLE);
		menu.latLon = AndroidUtils.getSerializable(bundle, KEY_SHARE_MENU_LATLON, LatLon.class);
		return menu;
	}

	public static void sendSms(@NonNull Activity activity, @NonNull String sms) {
		Intent sendIntent = new Intent(Intent.ACTION_VIEW);
		sendIntent.putExtra("sms_body", sms);
		sendIntent.setType("vnd.android-dir/mms-sms");
		AndroidUtils.startActivityIfSafe(activity, sendIntent);
	}

	public static void sendEmail(@NonNull Activity activity, @NonNull String email) {
		Intent intent = new Intent(Intent.ACTION_SEND);
		intent.setType("vnd.android.cursor.dir/email");
		intent.putExtra(Intent.EXTRA_SUBJECT, "Location");
		intent.putExtra(Intent.EXTRA_TEXT, Html.fromHtml(email));
		intent.setType("text/html");
		Intent chooserIntent = Intent.createChooser(intent, activity.getString(R.string.send_location));
		AndroidUtils.startActivityIfSafe(activity, intent, chooserIntent);
	}

	public static void sendMessage(@NonNull Context context, @NonNull String text) {
		Intent intent = new Intent(Intent.ACTION_SEND);
		intent.setAction(Intent.ACTION_SEND);
		intent.putExtra(Intent.EXTRA_TEXT, text);
		intent.setType("text/plain");
		Intent chooserIntent = Intent.createChooser(intent, context.getString(R.string.send_location));
		AndroidUtils.startActivityIfSafe(context, intent, chooserIntent);
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