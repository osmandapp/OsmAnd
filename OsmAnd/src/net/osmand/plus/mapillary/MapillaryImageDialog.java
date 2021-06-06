package net.osmand.plus.mapillary;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
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

import net.osmand.AndroidNetworkUtils;
import net.osmand.AndroidUtils;
import net.osmand.data.GeometryTile;
import net.osmand.data.LatLon;
import net.osmand.data.QuadPointDouble;
import net.osmand.data.QuadRect;
import net.osmand.data.RotatedTileBox;
import net.osmand.map.ITileSource;
import net.osmand.map.TileSourceManager;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.mapcontextmenu.builders.cards.dialogs.ContextMenuCardDialog;
import net.osmand.plus.mapcontextmenu.builders.cards.dialogs.ContextMenuCardDialogFragment;
import net.osmand.plus.resources.ResourceManager;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static net.osmand.plus.mapillary.MapillaryVectorLayer.EXTENT;
import static net.osmand.plus.mapillary.MapillaryVectorLayer.TILE_ZOOM;

public class MapillaryImageDialog extends ContextMenuCardDialog {

	private static final String KEY_MAPILLARY_DIALOG_IMAGE_KEY = "key_mapillary_dialog_image_key";
	private static final String KEY_MAPILLARY_DIALOG_IMAGE_SKEY = "key_mapillary_dialog_image_skey";
	private static final String KEY_MAPILLARY_DIALOG_IMAGE_URL = "key_mapillary_dialog_image_url";
	private static final String KEY_MAPILLARY_DIALOG_VIEWER_URL = "key_mapillary_dialog_viewer_url";
	private static final String KEY_MAPILLARY_DIALOG_LATLON = "key_mapillary_dialog_latlon";
	private static final String KEY_MAPILLARY_DIALOG_CA = "key_mapillary_dialog_ca";

	private static final String MAPILLARY_VIEWER_URL_TEMPLATE =
			"https://osmand.net/api/mapillary/photo-viewer?photo_id=";
	private static final String MAPILLARY_HIRES_IMAGE_URL_TEMPLATE =
			"https://osmand.net/api/mapillary/get_photo?hires=true&photo_id=";

	private static final String WEBGL_ERROR_MESSAGE = "Error creating WebGL context";

	private String key;
	private String sKey;
	private String imageUrl;
	private String viewerUrl;
	private LatLon latLon;
	private double ca = Double.NaN;
	private boolean sync;

	private View staticImageView;
	private View noInternetView;
	private List<Pair<QuadPointDouble, GeometryTile>> tiles = new ArrayList<>();
	private double fetchedTileLat = Double.NaN;
	private double fetchedTileLon = Double.NaN;
	private List<MapillaryImage> sequenceImages = new ArrayList<>();
	private AtomicInteger downloadRequestNumber = new AtomicInteger();
	private UiUtilities ic;

	public MapillaryImageDialog(@NonNull MapActivity mapActivity, @NonNull Bundle bundle) {
		super(mapActivity, CardDialogType.MAPILLARY);
		restoreFields(bundle);
		this.ic = mapActivity.getMyApplication().getUIUtilities();
	}

	public MapillaryImageDialog(MapActivity mapActivity, String key, String sKey, String imageUrl,
								String viewerUrl, LatLon latLon, double ca, String title, String description, boolean sync) {
		super(mapActivity, CardDialogType.MAPILLARY);
		this.title = title;
		this.description = description;
		this.key = key;
		this.sKey = sKey;
		this.imageUrl = imageUrl;
		this.viewerUrl = viewerUrl;
		this.latLon = latLon;
		this.ca = ca;
		this.ic = mapActivity.getMyApplication().getUIUtilities();
		this.sync = sync;
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
		bundle.putSerializable(KEY_MAPILLARY_DIALOG_IMAGE_SKEY, sKey);
		bundle.putSerializable(KEY_MAPILLARY_DIALOG_IMAGE_URL, imageUrl);
		bundle.putSerializable(KEY_MAPILLARY_DIALOG_VIEWER_URL, viewerUrl);
		bundle.putSerializable(KEY_MAPILLARY_DIALOG_LATLON, latLon);
		bundle.putDouble(KEY_MAPILLARY_DIALOG_CA, ca);
	}

