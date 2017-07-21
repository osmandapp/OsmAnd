package net.osmand.plus.mapcontextmenu.other;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import net.osmand.data.LatLon;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.actions.ShareDialog;
import net.osmand.plus.mapcontextmenu.BaseMenuController;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.util.LinkedList;
import java.util.List;

public class ShareMenu extends BaseMenuController {

	private LatLon latLon;
	private String title;
	private String address;

	private static final String KEY_SHARE_MENU_LATLON = "key_share_menu_latlon";
	private static final String KEY_SHARE_MENU_POINT_TITLE = "key_share_menu_point_title";

	public enum ShareItem {
		MESSAGE(R.drawable.ic_action_message, R.string.shared_string_send),
		CLIPBOARD(R.drawable.ic_action_copy, R.string.shared_string_copy),
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

	public static void show(LatLon latLon, String title, String address, MapActivity mapActivity) {

		ShareMenu menu = new ShareMenu(mapActivity);

		menu.latLon = latLon;
		menu.title = title;
		menu.address = address;

		ShareMenuFragment.showInstance(menu);
	}

	public void share(ShareItem item) {
		final int zoom = getMapActivity().getMapView().getZoom();
		final String geoUrl = MapUtils.buildGeoUrl(latLon.getLatitude(), latLon.getLongitude(), zoom);
		final String httpUrl = "https://osmand.net/go?lat=" + ((float) latLon.getLatitude())
				+ "&lon=" + ((float) latLon.getLongitude()) + "&z=" + zoom;
		StringBuilder sb = new StringBuilder();
		if (!Algorithms.isEmpty(title)) {
			sb.append(title).append("\n");
		}
		if (!Algorithms.isEmpty(address) && !address.equals(title) && !address.equals(getMapActivity().getString(R.string.no_address_found))) {
			sb.append(address).append("\n");
		}
		sb.append(getMapActivity().getString(R.string.shared_string_location)).append(": ");
		sb.append(geoUrl).append("\n").append(httpUrl);
		String sms = sb.toString();
		switch (item) {
			case MESSAGE:
				ShareDialog.sendMessage(getMapActivity(), sms);
				break;
			case CLIPBOARD:
				ShareDialog.sendToClipboard(getMapActivity(), sms);
				break;
			case GEO:
				Intent mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(geoUrl));
				getMapActivity().startActivity(mapIntent);
				break;
			case QR_CODE:
				Bundle bundle = new Bundle();
				bundle.putFloat("LAT", (float) latLon.getLatitude());
				bundle.putFloat("LONG", (float) latLon.getLongitude());
				ShareDialog.sendQRCode(getMapActivity(), "LOCATION_TYPE", bundle, null);
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
		Object latLonObj = bundle.getSerializable(KEY_SHARE_MENU_LATLON);
		if (latLonObj != null) {
			menu.latLon = (LatLon) latLonObj;
		}

		return menu;
	}

}
