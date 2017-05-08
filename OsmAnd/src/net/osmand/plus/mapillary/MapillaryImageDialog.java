package net.osmand.plus.mapillary;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import net.osmand.AndroidNetworkUtils;
import net.osmand.AndroidUtils;
import net.osmand.data.LatLon;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.builders.cards.ImageCard;
import net.osmand.plus.mapcontextmenu.builders.cards.dialogs.ContextMenuCardDialog;
import net.osmand.plus.mapcontextmenu.builders.cards.dialogs.ContextMenuCardDialogFragment;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.util.Algorithms;

public class MapillaryImageDialog extends ContextMenuCardDialog {

	private static final String KEY_MAPILLARY_DIALOG_IMAGE_KEY = "key_mapillary_dialog_image_key";
	private static final String KEY_MAPILLARY_DIALOG_IMAGE_URL = "key_mapillary_dialog_image_url";
	private static final String KEY_MAPILLARY_DIALOG_VIEWER_URL = "key_mapillary_dialog_viewer_url";
	private static final String KEY_MAPILLARY_DIALOG_LATLON = "key_mapillary_dialog_latlon";
	private static final String KEY_MAPILLARY_DIALOG_CA = "key_mapillary_dialog_ca";

	private static final String MAPILLARY_VIEWER_URL_TEMPLATE =
			"https://osmand.net/api/mapillary/photo-viewer.php?photo_id=";
	private static final String MAPILLARY_HIRES_IMAGE_URL_TEMPLATE =
			"https://osmand.net/api/mapillary/get_photo.php?hires=true&photo_id=";

	private static final String WEBGL_ERROR_MESSAGE = "Error creating WebGL context";

	private String key;
	private String imageUrl;
	private String viewerUrl;
	private LatLon latLon;
	private double ca = Double.NaN;

	public MapillaryImageDialog(@NonNull MapActivity mapActivity, @NonNull Bundle bundle) {
		super(mapActivity, CardDialogType.MAPILLARY);
		restoreFields(bundle);
	}

	public MapillaryImageDialog(MapActivity mapActivity, String key, String imageUrl, String viewerUrl, LatLon latLon, double ca,
								String title, String description) {
		super(mapActivity, CardDialogType.MAPILLARY);
		this.title = title;
		this.description = description;
		this.key = key;
		this.imageUrl = imageUrl;
		this.viewerUrl = viewerUrl;
		this.latLon = latLon;
		this.ca = ca;
	}

