package net.osmand.plus.plugins.panoramax;

import static net.osmand.plus.plugins.panoramax.PanoramaxImage.IMAGE_ID_KEY;
import static net.osmand.plus.plugins.panoramax.PanoramaxImage.SEQUENCE_ID_KEY;
import static net.osmand.plus.plugins.panoramax.PanoramaxVectorLayer.EXTENT;
import static net.osmand.plus.plugins.panoramax.PanoramaxVectorLayer.MIN_IMAGE_LAYER_ZOOM;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

import net.osmand.data.GeometryTile;
import net.osmand.data.LatLon;
import net.osmand.data.QuadPointDouble;
import net.osmand.data.QuadRect;
import net.osmand.data.RotatedTileBox;
import net.osmand.map.ITileSource;
import net.osmand.map.TileSourceManager;
import net.osmand.plus.OsmAndTaskManager;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.builders.cards.dialogs.ContextMenuCardDialog;
import net.osmand.plus.mapcontextmenu.builders.cards.dialogs.ContextMenuCardDialogFragment;
import net.osmand.plus.resources.ResourceManager;
import net.osmand.plus.utils.AndroidNetworkUtils;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class PanoramaxImageDialog extends ContextMenuCardDialog {

	private static final String KEY_PANORAMAX_DIALOG_IMAGE_ID = "key_panoramax_dialog_image_id";
	private static final String KEY_PANORAMAX_DIALOG_SEQUENCE_ID = "key_panoramax_dialog_sequence_id";
	private static final String KEY_PANORAMAX_DIALOG_IMAGE_URL = "key_panoramax_dialog_image_url";
	private static final String KEY_PANORAMAX_DIALOG_VIEWER_URL = "key_panoramax_dialog_viewer_url";
	private static final String KEY_PANORAMAX_DIALOG_LATLON = "key_panoramax_dialog_latlon";
	private static final String KEY_PANORAMAX_DIALOG_COMPASS_ANGLE = "key_panoramax_dialog_compass_angle";

	// Mapillary goes through osmand.net proxy endpoints; there is no Panoramax equivalent on
	// osmand.net, so these address the instance directly. The picture endpoints answer on the
	// federated instance too and redirect to whichever member instance actually stores the
	// file, so a URL can be built from the id alone with no metadata request first.
	public static final String PANORAMAX_VIEWER_URL_TEMPLATE = PanoramaxConstants.VIEWER_URL_TEMPLATE;

	private static final String WEBGL_ERROR_MESSAGE = "Error creating WebGL context";

	private String imageId;
	private String sequenceId;
	private String imageUrl;
	private String viewerUrl;
	private LatLon latLon;
	private double compassAngle = Double.NaN;
	private boolean sync;

	private View staticImageView;
	private View noInternetView;
	private List<Pair<QuadPointDouble, GeometryTile>> tiles = new ArrayList<>();
	private double fetchedTileLat = Double.NaN;
	private double fetchedTileLon = Double.NaN;
	private List<PanoramaxImage> sequenceImages = new ArrayList<>();
	private final AtomicInteger downloadRequestNumber = new AtomicInteger();
	private final UiUtilities iconsCache;

	public PanoramaxImageDialog(@NonNull MapActivity mapActivity, @NonNull Bundle bundle) {
		super(mapActivity, CardDialogType.PANORAMAX);
		restoreFields(bundle);
		this.iconsCache = mapActivity.getApp().getUIUtilities();
	}

	public PanoramaxImageDialog(MapActivity mapActivity, String imageId, String sequenceId,
	                            String imageUrl, String viewerUrl, LatLon latLon, double compassAngle,
	                            String title, String description, boolean sync) {
		super(mapActivity, CardDialogType.PANORAMAX);
		this.title = title;
		this.description = description;
		this.imageId = imageId;
		this.sequenceId = sequenceId;
		this.imageUrl = imageUrl;
		this.viewerUrl = viewerUrl;
		this.latLon = latLon;
		this.compassAngle = compassAngle;
		this.iconsCache = mapActivity.getApp().getUIUtilities();
		this.sync = sync;
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
		bundle.putSerializable(KEY_PANORAMAX_DIALOG_SEQUENCE_ID, sequenceId);
		bundle.putSerializable(KEY_PANORAMAX_DIALOG_IMAGE_URL, imageUrl);
		bundle.putSerializable(KEY_PANORAMAX_DIALOG_VIEWER_URL, viewerUrl);
		bundle.putSerializable(KEY_PANORAMAX_DIALOG_LATLON, latLon);
		bundle.putDouble(KEY_PANORAMAX_DIALOG_COMPASS_ANGLE, compassAngle);
	}

	@Override
	protected void restoreFields(Bundle bundle) {
		super.restoreFields(bundle);
		this.imageId = bundle.getString(KEY_PANORAMAX_DIALOG_IMAGE_ID);
		this.sequenceId = bundle.getString(KEY_PANORAMAX_DIALOG_SEQUENCE_ID);
		this.imageUrl = bundle.getString(KEY_PANORAMAX_DIALOG_IMAGE_URL);
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
				.setIcon(iconsCache.getThemedIcon(R.drawable.ic_action_panoramax));
		item.setOnMenuItemClickListener(i -> {
			PanoramaxPlugin.openPanoramax(getMapActivity(), imageId);
			return true;
		});
	}

	@SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
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
		webView.addJavascriptInterface(new PanoramaxWebAppInterface(), "Android");
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
				// Redirect to deprecated method, so you can use it in all SDK versions
				onReceivedError(view, rerr.getErrorCode(), rerr.getDescription().toString(), req.getUrl().toString());
			}
		});
		noInternetView.findViewById(R.id.retry_button).setOnClickListener(v -> {
			noInternetView.setVisibility(View.GONE);
			webView.loadUrl(viewerUrl);
		});
		webView.loadUrl(viewerUrl);
		return view;
	}

	private class PanoramaxWebAppInterface {

		@JavascriptInterface
		public void onNodeChanged(double latitude, double longitude, double compassAngle, String imageId) {
			LatLon latLon = null;
			if (!Double.isNaN(latitude) && !Double.isNaN(longitude)) {
				latLon = new LatLon(latitude, longitude);
				PanoramaxImageDialog.this.latLon = latLon;
				PanoramaxImageDialog.this.compassAngle = compassAngle;
				if (!Algorithms.isEmpty(imageId)) {
					PanoramaxImageDialog.this.imageId = imageId;
					PanoramaxImageDialog.this.imageUrl = PanoramaxConstants.getHiResImageUrl(imageId);
					PanoramaxImageDialog.this.viewerUrl = PANORAMAX_VIEWER_URL_TEMPLATE + imageId;
				}
			}
			setImageLocation(latLon, compassAngle, false);
		}
	}

	private View getStaticImageView() {
		View view = getMapActivity().getLayoutInflater().inflate(R.layout.panoramax_static_image_view, null);
		view.setClickable(true);
		FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
				isPortrait() ? ViewGroup.LayoutParams.MATCH_PARENT : AndroidUtils.dpToPx(getMapActivity(), 360f),
				isPortrait() ? AndroidUtils.dpToPx(getMapActivity(), 270f) : ViewGroup.LayoutParams.MATCH_PARENT);
		view.setLayoutParams(lp);

		view.findViewById(R.id.leftArrowButton).setOnClickListener(this::clickLeftArrowButton);
		view.findViewById(R.id.rightArrowButton).setOnClickListener(this::clickRightArrowButton);

		staticImageView = view.findViewById(R.id.staticImageViewLayout);

		noInternetView = view.findViewById(R.id.panoramaxNoInternetLayout);
		((ImageView) noInternetView.findViewById(R.id.wifiOff))
				.setImageDrawable(iconsCache.getThemedIcon(R.drawable.ic_action_wifi_off));
		noInternetView.findViewById(R.id.retry_button).setOnClickListener(v -> {
			DownloadImageTask downloadTask = new DownloadImageTask(staticImageView,
					downloadRequestNumber.incrementAndGet(), downloadRequestNumber);
			OsmAndTaskManager.executeTask(downloadTask);
			fetchSequence();
		});

		if (!Algorithms.isEmpty(imageUrl)) {
			DownloadImageTask downloadTask = new DownloadImageTask(staticImageView,
					downloadRequestNumber.incrementAndGet(), downloadRequestNumber);
			OsmAndTaskManager.executeTask(downloadTask);
			fetchSequence();
		}
		updateArrowButtons();
		return view;
	}

	private void fetchSequence() {
		if (Algorithms.isEmpty(sequenceId)) {
			acquireSequenceKey();
		}
		if (!Algorithms.isEmpty(sequenceId)) {
			acquireSequenceImages();
		}
	}

	private void acquireSequenceKey() {
		fetchTiles();
		for (Pair<QuadPointDouble, GeometryTile> pt : tiles) {
			GeometryTile tile = pt.second;
			for (Geometry g : tile.getData()) {
				if (g instanceof Point && !g.isEmpty() && g.getUserData() != null && g.getUserData() instanceof HashMap) {
					HashMap userData = (HashMap) g.getUserData();
					String imageId = Objects.toString(userData.get(IMAGE_ID_KEY), null);
					if (this.imageId.equals(imageId)) {
						sequenceId = Objects.toString(userData.get(SEQUENCE_ID_KEY), null);
						return;
					}
				}
			}
		}
	}

	private void acquireSequenceImages() {
		fetchTiles();
		List<PanoramaxImage> sequenceImages = new ArrayList<>();
		if (!Algorithms.isEmpty(sequenceId)) {
			double px, py;
			for (Pair<QuadPointDouble, GeometryTile> pt : tiles) {
				QuadPointDouble point = pt.first;
				GeometryTile tile = pt.second;
				for (Geometry g : tile.getData()) {
					if (g instanceof Point && !g.isEmpty() && g.getUserData() != null && g.getUserData() instanceof HashMap) {
						HashMap userData = (HashMap) g.getUserData();
						String sequenceId = Objects.toString(userData.get(SEQUENCE_ID_KEY), null);
						if (this.sequenceId.equals(sequenceId)) {
							Point p = (Point) g;
							px = p.getCoordinate().x / EXTENT;
							py = p.getCoordinate().y / EXTENT;
							double lat = MapUtils.getLatitudeFromTile(MIN_IMAGE_LAYER_ZOOM, point.y + py);
							double lon = MapUtils.getLongitudeFromTile(MIN_IMAGE_LAYER_ZOOM, point.x + px);
							PanoramaxImage image = new PanoramaxImage(lat, lon);
							if (image.setData(userData)) {
								sequenceImages.add(image);
							}
						}
					}
				}
			}
		}
		Collections.sort(sequenceImages, (img1, img2) -> Long.compare(img1.getCapturedAt(), img2.getCapturedAt()));
		this.sequenceImages = sequenceImages;
	}

	private void updateArrowButtons() {
		if (staticImageView != null) {
			boolean showLeftButton = false;
			boolean showRightButton = false;
			if (sequenceImages.size() > 1 && !Algorithms.isEmpty(imageId)) {
				showLeftButton = !sequenceImages.get(0).getImageId().equals(imageId);
				showRightButton = !sequenceImages.get(sequenceImages.size() - 1).getImageId().equals(imageId);
			}
			staticImageView.findViewById(R.id.leftArrowButton)
					.setVisibility(showLeftButton ? View.VISIBLE : View.GONE);
			staticImageView.findViewById(R.id.rightArrowButton)
					.setVisibility(showRightButton ? View.VISIBLE : View.GONE);
		}
	}

	private int getImageIndex(String key) {
		for (int i = 0; i < sequenceImages.size(); i++) {
			if (sequenceImages.get(i).getImageId().equals(key)) {
				return i;
			}
		}
		return -1;
	}

	private void clickLeftArrowButton(View v) {
		fetchSequence();
		if (sequenceImages != null) {
			int index = getImageIndex(imageId);
			if (index > 0) {
				setImage(sequenceImages.get(index - 1));
			}
		}
		updateArrowButtons();
	}

	private void clickRightArrowButton(View v) {
		fetchSequence();
		if (sequenceImages != null) {
			int index = getImageIndex(imageId);
			if (index != -1 && index < sequenceImages.size() - 1) {
				setImage(sequenceImages.get(index + 1));
			}
		}
		updateArrowButtons();
	}

	private void setImage(PanoramaxImage image) {
		this.latLon = new LatLon(image.getLatitude(), image.getLongitude());
		this.compassAngle = image.getCompassAngle();
		this.imageId = image.getImageId();
		this.imageUrl = PanoramaxConstants.getHiResImageUrl(image.getImageId());
		this.viewerUrl = PANORAMAX_VIEWER_URL_TEMPLATE + image.getImageId();
		OsmAndTaskManager.executeTask(new DownloadImageTask(staticImageView, downloadRequestNumber.incrementAndGet(), downloadRequestNumber));
		setImageLocation(latLon, compassAngle, false);
	}

	public void fetchTiles() {
		RotatedTileBox tileBox = getMapActivity().getMapView().getRotatedTileBox();
		if (fetchedTileLat == tileBox.getLatitude() && fetchedTileLon == tileBox.getLongitude()) {
			return;
		}
		ITileSource map = TileSourceManager.getPanoramaxVectorSource();
		int nzoom = tileBox.getZoom();
		if (nzoom < map.getMinimumZoomSupported()) {
			return;
		}
		ResourceManager mgr = getMapActivity().getApp().getResourceManager();
		QuadRect tilesRect = tileBox.getTileBounds();

		// recalculate for ellipsoid coordinates
		float ellipticTileCorrection = 0;
		if (map.isEllipticYTile()) {
			ellipticTileCorrection = (float) (MapUtils.getTileEllipsoidNumberY(nzoom, tileBox.getLatitude()) - tileBox.getCenterTileY());
		}

		int left = (int) Math.floor(tilesRect.left);
		int top = (int) Math.floor(tilesRect.top + ellipticTileCorrection);
		int width = (int) Math.ceil(tilesRect.right - left);
		int height = (int) Math.ceil(tilesRect.bottom + ellipticTileCorrection - top);
		int dzoom = nzoom - MIN_IMAGE_LAYER_ZOOM;
		int div = (int) Math.pow(2.0, dzoom);

		long requestTimestamp = System.currentTimeMillis();

		Map<String, Pair<QuadPointDouble, GeometryTile>> tiles = new HashMap<>();
		for (int i = 0; i < width; i++) {
			for (int j = 0; j < height; j++) {
				int tileX = (left + i) / div;
				int tileY = (top + j) / div;
				String tileId = mgr.calculateTileId(map, tileX, tileY, MIN_IMAGE_LAYER_ZOOM);
				Pair<QuadPointDouble, GeometryTile> p = tiles.get(tileId);
				if (p == null) {
					GeometryTile tile = null;
					// asking tile image async
					boolean imgExist = mgr.isTileDownloaded(tileId, map, tileX, tileY, MIN_IMAGE_LAYER_ZOOM);
					if (imgExist) {
						if (sync) {
							tile = mgr.getMapillaryVectorTilesCache().getTileForMapSync(tileId, map,
									tileX, tileY, MIN_IMAGE_LAYER_ZOOM, false, requestTimestamp);
							sync = false;
						} else {
							tile = mgr.getMapillaryVectorTilesCache().getTileForMapAsync(tileId, map,
									tileX, tileY, MIN_IMAGE_LAYER_ZOOM, false, requestTimestamp);
						}
					}
					if (tile != null) {
						tiles.put(tileId, new Pair<>(new QuadPointDouble(tileX, tileY), tile));
					}
				}
			}
		}
		fetchedTileLat = tileBox.getLatitude();
		fetchedTileLon = tileBox.getLongitude();
		this.tiles = new ArrayList<>(tiles.values());
	}

	public static PanoramaxImageDialog show(MapActivity mapActivity, String imageId, String imageUrl,
	                                        String viewerUrl, LatLon latLon, double compassAngle,
	                                        String title, String description) {
		PanoramaxImageDialog dialog = new PanoramaxImageDialog(mapActivity, imageId, null, imageUrl,
				viewerUrl, latLon, compassAngle, title, description, false);
		ContextMenuCardDialogFragment.showInstance(dialog);
		return dialog;
	}

	public static PanoramaxImageDialog show(MapActivity mapActivity, String imageId, String imageUrl,
	                                        String viewerUrl, LatLon latLon, double compassAngle,
	                                        String title, String description, boolean sync) {
		PanoramaxImageDialog dialog = new PanoramaxImageDialog(mapActivity, imageId, null, imageUrl,
				viewerUrl, latLon, compassAngle, title, description, sync);
		ContextMenuCardDialogFragment.showInstance(dialog);
		return dialog;
	}

	public static PanoramaxImageDialog show(MapActivity mapActivity, double latitude, double longitude,
	                                        String imageId, String sequenceId, double compassAngle,
	                                        String title, String description) {
		String imageUrl = PanoramaxConstants.getHiResImageUrl(imageId);
		String viewerUrl = PANORAMAX_VIEWER_URL_TEMPLATE + imageId;
		LatLon latLon = new LatLon(latitude, longitude);
		PanoramaxImageDialog dialog = new PanoramaxImageDialog(mapActivity, imageId, sequenceId, imageUrl, viewerUrl,
				latLon, compassAngle, title, description, false);
		ContextMenuCardDialogFragment.showInstance(dialog);
		return dialog;
	}

	private class DownloadImageTask extends AsyncTask<Void, Void, Bitmap> {

		private int request;
		private AtomicInteger downloadRequestNumber;
		private ProgressBar progressBar;
		private ImageView imageView;

		public DownloadImageTask(View staticImageView, int request, AtomicInteger downloadRequestNumber) {
			if (staticImageView != null) {
				this.request = request;
				this.downloadRequestNumber = downloadRequestNumber;
				ProgressBar progressBar = staticImageView.findViewById(R.id.progressBar);
				ImageView imageView = staticImageView.findViewById(R.id.imageView);
				this.progressBar = progressBar;
				this.imageView = imageView;
			}
		}

		@Override
		protected void onPreExecute() {
			noInternetView.setVisibility(View.GONE);
			staticImageView.setVisibility(View.VISIBLE);
			if (progressBar != null) {
				progressBar.setVisibility(View.VISIBLE);
			}
		}

		@Override
		protected Bitmap doInBackground(Void... params) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// ignore
			}
			if (isValidRequest()) {
				// The /api/pictures/{id}/hd.jpg convenience url answers with an HTTP 308 to the
				// instance that stores the file, and HttpURLConnection does not follow 308. Ask
				// the STAC API for the real location first, and fall back to the constructed url
				// so a failed lookup degrades rather than showing nothing.
				String url = imageUrl;
				if (!Algorithms.isEmpty(imageId)) {
					PanoramaxImageHelper.PictureInfo info = PanoramaxImageHelper.fetchPictureInfo(imageId);
					if (info != null && !Algorithms.isEmpty(info.hiResUrl)) {
						url = info.hiResUrl;
					} else if (info != null && !Algorithms.isEmpty(info.imageUrl)) {
						url = info.imageUrl;
					}
				}
				return AndroidNetworkUtils.downloadImage(getMapActivity().getApp(), url);
			} else {
				return null;
			}
		}

		@Override
		protected void onPostExecute(Bitmap bitmap) {
			if (isValidRequest()) {
				if (progressBar != null) {
					progressBar.setVisibility(View.GONE);
				}
				if (imageView != null) {
					if (bitmap != null) {
						imageView.setImageDrawable(new BitmapDrawable(getMapActivity().getResources(), bitmap));
					} else {
						imageView.setImageDrawable(null);
						staticImageView.setVisibility(View.GONE);
						noInternetView.setVisibility(View.VISIBLE);
					}
				}
			}
		}

		private boolean isValidRequest() {
			return request == downloadRequestNumber.get();
		}
	}
}