package net.osmand.plus.views;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.net.Uri;
import android.os.AsyncTask;
import android.text.TextUtils;

import net.osmand.AndroidUtils;
import net.osmand.aidl.map.ALatLon;
import net.osmand.aidl.maplayer.AMapLayer;
import net.osmand.aidl.maplayer.point.AMapPoint;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.ContextMenuLayer.IContextMenuProvider;
import net.osmand.plus.widgets.tools.CropCircleTransformation;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static net.osmand.aidl.maplayer.point.AMapPoint.POINT_IMAGE_SIZE_PX;

public class AidlMapLayer extends OsmandMapLayer implements IContextMenuProvider {
	private static int POINT_OUTER_COLOR = 0x88555555;
	private static int PAINT_TEXT_ICON_COLOR = Color.BLACK;

	private final MapActivity map;
	private AMapLayer aidlLayer;
	private OsmandMapTileView view;
	private Paint pointInnerCircle;
	private Paint pointOuter;
	private Paint bitmapPaint;
	private final static float startZoom = 7;
	private Paint paintTextIcon;

	private Map<String, Bitmap> pointImages = new ConcurrentHashMap<>();

	public AidlMapLayer(MapActivity map, AMapLayer aidlLayer) {
		this.map = map;
		this.aidlLayer = aidlLayer;
	}

	@Override
	public void initLayer(OsmandMapTileView view) {
		this.view = view;

		pointInnerCircle = new Paint();
		pointInnerCircle.setColor(view.getApplication().getResources().getColor(R.color.poi_background));
		pointInnerCircle.setStyle(Paint.Style.FILL);
		pointInnerCircle.setAntiAlias(true);

		paintTextIcon = new Paint();
		paintTextIcon.setTextSize(10 * view.getDensity());
		paintTextIcon.setTextAlign(Paint.Align.CENTER);
		paintTextIcon.setFakeBoldText(true);
		paintTextIcon.setColor(PAINT_TEXT_ICON_COLOR);
		paintTextIcon.setAntiAlias(true);

		pointOuter = new Paint();
		pointOuter.setColor(POINT_OUTER_COLOR);
		pointOuter.setAntiAlias(true);
		pointOuter.setStyle(Paint.Style.FILL_AND_STROKE);

		bitmapPaint = new Paint();
		bitmapPaint.setAntiAlias(true);
		bitmapPaint.setDither(true);
		bitmapPaint.setFilterBitmap(true);
	}

	private int getRadiusPoi(RotatedTileBox tb) {
		int r;
		final double zoom = tb.getZoom();
		if (zoom < startZoom) {
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

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
	}

	@Override
	public void onPrepareBufferImage(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
		float density = (float) Math.ceil(tileBox.getDensity());
		final int radius = getRadiusPoi(tileBox);
		final int maxRadius = (int) (Math.max(radius, POINT_IMAGE_SIZE_PX) + density);
		canvas.rotate(-tileBox.getRotate(), tileBox.getCenterPixelX(), tileBox.getCenterPixelY());
		paintTextIcon.setTextSize(radius * 3 / 2);

		Set<String> imageRequests = new HashSet<>();
		List<AMapPoint> points = aidlLayer.getPoints();
		for (AMapPoint point : points) {
			ALatLon l = point.getLocation();
			if (l != null) {
				int x = (int) tileBox.getPixXFromLatLon(l.getLatitude(), l.getLongitude());
				int y = (int) tileBox.getPixYFromLatLon(l.getLatitude(), l.getLongitude());
				if (tileBox.containsPoint(x, y, maxRadius)) {
					Map<String, String> params = point.getParams();
					String imageUriStr = params.get(AMapPoint.POINT_IMAGE_URI_PARAM);
					if (!TextUtils.isEmpty(imageUriStr)) {
						Bitmap bitmap = pointImages.get(imageUriStr);
						if (bitmap == null) {
							imageRequests.add(imageUriStr);
						} else {
							canvas.drawBitmap(bitmap, x - bitmap.getHeight() / 2, y - bitmap.getWidth() / 2, bitmapPaint);
							canvas.drawText(point.getShortName(), x, y + maxRadius * 0.9f, paintTextIcon);
						}
					} else {
						pointInnerCircle.setColor(point.getColor());
						pointOuter.setColor(POINT_OUTER_COLOR);
						canvas.drawCircle(x, y, radius + density, pointOuter);
						canvas.drawCircle(x, y, radius - density, pointInnerCircle);
						canvas.drawText(point.getShortName(), x, y + radius * 2.5f, paintTextIcon);
					}
				}
			}
		}
		if (imageRequests.size() > 0) {
			executeTaskInBackground(new PointImageReaderTask(this), imageRequests.toArray(new String[imageRequests.size()]));
		}
	}

	@Override
	public void destroyLayer() {
	}

	@Override
	public boolean drawInScreenPixels() {
		return false;
	}

	public void refresh() {
		if (view != null) {
			view.refreshMap();
		}
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
		return o instanceof AMapPoint;
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
		if (o instanceof AMapPoint) {
			ALatLon loc = ((AMapPoint) o).getLocation();
			if (loc != null) {
				return new LatLon(loc.getLatitude(), loc.getLongitude());
			}
		}
		return null;
	}

	@Override
	public PointDescription getObjectName(Object o) {
		if (o instanceof AMapPoint) {
			return new PointDescription(PointDescription.POINT_TYPE_MARKER, ((AMapPoint) o).getFullName());
		} else {
			return null;
		}
	}

	private void getFromPoint(RotatedTileBox tb, PointF point, List<? super AMapPoint> points) {
		if (view != null) {
			int ex = (int) point.x;
			int ey = (int) point.y;
			final int rp = getRadiusPoi(tb);
			int compare = rp;
			int radius = rp * 3 / 2;
			for (AMapPoint p : aidlLayer.getPoints()) {
				ALatLon position = p.getLocation();
				if (position != null) {
					int x = (int) tb.getPixXFromLatLon(position.getLatitude(), position.getLongitude());
					int y = (int) tb.getPixYFromLatLon(position.getLatitude(), position.getLongitude());
					if (Math.abs(x - ex) <= compare && Math.abs(y - ey) <= compare) {
						compare = radius;
						points.add(p);
					}
				}
			}
		}
	}

	private static class PointImageReaderTask extends AsyncTask<String, Void, Void> {

		private WeakReference<AidlMapLayer> layerRef;
		private CropCircleTransformation circleTransformation = new CropCircleTransformation();

		PointImageReaderTask(AidlMapLayer layer) {
			this.layerRef = new WeakReference<>(layer);
		}

		@Override
		protected Void doInBackground(String... imageUriStrs) {
			for (String imageUriStr : imageUriStrs) {
				Uri fileUri = Uri.parse(imageUriStr);
				try {
					AidlMapLayer layer = layerRef.get();
					if (layer != null) {
						try {
							InputStream ims = layer.map.getContentResolver().openInputStream(fileUri);
							if (ims != null) {
								Bitmap bitmap = BitmapFactory.decodeStream(ims);
								if (bitmap != null) {
									bitmap = circleTransformation.transform(bitmap);
									if (bitmap.getWidth() != POINT_IMAGE_SIZE_PX || bitmap.getHeight() != POINT_IMAGE_SIZE_PX) {
										bitmap = AndroidUtils.scaleBitmap(bitmap, POINT_IMAGE_SIZE_PX, POINT_IMAGE_SIZE_PX, false);
									}
									layer.pointImages.put(imageUriStr, bitmap);
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
			return null;
		}
	}
}
