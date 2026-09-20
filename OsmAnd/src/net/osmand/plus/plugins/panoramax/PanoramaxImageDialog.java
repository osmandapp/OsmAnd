package net.osmand.plus.plugins.panoramax;

import android.annotation.SuppressLint;
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
import android.widget.TextView;
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
	private static final String KEY_PANORAMAX_DIALOG_LATLON = "key_panoramax_dialog_latlon";
	private static final String KEY_PANORAMAX_DIALOG_COMPASS_ANGLE = "key_panoramax_dialog_compass_angle";

	private static final String VIEWER_ERROR_URL = "osmand-panoramax://viewer-failed";

	private static final int VIEWER_TIMEOUT_MS = 20000;

	private static final String VIEWER_STYLE =
			"html,body{margin:0;height:100%;background:#000;overflow:hidden}"
					+ "pnx-photo-viewer{display:block;width:100%;height:100%}"
					+ "#attribution{position:absolute;left:0;right:0;bottom:0;padding:5px 8px;"
					+ "font:12px/1.2 sans-serif;color:#fff;background:rgba(0,0,0,0.45);"
					+ "white-space:nowrap;overflow:hidden;text-overflow:ellipsis;pointer-events:none;"
					+ "transition:opacity 0.5s ease-in-out,transform 0.5s ease-in-out}"
					+ "#viewer.pnx-grid-toggled ~ #attribution{opacity:0;transform:translateY(100%)}";

	private static final String VIEWER_SCRIPT =
			"(function(){"
					+ "var viewer=document.getElementById('viewer');"
					+ "var attribution=document.getElementById('attribution');"
					+ "function attributionText(){"
					+ "var meta=viewer.psv&&viewer.psv.getPictureMetadata();"
					+ "var caption=meta&&meta.caption;"
					+ "var parts=['\\u00A9 Panoramax'];"
					+ "if(caption&&caption.producer&&caption.producer.length){"
					+ "parts.push(caption.producer[caption.producer.length-1]);}"
					+ "if(caption&&caption.date instanceof Date&&!isNaN(caption.date)){"
					+ "parts.push(caption.date.toLocaleDateString());}"
					+ "if(meta&&meta.properties&&meta.properties.license){"
					+ "parts.push(meta.properties.license);}"
					+ "return parts.join(' \\u00B7 ');}"
					+ "function onPictureLoaded(){attribution.textContent=attributionText();}"
					+ "viewer.addEventListener('psv:picture-loaded',onPictureLoaded);"
					+ "setTimeout(function(){"
					+ "if(!customElements.get('pnx-photo-viewer')){pnxFail();}"
					+ "}," + VIEWER_TIMEOUT_MS + ");"
					+ "})();";

	private String imageId;
	private LatLon latLon;
	private double compassAngle = Double.NaN;
	private final UiUtilities iconsCache;

	public PanoramaxImageDialog(@NonNull MapActivity mapActivity, @NonNull Bundle bundle) {
		super(mapActivity, CardDialogType.PANORAMAX);
		restoreFields(bundle);
		this.iconsCache = mapActivity.getApp().getUIUtilities();
	}

	public PanoramaxImageDialog(MapActivity mapActivity, String imageId,
	                            LatLon latLon, double compassAngle, String title, String description) {
		super(mapActivity, CardDialogType.PANORAMAX);
		this.title = title;
		this.description = description;
		this.imageId = imageId;
		this.latLon = latLon;
		this.compassAngle = compassAngle;
		this.iconsCache = mapActivity.getApp().getUIUtilities();
	}

	public String getImageId() {
		return imageId;
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
		bundle.putSerializable(KEY_PANORAMAX_DIALOG_LATLON, latLon);
		bundle.putDouble(KEY_PANORAMAX_DIALOG_COMPASS_ANGLE, compassAngle);
	}

	@Override
	protected void restoreFields(Bundle bundle) {
		super.restoreFields(bundle);
		this.imageId = bundle.getString(KEY_PANORAMAX_DIALOG_IMAGE_ID);
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
		((TextView) noInternetView.findViewById(R.id.no_internet_description))
				.setText(R.string.panoramax_no_internet_desc);
		view.setScrollContainer(false);
		webView.getSettings().setJavaScriptEnabled(true);
		// The viewer reads localStorage on startup and stalls without it.
		webView.getSettings().setDomStorageEnabled(true);
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
				isPortrait() ? ViewGroup.LayoutParams.MATCH_PARENT : AndroidUtils.dpToPx(getMapActivity(), 360f),
				isPortrait() ? AndroidUtils.dpToPx(getMapActivity(), 270f) : ViewGroup.LayoutParams.MATCH_PARENT);
		view.setLayoutParams(lp);
		webView.setWebViewClient(new WebViewClient() {
			@Override
			public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
				if (VIEWER_ERROR_URL.equals(request.getUrl().toString())) {
					showViewerError(webView, noInternetView);
					return true;
				}
				return false;
			}

			@Override
			public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
				// The page itself is loaded from memory and never fails, so the bundle is the
				// only request whose failure leaves the viewer unusable.
				if (PanoramaxConstants.VIEWER_BUNDLE_URL.equals(request.getUrl().toString())) {
					showViewerError(webView, noInternetView);
				}
			}
		});
		noInternetView.findViewById(R.id.retry_button).setOnClickListener(v -> loadViewer(webView, noInternetView));
		loadViewer(webView, noInternetView);
		return view;
	}

	private void showViewerError(@NonNull WebView webView, @NonNull View noInternetView) {
		webView.post(() -> {
			webView.loadUrl("about:blank");
			noInternetView.setVisibility(View.VISIBLE);
		});
	}

	private void loadViewer(@NonNull WebView webView, @NonNull View noInternetView) {
		boolean online = getMapActivity().getApp().getSettings().isInternetConnectionAvailable(true);
		noInternetView.setVisibility(online ? View.GONE : View.VISIBLE);
		if (online) {
			webView.loadDataWithBaseURL(PanoramaxConstants.INSTANCE_URL, buildViewerHtml(),
					"text/html", "UTF-8", null);
		}
	}

	@NonNull
	private String buildViewerHtml() {
		return "<!DOCTYPE html><html><head>"
				+ "<meta charset='utf-8'>"
				+ "<meta name='viewport' content='width=device-width,initial-scale=1,user-scalable=no'>"
				+ "<style>" + VIEWER_STYLE + "</style>"
				// The bundle ends with a CommonJS assignment that throws without this.
				+ "<script>var exports={};"
				+ "function pnxFail(){location.href='" + VIEWER_ERROR_URL + "';}</script>"
				+ "<script src='" + PanoramaxConstants.VIEWER_BUNDLE_URL + "'"
				+ " integrity='" + PanoramaxConstants.VIEWER_BUNDLE_INTEGRITY + "'"
				+ " crossorigin='anonymous' onerror='pnxFail()'></script>"
				+ "</head><body>"
				+ "<pnx-photo-viewer id='viewer'"
				+ " endpoint='" + PanoramaxConstants.API_URL + "'"
				+ " picture='" + escapeAttribute(imageId) + "'"
				+ " widgets='false' url-parameters='false' keyboard-shortcuts='false'>"
				+ "<pnx-widget-player slot='top' size='md'></pnx-widget-player>"
				+ "</pnx-photo-viewer>"
				+ "<div id='attribution'>&#169; Panoramax</div>"
				+ "<script>" + VIEWER_SCRIPT + "</script>"
				+ "</body></html>";
	}

	/** Picture ids come from third party tiles, so they cannot be trusted inside the markup. */
	@NonNull
	private static String escapeAttribute(String value) {
		return value == null ? "" : value
				.replace("&", "&amp;")
				.replace("'", "&#39;")
				.replace("<", "&lt;");
	}

	public static PanoramaxImageDialog show(MapActivity mapActivity, double latitude, double longitude,
	                                        String imageId, double compassAngle,
	                                        String title, String description) {
		LatLon latLon = new LatLon(latitude, longitude);
		PanoramaxImageDialog dialog = new PanoramaxImageDialog(mapActivity, imageId,
				latLon, compassAngle, title, description);
		ContextMenuCardDialogFragment.showInstance(dialog);
		return dialog;
	}
}
