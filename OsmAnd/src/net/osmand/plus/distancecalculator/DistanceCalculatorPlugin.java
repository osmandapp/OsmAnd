package net.osmand.plus.distancecalculator;

import gnu.trove.list.array.TIntArrayList;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import net.osmand.CallbackWithObject;
import net.osmand.IndexConstants;
import net.osmand.access.AccessibleToast;
import net.osmand.data.LatLon;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuAdapter.OnContextMenuClick;
import net.osmand.plus.GPXUtilities;
import net.osmand.plus.GPXUtilities.GPXFile;
import net.osmand.plus.GPXUtilities.Route;
import net.osmand.plus.GPXUtilities.Track;
import net.osmand.plus.GPXUtilities.TrkSegment;
import net.osmand.plus.GPXUtilities.WptPt;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings.CommonPreference;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.views.ContextMenuLayer;
import net.osmand.plus.views.MapInfoLayer;
import net.osmand.plus.views.OsmandMapLayer;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.mapwidgets.TextInfoWidget;
import net.osmand.util.MapUtils;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Join;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.AsyncTask;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class DistanceCalculatorPlugin extends OsmandPlugin {
	private static final String ID = "osmand.distance";
	private OsmandApplication app;
	private DistanceCalculatorLayer distanceCalculatorLayer;
	private TextInfoWidget distanceControl;
	
	private List<LinkedList<WptPt>> measurementPoints = new ArrayList<LinkedList<WptPt>>();
	private GPXFile originalGPX;
	private String distance = null;
	private DisplayMetrics dm;
	
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
		return app.getString(R.string.osmand_distance_planning_plugin_description);
	}

	@Override
	public String getName() {
		return app.getString(R.string.osmand_distance_planning_plugin_name);
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
		activity.getMapView().addLayer(distanceCalculatorLayer, 4.5f);
		
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
			String ds = distance;
			if (ds == null) {
				if(distanceMeasurementMode == 0) {
					distanceControl.setText(app.getString(R.string.dist_control_start), "");
				} else {
					distanceControl.setText("0", ""); //$NON-NLS-1$
				}
			} else {
				int ls = ds.lastIndexOf(' ');
				if (ls == -1) {
					distanceControl.setText(ds, null);
				} else {
					distanceControl.setText(ds.substring(0, ls), ds.substring(ls + 1));
				}
			}
		}
	}
	private void showDialog(final MapActivity activity) {
		Builder bld = new AlertDialog.Builder(activity);
		final TIntArrayList  list = new TIntArrayList();
		if(distanceMeasurementMode == 0) {
			list.add(R.string.distance_measurement_start_editing);
		} else {
			list.add(R.string.distance_measurement_finish_editing);
		}
		if(measurementPoints.size() > 0) {
			list.add(R.string.distance_measurement_finish_subtrack);
			list.add(R.string.distance_measurement_clear_route);
			list.add(R.string.distance_measurement_save_gpx);
		}
		list.add(R.string.distance_measurement_load_gpx);
		String[] items = new String[list.size()];
		for(int i = 0; i < items.length; i++) {
			items[i] = activity.getString(list.get(i));
		}
		bld.setItems(items, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				int id = list.get(which);
				if (id == R.string.distance_measurement_start_editing) {
					distanceMeasurementMode = 1;
					startEditingHelp(activity) ;
				} else if (id == R.string.distance_measurement_finish_editing) {
					distanceMeasurementMode = 0;
				} else if (id == R.string.distance_measurement_finish_subtrack) {
					measurementPoints.add(new LinkedList<GPXUtilities.WptPt>());
				} else if (id == R.string.distance_measurement_clear_route) {
					distanceMeasurementMode = 0;
					measurementPoints.clear();
					calculateDistance();
				} else if (id == R.string.distance_measurement_save_gpx) {
					saveGpx(activity);
				} else if (id == R.string.distance_measurement_load_gpx) {
					loadGpx(activity);
				}
				activity.getMapView().refreshMap();
				updateText();
			}
		});
		bld.show();
	}
	

	protected void loadGpx(final MapActivity activity) {
		activity.getMapLayers().selectGPXFileLayer(true, false, false, new CallbackWithObject<GPXUtilities.GPXFile>() {
			
			@Override
			public boolean processResult(GPXFile result) {
				measurementPoints.clear();
				if (result != null) {
					originalGPX = result;
					for (Track t : result.tracks) {
						for (TrkSegment s : t.segments) {
							if (s.points.size() > 0) {
								LinkedList<WptPt> l = new LinkedList<WptPt>(s.points);
								measurementPoints.add(l);
							}
						}
					}
					for (Route r : result.routes) {
						LinkedList<WptPt> l = new LinkedList<WptPt>(r.points);
						measurementPoints.add(l);
					}
					for (WptPt p : result.points) {
						LinkedList<WptPt> l = new LinkedList<WptPt>();
						l.add(p);
						measurementPoints.add(l);
					}
					WptPt pt = result.findPointToShow();
					OsmandMapTileView mapView = activity.getMapView();
					if(pt != null){
						mapView.getAnimatedDraggingThread().startMoving(pt.lat, pt.lon, 
								mapView.getFloatZoom(), true);
					}
				}
				calculateDistance();
				return true;
			}
		});
	}

	protected void saveGpx(final MapActivity activity) {
		Builder b = new AlertDialog.Builder(activity);
		final File dir = app.getAppPath(IndexConstants.GPX_INDEX_DIR);
		LinearLayout ll = new LinearLayout(activity);
		ll.setOrientation(LinearLayout.VERTICAL);
		ll.setPadding(5, 5, 5, 5);
		final TextView tv = new TextView(activity);
		tv.setText("");
		tv.setTextColor(Color.RED);
		ll.addView(tv);
		final EditText editText = new EditText(activity);
		editText.setHint(R.string.gpx_file_name);
		editText.addTextChangedListener(new TextWatcher() {
			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}
			
			@Override
			public void afterTextChanged(Editable s) {
				boolean e = false;
				try {
					e = new File(dir, s.toString()).exists() || new File(dir, s.toString() +".gpx").exists();
				} catch (Exception e1) {
				}
				if (e) {
					tv.setText(R.string.file_with_name_already_exists);
				} else {
					tv.setText("");
				}
			}
		});
		ll.addView(editText);
		b.setView(ll);
		b.setPositiveButton(R.string.default_buttons_save, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				String newName = editText.getText().toString();
				if(!newName.endsWith(".gpx")){
					newName += ".gpx";
				}
				saveGpx(activity, newName);
			}
		});
		b.setNegativeButton(R.string.default_buttons_cancel, null);
		b.show();
	}
	
	private void saveGpx(final MapActivity activity, final String fileNameSave) {
		final AsyncTask<Void, Void, String> exportTask = new AsyncTask<Void, Void, String>() {
			private ProgressDialog dlg;
			private File toSave;

			@Override
			protected String doInBackground(Void... params) {
				toSave = new File(app.getAppPath(IndexConstants.GPX_INDEX_DIR), fileNameSave);
				GPXFile gpx;
				boolean saveTrackToRte = false;
				if(originalGPX != null) {
					gpx = originalGPX;
					saveTrackToRte = originalGPX.routes.size() > 0 && originalGPX.tracks.size() == 0;
					gpx.tracks.clear();
					gpx.routes.clear();
					gpx.points.clear();
				} else {
					gpx = new GPXFile(); 
				}
				for(int i = 0; i<measurementPoints.size(); i++) {
					LinkedList<WptPt> lt = measurementPoints.get(i);
					if(lt.size() == 1) {
						gpx.points.add(lt.getFirst());
					} else if(lt.size() > 1) {
						if(saveTrackToRte) {
							Route rt = new Route();
							gpx.routes.add(rt);
							rt.points.addAll(lt);
						} else {
							if(gpx.tracks.size() == 0) {
								gpx.tracks.add(new Track());
							}
							Track ts = gpx.tracks.get(gpx.tracks.size() - 1);
							TrkSegment sg = new TrkSegment();
							ts.segments.add(sg);
							sg.points.addAll(lt);
						}
					}
				}
				return GPXUtilities.writeGpxFile(toSave, gpx, app);
			}

			@Override
			protected void onPreExecute() {
				dlg = new ProgressDialog(activity);
				dlg.setMessage(app.getString(R.string.saving_gpx_tracks));
				dlg.show();
			};

			@Override
			protected void onPostExecute(String warning) {
				if (warning == null) {
					AccessibleToast.makeText(activity,
							MessageFormat.format(app.getString(R.string.gpx_saved_sucessfully), toSave.getAbsolutePath()),
							Toast.LENGTH_LONG).show();
				} else {
					AccessibleToast.makeText(activity, warning, Toast.LENGTH_LONG).show();
				}
				if(dlg != null && dlg.isShowing()) {
					dlg.dismiss();
				}
			};
		};
		exportTask.execute(new Void[0]);
		
		
	}
	private void startEditingHelp(MapActivity ctx) {
		final CommonPreference<Boolean> pref = app.getSettings().registerBooleanPreference("show_measurement_help_first_time", true);
		pref.makeGlobal();
		if(pref.get()) {
			Builder builder = new AlertDialog.Builder(ctx);
			builder.setMessage(R.string.use_distance_measurement_help);
			builder.setNegativeButton(R.string.default_buttons_do_not_show_again, new OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					pref.set(false);
				}
			});
			builder.setPositiveButton(R.string.default_buttons_ok, null);
			
			builder.show();
		}
		
	}
	
	private TextInfoWidget createDistanceControl(final MapActivity activity, Paint paintText, Paint paintSubText) {
		final TextInfoWidget distanceControl = new TextInfoWidget(activity, 0, paintText, paintSubText);
		distanceControl.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showDialog(activity);
			}
		});
		distanceControl.setImageDrawable(app.getResources().getDrawable(R.drawable.widget_distance));
		return distanceControl;
	}

	private void calculateDistance() {
		float dist = 0;
		if (measurementPoints.size() == 0 && distanceMeasurementMode == 0 ) {
			distance = null;
		} else {
			for (int j = 0; j < measurementPoints.size(); j++) {
				List<WptPt> ls = measurementPoints.get(j);
				for (int i = 1; i < ls.size(); i++) {
					dist += MapUtils.getDistance(ls.get(i - 1).lat, ls.get(i - 1).lon, ls.get(i).lat, ls.get(i).lon);
				}
			}
			distance = OsmAndFormatter.getFormattedDistance(dist, app);
		}
		updateText();
	}


	public class DistanceCalculatorLayer extends OsmandMapLayer implements ContextMenuLayer.IContextMenuProvider {
		private OsmandMapTileView view;

		private Bitmap originIcon;
		private Bitmap destinationIcon;
		private Paint bitmapPaint;

		private Path path;

		private Paint paint;
		private Paint paint2;

		public DistanceCalculatorLayer() {
		}

		@Override
		public void initLayer(OsmandMapTileView view) {
			this.view = view;
			dm = new DisplayMetrics();
			WindowManager wmgr = (WindowManager) view.getContext().getSystemService(Context.WINDOW_SERVICE);
			wmgr.getDefaultDisplay().getMetrics(dm);
			originIcon = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_pin_origin);
			destinationIcon = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_pin_destination);
			bitmapPaint = new Paint();
			bitmapPaint.setDither(true);
			bitmapPaint.setAntiAlias(true);
			bitmapPaint.setFilterBitmap(true);
			path = new Path();
			
			int distanceColor = view.getResources().getColor(R.color.distance_color);
			paint = new Paint();
			paint.setStyle(Style.STROKE);
			paint.setStrokeWidth(7 * dm.density);
			paint.setAntiAlias(true);
			paint.setStrokeCap(Cap.ROUND);
			paint.setStrokeJoin(Join.ROUND);
			paint.setColor(distanceColor);
			
			paint2 = new Paint();
			paint2.setStyle(Style.FILL_AND_STROKE);
			paint2.setAntiAlias(true);
			paint2.setColor(distanceColor);
		}
		
		@Override
		public boolean onSingleTap(PointF point) {
			if(distanceMeasurementMode == 1) {
				LatLon l = view.getLatLonFromScreenPoint(point.x, point.y);
				if(measurementPoints.size() == 0) {
					measurementPoints.add(new LinkedList<GPXUtilities.WptPt>());
				}
				WptPt pt = new WptPt();
				pt.lat = l.getLatitude();
				pt.lon = l.getLongitude();
				measurementPoints.get(measurementPoints.size() - 1).add(pt);
				calculateDistance();
				view.refreshMap();
				updateText();
				return true;
			}
			return false;
		}
		
		@Override
		public boolean onLongPressEvent(PointF point) {
			if (distanceMeasurementMode == 1 && measurementPoints.size() > 0) {
				LinkedList<WptPt> lt = measurementPoints.get(measurementPoints.size() - 1);
				if (lt.size() > 0) {
					lt.removeLast();
				}
				calculateDistance();
				view.refreshMap();
				updateText();
				return true;
			}
			return false;
		}

		@Override
		public void onDraw(Canvas canvas, RectF latlonRect, RectF tilesRect, DrawSettings settings) {
			if (measurementPoints.size() > 0) {
				path.reset();
				int marginY = originIcon.getHeight();
				int marginX = originIcon.getWidth() / 2;
				for (int i = 0; i < measurementPoints.size(); i++) {
					Iterator<WptPt> it = measurementPoints.get(i).iterator();
					boolean first = true;
					while (it.hasNext()) {
						WptPt point = it.next();
						int locationX = view.getMapXForPoint(point.lon);
						int locationY = view.getMapYForPoint(point.lat);
						if (first) {
							path.moveTo(locationX, locationY);
							first = false;
						} else {
							path.lineTo(locationX, locationY);
						}
					}
				}
				canvas.drawPath(path, paint);
				for (int i = 0; i < measurementPoints.size(); i++) {
					Iterator<WptPt> it = measurementPoints.get(i).iterator();
					boolean first = true;
					while(it.hasNext()) {
						WptPt pt = it.next();
						if (view.isPointOnTheRotatedMap(pt.lat, pt.lon)) {
							int locationX = view.getMapXForPoint(pt.lon);
							int locationY = view.getMapYForPoint(pt.lat);
							
							if(first || !it.hasNext() || pt.desc != null) {
								canvas.rotate(-view.getRotate(), locationX, locationY);
								canvas.drawBitmap(distanceMeasurementMode == 1? originIcon : destinationIcon, 
										locationX - marginX, locationY - marginY, bitmapPaint);
								canvas.rotate(view.getRotate(), locationX, locationY);	
							} else {
								canvas.drawCircle(locationX, locationY, 10 * dm.density, paint2);
							}
						}
						first = false;
					}
				}
			}
		}

		@Override
		public void destroyLayer() {
		}

		@Override
		public boolean drawInScreenPixels() {
			return false;
		}

		@Override
		public void collectObjectsFromPoint(PointF point, List<Object> o) {
			getMPointsFromPoint(point, o);
		}
		
		public void getMPointsFromPoint(PointF point, List<? super WptPt> res) {
			int r = (int) (14 * dm.density);
			int rs = (int) (10 * dm.density);
			int ex = (int) point.x;
			int ey = (int) point.y;
			for (int i = 0; i < measurementPoints.size(); i++) {
				Iterator<WptPt> it = measurementPoints.get(i).iterator();
				boolean first = true;
				while (it.hasNext()) {
					WptPt pt = it.next();
					int x = view.getRotatedMapXForPoint(pt.lat, pt.lon);
					int y = view.getRotatedMapYForPoint(pt.lat, pt.lon);
					if (pt.desc != null || !it.hasNext() || first) {
						if (calculateBelongsBig(ex, ey, x, y, r)) {
							res.add(pt);
						}
					} else {
						if (calculateBelongsSmall(ex, ey, x, y, rs)) {
							res.add(pt);
						}
					}
					first = false;
				}
			}
		}
		
		private boolean calculateBelongsBig(int ex, int ey, int objx, int objy, int radius) {
			return Math.abs(objx - ex) <= radius && (ey - objy) <= radius / 2 && (objy - ey) <= 3 * radius ;
		}
		
		private boolean calculateBelongsSmall(int ex, int ey, int objx, int objy, int radius) {
			return Math.abs(objx - ex) <= radius && Math.abs(ey - objy) <= radius ;
		}

		@Override
		public LatLon getObjectLocation(Object o) {
			if (o instanceof WptPt) {
				return new LatLon(((WptPt) o).lat, ((WptPt) o).lon);
			}
			return null;
		}
		
		@Override
		public void populateObjectContextMenu(Object o, ContextMenuAdapter adapter) {
			if(o instanceof WptPt) {
				final WptPt p = (WptPt) o;
				OnContextMenuClick listener = new OnContextMenuClick() {
					
					@Override
					public void onContextMenuClick(int itemId, int pos, boolean isChecked, DialogInterface dialog) {
						if (itemId == R.string.delete_point) {
							for (int i = 0; i < measurementPoints.size(); i++) {
								Iterator<WptPt> it = measurementPoints.get(i).iterator();
								while (it.hasNext()) {
									if (it.next() == p) {
										it.remove();
									}
								}
							}
							calculateDistance();
						}
					}
				};
				adapter.item(R.string.delete_point).icons(R.drawable.ic_action_delete_dark, 
						R.drawable.ic_action_delete_light).listen(listener).reg();
			}
		}

		@Override
		public String getObjectDescription(Object o) {
			if(o instanceof WptPt) {
				String desc = getObjectName(o); 
				List<String> l = new ArrayList<String>();
				if(!Double.isNaN(((WptPt) o).ele)) {
					l.add(app.getString(R.string.plugin_distance_point_ele) + " "+ OsmAndFormatter.getFormattedDistance((float) ((WptPt) o).ele, app)); 
				}
				if(!Double.isNaN(((WptPt) o).speed)) {
					l.add(app.getString(R.string.plugin_distance_point_speed) + " "+ OsmAndFormatter.getFormattedSpeed((float) ((WptPt) o).speed, app)); 
				}
				if(!Double.isNaN(((WptPt) o).hdop)) {
					l.add(app.getString(R.string.plugin_distance_point_hdop) + " "+ OsmAndFormatter.getFormattedDistance((float) ((WptPt) o).hdop, app)); 
				}
				if(((WptPt) o).time != 0) {
					Date date = new Date(((WptPt) o).time);
					java.text.DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(app);
					java.text.DateFormat timeFormat = android.text.format.DateFormat.getTimeFormat(app);
					l.add(app.getString(R.string.plugin_distance_point_time) + " "+ dateFormat.format(date) + " " + timeFormat.format(date)); 
				}
				return desc + " " + l;
			}
			return null;
		}

		@Override
		public String getObjectName(Object o) {
			if(o instanceof WptPt) {
				if(((WptPt) o).desc == null) {
					return app.getString(R.string.plugin_distance_point); 
				}
				return ((WptPt) o).desc;
			}
			return null;
		}

	}
}
