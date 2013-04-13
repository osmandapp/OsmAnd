package net.osmand.plus.distancecalculator;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import net.osmand.access.AccessibleToast;
import net.osmand.data.LatLon;
import net.osmand.util.MapUtils;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.MapInfoLayer;
import net.osmand.plus.views.OsmandMapLayer;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.mapwidgets.TextInfoWidget;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Join;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.view.View;
import android.widget.Toast;

public class DistanceCalculatorPlugin extends OsmandPlugin {
	private static final String ID = "osmand.distance";
	private OsmandApplication app;
	private DistanceCalculatorLayer distanceCalculatorLayer;
	private TextInfoWidget distanceControl;
	private List<LatLon> measurementPoints = new ArrayList<LatLon>();
	
	private int distanceMeasurementMode = 0; 

	public DistanceCalculatorPlugin(OsmandApplication app) {
		this.app = app;
	}
	
	@Override
	public String getId() {
		return ID;
	}

	@Override
	public String getDescription() {
		return app.getString(R.string.osmand_distance_plugin_description);
	}

	@Override
	public String getName() {
		return app.getString(R.string.osmand_distance_plugin_name);
	}

	@Override
	public boolean init(OsmandApplication app) {
		return true;
	}

	@Override
	public void registerLayers(MapActivity activity) {
		// remove old if existing
		if(distanceCalculatorLayer != null) {
			activity.getMapView().removeLayer(distanceCalculatorLayer);
		}
		distanceCalculatorLayer = new DistanceCalculatorLayer();
		activity.getMapView().addLayer(distanceCalculatorLayer, 8.5f);
		
		registerWidget(activity);
	}
	
	private void registerWidget(MapActivity activity) {
		MapInfoLayer mapInfoLayer = activity.getMapLayers().getMapInfoLayer();
		if (mapInfoLayer != null ) {
			distanceControl = createDistanceControl(activity, mapInfoLayer.getPaintText(), mapInfoLayer.getPaintSubText());
			mapInfoLayer.getMapInfoControls().registerSideWidget(distanceControl,
					R.drawable.widget_distance, R.string.map_widget_distancemeasurement, "distance.measurement", false,
					EnumSet.of(ApplicationMode.DEFAULT, ApplicationMode.PEDESTRIAN),
					EnumSet.noneOf(ApplicationMode.class), 21);
			mapInfoLayer.recreateControls();
			updateText();
		}
	}
	
	private void updateText() {
		if (distanceControl != null) {
			if (distanceMeasurementMode == 0) {
				distanceControl.setText(app.getString(R.string.dist_control_start), "");
			} else {
				float dist = 0;
				for (int j = 1; j < measurementPoints.size(); j++) {
					dist += MapUtils.getDistance(measurementPoints.get(j - 1), measurementPoints.get(j));
				}
				String ds = OsmAndFormatter.getFormattedDistance(dist, app);
				int ls = ds.lastIndexOf(' ');
				if (ls == -1) {
					distanceControl.setText(ds, null);
				} else {
					distanceControl.setText(ds.substring(0, ls), ds.substring(ls + 1));
				}
			}
		}
	}
	
	private TextInfoWidget createDistanceControl(final MapActivity activity, Paint paintText, Paint paintSubText) {
		final TextInfoWidget distanceControl = new TextInfoWidget(activity, 0, paintText, paintSubText);
		distanceControl.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if(distanceMeasurementMode == 0) {
					AccessibleToast.makeText(app, app.getString(R.string.use_distance_measurement), Toast.LENGTH_LONG).show();
					distanceMeasurementMode++;
				}  else if(distanceMeasurementMode == 1){
					AccessibleToast.makeText(app, app.getString(R.string.use_clear_distance_measurement), Toast.LENGTH_LONG).show();
					distanceMeasurementMode++;
				} else {
					measurementPoints.clear();
					distanceMeasurementMode = 0;
				}
				activity.getMapView().refreshMap();
				updateText();
			}
		});
		distanceControl.setImageDrawable(app.getResources().getDrawable(R.drawable.widget_distance));
		return distanceControl;
	}


	public class DistanceCalculatorLayer extends OsmandMapLayer {
		private OsmandMapTileView view;

		private Bitmap originIcon;
		private Bitmap destinationIcon;
		private Paint bitmapPaint;

		private Path path;

		private Paint paint;

		public DistanceCalculatorLayer() {
		}

		@Override
		public void initLayer(OsmandMapTileView view) {
			this.view = view;
			originIcon = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_pin_origin);
			destinationIcon = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_pin_destination);
			bitmapPaint = new Paint();
			bitmapPaint.setDither(true);
			bitmapPaint.setAntiAlias(true);
			bitmapPaint.setFilterBitmap(true);
			path = new Path();
			
			paint = new Paint();
			paint.setStyle(Style.STROKE);
			paint.setStrokeWidth(10);
			paint.setAntiAlias(true);
			paint.setStrokeCap(Cap.ROUND);
			paint.setStrokeJoin(Join.ROUND);
			paint.setColor(0xdd6CB336);
		}
		
		@Override
		public boolean onSingleTap(PointF point) {
			if(distanceMeasurementMode == 1) {
				LatLon l = view.getLatLonFromScreenPoint(point.x, point.y);
				measurementPoints.add(l);
				view.refreshMap();
				updateText();
				return true;
			}
			return false;
		}
		
		@Override
		public boolean onLongPressEvent(PointF point) {
			if(distanceMeasurementMode == 1 && measurementPoints.size() > 0) {
				measurementPoints.remove(measurementPoints.size() - 1);
				view.refreshMap();
				updateText();
				return true;
			}
			return false;
		}

		@Override
		public void onDraw(Canvas canvas, RectF latlonRect, RectF tilesRect, DrawSettings settings) {
			if (distanceMeasurementMode != 0) {
				path.reset();
				int marginY = originIcon.getHeight();
				int marginX = originIcon.getWidth() / 2;
				for (int i = 0; i < measurementPoints.size(); i++) {
					LatLon point = measurementPoints.get(i);
					double lat = point.getLatitude();
					double lon = point.getLongitude();
					int locationX = view.getMapXForPoint(lon);
					int locationY = view.getMapYForPoint(lat);
					if (view.isPointOnTheRotatedMap(lat, lon)) {
						canvas.rotate(-view.getRotate(), locationX, locationY);
						canvas.drawBitmap(distanceMeasurementMode == 1? originIcon : destinationIcon, 
								locationX - marginX, locationY - marginY, bitmapPaint);
						canvas.rotate(view.getRotate(), locationX, locationY);
					}
					if (i == 0) {
						path.moveTo(locationX, locationY);
					} else {
						path.lineTo(locationX, locationY);
					}
				}
				canvas.drawPath(path, paint);
			}
		}

		@Override
		public void destroyLayer() {
		}

		@Override
		public boolean drawInScreenPixels() {
			return false;
		}

	}
}