	public String getKey() {
		return key;
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

	public void saveMenu(Bundle bundle) {
		super.saveMenu(bundle);
		bundle.putSerializable(KEY_MAPILLARY_DIALOG_IMAGE_KEY, key);
		bundle.putSerializable(KEY_MAPILLARY_DIALOG_IMAGE_URL, imageUrl);
		bundle.putSerializable(KEY_MAPILLARY_DIALOG_VIEWER_URL, viewerUrl);
		bundle.putSerializable(KEY_MAPILLARY_DIALOG_LATLON, latLon);
		bundle.putDouble(KEY_MAPILLARY_DIALOG_CA, ca);
	}

	@Override
	protected void restoreFields(Bundle bundle) {
		super.restoreFields(bundle);
		this.key = bundle.getString(KEY_MAPILLARY_DIALOG_IMAGE_KEY);
		this.imageUrl = bundle.getString(KEY_MAPILLARY_DIALOG_IMAGE_URL);
		this.viewerUrl = bundle.getString(KEY_MAPILLARY_DIALOG_VIEWER_URL);
		Object latLonObj = bundle.getSerializable(KEY_MAPILLARY_DIALOG_LATLON);
		if (latLonObj != null) {
			this.latLon = (LatLon) latLonObj;
		}
		this.ca = bundle.getDouble(KEY_MAPILLARY_DIALOG_CA, Double.NaN);
	}

	public void onResume() {
		super.onResume();
		setImageLocation(latLon, ca, true);
	}

	public void onPause() {
		super.onPause();
		setImageLocation(null, Double.NaN, false);
	}

	private void setImageLocation(LatLon latLon, double ca, boolean animated) {
		OsmandMapTileView mapView = getMapActivity().getMapView();
		MapillaryLayer layer = mapView.getLayerByClass(MapillaryLayer.class);
		if (layer != null) {
			layer.setSelectedImageLocation(latLon);
			if (!Double.isNaN(ca)) {
				layer.setSelectedImageCameraAngle((float) ca);
			} else {
				layer.setSelectedImageCameraAngle(null);
			}
		}
		if (latLon != null) {
			if (animated) {
				mapView.getAnimatedDraggingThread().startMoving(
						latLon.getLatitude(), latLon.getLongitude(), mapView.getZoom(), true);
			} else {
				getMapActivity().setMapLocation(latLon.getLatitude(), latLon.getLongitude());
			}
		} else {
			getMapActivity().refreshMap();
		}
	}

	public View getContentView() {
		if (MapillaryPlugin.isWebGlSupported()) {
			return getWebView();
		} else {
			return getStaticImageView();
		}
	}

	@Override
	protected boolean haveMenuItems() {
		return true;
	}

	@Override
	protected void createMenuItems(Menu menu) {
		MenuItem item = menu.add(R.string.open_mapillary)
				.setIcon(getMapActivity().getMyApplication().getIconsCache().getThemedIcon(
						R.drawable.ic_action_mapillary));
		item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				MapillaryPlugin.openMapillary(getMapActivity(), key);
				return true;
			}
		});
	}


	@SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
	private WebView getWebView() {
		final WebView webView = new WebView(getMapActivity());
		webView.setBackgroundColor(Color.argb(1, 0, 0, 0));
		//webView.setLayerType(WebView.LAYER_TYPE_SOFTWARE, null);
		webView.setScrollContainer(false);
		webView.getSettings().setJavaScriptEnabled(true);
		webView.addJavascriptInterface(new MapillaryWebAppInterface(), "Android");
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
				isPortrait() ? ViewGroup.LayoutParams.MATCH_PARENT : AndroidUtils.dpToPx(getMapActivity(), 360f),
				isPortrait() ? AndroidUtils.dpToPx(getMapActivity(), 270f) : ViewGroup.LayoutParams.MATCH_PARENT);
		webView.setLayoutParams(lp);
		webView.setWebChromeClient(new WebChromeClient() {
			@Override
			public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
				if (!Algorithms.isEmpty(consoleMessage.message()) && consoleMessage.message().contains(WEBGL_ERROR_MESSAGE)) {
					MapillaryPlugin.setWebGlSupported(false);
					show(getMapActivity(), key, imageUrl, viewerUrl, getLatLon(), getCa(), getTitle(), getDescription());
				}
				return false;
			}
		});
		webView.loadUrl(viewerUrl);
		return webView;
	}

	private class MapillaryWebAppInterface {

		@JavascriptInterface
		public void onNodeChanged(double latitude, double longitude, double ca, String key) {
			LatLon latLon = null;
			if (!Double.isNaN(latitude) && !Double.isNaN(longitude)) {
				latLon = new LatLon(latitude, longitude);
				MapillaryImageDialog.this.latLon = latLon;
				MapillaryImageDialog.this.ca = ca;
				if (!Algorithms.isEmpty(key)) {
					MapillaryImageDialog.this.key = key;
					MapillaryImageDialog.this.imageUrl = MAPILLARY_HIRES_IMAGE_URL_TEMPLATE + key;
					MapillaryImageDialog.this.viewerUrl = MAPILLARY_VIEWER_URL_TEMPLATE + key;
				}
			}
			setImageLocation(latLon, ca, false);
		}
	}

	private View getStaticImageView() {
		LinearLayout ll = new LinearLayout(getMapActivity());
		ll.setClickable(true);
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
				isPortrait() ? ViewGroup.LayoutParams.MATCH_PARENT : AndroidUtils.dpToPx(getMapActivity(), 360f),
				isPortrait() ? AndroidUtils.dpToPx(getMapActivity(), 270f) : ViewGroup.LayoutParams.MATCH_PARENT);
		ll.setGravity(Gravity.CENTER);
		ll.setLayoutParams(lp);

		ProgressBar progressBar = new ProgressBar(getMapActivity());
		progressBar.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.WRAP_CONTENT));
		ll.addView(progressBar);

		ImageView imageView = new ImageView(getMapActivity());
		imageView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
		imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
		ll.addView(imageView);

		if (!Algorithms.isEmpty(imageUrl)) {
			ImageCard.execute(new DownloadImageTask(progressBar, imageView));
		}
		return ll;
	}


	public static MapillaryImageDialog show(MapActivity mapActivity, String key, String imageUrl,
											String viewerUrl, LatLon latLon, double ca,
											String title, String description) {
		MapillaryImageDialog dialog = new MapillaryImageDialog(mapActivity, key, imageUrl, viewerUrl,
				latLon, ca, title, description);
		ContextMenuCardDialogFragment.showInstance(dialog);
		return dialog;
	}

	private class DownloadImageTask extends AsyncTask<Void, Void, Bitmap> {

		private ProgressBar progressBar;
		private ImageView imageView;

		public DownloadImageTask(ProgressBar progressBar, ImageView imageView) {
			this.progressBar = progressBar;
			this.imageView = imageView;
		}

		@Override
		protected void onPreExecute() {
			progressBar.setVisibility(View.VISIBLE);
		}

		@Override
		protected Bitmap doInBackground(Void... params) {
			return AndroidNetworkUtils.downloadImage(getMapActivity().getMyApplication(), imageUrl);
		}

		@Override
		protected void onPostExecute(Bitmap bitmap) {
			progressBar.setVisibility(View.GONE);
			if (bitmap != null) {
				imageView.setImageDrawable(new BitmapDrawable(getMapActivity().getResources(), bitmap));
			}
		}
	}
}