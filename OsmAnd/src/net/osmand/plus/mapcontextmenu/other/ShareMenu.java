package net.osmand.plus.mapcontextmenu.other;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.MapActivityActions;
import net.osmand.plus.activities.actions.ShareDialog;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.mapcontextmenu.details.MenuController;
import net.osmand.util.MapUtils;

import java.util.LinkedList;
import java.util.List;

public class ShareMenu {

	private final MapActivity mapActivity;

	private LatLon latLon;
	private PointDescription pointDescription;
	private boolean portraitMode;
	private boolean largeDevice;

	private static final String KEY_SHARE_MENU_LATLON = "key_share_menu_latlon";
	private static final String KEY_SHARE_MENU_POINT_DESC = "key_share_menu_point_desc";

	public enum ShareItem {
		MESSAGE(R.drawable.ic_action_aircraft, R.string.shared_string_message),
		CLIPBOARD(R.drawable.ic_action_aircraft, R.string.share_clipboard),
		GEO(R.drawable.ic_action_aircraft, R.string.share_geo),
		QR_CODE(R.drawable.ic_action_aircraft, R.string.share_qr_code);

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
		this.mapActivity = mapActivity;
		portraitMode = AndroidUiHelper.isOrientationPortrait(mapActivity);
		largeDevice = AndroidUiHelper.isXLargeDevice(mapActivity);
	}

	public List<ShareItem> getItems() {
		List<ShareItem> list = new LinkedList<>();
		list.add(ShareItem.MESSAGE);
		list.add(ShareItem.CLIPBOARD);
		list.add(ShareItem.GEO);
		list.add(ShareItem.QR_CODE);
		return list;
	}

	public boolean isLandscapeLayout() {
		return !portraitMode && !largeDevice;
	}

	public int getSlideInAnimation() {
		if (isLandscapeLayout()) {
			return R.anim.slide_in_left;
		} else {
			return R.anim.slide_in_bottom;
		}
	}

	public int getSlideOutAnimation() {
		if (isLandscapeLayout()) {
			return R.anim.slide_out_left;
		} else {
			return R.anim.slide_out_bottom;
		}
	}

	public MapActivity getMapActivity() {
		return mapActivity;
	}

	public LatLon getLatLon() {
		return latLon;
	}

	public PointDescription getPointDescription() {
		return pointDescription;
	}

	public static void show(LatLon latLon, PointDescription pointDescription, MapActivity mapActivity) {

		ShareMenu menu = new ShareMenu(mapActivity);

		if (pointDescription == null) {
			menu.pointDescription = new PointDescription(latLon.getLatitude(), latLon.getLongitude());
		} else {
			menu.pointDescription = pointDescription;
		}

		menu.latLon = latLon;

		ShareMenuFragment.showInstance(menu);
	}

	public void share(ShareItem item) {
		final int zoom = mapActivity.getMapView().getZoom();
		final String geoUrl = MapUtils.buildGeoUrl(latLon.getLatitude(), latLon.getLongitude(), zoom);
		final String httpUrl = "http://osmand.net/go?lat=" + ((float) latLon.getLatitude())
				+ "&lon=" + ((float) latLon.getLongitude()) + "&z=" + zoom;
		String sms = mapActivity.getString(R.string.send_location_sms_pattern, geoUrl, httpUrl);
		switch (item) {
			case MESSAGE:
				ShareDialog.sendMessage(mapActivity, sms);
				break;
			case CLIPBOARD:
				ShareDialog.sendToClipboard(mapActivity, sms);
				break;
			case GEO:
				Intent mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(geoUrl));
				mapActivity.startActivity(mapIntent);
				break;
			case QR_CODE:
				Bundle bundle = new Bundle();
				bundle.putFloat("LAT", (float) latLon.getLatitude());
				bundle.putFloat("LONG", (float) latLon.getLongitude());
				ShareDialog.sendQRCode(mapActivity, "LOCATION_TYPE", bundle, null);
				break;
		}
	}

	public float getLandscapeWidthDp() {
		return MenuController.LANDSCAPE_WIDTH_DP;
	}

	public void saveMenu(Bundle bundle) {
		bundle.putSerializable(KEY_SHARE_MENU_LATLON, latLon);
		bundle.putSerializable(KEY_SHARE_MENU_POINT_DESC, pointDescription);
	}

	public static ShareMenu restoreMenu(Bundle bundle, MapActivity mapActivity) {

		ShareMenu menu = new ShareMenu(mapActivity);

		Object pDescObj = bundle.getSerializable(KEY_SHARE_MENU_POINT_DESC);
		if (pDescObj != null) {
			menu.pointDescription = (PointDescription) pDescObj;
		}
		Object latLonObj = bundle.getSerializable(KEY_SHARE_MENU_LATLON);
		if (latLonObj != null) {
			menu.latLon = (LatLon) latLonObj;
		}

		return menu;
	}

}
