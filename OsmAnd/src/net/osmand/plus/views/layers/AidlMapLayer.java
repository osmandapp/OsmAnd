package net.osmand.plus.views.layers;

import static net.osmand.aidl.ConnectedApp.AIDL_LAYERS_PREFIX;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.net.Uri;
import android.os.AsyncTask;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.core.android.MapRendererView;
import net.osmand.core.jni.MapMarker;
import net.osmand.core.jni.MapMarkerBuilder;
import net.osmand.core.jni.MapMarkersCollection;
import net.osmand.core.jni.PointI;
import net.osmand.core.jni.TextRasterizer;
import net.osmand.plus.AppInitializer;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.aidl.AidlMapLayerWrapper;
import net.osmand.aidl.AidlMapPointWrapper;
import net.osmand.aidlapi.maplayer.point.AMapPoint;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.MapContextMenu;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.layers.ContextMenuLayer.IContextMenuProvider;
import net.osmand.plus.views.layers.MapTextLayer.MapTextProvider;
import net.osmand.plus.views.layers.base.OsmandMapLayer;
import net.osmand.plus.views.layers.core.AidlTileProvider;
import net.osmand.plus.widgets.tools.CropCircleTransformation;
import net.osmand.util.MapUtils;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class AidlMapLayer extends OsmandMapLayer implements IContextMenuProvider, MapTextProvider<AidlMapPointWrapper> {

	public static final float POINT_IMAGE_VERTICAL_OFFSET = 0.91f;

	private static final int POINT_OUTER_COLOR = 0x88555555;
	private static final float START_ZOOM = 7;
	private static final int SMALL_ICON_SIZE_DP = 20;
	private static final int BIG_ICON_SIZE_DP = 40;

	private final String packName;
	private final AidlMapLayerWrapper aidlLayer;

	private final CommonPreference<Boolean> layerPref;
	private final CommonPreference<Boolean> appLayersPref;

	private Paint pointInnerCircle;
	private Paint pointOuterCircle;
	private Paint bitmapPaint;

	private Bitmap circle;
	private Bitmap smallIconBg;
	private Bitmap bigIconBg;
	private Bitmap bigIconBgStale;
	private Bitmap bigIconBgSelected;
	private Bitmap bigIconBgSelectedStale;
	private Bitmap placeholder;

	private int smallIconSize;
	private int bigIconSize;

	private PointsType pointsType;

	private MapTextLayer mapTextLayer;

	private final Map<String, Bitmap> pointImages = new ConcurrentHashMap<>();

	private final Set<String> imageRequests = new HashSet<>();
	private final List<AidlMapPointWrapper> displayedPoints = new ArrayList<>();

	//OpenGL
	private MapMarkersCollection mapMarkersCollection;
	private AidlTileProvider aidlMapLayerProvider;
	private int pointImagesSize = 0;
	private boolean nightMode = false;
	private int radius;
	@Nullable
	private String selectedPointId = null;
	private int aidlPointsCount = 0;
	private boolean mapsInitialized = false;

	public AidlMapLayer(@NonNull Context context, @NonNull AidlMapLayerWrapper aidlLayer,
						@NonNull String packName, int baseOrder) {
		super(context);
		this.aidlLayer = aidlLayer;
		this.packName = packName;
		this.baseOrder = baseOrder;

		OsmandApplication app = getApplication();
		layerPref = app.getSettings().registerBooleanPreference(packName + "_" + aidlLayer.getId(), true);
		appLayersPref = app.getSettings().registerBooleanPreference(AIDL_LAYERS_PREFIX + packName, true);
	}

	@Override
	public void initLayer(@NonNull OsmandMapTileView view) {
		super.initLayer(view);

		Resources res = view.getResources();
		boolean night = getApplication().getDaynightHelper().isNightMode();

		pointInnerCircle = new Paint();
		pointInnerCircle.setColor(res.getColor(R.color.poi_background));
		pointInnerCircle.setStyle(Paint.Style.FILL);
		pointInnerCircle.setAntiAlias(true);

		pointOuterCircle = new Paint();
		pointOuterCircle.setColor(POINT_OUTER_COLOR);
		pointOuterCircle.setStyle(Paint.Style.FILL_AND_STROKE);
		pointOuterCircle.setAntiAlias(true);

		bitmapPaint = new Paint();
		bitmapPaint.setAntiAlias(true);
		bitmapPaint.setDither(true);
		bitmapPaint.setFilterBitmap(true);

		circle = BitmapFactory.decodeResource(res, R.drawable.ic_white_shield_small);
		smallIconBg = BitmapFactory.decodeResource(res, night
				? R.drawable.map_pin_user_location_small_night : R.drawable.map_pin_user_location_small_day);
		bigIconBg = BitmapFactory.decodeResource(res, night
				? R.drawable.map_pin_user_location_night : R.drawable.map_pin_user_location_day);
		bigIconBgStale = BitmapFactory.decodeResource(res, night
				? R.drawable.map_pin_user_stale_location_night : R.drawable.map_pin_user_stale_location_day);
		bigIconBgSelected = BitmapFactory.decodeResource(res, night
				? R.drawable.map_pin_user_location_selected_night : R.drawable.map_pin_user_location_selected_day);
		bigIconBgSelectedStale = BitmapFactory.decodeResource(res, night
				? R.drawable.map_pin_user_stale_location_selected_night : R.drawable.map_pin_user_stale_location_selected_day);
		placeholder = BitmapFactory.decodeResource(res, R.drawable.img_user_picture);

		smallIconSize = AndroidUtils.dpToPx(getContext(), SMALL_ICON_SIZE_DP);
		bigIconSize = AndroidUtils.dpToPx(getContext(), BIG_ICON_SIZE_DP);

		mapTextLayer = view.getLayerByClass(MapTextLayer.class);
		addMapsInitializedListener();
	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
	}

	@Override
	public void onPrepareBufferImage(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
		if (!mapsInitialized) {
			return;
		}
		boolean pointsTypeChanged = pointsType != getPointsType(tileBox.getZoom());
		pointsType = getPointsType(tileBox.getZoom());
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer != null) {
			if (!isLayerEnabled() || pointsType == PointsType.INVISIBLE || pointsTypeChanged
					|| aidlPointsCount != aidlLayer.getPointsSize()
					|| nightMode != settings.isNightMode() || pointImagesSize != pointImages.size()
					|| (pointsType == PointsType.STANDARD && radius != getRadiusPoi(tileBox))
					|| (selectedPointId != null && !selectedPointId.equals(getSelectedContextMenuPointId()))
					|| (selectedPointId == null && getSelectedContextMenuPointId() != null)) {
				clearAidlTileProvider();
			}
			pointImagesSize = pointImages.size();
			nightMode = settings.isNightMode();
			radius = getRadiusPoi(tileBox);
			selectedPointId = getSelectedContextMenuPointId();
			float density = tileBox.getDensity();
			aidlPointsCount = aidlLayer.getPointsSize();
			showAidlTileProvider(density);
			return;
		}
		if (pointsType == PointsType.INVISIBLE) {
			mapTextLayer.putData(this, Collections.emptyList());
			return;
		}
		displayedPoints.clear();
		imageRequests.clear();

		if (isLayerEnabled()) {
			canvas.rotate(-tileBox.getRotate(), tileBox.getCenterPixelX(), tileBox.getCenterPixelY());

			String selectedPointId = getSelectedContextMenuPointId();
			for (AidlMapPointWrapper point : aidlLayer.getPoints()) {
				LatLon l = point.getLocation();
				if (l != null) {
					int x = (int) tileBox.getPixXFromLatLon(l.getLatitude(), l.getLongitude());
					int y = (int) tileBox.getPixYFromLatLon(l.getLatitude(), l.getLongitude());
					if (tileBox.containsPoint(x, y, bigIconSize)) {
						Bitmap image = null;
						if (pointsType != PointsType.STANDARD) {
							String imageUri = point.getParams().get(AMapPoint.POINT_IMAGE_URI_PARAM);
							if (!TextUtils.isEmpty(imageUri)) {
								image = pointImages.get(imageUri);
								if (image == null) {
									imageRequests.add(imageUri);
								}
							}
						}
						displayedPoints.add(point);
						boolean selected = selectedPointId != null && selectedPointId.equals(point.getId());
						drawPoint(canvas, x, y, tileBox, point, image, selected);
					}
				}
			}

			if (imageRequests.size() > 0) {
				executeTaskInBackground(new PointImageReaderTask(this), imageRequests.toArray(new String[0]));
			}
		}
		mapTextLayer.putData(this, displayedPoints);
	}

	private void drawPoint(Canvas canvas, int x, int y, RotatedTileBox tb, AidlMapPointWrapper point, Bitmap image, boolean selected) {
		if (image == null) {
			image = placeholder;
		}
		if (selected) {
			Bitmap bg = isStale(point) ? bigIconBgSelectedStale : bigIconBgSelected;
			drawBigIcon(canvas, x, y, image, bg);
		} else if (pointsType == PointsType.STANDARD) {
			int radius = getRadiusPoi(tb);
			float density = tb.getDensity();
			pointInnerCircle.setColor(point.getColor());
			canvas.drawCircle(x, y, radius + density, pointOuterCircle);
			canvas.drawCircle(x, y, radius - density, pointInnerCircle);
		} else if (pointsType == PointsType.CIRCLE) {
			drawColoredBitmap(canvas, x, y, circle, point.getColor());
		} else if (pointsType == PointsType.SMALL_ICON) {
			drawColoredBitmap(canvas, x, y, smallIconBg, point.getColor());
			bitmapPaint.setColorFilter(null);
			canvas.drawBitmap(image, null, getDstRect(x, y, smallIconSize / 2), bitmapPaint);
		} else if (pointsType == PointsType.BIG_ICON) {
			Bitmap bg = isStale(point) ? bigIconBgStale : bigIconBg;
			drawBigIcon(canvas, x, y, image, bg);
		}
	}

	private void drawBigIcon(Canvas canvas, int x, int y, Bitmap image, Bitmap bg) {
		bitmapPaint.setColorFilter(null);
		float vOffset = bg.getHeight() * POINT_IMAGE_VERTICAL_OFFSET;
		int imageCenterY = (int) (y - vOffset + bg.getHeight() / 2);
		canvas.drawBitmap(bg, x - bg.getWidth() / 2, y - vOffset, bitmapPaint);
		canvas.drawBitmap(image, null, getDstRect(x, imageCenterY, bigIconSize / 2), bitmapPaint);
	}

	private String getSelectedContextMenuPointId() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			MapContextMenu mapContextMenu = mapActivity.getContextMenu();
			Object object = mapContextMenu.getObject();
			if (mapContextMenu.isVisible() && object instanceof AidlMapPointWrapper) {
				AidlMapPointWrapper aMapPoint = (AidlMapPointWrapper) object;
				return aMapPoint.getId();
			}
		}
		return null;
	}

	private void drawColoredBitmap(Canvas canvas, int x, int y, Bitmap bitmap, int color) {
		bitmapPaint.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.MULTIPLY));
		canvas.drawBitmap(bitmap, x - bitmap.getWidth() / 2, y - bitmap.getHeight() / 2, bitmapPaint);
	}

	private Rect getDstRect(int centerX, int centerY, int offset) {
		Rect rect = new Rect();
		rect.left = centerX - offset;
		rect.top = centerY - offset;
		rect.right = centerX + offset;
		rect.bottom = centerY + offset;
		return rect;
	}

	private boolean isStale(AidlMapPointWrapper point) {
		return Boolean.parseBoolean(point.getParams().get(AMapPoint.POINT_STALE_LOC_PARAM));
	}

	private boolean isLayerEnabled() {
		return getApplication().getAidlApi().isAppEnabled(packName) && appLayersPref.get() && layerPref.get();
	}

	@Override
	public void destroyLayer() {
		clearAidlTileProvider();
	}

	@Override
	public boolean drawInScreenPixels() {
		return false;
	}

	@Override
	public boolean disableSingleTap() {
		return false;
	}

	@Override
	public boolean disableLongPressOnMap(PointF point, RotatedTileBox tileBox) {
		return false;
	}

	@Override
	public boolean isObjectClickable(Object o) {
		return o instanceof AidlMapPointWrapper;
	}

	@Override
	public boolean runExclusiveAction(Object o, boolean unknownLocation) {
		return false;
	}

	@Override
	public boolean showMenuAction(@Nullable Object o) {
		return false;
	}

	@Override
	public void collectObjectsFromPoint(PointF point, RotatedTileBox tileBox, List<Object> o, boolean unknownLocation) {
		if (isLayerEnabled()) {
			getFromPoint(tileBox, point, o);
		}
	}

	@Override
	public LatLon getObjectLocation(Object o) {
		if (o instanceof AidlMapPointWrapper) {
			return ((AidlMapPointWrapper) o).getLocation();
		}
		return null;
	}

	@Override
	public PointDescription getObjectName(Object o) {
		if (o instanceof AidlMapPointWrapper) {
			return new PointDescription(PointDescription.POINT_TYPE_MARKER, ((AidlMapPointWrapper) o).getFullName());
		} else {
			return null;
		}
	}

	@Nullable
	public AidlMapPointWrapper getPoint(@NonNull String id) {
		return aidlLayer.getPoint(id);
	}

	@Override
	public LatLon getTextLocation(AidlMapPointWrapper o) {
		return o.getLocation();
	}

	@Override
	public int getTextShift(AidlMapPointWrapper o, RotatedTileBox rb) {
		double result = 0;
		if (pointsType == PointsType.STANDARD) {
			result = getRadiusPoi(rb) * 1.5;
		} else if (pointsType == PointsType.CIRCLE) {
			result = circle.getHeight() * 0.6;
		} else if (pointsType == PointsType.SMALL_ICON) {
			result = smallIconBg.getHeight() / 2.0;
		} else if (pointsType == PointsType.BIG_ICON) {
			result = bigIconBg.getHeight() / 6.0;
		}
		return (int) (result * getTextScale());
	}

	@Override
	public String getText(AidlMapPointWrapper o) {
		return o.getShortName();
	}

	@Override
	public boolean isTextVisible() {
		return true;
	}

	@Override
	public boolean isFakeBoldText() {
		return true;
	}

	public void refresh() {
		if (view != null) {
			view.refreshMap();
		}
	}

	private PointsType getPointsType(int zoom) {
		if (!aidlLayer.isImagePoints()) {
			return zoom >= START_ZOOM ? PointsType.STANDARD : PointsType.INVISIBLE;
		}
		if (zoom >= aidlLayer.getCirclePointMinZoom() && zoom <= aidlLayer.getCirclePointMaxZoom()) {
			return PointsType.CIRCLE;
		} else if (zoom >= aidlLayer.getSmallPointMinZoom() && zoom <= aidlLayer.getSmallPointMaxZoom()) {
			return PointsType.SMALL_ICON;
		} else if (zoom >= aidlLayer.getBigPointMinZoom() && zoom <= aidlLayer.getBigPointMaxZoom()) {
			return PointsType.BIG_ICON;
		}
		return PointsType.INVISIBLE;
	}

	private int getRadiusPoi(RotatedTileBox tb) {
		int r;
		final double zoom = tb.getZoom();
		if (zoom < START_ZOOM) {
			r = 0;
		} else if (zoom <= 11) {
			r = 10;
		} else if (zoom <= 14) {
			r = 12;
		} else {
			r = 14;
		}
		return (int) (r * tb.getDensity());
	}

	private int getPointRadius(RotatedTileBox tb) {
		int r = 0;
		if (pointsType == PointsType.STANDARD) {
			r = getRadiusPoi(tb);
		} else if (pointsType == PointsType.CIRCLE) {
			r = circle.getHeight() / 2;
		} else if (pointsType == PointsType.SMALL_ICON) {
			r = smallIconSize / 2;
		} else if (pointsType == PointsType.BIG_ICON) {
			r = bigIconSize / 2;
		}
		return r * 3 / 2;
	}

	private void getFromPoint(RotatedTileBox tb, PointF point, List<? super AidlMapPointWrapper> points) {
		if (view != null) {
			int ex = (int) point.x;
			int ey = (int) point.y;
			int radius = getPointRadius(tb);
			for (AidlMapPointWrapper p : aidlLayer.getPoints()) {
				LatLon position = p.getLocation();
				if (position != null) {
					int x = (int) tb.getPixXFromLatLon(position.getLatitude(), position.getLongitude());
					int y = (int) tb.getPixYFromLatLon(position.getLatitude(), position.getLongitude());
					if (Math.abs(x - ex) <= radius && Math.abs(y - ey) <= radius) {
						points.add(p);
					}
				}
			}
		}
	}

	/**OpenGL*/
	private Bitmap getSelectedBitmap(@NonNull AidlMapPointWrapper point, @NonNull Bitmap image) {
		Bitmap bg = isStale(point) ? bigIconBgSelectedStale : bigIconBgSelected;
		Bitmap bitmapResult = Bitmap.createBitmap(bg.getWidth(), bg.getHeight(), Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmapResult);
		bitmapPaint.setColorFilter(null);
		int cx = bg.getWidth() / 2;
		int cy = bg.getHeight() / 2;
		canvas.drawBitmap(bg, 0, 0, bitmapPaint);
		canvas.drawBitmap(image, null, getDstRect(cx, cy, bigIconSize / 2), bitmapPaint);
		return bitmapResult;
	}

	/** OpenGL */
	public TextRasterizer.Style getTextStyle() {
		return MapTextLayer.getTextStyle(getContext(), nightMode, getTextScale(), view.getDensity());
	}

	/** OpenGL */
	public void showAidlTileProvider(float density) {
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer == null || !isLayerEnabled() || aidlMapLayerProvider != null) {
			return;
		}

		clearAidlTileProvider();
		imageRequests.clear();
		float y_offset = ((float)bigIconBg.getHeight()) * (1.0f - POINT_IMAGE_VERTICAL_OFFSET);
		aidlMapLayerProvider = new AidlTileProvider(this, density, y_offset);
		mapMarkersCollection = new MapMarkersCollection();

		for (AidlMapPointWrapper point : aidlLayer.getPoints()) {
			LatLon l = point.getLocation();
			if (l != null) {
				Bitmap image = null;
				if (pointsType != PointsType.STANDARD) {
					String imageUri = point.getParams().get(AMapPoint.POINT_IMAGE_URI_PARAM);
					if (!TextUtils.isEmpty(imageUri)) {
						image = pointImages.get(imageUri);
						if (image == null) {
							imageRequests.add(imageUri);
						}
					}
				}
				if (image == null) {
					image = placeholder;
				}
				boolean selected = selectedPointId != null && selectedPointId.equals(point.getId());
				if (!selected) {
					aidlMapLayerProvider.addToData(point, image, isStale(point), getText(point));
				} else {
					Bitmap bitmap = getSelectedBitmap(point, image);
					int baseOrder = selected ? getBaseOrder() - 1 : getBaseOrder();
					MapMarkerBuilder mapMarkerBuilder = new MapMarkerBuilder();
					int x = MapUtils.get31TileNumberX(l.getLongitude());
					int y = MapUtils.get31TileNumberY(l.getLatitude());
					PointI pointI = new PointI(x, y);
					mapMarkerBuilder
							.setPosition(pointI)
							.setIsHidden(false)
							.setBaseOrder(baseOrder - 1)
							.setPinIcon(NativeUtilities.createSkImageFromBitmap(bitmap))
							.setPinIconVerticalAlignment(MapMarker.PinIconVerticalAlignment.Top)
							.setPinIconHorisontalAlignment(MapMarker.PinIconHorisontalAlignment.CenterHorizontal)
							.setPinIconOffset(new PointI(0, (int) y_offset))
							.setCaptionStyle(getTextStyle())
							.setCaptionTopSpace(y_offset)
							.setCaption(getText(point));
					mapMarkerBuilder.buildAndAddToCollection(mapMarkersCollection);
				}
			}
		}
		aidlMapLayerProvider.drawSymbols(mapRenderer);
		mapRenderer.addSymbolsProvider(mapMarkersCollection);
		if (imageRequests.size() > 0) {
			executeTaskInBackground(new PointImageReaderTask(this), imageRequests.toArray(new String[0]));
		}
	}

	/** OpenGL */
	public void clearAidlTileProvider() {
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer == null) {
			return;
		}
		if (aidlMapLayerProvider != null) {
			aidlMapLayerProvider.clearSymbols(mapRenderer);
			aidlMapLayerProvider = null;
		}
		if (mapMarkersCollection != null) {
			mapRenderer.removeSymbolsProvider(mapMarkersCollection);
			mapMarkersCollection = null;
		}
	}

	public enum PointsType {
		STANDARD,
		CIRCLE,
		SMALL_ICON,
		BIG_ICON,
		INVISIBLE
	}

	private static class PointImageReaderTask extends AsyncTask<String, Void, Boolean> {

		private final WeakReference<AidlMapLayer> layerRef;
		private final CropCircleTransformation circleTransformation = new CropCircleTransformation();

		PointImageReaderTask(AidlMapLayer layer) {
			this.layerRef = new WeakReference<>(layer);
		}

		@Override
		protected Boolean doInBackground(String... imageUriStrs) {
			boolean res = false;
			for (String imageUriStr : imageUriStrs) {
				try {
					AidlMapLayer layer = layerRef.get();
					Uri fileUri = Uri.parse(imageUriStr);
					if (layer != null && fileUri != null) {
						try {
							InputStream ims = layer.getContext().getContentResolver().openInputStream(fileUri);
							if (ims != null) {
								Bitmap bitmap = BitmapFactory.decodeStream(ims);
								if (bitmap != null) {
									bitmap = circleTransformation.transform(bitmap);
									if (bitmap.getWidth() != layer.bigIconSize || bitmap.getHeight() != layer.bigIconSize) {
										bitmap = AndroidUtils.scaleBitmap(bitmap, layer.bigIconSize, layer.bigIconSize, false);
									}
									layer.pointImages.put(imageUriStr, bitmap);
									res = true;
								}
								ims.close();
							}
						} catch (IOException e) {
							// ignore
						}
					} else {
						break;
					}
				} catch (Throwable e) {
					// ignore
				}
			}
			return res;
		}

		@Override
		protected void onPostExecute(Boolean res) {
			AidlMapLayer layer = layerRef.get();
			if (layer != null && res) {
				layer.refresh();
			}
		}
	}

	private void addMapsInitializedListener() {
		OsmandApplication app = getApplication();
		if (app.isApplicationInitializing()) {
			app.getAppInitializer().addListener(new AppInitializer.AppInitializeListener() {
				@Override
				public void onStart(AppInitializer init) {
				}

				@Override
				public void onProgress(AppInitializer init, AppInitializer.InitEvents event) {
					if (event == AppInitializer.InitEvents.MAPS_INITIALIZED) {
						mapsInitialized = true;
					}
				}

				@Override
				public void onFinish(AppInitializer init) {
				}
			});
		} else {
			mapsInitialized = true;
		}
	}

	public Paint getPointInnerCircle() {
		return pointInnerCircle;
	}

	public Paint getPointOuterCircle() {
		return pointOuterCircle;
	}
	public Paint getBitmapPaint() {
		return bitmapPaint;
	}

	public Bitmap getCircle() {
		return circle;
	}

	public Bitmap getSmallIconBg() {
		return smallIconBg;
	}

	public Bitmap getBigIconBg() {
		return bigIconBg;
	}

	public Bitmap getBigIconBgStale() {
		return bigIconBgStale;
	}

	public PointsType getPointsType() {
		return pointsType;
	}

	public int getBigIconSize() {
		return bigIconSize;
	}

	public int getSmallIconSize() {
		return smallIconSize;
	}

	public int getRadius() {
		return radius;
	}
}
