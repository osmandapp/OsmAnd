package net.osmand.plus.distancecalculator;

import gnu.trove.list.array.TIntArrayList;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.osmand.CallbackWithObject;
import net.osmand.IndexConstants;
import net.osmand.access.AccessibleToast;
import net.osmand.data.LatLon;
import net.osmand.data.RotatedTileBox;
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
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Join;
import android.graphics.Paint.Style;
import android.graphics.drawable.Drawable;
import android.graphics.Path;
import android.graphics.PointF;
import android.os.AsyncTask;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.MotionEvent;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.FrameLayout.LayoutParams;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;  

public class DistanceCalculatorPlugin extends OsmandPlugin {
	private static final String ID = "osmand.distance";
	private OsmandApplication app;
	private DistanceCalculatorLayer distanceCalculatorLayer;
	private ContextMenuLayer contextMenuLayer;
	private TextInfoWidget distanceControl;
	private MapActivity mapActivity;
	private DistanceCalculatorPlugin distanceCalculatorPlugin;
	
	private List<LinkedList<WptPt>> measurementPoints = new ArrayList<LinkedList<WptPt>>();
	public int selectedPointIndex = -1;
	public int selectedSubtrackIndex = -1;
	public int insertionPointIndex = -1;
	public int insertionSubtrackIndex = -1;
	public int insertionPointIndices[] = {-1, -1};
	private GPXFile originalGPX;
	private String distance = null;
	private String subTrackDistance = null;
    private TextView currentValue; 
    private int min = 0;
    private int max = 0;
    private int current = 0;
    private String prompt = null;
    private String warning = null;

	private int distanceMeasurementMode = 0; 
	private boolean displayNotesFlag = false;

	public DistanceCalculatorPlugin(OsmandApplication app) {
		this.app = app;
		distanceCalculatorPlugin = this;
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
		contextMenuLayer = activity.getMapLayers().getContextMenuLayer();
		// remove any existing layer
		if(distanceCalculatorLayer != null) {
			activity.getMapView().removeLayer(distanceCalculatorLayer);
		}
		distanceCalculatorLayer = new DistanceCalculatorLayer(activity);
		int contextMenuLayerNumber = 0;
		for (contextMenuLayerNumber = 0; contextMenuLayerNumber <= activity.getMapView().getLayers().size(); contextMenuLayerNumber++) {
			if (activity.getMapView().getLayers().get(contextMenuLayerNumber) instanceof ContextMenuLayer) {
				break;
			}
		}
		activity.getMapView().addLayer(distanceCalculatorLayer, (float)contextMenuLayerNumber + 0.5f);	//place above ContextMenuLayer
		
		registerWidget(activity);
		mapActivity = activity;
	}
	
