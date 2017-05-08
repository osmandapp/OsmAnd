package net.osmand.plus.mapcontextmenu.other;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.LinearLayout;

import net.osmand.AndroidUtils;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.mapillary.MapillaryLayer;
import net.osmand.plus.views.OsmandMapTileView;

public class WebImageMenu {

	private MapActivity mapActivity;

	private static final String KEY_WEB_IMAGE_MENU_TYPE = "key_web_image_menu_type";
	private static final String KEY_WEB_IMAGE_MENU_VIEWER_URL = "key_web_image_menu_viewer_url";
	private static final String KEY_WEB_IMAGE_MENU_LATLON = "key_web_image_menu_latlon";
	private static final String KEY_WEB_IMAGE_MENU_CA = "key_web_image_menu_ca";
	private static final String KEY_WEB_IMAGE_MENU_TITLE = "key_web_image_menu_title";
	private static final String KEY_WEB_IMAGE_MENU_DESCRIPTION = "key_web_image_menu_description";

	private WebImageType type;
	private String viewerUrl;
	private LatLon latLon;
	private double ca = Double.NaN;
	private String title;
	private String description;

	private int prevMapPosition = OsmandSettings.CENTER_CONSTANT;

	public enum WebImageType {
		MAPILLARY
	}

	private WebImageMenu(MapActivity mapActivity, @NonNull WebImageType type, @NonNull String viewerUrl,
						 LatLon latLon, double ca, String title, String description) {
		this.mapActivity = mapActivity;
		this.type = type;
		this.viewerUrl = viewerUrl;
		this.latLon = latLon;
		this.ca = ca;
		this.title = title;
		this.description = description;
	}

	public MapActivity getMapActivity() {
		return mapActivity;
	}

	public WebImageType getType() {
		return type;
	}

	public String getViewerUrl() {
		return viewerUrl;
	}

	public LatLon getLatLon() {
		return latLon;
	}

	public double getCa() {
		return ca;
	}

	public String getTitle() {
		return title;
	}

	public String getDescription() {
		return description;
	}

	public void saveMenu(Bundle bundle) {
		bundle.putString(KEY_WEB_IMAGE_MENU_TYPE, type.name());
		bundle.putSerializable(KEY_WEB_IMAGE_MENU_VIEWER_URL, viewerUrl);
		bundle.putSerializable(KEY_WEB_IMAGE_MENU_LATLON, latLon);
		bundle.putString(KEY_WEB_IMAGE_MENU_TITLE, title);
		bundle.putDouble(KEY_WEB_IMAGE_MENU_CA, ca);
		if (description != null) {
			bundle.putString(KEY_WEB_IMAGE_MENU_DESCRIPTION, description);
		}
	}

	public static WebImageMenu restoreMenu(Bundle bundle, MapActivity mapActivity) {

		try {
			WebImageType type = WebImageType.valueOf(bundle.getString(KEY_WEB_IMAGE_MENU_TYPE));
			String viewerUrl = bundle.getString(KEY_WEB_IMAGE_MENU_VIEWER_URL);
			LatLon latLon = null;
			String title = bundle.getString(KEY_WEB_IMAGE_MENU_TITLE);
			Object latLonObj = bundle.getSerializable(KEY_WEB_IMAGE_MENU_LATLON);
			if (latLonObj != null) {
				latLon = (LatLon) latLonObj;
			}
			Double ca = bundle.getDouble(KEY_WEB_IMAGE_MENU_CA, Double.NaN);
			String description = bundle.getString(KEY_WEB_IMAGE_MENU_TITLE);
			if (viewerUrl != null) {
				return new WebImageMenu(mapActivity, type, viewerUrl, latLon, ca, title, description);
			} else {
				return null;
			}
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	public void onResume() {
		setImageLocation(latLon, ca, true);
	}

	public void onPause() {
		restoreMapPosition();
		setImageLocation(null, Double.NaN, false);
	}

	public void setImageLocation(LatLon latLon, double ca, boolean animated) {
		switch (type) {

			case MAPILLARY:
				MapillaryLayer layer = mapActivity.getMapView().getLayerByClass(MapillaryLayer.class);
				if (layer != null) {
					layer.setSelectedImageLocation(latLon);
					if (!Double.isNaN(ca)) {
						layer.setSelectedImageCameraAngle((float) ca);
					} else {
						layer.setSelectedImageCameraAngle(null);
					}
				}
				break;
		}

		if (latLon != null) {
			shiftMapPosition();
			if (animated) {
				mapActivity.getMapView().getAnimatedDraggingThread().startMoving(
						latLon.getLatitude(), latLon.getLongitude(), mapActivity.getMapView().getZoom(), true);
			} else {
				mapActivity.setMapLocation(latLon.getLatitude(), latLon.getLongitude());
			}
		} else {
			mapActivity.refreshMap();
		}
	}

	private void shiftMapPosition() {
		OsmandMapTileView mapView = mapActivity.getMapView();
		if (AndroidUiHelper.isOrientationPortrait(mapActivity)) {
			if (mapView.getMapPosition() != OsmandSettings.MIDDLE_CONSTANT) {
				prevMapPosition = mapView.getMapPosition();
				mapView.setMapPosition(OsmandSettings.MIDDLE_CONSTANT);
			}
		} else {
			mapView.setMapPositionX(1);
		}
	}

	private void restoreMapPosition() {
		if (AndroidUiHelper.isOrientationPortrait(mapActivity)) {
			mapActivity.getMapView().setMapPosition(prevMapPosition);
		} else {
			mapActivity.getMapView().setMapPositionX(0);
		}
	}

	public View getContentView() {
		switch (type) {

			case MAPILLARY:
				return getWebView(viewerUrl);

			default:
				return null;
		}
	}

	@SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
	private WebView getWebView(String url) {
		final WebView webView = new WebView(mapActivity);
		webView.setBackgroundColor(Color.argb(1, 0, 0, 0));
		//webView.setLayerType(WebView.LAYER_TYPE_SOFTWARE, null);
		webView.setScrollContainer(false);
		webView.getSettings().setJavaScriptEnabled(true);
		if (type == WebImageType.MAPILLARY) {
			webView.addJavascriptInterface(new MapillaryWebAppInterface(mapActivity), "Android");
		}
		boolean portrait = AndroidUiHelper.isOrientationPortrait(mapActivity);
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
				portrait ? ViewGroup.LayoutParams.MATCH_PARENT : AndroidUtils.dpToPx(mapActivity, 360f),
				portrait ? AndroidUtils.dpToPx(mapActivity, 270f) : ViewGroup.LayoutParams.MATCH_PARENT);
		webView.setLayoutParams(lp);
		webView.loadUrl(url);
		return webView;
	}

	private class MapillaryWebAppInterface {
		Context mContext;

		MapillaryWebAppInterface(Context c) {
			mContext = c;
		}

		@JavascriptInterface
		public void onNodeChanged(double latitude, double longitude, double ca) {
			LatLon latLon = null;
			if (!Double.isNaN(latitude) && !Double.isNaN(longitude)) {
				latLon = new LatLon(latitude, longitude);
			}
			setImageLocation(latLon, ca, false);
		}
	}

	public static WebImageMenu show(MapActivity mapActivity, WebImageType type, String viewerUrl, LatLon latLon, double ca,
							String title, String description) {
		WebImageMenu menu = new WebImageMenu(mapActivity, type, viewerUrl, latLon, ca, title, description);
		WebImageMenuFragment.showInstance(menu);
		return menu;
	}
}