	@Override
	protected void restoreFields(Bundle bundle) {
		super.restoreFields(bundle);
		this.key = bundle.getString(KEY_MAPILLARY_DIALOG_IMAGE_KEY);
		this.sKey = bundle.getString(KEY_MAPILLARY_DIALOG_IMAGE_SKEY);
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
		updateLayer(mapView.getLayerByClass(MapillaryRasterLayer.class), latLon, ca);
		updateLayer(mapView.getLayerByClass(MapillaryVectorLayer.class), latLon, ca);
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

	private void updateLayer(MapillaryLayer layer, LatLon latLon, double ca) {
		if (layer != null) {
			layer.setSelectedImageLocation(latLon);
			if (!Double.isNaN(ca)) {
				layer.setSelectedImageCameraAngle((float) ca);
			} else {
				layer.setSelectedImageCameraAngle(null);
			}
		}
	}

	public View getContentView() {
		return getWebView();
		/*
		if (getMapActivity().getMyApplication().getSettings().WEBGL_SUPPORTED.get()) {
			return getWebView();
		} else {
			return getStaticImageView();
		}
		*/
	}

	@Override
	protected boolean haveMenuItems() {
		return true;
	}

	@Override
	protected void createMenuItems(Menu menu) {
		MenuItem item = menu.add(R.string.open_mapillary)
				.setIcon(getMapActivity().getMyApplication().getUIUtilities().getThemedIcon(
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
	private View getWebView() {
		View view = getMapActivity().getLayoutInflater().inflate(R.layout.mapillary_web_view, null);
		final WebView webView = view.findViewById(R.id.webView);
		webView.setBackgroundColor(Color.argb(1, 0, 0, 0));
		final View noInternetView = view.findViewById(R.id.mapillaryNoInternetLayout);
		((ImageView) noInternetView.findViewById(R.id.wifiOff)).setImageDrawable(ic.getThemedIcon(R.drawable.ic_action_wifi_off));
		//webView.setLayerType(WebView.LAYER_TYPE_SOFTWARE, null);
		view.setScrollContainer(false);
		webView.getSettings().setJavaScriptEnabled(true);
		webView.addJavascriptInterface(new MapillaryWebAppInterface(), "Android");
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
				isPortrait() ? ViewGroup.LayoutParams.MATCH_PARENT : AndroidUtils.dpToPx(getMapActivity(), 360f),
				isPortrait() ? AndroidUtils.dpToPx(getMapActivity(), 270f) : ViewGroup.LayoutParams.MATCH_PARENT);
		view.setLayoutParams(lp);
		webView.setWebViewClient(new WebViewClient() {
			@Override
			public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
				webView.loadUrl("about:blank");
				noInternetView.setVisibility(View.VISIBLE);
			}
		});
		/*
		webView.setWebChromeClient(new WebChromeClient() {
			@Override
			public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
				if (!Algorithms.isEmpty(consoleMessage.message()) && consoleMessage.message().contains(WEBGL_ERROR_MESSAGE)) {
					getMapActivity().getMyApplication().getSettings().WEBGL_SUPPORTED.set(false);
					show(getMapActivity(), key, imageUrl, viewerUrl, getLatLon(), getCa(), getTitle(), getDescription());
				}
				return false;
			}
		});
		*/
		noInternetView.findViewById(R.id.retry_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				noInternetView.setVisibility(View.GONE);
				webView.loadUrl(viewerUrl);
			}
		});
		webView.loadUrl(viewerUrl);
		return view;
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
		View view = getMapActivity().getLayoutInflater().inflate(R.layout.mapillary_static_image_view, null);
		view.setClickable(true);
		FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
				isPortrait() ? ViewGroup.LayoutParams.MATCH_PARENT : AndroidUtils.dpToPx(getMapActivity(), 360f),
				isPortrait() ? AndroidUtils.dpToPx(getMapActivity(), 270f) : ViewGroup.LayoutParams.MATCH_PARENT);
		view.setLayoutParams(lp);

