package net.osmand.plus.views.layers;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import net.osmand.PlatformUtil;
import net.osmand.core.android.MapRendererView;
import net.osmand.core.jni.TextRasterizer;
import net.osmand.data.LatLon;
import net.osmand.data.NearbyPlacePoint;
import net.osmand.data.PointDescription;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.R;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.layers.ContextMenuLayer.IContextMenuProvider;
import net.osmand.plus.views.layers.base.OsmandMapLayer;
import net.osmand.plus.views.layers.core.NearbyPlacesTileProvider;
import net.osmand.wiki.WikiCoreHelper;
import net.osmand.wiki.WikiImage;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.List;

public class NearbyPlacesLayer extends OsmandMapLayer implements IContextMenuProvider {

	private static final int START_ZOOM = 6;
	private static final Log LOG = PlatformUtil.getLog(NearbyPlacesLayer.class);

	protected List<NearbyPlacePoint> cache = new ArrayList<>();
	private ContextMenuLayer contextMenuLayer;
	private boolean showNearbyPoints;
	private float textScale = 1f;
	private boolean nightMode;

	private Bitmap circle;
	private Bitmap smallIconBg;
	private Bitmap bigIconBg;
	private Paint pointInnerCircle;
	private Paint pointOuterCircle;
	private Paint bitmapPaint;
	private static final int SMALL_ICON_BORDER_DP = 2;
	private static final int BIG_ICON_BORDER_DP = 2;
	private static final int SMALL_ICON_SIZE_DP = 20;
	private static final int BIG_ICON_SIZE_DP = 40;
	private static final int POINT_OUTER_COLOR = 0xffffffff;
	private int smallIconSize;
	private int bigIconSize;
	private int bigIconBorderSize;
	private int smallIconBorderSize;


	public CustomMapObjects<NearbyPlacePoint> customObjectsDelegate = new OsmandMapLayer.CustomMapObjects<>();

	//OpenGl
	private NearbyPlacesTileProvider nearbyPlacesMapLayerProvider;

	public NearbyPlacesLayer(@NonNull Context ctx) {
		super(ctx);
	}

	@Override
	public void initLayer(@NonNull OsmandMapTileView view) {
		super.initLayer(view);

		boolean night = getApplication().getDaynightHelper().isNightMode();
		contextMenuLayer = view.getLayerByClass(ContextMenuLayer.class);

		nightMode = night;

		pointInnerCircle = new Paint();
		pointInnerCircle.setColor(getColor(R.color.poi_background));
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
		recreateBitmaps(night);
	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
	}

	private void recreateBitmaps(boolean night) {
		float scale = getApplication().getOsmandMap().getCarDensityScaleCoef();
		circle = RenderingIcons.getBitmapFromVectorDrawable(getApplication(), R.drawable.bg_point_circle);
		smallIconBg = getScaledBitmap(night
				? R.drawable.map_pin_user_location_small_night
				: R.drawable.map_pin_user_location_small_day, scale);
		bigIconBg = getScaledBitmap(night
				? R.drawable.map_pin_user_location_night
				: R.drawable.map_pin_user_location_day, scale);

		smallIconSize = AndroidUtils.dpToPxAuto(getContext(), SMALL_ICON_SIZE_DP);
		bigIconSize = AndroidUtils.dpToPxAuto(getContext(), BIG_ICON_SIZE_DP);
		bigIconBorderSize = AndroidUtils.dpToPxAuto(getContext(), BIG_ICON_BORDER_DP);
		smallIconBorderSize = AndroidUtils.dpToPxAuto(getContext(), SMALL_ICON_BORDER_DP);
	}

	@Override
	protected void updateResources() {
		super.updateResources();
		recreateBitmaps(nightMode);
	}

	@Override
	protected void cleanupResources() {
		super.cleanupResources();
		clearNearbyPoints();
	}

	@Override
	public boolean drawInScreenPixels() {
		return true;
	}