	private void registerWidget(MapActivity activity) {
		MapInfoLayer mapInfoLayer = activity.getMapLayers().getMapInfoLayer();
		if (mapInfoLayer != null ) {
			distanceControl = createDistanceControl(activity, mapInfoLayer.getPaintText(), mapInfoLayer.getPaintSubText());
			mapInfoLayer.getMapInfoControls().registerSideWidget(distanceControl,
					R.drawable.widget_distance, R.string.map_widget_distancemeasurement, "distance.measurement", false, 21);
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
			list.add(R.string.measurement_point_menu_list_notes);
			list.add(R.string.measurement_point_menu_show_notes);
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
					if(measurementPoints.size() > 0){
						WptPt temp = measurementPoints.get(measurementPoints.size() - 1).getLast();	//subtrack starts at end of previous segment
						measurementPoints.add(new LinkedList<GPXUtilities.WptPt>());
						measurementPoints.get(measurementPoints.size() - 1).add(0, temp);
						selectedSubtrackIndex = measurementPoints.size() - 1;
						selectedPointIndex = 0;
					}
				} else if (id == R.string.distance_measurement_clear_route) {
					measurementPoints.clear();
					selectedSubtrackIndex = - 1;
					selectedPointIndex = -1;
					calculateDistance();
					contextMenuLayer.setLocation(null, null);	//clear any open info box
					distanceCalculatorLayer.setLocation(null, null);	//clear any open info box
					activity.getMapView().refreshMap();
				} else if (id == R.string.distance_measurement_save_gpx) {
					saveGpx(activity);
				} else if (id == R.string.distance_measurement_load_gpx) {
					loadGpx(activity);
				} else if (id == R.string.measurement_point_menu_list_notes) {
					dialog.dismiss();
					showNotesListDialog(activity);
				} else if (id == R.string.measurement_point_menu_show_notes) {
					displayNotesFlag = (displayNotesFlag ? false:true);
					activity.getMapView().refreshMap();
				}else{
					activity.getMapView().refreshMap();
					updateText();
				}
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
								mapView.getZoom(), true);
					}
				}
				selectedSubtrackIndex = measurementPoints.size() - 1;
				selectedPointIndex = measurementPoints.get(measurementPoints.size() - 1).size() - 1;
				calculateDistance();
				return true;
			}
		});
	}

	protected void saveGpx(final MapActivity activity) {
		Builder b = new AlertDialog.Builder(activity);
		final File dir = app.getAppPath(IndexConstants.GPX_INDEX_DIR);
		if (dir == null || !dir.canRead()) {
			dir.mkdir();
		}

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
				for(int i = 0; i < measurementPoints.size(); i++) {
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
	
	private void startEditingHelp(MapActivity mapActivity) {
		final CommonPreference<Boolean> pref = app.getSettings().registerBooleanPreference("show_measurement_help_first_time", true);
		pref.makeGlobal();
		if(pref.get()) {
			Builder builder = new AlertDialog.Builder(mapActivity);
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
	
	private TextInfoWidget createDistanceControl( MapActivity activity, Paint paintText, Paint paintSubText) {
		final TextInfoWidget distanceControl = new TextInfoWidget(activity, 0, paintText, paintSubText);
		distanceControl.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showDialog(mapActivity);
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

	private void calculatePartialDistance(int lastSegment, int lastIndex) {
		float dist = 0;
		float subTrackDist = 0;
		double delta = 0;
		if (measurementPoints.size() == 0 && distanceMeasurementMode == 0 ) {
			distance = null;
			subTrackDistance = null;
		} else {
			for (int j = 0; j <= lastSegment; j++) {
				List<WptPt> ls = measurementPoints.get(j);
				int maxIndex = ls.size();
				if(j == lastSegment) maxIndex = lastIndex + 1;	//only measure to selected point
				for (int i = 1; i < maxIndex; i++) {
					delta = MapUtils.getDistance(ls.get(i - 1).lat, ls.get(i - 1).lon, ls.get(i).lat, ls.get(i).lon);
					dist += delta;
					if(j == lastSegment) subTrackDist += delta;
				}
			}
			distance = OsmAndFormatter.getFormattedDistance(dist, app);
			subTrackDistance = OsmAndFormatter.getFormattedDistance(subTrackDist, app);
		}
		updateText();
	}

	public void showNotesListDialog(MapActivity activity){
		final List<WptPt> notePoints = new ArrayList<WptPt>();
		final List<String> entries = new ArrayList<String>();
		final List<Integer> pointIndex = new ArrayList<Integer>();
		final List<Integer> subtrackIndex = new ArrayList<Integer>();
		if(measurementPoints.size() > 0) {
			int points = measurementPoints.size();
			//find points that have notes and add info to list
			for (int i = 0; i < points;i++){
				Iterator<WptPt> it = measurementPoints.get(i).iterator();
				for (int j = 0; j < measurementPoints.get(i).size(); j++){
					WptPt pt = it.next();
					if (pt.desc != null){
						notePoints.add(pt);
						pointIndex.add(j);
						subtrackIndex.add(i);
						entries.add(pt.desc);
					}
				}
			}
			
			if(notePoints.size() > 0) {
			    final AlertDialog.Builder bld1 = new AlertDialog.Builder(activity); // change to new AlertDialog.Builder(this)?
			    bld1.setTitle(R.string.notes_list_title);
			    bld1.setNegativeButton(R.string.default_buttons_cancel, new DialogInterface.OnClickListener(){
			    	
				@Override
					public void onClick(DialogInterface dlg, int which) {
					dlg.dismiss();
					}
				});

			    bld1.setItems(entries.toArray(new String[entries.size()]), new DialogInterface.OnClickListener(){
		    		
				@Override
					public void onClick(DialogInterface dlg, int which) {
						dlg.dismiss();
						final double lat = notePoints.get(which).lat;
						final double lon = notePoints.get(which).lon;
						mapActivity.getMapView().getAnimatedDraggingThread().startMoving(lat, lon,
								mapActivity.getMapView().getCurrentRotatedTileBox().getZoom(), true);
						selectedPointIndex = pointIndex.get(which);
						selectedSubtrackIndex = subtrackIndex.get(which);
						calculatePartialDistance(selectedSubtrackIndex, selectedPointIndex);
						distanceCalculatorLayer.setLocation(new LatLon(lat,lon), distanceCalculatorLayer.setDescription(true, null));
						mapActivity.getMapView().refreshMap();
					}
				});
			    bld1.show();
			}else{
				AccessibleToast.makeText(mapActivity,
						mapActivity.getMapView().getResources().getString(R.string.measurement_point_no_notes_warning), Toast.LENGTH_LONG).show();
			}			
		}
	}

	public DistanceCalculatorLayer getDistanceCalculatorLayer(){
		return distanceCalculatorLayer;
	}
	
	public class DistanceCalculatorLayer extends OsmandMapLayer{
		private OsmandMapTileView view;
		private Bitmap originIcon;
		private Bitmap destinationIcon;
		private Paint bitmapPaint;
		private Path path;
		private Paint paint;
		private Paint paint2;
		private TextView textView;
		private ImageView closeButton;
		private Drawable boxLeg;

		public final int defaultMeasurementPointDisplayRadius = 17;
		public final int maxMeasurementPointSelectionRadius = 29;
		public final int minMeasurementPointSelectionRadius = 7;
		private boolean scrollingFlag = false;		//For measurement point dragging
		private boolean showDragAnimation = false;		//For measurement point dragging
		private boolean insertFlag = false;
		private Rect textPadding;
		public int distanceColor = 0;
		public int inactiveSubtrackColor = 0;
		public int subtrackRemainderColor = 0;
		public int noteHighlightColor = 0;
		public int dragColor = 0;
		private PointF movingPoint = null;
		private final int DEFAULT_TEXT_SIZE = 15;
		private int TEXTBOX_SIZE = 1;
		private static final String KEY_DESCRIPTION = "distance_calculator_description";
		private static final String KEY_SELECTED_SUBTRACK_INDEX = "distance_calculator_selected_subtrack_index";
		private static final String KEY_SELECTED_POINT_INDEX = "distance_calculator_selected_point_index";
		public String description = null;
	    private SeekBar seek;

		public DistanceCalculatorLayer(MapActivity activity){
			if(activity.getLastNonConfigurationInstanceByKey(KEY_SELECTED_SUBTRACK_INDEX) != null) {
				selectedSubtrackIndex = (Integer) activity.getLastNonConfigurationInstanceByKey(KEY_SELECTED_SUBTRACK_INDEX);
				description = (String) activity.getLastNonConfigurationInstanceByKey(KEY_DESCRIPTION);
				selectedPointIndex = (Integer) activity.getLastNonConfigurationInstanceByKey(KEY_SELECTED_POINT_INDEX);
			}
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
			distanceColor = view.getResources().getColor(R.color.distance_color);
			dragColor = view.getResources().getColor(R.color.color_distance_drag);
			inactiveSubtrackColor = view.getResources().getColor(R.color.color_distance_inactive_subtrack);
			subtrackRemainderColor = view.getResources().getColor(R.color.color_distance_remainder);
			noteHighlightColor = view.getResources().getColor(R.color.color_note_highlight);
			paint = new Paint();
			paint.setStyle(Style.STROKE);
			paint.setStrokeWidth(7 * view.getDensity());
			paint.setAntiAlias(true);
			paint.setStrokeCap(Cap.ROUND);
			paint.setStrokeJoin(Join.ROUND);
			paint2 = new Paint();
			paint2.setStyle(Style.FILL_AND_STROKE);
			paint2.setAntiAlias(true);
			textView = new TextView(view.getContext());
			LayoutParams lp = new LayoutParams(contextMenuLayer.BASE_TEXT_SIZE, LayoutParams.WRAP_CONTENT);
			textView.setLayoutParams(lp);
			float factor = view.getSettings().MAP_ZOOM_SCALE_BY_DENSITY.get() + 0.25f;
			if (factor < 1.0f) factor = 1.0f;
			TEXTBOX_SIZE = (int)(contextMenuLayer.BASE_TEXT_SIZE * factor * view.getDensity());
			textView.setTextColor(Color.argb(255, 0, 0, 0));
			textView.setMinLines(1);
			textView.setGravity(Gravity.CENTER_HORIZONTAL);		
			textView.setClickable(true);			
			textView.setBackgroundDrawable(view.getResources().getDrawable(R.drawable.box_free));
			textPadding = new Rect();
			textView.getBackground().getPadding(textPadding);
			textView.setTextSize((int)(DEFAULT_TEXT_SIZE * factor));
			closeButton = new ImageView(view.getContext());
			lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			closeButton.setLayoutParams(lp);
			closeButton.setImageDrawable(view.getResources().getDrawable(R.drawable.headliner_close));
			closeButton.setClickable(true);			
			boxLeg = view.getResources().getDrawable(R.drawable.box_leg);
			boxLeg.setBounds(0, 0, boxLeg.getMinimumWidth(), boxLeg.getMinimumHeight());
			
			movingPoint = new PointF(0,0);
			if(selectedSubtrackIndex >= 0 && selectedPointIndex >= 0){
				LatLon l = new LatLon(measurementPoints.get(selectedSubtrackIndex).get(selectedPointIndex).lat,
						measurementPoints.get(selectedSubtrackIndex).get(selectedPointIndex).lon);
				setLocation(l, description);
				view.refreshMap();
				calculateDistance();
				updateText();				
			}
		}
		
		public void updateTextSize(){
			float factor = view.getSettings().MAP_ZOOM_SCALE_BY_DENSITY.get() + 0.25f;
			if (factor < 1.0f) factor = 1.0f;
			TEXTBOX_SIZE = (int)(contextMenuLayer.BASE_TEXT_SIZE * factor * view.getDensity());
			textView.setTextSize((int)(DEFAULT_TEXT_SIZE * factor));
			layoutText();
			if(contextMenuLayer != null){
				contextMenuLayer.updateTextSize();
			}
		}
		
		@Override
		public boolean onSingleTap(PointF point, RotatedTileBox tileBox) {
			description = "";
			LatLon l = tileBox.getLatLonFromPixel(point.x, point.y);
			int textBoxPressed = pressedInTextView(point.x, point.y, tileBox);
			if(textBoxPressed == 2){
				setLocation(null, null);
				view.refreshMap();
				return true;
			}else if (textBoxPressed == 1){
				if(distanceMeasurementMode == 1){
					createMeasurementPointMenuDialog(mapActivity);
				}else{
					isMeasurementPointSelected(point, tileBox);	//set selected point indices
					double lat = measurementPoints.get(selectedSubtrackIndex).get(selectedPointIndex).lat;
					double lon = measurementPoints.get(selectedSubtrackIndex).get(selectedPointIndex).lon;
					mapActivity.getMapActions().contextMenuPoint(lat, lon);
				}
				return true;										
			}else{
				int res = contextMenuLayer.pressedInTextView(tileBox, point.x, point.y);
				if(res > 0) return false;	//pass through a click on a text box on the context menu layer
				if(distanceMeasurementMode == 1) {
					if(measurementPoints.size() == 0) {
						measurementPoints.add(new LinkedList<GPXUtilities.WptPt>());
						selectedSubtrackIndex = -1;
						selectedPointIndex = -1;
					}
					if(!isMeasurementPointSelected(point, tileBox))
					{
						WptPt pt = new WptPt();
						pt.lat = l.getLatitude();
						pt.lon = l.getLongitude();
						if(insertFlag){
							measurementPoints.get(insertionSubtrackIndex).add(insertionPointIndex, pt);
							selectedSubtrackIndex = insertionSubtrackIndex;
							selectedPointIndex = insertionPointIndex;
						}else{
							measurementPoints.get(measurementPoints.size() - 1).add(pt);
							selectedSubtrackIndex = measurementPoints.size() - 1;
							selectedPointIndex = measurementPoints.get(measurementPoints.size() - 1).size() - 1;
						}				
						insertFlag = false;
					}
				}
				if(measurementPoints.size() == 0 || !isMeasurementPointSelected(point, tileBox)) {
					distance = null;
					description = null;
				}else{
					calculatePartialDistance(selectedSubtrackIndex, selectedPointIndex);
					description = setDescription(true, null);						
				}
			}
			setLocation(l, description);
			view.refreshMap();
			calculateDistance();
			updateText();
			return true;
		}

		public String setDescription(boolean withNote, LatLon latLon){
			String description = "";
			if(latLon != null){
				description = view.getContext().getString(R.string.point_on_map, 
						latLon.getLatitude(), latLon.getLongitude()) + "\n";				
			}
			if(selectedSubtrackIndex > 0){
				description = description + view.getContext().getString(R.string.measurement_track_distance) + distance;
				if(selectedPointIndex > 0)
					description = description + "\n" + view.getContext().getString(R.string.measurement_subTrack_distance) + subTrackDistance;
			}else{
				description = description + distance;
			}
			if(withNote){
				String note = measurementPoints.get(selectedSubtrackIndex).get(selectedPointIndex).desc;
				if(note != "" && note != null) description = description + "\nNote: " + note;
			}
			return description;
		}
		
		@Override
		public boolean onLongPressEvent(PointF point, RotatedTileBox tileBox) {
			int textBoxReturnCode = pressedInTextView(point.x, point.y, tileBox);
			if(textBoxReturnCode == 1){
				setLocation(null, null);	//remove textbox
				view.refreshMap();
				return true;					
			}
			if (measurementPoints.size() < 0) return false;	//pass event through to context menu layer
			if(isMeasurementPointSelected(point, tileBox)){
				if (distanceMeasurementMode == 1){
					createMeasurementPointMenuDialog(mapActivity);
				}else{
					LatLon l = tileBox.getLatLonFromPixel(point.x, point.y);
					mapActivity.getMapActions().contextMenuPoint(l.getLatitude(), l.getLongitude());					
				}
				return true;
			}
			return false;
		}
		
		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY, RotatedTileBox tileBox) {
			if (distanceMeasurementMode == 0) return false;		//Check if in measurement mode
			if(scrollingFlag){
				movingPoint.set(e2.getX(), e2.getY());
				view.refreshMap();	//drag animation
				return true;	//delay activity until scrolling has finished
			}
			PointF point = new PointF(e1.getX(), e1.getY());
			if(isMeasurementPointSelected(point, tileBox)){
				scrollingFlag = true;	//must remain true until next touch event
				showDragAnimation = true;
				movingPoint.set(point);
				return true;
			}
			return false;
		}

		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY, RotatedTileBox tileBox) {
			if (distanceMeasurementMode == 0) return false;		//Check if in measurement mode
			if(scrollingFlag) return true;	//block fling while dragging a point. Note scroll event occurs before fling event
			return false;
		}
		
		@Override
		public boolean onTouchEvent(MotionEvent event, RotatedTileBox tileBox) {
			if (distanceMeasurementMode == 0) return false;		//Check if in measurement mode
			if (event.getAction() == MotionEvent.ACTION_DOWN) {	//must clear at start of new event, not end of last event to ensure fling is blocked
				scrollingFlag = false;
				movingPoint.set(event.getX(), event.getY());
			}
			
			if (event.getAction() == MotionEvent.ACTION_UP) {	//Support for dragging measurement point
				if(scrollingFlag){
					if(selectedPointIndex >= 0){	//move selected point to new location
						WptPt pt = new WptPt();
						pt.lat = tileBox.getLatLonFromPixel(event.getX(),event.getY()).getLatitude();
						pt.lon = tileBox.getLatLonFromPixel(event.getX(),event.getY()).getLongitude();
						String s = measurementPoints.get(selectedSubtrackIndex).get(selectedPointIndex).desc;
						measurementPoints.get(selectedSubtrackIndex).set(selectedPointIndex, pt);
						if(s != null && s.length() > 0) measurementPoints.get(selectedSubtrackIndex).get(selectedPointIndex).desc = s.toString();
						description = null;
						calculatePartialDistance(selectedSubtrackIndex, selectedPointIndex);
						description = setDescription(true, null);
						LatLon l = tileBox.getLatLonFromPixel(event.getX(),event.getY());						
						showDragAnimation = false;
						setLocation(l, description);
						view.refreshMap();
						calculateDistance();
						updateText();
					}
				}
			}
			return false;
		}

		@Override
		public void onPrepareBufferImage(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
			if (measurementPoints.size() > 0) {
				path.reset();
				int marginY = originIcon.getHeight();
				int marginX = originIcon.getWidth() / 2;
				int lastSegment = selectedSubtrackIndex;
				int lastPoint = selectedPointIndex;
				int points =0;
				boolean showLine = false;
				float factor = view.getSettings().MAP_ZOOM_SCALE_BY_DENSITY.get() + 0.3f;
				if(factor < 1) factor = 1;
				if(showDragAnimation && scrollingFlag && measurementPoints.get(0).size() > 0){	//provide rubber-banding if a drag is occurring
					paint.setStrokeWidth(4 * view.getDensity() * factor);
					paint.setColor(dragColor);
					path.reset();
					int x = 0;
					int y = 0;
					int pointIndex = 0;
					int subtrackIndex = 0;
					if(selectedPointIndex > 0){
						pointIndex = selectedPointIndex - 1;
						subtrackIndex = selectedSubtrackIndex;
						showLine = true;
					}else{
						if(selectedSubtrackIndex > 0){
							subtrackIndex = selectedSubtrackIndex - 1;
							pointIndex = measurementPoints.get(subtrackIndex).size() - 1;
							showLine = true;
						}else{
							subtrackIndex = 0;
							pointIndex = 0;
						}
					}
					x = tileBox.getPixXFromLonNoRot(measurementPoints.get(subtrackIndex).get(pointIndex).lon);
					y = tileBox.getPixYFromLatNoRot(measurementPoints.get(subtrackIndex).get(pointIndex).lat);						
					if(!showLine){
						path.moveTo(movingPoint.x, movingPoint.y);
					}else{
						path.moveTo(x, y);
						path.lineTo(movingPoint.x, movingPoint.y);
					}
					showLine = false;
					if(selectedPointIndex < measurementPoints.get(selectedSubtrackIndex).size() - 1){
						pointIndex = selectedPointIndex + 1;
						subtrackIndex = selectedSubtrackIndex;
						showLine = true;
					}else{
						if(selectedSubtrackIndex < measurementPoints.size() - 1){
							subtrackIndex = selectedSubtrackIndex + 1;
							if(measurementPoints.get(subtrackIndex).size() > 1){	//is there only one point in the subtrack?
								pointIndex = 0;
								showLine = true;
							}else{
								subtrackIndex = selectedSubtrackIndex;
								pointIndex = selectedPointIndex;								
							}
						}else{
							subtrackIndex = selectedSubtrackIndex;
							pointIndex = selectedPointIndex;
						}
					}
					if(showLine){
						x = tileBox.getPixXFromLonNoRot(measurementPoints.get(subtrackIndex).get(pointIndex).lon);
						y = tileBox.getPixYFromLatNoRot(measurementPoints.get(subtrackIndex).get(pointIndex).lat);						
						path.lineTo(x, y);
					}
					canvas.drawPath(path, paint);
					path.reset();
				}
				if(lastPoint < 0) lastSegment = measurementPoints.size() - 1;	//adjust for partial measurements
				for (int i = 0; i < measurementPoints.size(); i++) {	//for all gpx subtracks
					if(i != selectedSubtrackIndex){
						paint.setStrokeWidth(4 * view.getDensity() * factor);
						paint.setColor(inactiveSubtrackColor);
						paint2.setColor(inactiveSubtrackColor);
					}else{
						paint.setStrokeWidth(7 * view.getDensity() * factor);
						paint.setColor(distanceColor);
						paint2.setColor(distanceColor);
					}
					Iterator<WptPt> it = measurementPoints.get(i).iterator();
					points = measurementPoints.get(i).size() - 1;
					for (int j = 0; j <= points; j++){
						WptPt point = it.next();
						int locationX = tileBox.getPixXFromLonNoRot(point.lon);
						int locationY = tileBox.getPixYFromLatNoRot(point.lat);
						if (j == 0 ) {
							path.moveTo(locationX, locationY);
						} else {
							path.lineTo(locationX, locationY);
						}
						if(i == lastSegment && j == lastPoint) {
							canvas.drawPath(path, paint);
							path.reset();
							path.moveTo(locationX, locationY);
							paint.setStrokeWidth(4 * view.getDensity() * factor);
							paint.setColor(subtrackRemainderColor);
					}
				}
				canvas.drawPath(path, paint);
				path.reset();
			}
			if(lastPoint < 0){
				lastSegment = measurementPoints.size() - 1;	//adjust for partial measurements
				if(measurementPoints.get(lastSegment).size() < 1) lastSegment --;	//allow for start of sub-tracks
				lastPoint = measurementPoints.get(lastSegment).size() - 1;
			}
			for (int i = 0; i < measurementPoints.size(); i++) {
				if(i != selectedSubtrackIndex){
					paint.setColor(inactiveSubtrackColor);
					paint2.setColor(inactiveSubtrackColor);
				}else{
					paint.setColor(distanceColor);
					paint2.setColor(distanceColor);
				}
					Iterator<WptPt> it = measurementPoints.get(i).iterator();
					points = measurementPoints.get(i).size() - 1;
					int temp = 0;
					boolean highlightNote = false;
					for (int j = 0; j <= points; j++){
						temp = paint2.getColor();
						WptPt pt = it.next();
						if (tileBox.containsLatLon(pt.lat, pt.lon)) {
							highlightNote = displayNotesFlag && measurementPoints.get(i).get(j).desc != null;
							int locationX = tileBox.getPixXFromLonNoRot(pt.lon);
							int locationY = tileBox.getPixYFromLatNoRot(pt.lat);						
							if(highlightNote){
								paint2.setColor(noteHighlightColor);
							}
							if(( j == 0 || j == points) && (i == selectedSubtrackIndex )) {
										canvas.rotate(-view.getRotate(), locationX, locationY);
								if(highlightNote){
									paint2.setColor(noteHighlightColor);
									canvas.drawCircle(locationX, locationY, 10 * tileBox.getDensity() * factor, paint2);
								}
								if( j == 0){
									canvas.drawBitmap(originIcon, locationX - marginX, locationY - marginY, bitmapPaint);
								}else{
									canvas.drawBitmap(destinationIcon, locationX - marginX, locationY - marginY, bitmapPaint);									
								}
								canvas.rotate(view.getRotate(), locationX, locationY);	
							} else {
								canvas.drawCircle(locationX, locationY, 10 * tileBox.getDensity() * factor, paint2);
							}
							paint2.setColor(temp);
						}
					}
				}
			}
			
			if (textView.getText().length() > 0) {
				int x = tileBox.getPixXFromLonNoRot(measurementPoints.get(selectedSubtrackIndex).get(selectedPointIndex).lon);
				int y = tileBox.getPixYFromLatNoRot(measurementPoints.get(selectedSubtrackIndex).get(selectedPointIndex).lat);						
				
				int tx = x - boxLeg.getMinimumWidth() / 2;
				int ty = y - boxLeg.getMinimumHeight() + contextMenuLayer.SHADOW_OF_LEG;
				canvas.translate(tx, ty);
				boxLeg.draw(canvas);
				canvas.translate(-tx, -ty);
			
				canvas.translate(x - textView.getWidth() / 2, ty - textView.getBottom() + textPadding.bottom - textPadding.top);
				int c = textView.getLineCount();
				
				textView.draw(canvas);
				canvas.translate(textView.getWidth() - closeButton.getWidth(), contextMenuLayer.CLOSE_BTN / 2);
				closeButton.draw(canvas);
				if (c == 0) {
					// special case relayout after on draw method
					layoutText();
					view.refreshMap();
				}
			}
		}
		
		@Override
		public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
			
		}

		@Override
		public void destroyLayer() {
		}

		@Override
		public boolean drawInScreenPixels() {
			return false;
		}

		/**
		 * Method to determine if a measurement track point exists within the 
		 * defined selection radius at the screen location tapped.
		 */
		public boolean isMeasurementPointSelected(PointF point, RotatedTileBox tileBox){	//test if point on map is a point in measurement set
			int locationX = 0;
			int locationY = 0;
			int size = 0;
			int measurementPointSelectionRadius = view.getSettings().MAP_POINT_SELECTION_RADIUS.get();
			if(measurementPoints.size() <= 0) return false;
			for (int j = 0; j < measurementPoints.size(); j++){			
				size = measurementPoints.get(j).size();
				if(size > 0){
					for (int i = 0;i < size; i++){
						locationX = tileBox.getPixXFromLonNoRot(measurementPoints.get(j).get(i).lon);
						locationY = tileBox.getPixYFromLatNoRot(measurementPoints.get(j).get(i).lat);
						if(Math.abs(locationX - point.x) < measurementPointSelectionRadius &&
								Math.abs(locationY - point.y) < measurementPointSelectionRadius){
							selectedSubtrackIndex = j;	//segment index
							selectedPointIndex = i;	//point index
							return true;
						}
					}
				}
			}
			return false;
		}
					
		public void editPointNote(CharSequence s){
			if(selectedPointIndex >= 0){
				measurementPoints.get(selectedSubtrackIndex).get(selectedPointIndex).desc = s.toString();
			}
		}
			
		public void setLocation(LatLon loc, String description){
			if(loc != null){
				textView.setText(description);
			} else {
				textView.setText(null);
			}
			layoutText();
		}

		public void layoutText() {
			Rect padding = new Rect();
			if (textView.getLineCount() > 0) {
				textView.getBackground().getPadding(padding);
			}
			int w = TEXTBOX_SIZE;
			int h = (int) ((textView.getPaint().getTextSize() * 1.3f) * textView.getLineCount());
			
			textView.layout(0, -padding.bottom, w, h + padding.top);
			int minw = closeButton.getDrawable().getMinimumWidth();
			int minh = closeButton.getDrawable().getMinimumHeight();
			closeButton.layout(0, 0, minw, minh);
		}	

		public int pressedInTextView(float px, float py, RotatedTileBox tileBox) {
			if(selectedPointIndex >= 0){
				double lat = measurementPoints.get(selectedSubtrackIndex).get(selectedPointIndex).lat;
				double lon = measurementPoints.get(selectedSubtrackIndex).get(selectedPointIndex).lon;
				Rect bs = textView.getBackground().getBounds();
				Rect closes = closeButton.getDrawable().getBounds();
				int x = (int) (px - tileBox.getPixXFromLonNoRot(lon));
				int y = (int) (py - tileBox.getPixYFromLatNoRot(lat));
				x += bs.width() / 2;
				y += bs.height() + boxLeg.getMinimumHeight() - contextMenuLayer.SHADOW_OF_LEG;
				int localSize = contextMenuLayer.CLOSE_BTN * 3 / 2;
				int dclosex = x - bs.width() + closes.width();
				int dclosey = y - closes.height() / 2;
				if(closes.intersects(dclosex - localSize, dclosey - localSize, dclosex + localSize, dclosey + localSize)) {
					return 2;
				} else if (bs.contains(x, y)) {
					return 1;
				}
			}
			return 0;
	   	}

	    /**
		 * Menu items to be provided for the menu opened when
		 * the a measurement point or info box is tapped.
		 */
		final int[] pointMenuActions = new int[]{
				R.string.delete_point,
				R.string.measurement_point_menu_insert_point,
				R.string.measurement_point_menu_insert_note,
				R.string.measurement_point_menu_show_notes,
				R.string.measurement_point_menu_list_notes,
				R.string.measurement_point_menu_show_latlon,
				R.string.measurement_point_menu_edit_point_selection_radius
		};

		/**
		 * Method to open a menu when a measurement point or information text box is long pressed.
		 */
		void createMeasurementPointMenuDialog( MapActivity activity) {
	        // Menu opened when a measurement point or information text box is tapped	    	
	    	final Builder builder = new AlertDialog.Builder(mapActivity);
	    	List<String> actions = new ArrayList<String>();
	    	int actionsToUse = 0;
	    	actionsToUse =  pointMenuActions.length;	//measurement point selected items;
	    	for(int j = 0; j < actionsToUse; j++){
	    		actions.add(mapActivity.getResources().getString(pointMenuActions[j]));
	    	}		
	    	builder.setItems(actions.toArray(new String[actions.size()]), new DialogInterface.OnClickListener(){
				@Override
				public void onClick(DialogInterface dialog, int which) {
					int standardId = 0;
					final WptPt p = measurementPoints.get(selectedSubtrackIndex).get(selectedPointIndex);
					standardId = pointMenuActions[which];
					if(standardId == R.string.measurement_point_menu_insert_note){
						addPointNote(mapActivity);
					}else if(standardId == R.string.delete_point){	//test if current point is to be removed
						for (int i = 0; i < measurementPoints.size(); i++) {
							Iterator<WptPt> it = measurementPoints.get(i).iterator();
							for(int j = 0; j < measurementPoints.get(i).size(); j++){
								if (it.next() == p) {
									it.remove();
									if(measurementPoints.get(i).size() == 0){	//check if subtrack is empty
										measurementPoints.remove(i);	//remove empty subtrack
									}
									selectedPointIndex = -1;	//reset the selected point indices
									selectedSubtrackIndex = -i;
									setLocation(null, null);	//clear any open info box
									view.refreshMap();
									break;
								}
							}
						}
						calculateDistance();
						view.refreshMap();
						updateText();
					}else if(standardId == R.string.measurement_point_menu_insert_point){	//insert extra measurement point?
						insertFlag = true;
						insertionSubtrackIndex = selectedSubtrackIndex;
						insertionPointIndex = selectedPointIndex;
					}else if(standardId == R.string.measurement_point_menu_show_latlon){	//show lat/lon in info box?
						double x = measurementPoints.get(selectedSubtrackIndex).get(selectedPointIndex).lat;
						double y = measurementPoints.get(selectedSubtrackIndex).get(selectedPointIndex).lon;
						LatLon pt = new LatLon(x, y);
						setLocation(pt, setDescription(true, pt));
						view.refreshMap();
					}else if(standardId == R.string.measurement_point_menu_edit_point_selection_radius){	//change measurement point selection area size
						editPointSelectionRadius();
					} else if (standardId == R.string.measurement_point_menu_show_notes) {
						displayNotesFlag = (displayNotesFlag ? false:true);
						view.refreshMap();
					} else if (standardId == R.string.measurement_point_menu_list_notes) {
						dialog.dismiss();
						distanceCalculatorPlugin.showNotesListDialog(mapActivity);
					} else if (standardId == R.string.measurement_point_menu_show_notes) {
						displayNotesFlag = (displayNotesFlag ? false:true);
						mapActivity.getMapView().refreshMap();
					}
				}
			});
			builder.show();
		}
		
	    private void editPointSelectionRadius() {
	        Builder builder = new AlertDialog.Builder(mapActivity);
			LinearLayout linear = new LinearLayout(mapActivity); 
		    linear.setOrientation(1);
		    TextView text = new TextView(mapActivity); 
		    currentValue = new TextView(mapActivity); 
		    TextView range = new TextView(mapActivity); 
    		min = minMeasurementPointSelectionRadius;
    		max = maxMeasurementPointSelectionRadius;
			current = view.getSettings().MAP_POINT_SELECTION_RADIUS.get();
    		prompt = app.getString(R.string.plan_selection_size_prompt);
    		warning = app.getString(R.string.plan_selection_size_warning) + " ";			    	
		    text.setText(prompt); 		    
		    currentValue.setText(String.valueOf(current)); 
		    range.setText("min: " + String.valueOf(min) +
		    		"                  max: " + String.valueOf(max));
		    range.setGravity(1);
		    currentValue.setGravity(1);
		    text.setPadding(10, 10, 10, 10); 
		    seek = new SeekBar(mapActivity); 
		    linear.addView(range);
		    linear.addView(seek);
		    linear.addView(currentValue);
		    linear.addView(text);
		    seek.setMax(max - min);
		    seek.setProgress(current - min);
		    seek.setOnSeekBarChangeListener(new OnSeekBarChangeListener(){
		    	@Override
			    public void onProgressChanged(SeekBar seek, int progress,
			    		boolean fromUser) {
		    		currentValue.setText(String.valueOf(seek.getProgress() + min));
		    	}
		        public void onStopTrackingTouch(SeekBar arg0) {
		        }
		 
		        public void onStartTrackingTouch(SeekBar arg0) {
		        }
		    });

		    builder.setView(linear); 	
		    builder.setPositiveButton("Ok",new DialogInterface.OnClickListener() {
		        public void onClick(DialogInterface dialog,int id) {  
		            int newValue = seek.getProgress() + min;
		            if(newValue >= min && newValue <= max){
		            	// ensure that the selection area radius is at least as big as the display radius
		            		if(newValue < view.getSettings().MAP_POINT_SELECTION_RADIUS.get()) newValue = view.getSettings().MAP_POINT_SELECTION_RADIUS.get();
		            		view.getSettings().MAP_POINT_SELECTION_RADIUS.set(newValue);	
			            Toast.makeText(view.getContext(), warning + String.valueOf(newValue),Toast.LENGTH_LONG).show(); 
						view.refreshMap(true);
		            }
		        } 
		    });
		    
		    builder.setNegativeButton("Cancel",new DialogInterface.OnClickListener() {  
		        public void onClick(DialogInterface dialog,int id) { 
		        } 
		    }); 
		    
		    builder.show(); 
	    }

	    protected void addPointNote(final MapActivity mapActivity) {
			Builder b = new AlertDialog.Builder(mapActivity);
			LinearLayout ll = new LinearLayout(mapActivity);
			ll.setOrientation(LinearLayout.VERTICAL);
			ll.setPadding(5, 5, 5, 5);
			final TextView tv = new TextView(mapActivity);
			tv.setText("");
			tv.setTextColor(Color.RED);
			ll.addView(tv);
			final EditText editText = new EditText(mapActivity);
			if(selectedPointIndex >= 0){
				editText.setText(measurementPoints.get(selectedSubtrackIndex).get(selectedPointIndex).desc);
			}else{
				editText.setHint(R.string.measurement_point_note);
			}
			editText.addTextChangedListener(new TextWatcher() {
				
				@Override
				public void onTextChanged(CharSequence s, int start, int before, int count) {
					editPointNote(s);
					double x = measurementPoints.get(selectedSubtrackIndex).get(selectedPointIndex).lat;
					double y = measurementPoints.get(selectedSubtrackIndex).get(selectedPointIndex).lon;
					LatLon pt = new LatLon(x, y);
					setLocation(pt, setDescription(true, null));
					view.refreshMap();
				}
				
				@Override
				public void beforeTextChanged(CharSequence s, int start, int count, int after) {
				}
				
				@Override
				public void afterTextChanged(Editable s) {
				}
			});
			ll.addView(editText);
			b.setView(ll);
			b.setPositiveButton(R.string.default_buttons_save, new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					measurementPoints.get(selectedSubtrackIndex).get(selectedPointIndex).desc = editText.getText().toString();
				}
			});
			b.setNegativeButton(R.string.default_buttons_cancel, null);
			b.show();
		}
		
		public TextView getTextView (){
			return textView;
		}
		
		@Override
		public void onRetainNonConfigurationInstance(Map<String, Object> map) {
			map.put(KEY_SELECTED_SUBTRACK_INDEX, selectedSubtrackIndex);
			map.put(KEY_SELECTED_POINT_INDEX, selectedPointIndex);
			map.put(KEY_DESCRIPTION, description);
		}
	}
}