		view.findViewById(R.id.leftArrowButton).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				clickLeftArrowButton(v);
			}
		});
		view.findViewById(R.id.rightArrowButton).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				clickRightArrowButton(v);
			}
		});

		staticImageView = view.findViewById(R.id.staticImageViewLayout);

		noInternetView = view.findViewById(R.id.mapillaryNoInternetLayout);
		((ImageView) noInternetView.findViewById(R.id.wifiOff)).setImageDrawable(ic.getThemedIcon(R.drawable.ic_action_wifi_off));
		noInternetView.findViewById(R.id.retry_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				MenuBuilder.execute(new DownloadImageTask(staticImageView, downloadRequestNumber.incrementAndGet(), downloadRequestNumber));
				fetchSequence();
			}
		});

		if (!Algorithms.isEmpty(imageUrl)) {
			MenuBuilder.execute(new DownloadImageTask(staticImageView, downloadRequestNumber.incrementAndGet(), downloadRequestNumber));
			fetchSequence();
		}
		updateArrowButtons();
		return view;
	}

	private void fetchSequence() {
		if (Algorithms.isEmpty(sKey)) {
			acquireSequenceKey();
		}
		if (!Algorithms.isEmpty(sKey)) {
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
					String key = (String) userData.get("key");
					if (this.key.equals(key)) {
						sKey = (String) userData.get("skey");
						return;
					}
				}
			}
		}
	}

	private void acquireSequenceImages() {
		fetchTiles();
		List<MapillaryImage> sequenceImages = new ArrayList<>();
		if (!Algorithms.isEmpty(sKey)) {
			double px, py;
			for (Pair<QuadPointDouble, GeometryTile> pt : tiles) {
				QuadPointDouble point = pt.first;
				GeometryTile tile = pt.second;
				for (Geometry g : tile.getData()) {
					if (g instanceof Point && !g.isEmpty() && g.getUserData() != null && g.getUserData() instanceof HashMap) {
						HashMap userData = (HashMap) g.getUserData();
						String sKey = (String) userData.get("skey");
						if (this.sKey.equals(sKey)) {
							Point p = (Point) g;
							px = p.getCoordinate().x / EXTENT;
							py = p.getCoordinate().y / EXTENT;
							MapillaryImage image = new MapillaryImage(
									MapUtils.getLatitudeFromTile(TILE_ZOOM, point.y + py),
									MapUtils.getLongitudeFromTile(TILE_ZOOM, point.x + px));
							if (image.setData(userData)) {
								sequenceImages.add(image);
							}
						}
					}
				}
			}
		}
		Collections.sort(sequenceImages, new Comparator<MapillaryImage>() {
			@Override
			public int compare(MapillaryImage img1, MapillaryImage img2) {
				return img1.getCapturedAt() < img2.getCapturedAt() ?
						-1 : (img1.getCapturedAt() == img2.getCapturedAt() ? 0 : 1);
			}
		});
		this.sequenceImages = sequenceImages;
	}

	private void updateArrowButtons() {
		if (staticImageView != null) {
			boolean showLeftButton = false;
			boolean showRightButton = false;
			if (sequenceImages.size() > 1 && !Algorithms.isEmpty(key)) {
				showLeftButton = !sequenceImages.get(0).getKey().equals(key);
				showRightButton = !sequenceImages.get(sequenceImages.size() - 1).getKey().equals(key);
			}
			staticImageView.findViewById(R.id.leftArrowButton)
					.setVisibility(showLeftButton ? View.VISIBLE : View.GONE);
			staticImageView.findViewById(R.id.rightArrowButton)
					.setVisibility(showRightButton ? View.VISIBLE : View.GONE);
		}
	}

	private int getImageIndex(String key) {
		for (int i = 0; i < sequenceImages.size(); i++) {
			if (sequenceImages.get(i).getKey().equals(key)) {
				return i;
			}
		}
		return -1;
	}

	private void clickLeftArrowButton(View v) {
		fetchSequence();
		if (sequenceImages != null) {
			int index = getImageIndex(key);
			if (index != -1 && index > 0) {
				setImage(sequenceImages.get(index - 1));
			}
		}
		updateArrowButtons();
	}

	private void clickRightArrowButton(View v) {
		fetchSequence();
		if (sequenceImages != null) {
			int index = getImageIndex(key);
			if (index != -1 && index < sequenceImages.size() - 1) {
				setImage(sequenceImages.get(index + 1));
			}
		}
		updateArrowButtons();
	}

	private void setImage(MapillaryImage image) {
		this.latLon = new LatLon(image.getLatitude(), image.getLongitude());
		this.ca = image.getCa();
		this.key = image.getKey();
		this.imageUrl = MAPILLARY_HIRES_IMAGE_URL_TEMPLATE + image.getKey();
		this.viewerUrl = MAPILLARY_VIEWER_URL_TEMPLATE + image.getKey();
		MenuBuilder.execute(new DownloadImageTask(staticImageView, downloadRequestNumber.incrementAndGet(), downloadRequestNumber));
		setImageLocation(latLon, ca, false);
	}

	public void fetchTiles() {
		RotatedTileBox tileBox = getMapActivity().getMapView().getCurrentRotatedTileBox().copy();
		if (fetchedTileLat == tileBox.getLatitude() && fetchedTileLon == tileBox.getLongitude()) {
			return;
		}
		ITileSource map = TileSourceManager.getMapillaryVectorSource();
		int nzoom = tileBox.getZoom();
		if (nzoom < map.getMinimumZoomSupported()) {
			return;
		}
		ResourceManager mgr = getMapActivity().getMyApplication().getResourceManager();
		final QuadRect tilesRect = tileBox.getTileBounds();

		// recalculate for ellipsoid coordinates
		float ellipticTileCorrection  = 0;
		if (map.isEllipticYTile()) {
			ellipticTileCorrection = (float) (MapUtils.getTileEllipsoidNumberY(nzoom, tileBox.getLatitude()) - tileBox.getCenterTileY());
		}

		int left = (int) Math.floor(tilesRect.left);
		int top = (int) Math.floor(tilesRect.top + ellipticTileCorrection);
		int width = (int) Math.ceil(tilesRect.right - left);
		int height = (int) Math.ceil(tilesRect.bottom + ellipticTileCorrection - top);
		int dzoom = nzoom - TILE_ZOOM;
		int div = (int) Math.pow(2.0, dzoom);

		long requestTimestamp = System.currentTimeMillis();

		Map<String, Pair<QuadPointDouble, GeometryTile>> tiles = new HashMap<>();
		for (int i = 0; i < width; i++) {
			for (int j = 0; j < height; j++) {
				int tileX = (left + i) / div;
				int tileY = (top + j) / div;
				String tileId = mgr.calculateTileId(map, tileX, tileY, TILE_ZOOM);
				Pair<QuadPointDouble, GeometryTile> p = tiles.get(tileId);
				if (p == null) {
					GeometryTile tile = null;
					// asking tile image async
					boolean imgExist = mgr.tileExistOnFileSystem(tileId, map, tileX, tileY, TILE_ZOOM);
					if (imgExist) {
						if (sync) {
							tile = mgr.getGeometryTilesCache().getTileForMapSync(tileId, map,
									tileX, tileY, TILE_ZOOM, false, requestTimestamp);
							sync = false;
						} else {
							tile = mgr.getGeometryTilesCache().getTileForMapAsync(tileId, map,
									tileX, tileY, TILE_ZOOM, false, requestTimestamp);
						}
					}
					if (tile != null) {
						tiles.put(tileId, new Pair<>(new QuadPointDouble(tileX,  tileY), tile));
					}
				}
			}
		}
		fetchedTileLat = tileBox.getLatitude();
		fetchedTileLon = tileBox.getLongitude();
		this.tiles = new ArrayList<>(tiles.values());
	}

	public static MapillaryImageDialog show(MapActivity mapActivity, String key, String imageUrl,
											String viewerUrl, LatLon latLon, double ca,
											String title, String description) {
		MapillaryImageDialog dialog = new MapillaryImageDialog(mapActivity, key, null, imageUrl,
				viewerUrl, latLon, ca, title, description, false);
		ContextMenuCardDialogFragment.showInstance(dialog);
		return dialog;
	}

	public static MapillaryImageDialog show(MapActivity mapActivity, String key, String imageUrl,
											String viewerUrl, LatLon latLon, double ca,
											String title, String description, boolean sync) {
		MapillaryImageDialog dialog = new MapillaryImageDialog(mapActivity, key, null, imageUrl,
				viewerUrl, latLon, ca, title, description, sync);
		ContextMenuCardDialogFragment.showInstance(dialog);
		return dialog;
	}

	public static MapillaryImageDialog show(MapActivity mapActivity, double latitude, double longitude,
											String key, String sKey, double ca, String title, String description) {
		String imageUrl = MAPILLARY_HIRES_IMAGE_URL_TEMPLATE + key;
		String viewerUrl = MAPILLARY_VIEWER_URL_TEMPLATE + key;
		LatLon latLon = new LatLon(latitude, longitude);
		MapillaryImageDialog dialog = new MapillaryImageDialog(mapActivity, key, sKey, imageUrl, viewerUrl,
				latLon, ca, title, description, false);
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
				ProgressBar progressBar = (ProgressBar) staticImageView.findViewById(R.id.progressBar);
				ImageView imageView = (ImageView) staticImageView.findViewById(R.id.imageView);
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
				return AndroidNetworkUtils.downloadImage(getMapActivity().getMyApplication(), imageUrl);
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