	@Override
	public void onPrepareBufferImage(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
		super.onPrepareBufferImage(canvas, tileBox, settings);
		boolean nightMode = settings != null && settings.isNightMode();
		boolean nightModeChanged = this.nightMode != nightMode;
		this.nightMode = nightMode;
		float textScale = getTextScale();
		boolean textScaleChanged = this.textScale != textScale;
		this.textScale = textScale;
		boolean showNearbyPoints = !customObjectsDelegate.getMapObjects().isEmpty();
		boolean showNearbyplacesChanged = this.showNearbyPoints != showNearbyPoints;
		this.showNearbyPoints = showNearbyPoints;

		if (hasMapRenderer()) {
			if ((mapActivityInvalidated || mapRendererChanged || nightModeChanged || showNearbyplacesChanged
					|| textScaleChanged
					|| customObjectsDelegate.isChanged())) {
				android.util.Log.d("Corwin", "showNearbies: ");
				showNearbyPoints();
				if (customObjectsDelegate != null) {
					customObjectsDelegate.acceptChanges();
				}
				mapRendererChanged = false;
			}
		}
		mapActivityInvalidated = false;
	}

	public synchronized void showNearbyPoints() {
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer == null) {
			return;
		}
		clearNearbyPoints();
		float textScale = getTextScale();
		nearbyPlacesMapLayerProvider = new NearbyPlacesTileProvider(getApplication(), this,
				getPointsOrder(),
				view.getDensity());

		if (customObjectsDelegate != null) {
			List<NearbyPlacePoint> points = customObjectsDelegate.getMapObjects();
			showNearbyPoints(textScale, points);
			nearbyPlacesMapLayerProvider.drawSymbols(mapRenderer);
		}
	}

	private void showNearbyPoints(float textScale, List<NearbyPlacePoint> points) {
		for (NearbyPlacePoint nearbyPlacePoint : points) {
			nearbyPlacesMapLayerProvider.addToData(nearbyPlacePoint, textScale);
		}
	}

	private TextRasterizer.Style getTextStyle(float textScale) {
		return MapTextLayer.getTextStyle(getContext(), nightMode, textScale, view.getDensity());
	}

	public void clearNearbyPoints() {
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer == null || nearbyPlacesMapLayerProvider == null) {
			return;
		}
		nearbyPlacesMapLayerProvider.clearSymbols(mapRenderer);
		nearbyPlacesMapLayerProvider = null;
	}

	@Override
	public boolean onLongPressEvent(@NonNull PointF point, @NonNull RotatedTileBox tileBox) {
		return false;
	}

	@Override
	public PointDescription getObjectName(Object o) {
		if (o instanceof NearbyPlacePoint) {
			return ((NearbyPlacePoint) o).getPointDescription(getContext());
		}
		return null;
	}

	@Override
	public void collectObjectsFromPoint(PointF point, RotatedTileBox tileBox, List<Object> res,
	                                    boolean unknownLocation, boolean excludeUntouchableObjects) {
	}

	@Override
	public LatLon getObjectLocation(Object o) {
		if (o instanceof NearbyPlacePoint) {
			return new LatLon(((NearbyPlacePoint) o).getLatitude(), ((NearbyPlacePoint) o).getLongitude());
		}
		return null;
	}

	public void setCustomMapObjects(List<WikiCoreHelper.OsmandApiFeatureData> nearbyPlacePoints) {
		if (customObjectsDelegate != null) {
			List<NearbyPlacePoint> nearbyPlacePointsList = new ArrayList<>();
			for (WikiCoreHelper.OsmandApiFeatureData data :
					nearbyPlacePoints) {
				NearbyPlacePoint point = new NearbyPlacePoint(data);
				nearbyPlacePointsList.add(point);
				WikiImage wikiImage = WikiCoreHelper.getImageData(point.photoTitle);
				if (wikiImage != null) {
					Picasso.get().load(wikiImage.getImageStubUrl()).into(new Target() {
						@Override
						public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
							point.imageBitmap = bitmap;
							android.util.Log.d("Corwin", "onBitmapLoaded: " + point.photoTitle);
							customObjectsDelegate.onMapObjectUpdated(point);
						}

						@Override
						public void onBitmapFailed(Exception e, Drawable errorDrawable) {
							android.util.Log.d("Corwin", "onBitmapFailed: " + point.photoTitle);
						}

						@Override
						public void onPrepareLoad(Drawable placeHolderDrawable) {
						}
					});
				}
			}
			customObjectsDelegate.setCustomMapObjects(nearbyPlacePointsList);
			getApplication().getOsmandMap().refreshMap();
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

	public int getBigIconSize() {
		return bigIconSize;
	}

	public int getSmallIconSize() {
		return smallIconSize;
	}

	public int getSmallIconBorderSize() {
		return smallIconBorderSize;
	}

	public int getBigIconBorderSize() {
		return bigIconBorderSize;
	}

	public int getPointOuterColor() {
		return POINT_OUTER_COLOR;
	}
}


