package net.osmand.plus.views;

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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import net.osmand.AndroidUtils;
import net.osmand.aidl.AidlMapLayerWrapper;
import net.osmand.aidl.AidlMapPointWrapper;
import net.osmand.aidlapi.maplayer.point.AMapPoint;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.MapContextMenu;
import net.osmand.plus.views.ContextMenuLayer.IContextMenuProvider;
import net.osmand.plus.widgets.tools.CropCircleTransformation;

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

public class AidlMapLayer extends OsmandMapLayer implements IContextMenuProvider, MapTextLayer.MapTextProvider<AidlMapPointWrapper> {

	private static final float POINT_IMAGE_VERTICAL_OFFSET = 0.91f;

	private static final int POINT_OUTER_COLOR = 0x88555555;
	private static final float START_ZOOM = 7;
	private static final int SMALL_ICON_SIZE_DP = 20;
	private static final int BIG_ICON_SIZE_DP = 40;

	private final MapActivity map;
	private OsmandMapTileView view;

	private AidlMapLayerWrapper aidlLayer;

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

	private Map<String, Bitmap> pointImages = new ConcurrentHashMap<>();

	private Set<String> imageRequests = new HashSet<>();
	private List<AidlMapPointWrapper> displayedPoints = new ArrayList<>();

	public AidlMapLayer(MapActivity map, AidlMapLayerWrapper aidlLayer) {
		this.map = map;
		this.aidlLayer = aidlLayer;
	}

	@Override
	public void initLayer(OsmandMapTileView view) {
		this.view = view;

		Resources res = view.getResources();
		boolean night = map.getMyApplication().getDaynightHelper().isNightMode();

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

		circle = BitmapFactory.decodeResource(res, R.drawable.map_white_shield_small);
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

		smallIconSize = AndroidUtils.dpToPx(map, SMALL_ICON_SIZE_DP);
		bigIconSize = AndroidUtils.dpToPx(map, BIG_ICON_SIZE_DP);

		mapTextLayer = view.getLayerByClass(MapTextLayer.class);
	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
	}

	@Override
	public void onPrepareBufferImage(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
		pointsType = getPointsType(tileBox.getZoom());
		if (pointsType == PointsType.INVISIBLE) {
			mapTextLayer.putData(this, Collections.emptyList());
			return;
		}
		displayedPoints.clear();
		imageRequests.clear();

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
			executeTaskInBackground(new PointImageReaderTask(this), imageRequests.toArray(new String[imageRequests.size()]));
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
		MapContextMenu mapContextMenu = map.getContextMenu();
		Object object = mapContextMenu.getObject();
		if (mapContextMenu.isVisible() && object instanceof AidlMapPointWrapper) {
			AidlMapPointWrapper aMapPoint = (AidlMapPointWrapper) object;
			return aMapPoint.getId();
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

	@Override
	public void destroyLayer() {
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
	public boolean disableLongPressOnMap() {
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
	public void collectObjectsFromPoint(PointF point, RotatedTileBox tileBox, List<Object> o, boolean unknownLocation) {
		getFromPoint(tileBox, point, o);
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
		if (pointsType == PointsType.STANDARD) {
			return (int) (getRadiusPoi(rb) * 1.5);
		} else if (pointsType == PointsType.CIRCLE) {
			return (int) (circle.getHeight() * 0.6);
		} else if (pointsType == PointsType.SMALL_ICON) {
			return smallIconBg.getHeight() / 2;
		} else if (pointsType == PointsType.BIG_ICON) {
			return bigIconBg.getHeight() / 6;
		}
		return 0;
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

	private enum PointsType {
		STANDARD,
		CIRCLE,
		SMALL_ICON,
		BIG_ICON,
		INVISIBLE
	}

	private static class PointImageReaderTask extends AsyncTask<String, Void, Boolean> {

		private WeakReference<AidlMapLayer> layerRef;
		private CropCircleTransformation circleTransformation = new CropCircleTransformation();

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
							InputStream ims = layer.map.getContentResolver().openInputStream(fileUri);
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
}
