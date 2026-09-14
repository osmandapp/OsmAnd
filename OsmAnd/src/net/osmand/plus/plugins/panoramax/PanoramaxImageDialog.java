package net.osmand.plus.plugins.panoramax;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import net.osmand.data.LatLon;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.builders.cards.dialogs.ContextMenuCardDialog;
import net.osmand.plus.mapcontextmenu.builders.cards.dialogs.ContextMenuCardDialogFragment;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.OsmandMapTileView;

public class PanoramaxImageDialog extends ContextMenuCardDialog {

	private static final String KEY_PANORAMAX_DIALOG_IMAGE_ID = "key_panoramax_dialog_image_id";
	private static final String KEY_PANORAMAX_DIALOG_VIEWER_URL = "key_panoramax_dialog_viewer_url";
	private static final String KEY_PANORAMAX_DIALOG_LATLON = "key_panoramax_dialog_latlon";
	private static final String KEY_PANORAMAX_DIALOG_COMPASS_ANGLE = "key_panoramax_dialog_compass_angle";

	public static final String PANORAMAX_VIEWER_URL_TEMPLATE = PanoramaxConstants.VIEWER_URL_TEMPLATE;

	private String imageId;
	private String viewerUrl;
	private LatLon latLon;
	private double compassAngle = Double.NaN;
	private final UiUtilities iconsCache;

	public PanoramaxImageDialog(@NonNull MapActivity mapActivity, @NonNull Bundle bundle) {
		super(mapActivity, CardDialogType.PANORAMAX);
		restoreFields(bundle);
		this.iconsCache = mapActivity.getApp().getUIUtilities();
	}

	public PanoramaxImageDialog(MapActivity mapActivity, String imageId, String viewerUrl,
	                            LatLon latLon, double compassAngle, String title, String description) {
		super(mapActivity, CardDialogType.PANORAMAX);
		this.title = title;
		this.description = description;
		this.imageId = imageId;
		this.viewerUrl = viewerUrl;
		this.latLon = latLon;
		this.compassAngle = compassAngle;
		this.iconsCache = mapActivity.getApp().getUIUtilities();
	}

	public String getImageId() {
		return imageId;
	}

	public String getViewerUrl() {
		return viewerUrl;
	}

	public LatLon getLatLon() {
		return latLon;
	}

	public double getCompassAngle() {
		return compassAngle;
	}

	public void saveMenu(Bundle bundle) {
		super.saveMenu(bundle);
		bundle.putSerializable(KEY_PANORAMAX_DIALOG_IMAGE_ID, imageId);
		bundle.putSerializable(KEY_PANORAMAX_DIALOG_VIEWER_URL, viewerUrl);
		bundle.putSerializable(KEY_PANORAMAX_DIALOG_LATLON, latLon);
		bundle.putDouble(KEY_PANORAMAX_DIALOG_COMPASS_ANGLE, compassAngle);
	}

	@Override
	protected void restoreFields(Bundle bundle) {
		super.restoreFields(bundle);
		this.imageId = bundle.getString(KEY_PANORAMAX_DIALOG_IMAGE_ID);
		this.viewerUrl = bundle.getString(KEY_PANORAMAX_DIALOG_VIEWER_URL);
		this.latLon = AndroidUtils.getSerializable(bundle, KEY_PANORAMAX_DIALOG_LATLON, LatLon.class);
		this.compassAngle = bundle.getDouble(KEY_PANORAMAX_DIALOG_COMPASS_ANGLE, Double.NaN);
	}

	public void onResume() {
		super.onResume();
		setImageLocation(latLon, compassAngle, true);
	}

	public void onPause() {
		super.onPause();
		setImageLocation(null, Double.NaN, false);
	}

	private void setImageLocation(LatLon latLon, double compassAngle, boolean animated) {
		MapActivity mapActivity = getMapActivity();
		OsmandMapTileView mapView = mapActivity.getMapView();
		updateLayer(mapView.getLayerByClass(PanoramaxVectorLayer.class), latLon, compassAngle);
		if (latLon != null) {
			if (animated) {
				mapView.getAnimatedDraggingThread().startMoving(
						latLon.getLatitude(), latLon.getLongitude(), mapView.getZoom());
			} else {
				mapActivity.getApp().getOsmandMap().setMapLocation(latLon.getLatitude(), latLon.getLongitude());
			}
		} else {
			mapActivity.refreshMap();
		}
	}

	private void updateLayer(PanoramaxLayer layer, LatLon latLon, double compassAngle) {
		if (layer != null) {
			layer.setSelectedImageLocation(latLon);
			if (!Double.isNaN(compassAngle)) {
				layer.setSelectedImageCameraAngle((float) compassAngle);
			} else {
				layer.setSelectedImageCameraAngle(null);
			}
		}
	}

	public View getContentView() {
		return getWebView();
	}

	@Override
	protected boolean haveMenuItems() {
		return true;
	}

	@Override
	protected void createMenuItems(Menu menu) {
		MenuItem item = menu.add(R.string.open_panoramax)
				.setIcon(iconsCache.getThemedIcon(R.drawable.ic_action_photo_street));
		item.setOnMenuItemClickListener(i -> {
			PanoramaxPlugin.openPanoramax(getMapActivity(), imageId);
			return true;
		});
	}

	@SuppressLint("SetJavaScriptEnabled")
	private View getWebView() {
		View view = getMapActivity().getLayoutInflater().inflate(R.layout.panoramax_web_view, null);
		WebView webView = view.findViewById(R.id.webView);
		webView.setBackgroundColor(Color.argb(1, 0, 0, 0));
		View noInternetView = view.findViewById(R.id.panoramaxNoInternetLayout);
		Drawable icWifiOff = iconsCache.getThemedIcon(R.drawable.ic_action_wifi_off);
		((ImageView) noInternetView.findViewById(R.id.wifiOff)).setImageDrawable(icWifiOff);
		view.setScrollContainer(false);
		webView.getSettings().setJavaScriptEnabled(true);
		// The Panoramax viewer is a single page app that reads localStorage on startup. Mapillary
		// is served through an osmand.net proxy page that does not, so DOM storage was never
		// enabled here. Without it the viewer throws on window.localStorage.getItem and stalls
		// before it ever shows the picture.
		webView.getSettings().setDomStorageEnabled(true);
		// No Android JavaScript bridge here, unlike MapillaryImageDialog. That bridge works
		// only because Mapillary is loaded from an osmand.net proxy page that OsmAnd authors
		// and which calls Android.onNodeChanged() itself. This WebView loads the Panoramax
		// viewer directly, so an injected interface would never be called, and exposing one
		// to a third party page is a trust boundary OsmAnd does not need to cross.
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
				isPortrait() ? ViewGroup.LayoutParams.MATCH_PARENT : AndroidUtils.dpToPx(getMapActivity(), 360f),
				isPortrait() ? AndroidUtils.dpToPx(getMapActivity(), 270f) : ViewGroup.LayoutParams.MATCH_PARENT);
		view.setLayoutParams(lp);
		webView.setWebViewClient(new WebViewClient() {
			@SuppressWarnings("deprecation")
			@Override
			public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
				webView.loadUrl("about:blank");
				noInternetView.setVisibility(View.VISIBLE);
			}

			@TargetApi(android.os.Build.VERSION_CODES.M)
			@Override
			public void onReceivedError(WebView view, WebResourceRequest req, WebResourceError rerr) {
				// This overload also reports subresource failures; only a failed document means
				// the viewer is unusable. The page pulls scripts from several CDNs.
				if (req.isForMainFrame()) {
					onReceivedError(view, rerr.getErrorCode(), rerr.getDescription().toString(), req.getUrl().toString());
				}
			}
		});
		noInternetView.findViewById(R.id.retry_button).setOnClickListener(v -> {
			noInternetView.setVisibility(View.GONE);
			webView.loadUrl(viewerUrl);
		});
		webView.loadUrl(viewerUrl);
		return view;
	}

	public static PanoramaxImageDialog show(MapActivity mapActivity, double latitude, double longitude,
	                                        String imageId, double compassAngle,
	                                        String title, String description) {
		String viewerUrl = PANORAMAX_VIEWER_URL_TEMPLATE + imageId;
		LatLon latLon = new LatLon(latitude, longitude);
		PanoramaxImageDialog dialog = new PanoramaxImageDialog(mapActivity, imageId, viewerUrl,
				latLon, compassAngle, title, description);
		ContextMenuCardDialogFragment.showInstance(dialog);
		return dialog;
	}
}
