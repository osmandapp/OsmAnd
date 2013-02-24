package net.osmand.plus.planning;
/**
 * 
 * The planning plugin provides facilities to:
 * 1. Manually place/edit a track of sequential locations (points) on a map.
 * 2. Save/retrieve manually generated tracks using GPX files.
 * 3. Measure the total/partial distance of a manually generated track.
 * 4. Provide display screen zooming.
 * 
 * @author Car Michael
 */

	import java.util.ArrayList;
	import java.util.Arrays;
	import java.util.Comparator;
	import java.util.EnumSet;
	import java.util.Iterator;
	import java.util.List;
	import java.util.Map;
	import java.io.File;
	import net.osmand.plus.GPXUtilities.GPXFile;
	import net.osmand.CallbackWithObject;
	import net.osmand.IndexConstants;
	import net.osmand.PlatformUtil;
	import net.osmand.access.AccessibleToast;
	import net.osmand.plus.ContextMenuAdapter;
	import net.osmand.plus.ContextMenuAdapter.OnContextMenuClick;
	import net.osmand.plus.OptionsMenuHelper.OnOptionsMenuClick;
	import net.osmand.plus.OptionsMenuHelper;
	import net.osmand.plus.OsmandApplication;
	import net.osmand.plus.OsmandPlugin;
	import net.osmand.plus.OsmandSettings;
	import net.osmand.plus.R;
	import net.osmand.plus.ApplicationMode;
	import net.osmand.plus.activities.MapActivity;
	import net.osmand.plus.views.MapInfoControl;
	import net.osmand.plus.views.MapInfoLayer;
	import net.osmand.plus.views.OsmandMapTileView;
	import net.osmand.plus.views.TextInfoControl;
	import net.osmand.plus.views.MapInfoControls.MapInfoControlRegInfo;
	import net.osmand.plus.GPXUtilities;
	import net.osmand.plus.GPXUtilities.WptPt;
	import net.osmand.util.Algorithms;
	import android.app.AlertDialog;
	import android.app.AlertDialog.Builder;
	import android.app.ProgressDialog;
	import android.content.DialogInterface;
	import android.content.Context;
	import android.graphics.Paint;
	import android.view.Menu;
	import android.view.MenuItem;
	import android.view.View;
	import android.widget.EditText;
	import android.widget.FrameLayout;
	import android.widget.LinearLayout;
	import android.widget.TextView;
	import android.widget.Toast;
	import android.widget.SeekBar;
	import android.widget.SeekBar.OnSeekBarChangeListener;  
	import android.util.Log;

	public class PlanningPlugin extends OsmandPlugin {
		public static final String ID = "osmand.planning";
		private OsmandApplication app;
		private static PlanningLayer planningLayer;
		private static OsmandSettings settings;
		private MapInfoControl measurementControl;
		public static OsmandMapTileView mapView;
		private static ProgressDialog progressDlg;
		private static Context context;
		static final int DIALOG_REQUEST_FILENAME_ID = 502;
		private MapActivity mapActivity;
	    private SeekBar seek;
	    private TextView currentValue; 
	    private String prompt = null;
	    private String warning = null;
	    private int min = 0;
	    private int max = 0;
	    private int current = 0;
	    private int type = 1;
	    
	    /**
		 * Menu items to be provided for the menu opened when
		 * the a measurement point or info box is tapped.
		 */
		final int[] measurementPointMenuActions = new int[]{
				R.string.context_menu_item_cancel,
				R.string.context_menu_item_insert_point,
				R.string.context_menu_item_delete_point,
				R.string.context_menu_item_clear_all_points,
				R.string.context_menu_item_display_point_info,
				R.string.context_menu_item_display_distance_only
	    	};
		/**
		 * Menu items to be provided for the menu opened when
		 * the measurement map info bar is clicked.
		 */
		final int[] mapInfoBarMenuActions = new int[]{
				R.string.context_menu_item_cancel,
				R.string.context_menu_item_planning_help,
				R.string.context_menu_item_end_measurement,
				R.string.context_menu_item_clear_all_points,
				R.string.context_menu_item_display_point_info,
				R.string.context_menu_item_display_distance_only,
				R.string.context_menu_item_load_GPX_plan,
				R.string.context_menu_item_save_plan,
				R.string.context_menu_item_edit_point_size,
				R.string.context_menu_item_edit_point_selection_area
	    	};
		/**
		 * Menu items to be provided for the menu opened when
		 * the measurement map info bar is clicked.
		 */
		final int[] ContextMenuActions = new int[]{
				R.string.context_menu_item_planning_help,
				R.string.context_menu_item_clear_all_points,
				R.string.context_menu_item_display_point_info,
				R.string.context_menu_item_display_distance_only,
				R.string.context_menu_item_load_GPX_plan,
				R.string.context_menu_item_save_plan,
				R.string.context_menu_item_edit_point_size,
				R.string.context_menu_item_edit_point_selection_area
	    	};
		public int originalContextMenuItems;
		
		public PlanningPlugin(OsmandApplication app) {
			this.app = app;
		}
		
		@Override
		public boolean init(OsmandApplication app) {
			settings = app.getSettings();
			return true;
		}

		@Override
		public String getId() {
			return ID;
		}

		@Override
		public String getDescription() {
			return app.getString(R.string.osmand_planning_plugin_description);
		}

		@Override
		public String getName() {
			return app.getString(R.string.osmand_planning_plugin_name);
		}

		@Override
		public void registerLayers(MapActivity activity) {
			if(planningLayer == null) {
				planningLayer = new PlanningLayer(activity);
				mapView = planningLayer.getPlanningView();
				planningLayer.setPlanningPlugin(this);
			}
			activity.getMapView().addLayer(planningLayer, 4.5f);
			registerWidget(activity);
			if(settings.getDisplayScaleFactor() < planningLayer.MIN_DISPLAY_FACTOR || 
					settings.getDisplayScaleFactor() > planningLayer.MAX_DISPLAY_FACTOR) settings.setDisplayScaleFactor(1.0f);
		}

		@Override
		public void updateLayers(OsmandMapTileView mapView, MapActivity activity) {
			if (planningLayer == null) {
				registerLayers(activity);
			}
			registerWidget(activity);
			mapActivity = activity;
			context = (Context) mapActivity;
		}

		/**
		 * Turn off display zoom buttons if the plugin is turned off.
		 */
		@Override
		public void disable(OsmandApplication application) {
			planningLayer.hideDisplayZoomButtons();	//hide display zoom control if plugin has been disabled
			settings.setPlanningMode(false);	//disable planning mode
			MapInfoLayer mapInfoLayer = mapActivity.getMapLayers().getMapInfoLayer();
			if (mapInfoLayer != null) {	//remove the Options menu item entry for Planning
				Iterator<MapInfoControlRegInfo> it = mapInfoLayer.getMapInfoControls().getRight().iterator();
				while(it.hasNext()) {
					if(Algorithms.objectEquals(it.next().messageId, R.string.map_list_activities_planning)) {
						it.remove();
					}
				}
				mapInfoLayer.recreateControls();
			}
		}

		/**
		 * Method to: 
		 * 1. create an info bar control to enable/disable planning mode.
		 * 2. Add a menu item and associated image to use in the map context menu
		 * for controlling the visibility of the planning mode menu info bar .
		 */
		private void registerWidget(MapActivity activity) {
			MapInfoLayer mapInfoLayer = activity.getMapLayers().getMapInfoLayer();
			if (mapInfoLayer != null) {
				measurementControl = createMeasurementInfoControl(activity, mapInfoLayer.getPaintText(), mapInfoLayer.getPaintSubText());
				mapInfoLayer.getMapInfoControls().registerSideWidget(measurementControl,
							R.drawable.list_activities_planning, R.string.map_list_activities_planning, "Plannning/Measurement", false,
							EnumSet.allOf(ApplicationMode.class), EnumSet.noneOf(ApplicationMode.class), 8);
				mapInfoLayer.recreateControls();
			}
		}
		
		/**
		 * Establish an info bar control on the MapInfoLayer 
		 * that enables turning on/off planning/measurement mode.
		 */
		private TextInfoControl createMeasurementInfoControl(final MapActivity map, Paint paintText, Paint paintSubText) {
			TextInfoControl measurementControl = new TextInfoControl(map, 0, paintText, paintSubText) {
				@Override
				public boolean updateInfo() {
					if(settings.getEnabledPlugins().contains("osmand.planning")){	//hide control if plugin has been disabled
						if(settings.getPlanningMode()){
							setImageDrawable(map.getResources().getDrawable(R.drawable.widget_measurement_on));
							setText(" ", null);
						}else{
							setImageDrawable(map.getResources().getDrawable(R.drawable.widget_measurement_off));				
							setText(" ", null);
						}
					}else{
						setText(null, null);
					}
					return true;
				}		
			};
			measurementControl.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if(settings.getPlanningMode()){
						createMapInfoBarMenuDialog();
					}else{
						settings.setPlanningMode(true);
						map.getMapLayers().getContextMenuLayer().setLocation(null, null);	//remove any point info textbox from ContextMenuLayer 
					}
					mapActivity.getMapView().refreshMap();
				}
			});
			if(settings.getPlanningMode()){
				measurementControl.setImageDrawable(map.getResources().getDrawable(R.drawable.widget_measurement_on));
			}else{
				measurementControl.setImageDrawable(map.getResources().getDrawable(R.drawable.widget_measurement_off));				
			}
			measurementControl.setText(null, null);
			return measurementControl;
		}
		
	    public void createMeasurementMenu(){
	    	createMeasurementPointMenuDialog();
	    }
	  		
	    public MapActivity getMapActivity(){
	    	return mapActivity;
	    }
	    
	    protected void showToast(final String msg){
	    	mapActivity.runOnUiThread(new Runnable(){
				@Override
				public void run() {
					Toast.makeText(mapActivity, msg, Toast.LENGTH_LONG).show();
				}
	    	});
	    }

		/**
		 * Establish new items on the Options menu 
		 * that enable turning on/off planning/measurement mode
		 * and display zoom button visibility
		 */
		@Override
		public void registerOptionsMenuItems(final MapActivity mapActivity, OptionsMenuHelper helper) {
			if (planningLayer != null) {
				helper.registerOptionsMenuItem(R.string.context_menu_item_show_display_zoom_buttons, R.string.context_menu_item_show_display_zoom_buttons,
						android.R.drawable.ic_menu_mylocation, new OnOptionsMenuClick() {
					@Override
					public void prepareOptionsMenu(Menu menu, MenuItem showZoomButtonItem) {
						if (!settings.getMapDisplayZoomButtonVisibility() && settings.getEnabledPlugins().contains("osmand.planning")) {
							showZoomButtonItem.setVisible(true);
						} else {
							showZoomButtonItem.setVisible(false);
						}
					}
					
					@Override
					public boolean onClick(MenuItem item) {
						planningLayer.showDisplayZoomButtons();
						settings.setMapDisplayZoomButtonVisibility(true);
						planningLayer.getPlanningView().refreshMap(true);
						return true;
					}
				});
				
				helper.registerOptionsMenuItem(R.string.context_menu_item_hide_display_zoom_buttons, R.string.context_menu_item_hide_display_zoom_buttons,
						android.R.drawable.ic_menu_mylocation, new OnOptionsMenuClick() {
					@Override
					public void prepareOptionsMenu(Menu menu, MenuItem hideZoomButtonItem) {
						if (settings.getMapDisplayZoomButtonVisibility() && settings.getEnabledPlugins().contains("osmand.planning")) {
							hideZoomButtonItem.setVisible(true);
						} else {
							hideZoomButtonItem.setVisible(false);
						}
					}
					
					@Override
					public boolean onClick(MenuItem item) {
						planningLayer.hideDisplayZoomButtons();
						settings.setMapDisplayZoomButtonVisibility(false);
						planningLayer.getPlanningView().refreshMap(true);
						return true;
					}
				});
				helper.registerOptionsMenuItem(R.string.context_menu_item_end_measurement, R.string.context_menu_item_end_measurement,
						android.R.drawable.ic_menu_mylocation, new OnOptionsMenuClick() {
					@Override
					public void prepareOptionsMenu(Menu menu, MenuItem showMeasurementModeItem) {
						if (settings.getPlanningMode() && settings.getEnabledPlugins().contains("osmand.planning")) {
							showMeasurementModeItem.setVisible(true);
						} else {
							showMeasurementModeItem.setVisible(false);
						}
					}
					
					@Override
					public boolean onClick(MenuItem item) {
						settings.setPlanningMode(false);	//turn on planning measurement mode						
						planningLayer.getPlanningView().refreshMap(true);
						return true;
					}
				});
				
				helper.registerOptionsMenuItem(R.string.context_menu_item_start_measurement_mode, R.string.context_menu_item_start_measurement_mode,
						android.R.drawable.ic_menu_mylocation, new OnOptionsMenuClick() {
					@Override
					public void prepareOptionsMenu(Menu menu, MenuItem showMeasurementModeItem) {
						if (!settings.getPlanningMode() && settings.getEnabledPlugins().contains("osmand.planning")) {
							showMeasurementModeItem.setVisible(true);
						} else {
							showMeasurementModeItem.setVisible(false);
						}
					}
					
					@Override
					public boolean onClick(MenuItem item) {
						settings.setPlanningMode(true);	//turn on planning measurement mode						
						planningLayer.getPlanningView().refreshMap(true);
						return true;
					}
				});
			}
		}
	    
		/**
		 * Set up the additional menu items on the Context Menu ("More" button)
		 */
		@Override
		public void registerMapContextMenuActions(final MapActivity mapActivity,
				final double latitude, final double longitude,
				ContextMenuAdapter adapter, Object selectedObj) {		
			OnContextMenuClick addListener = new OnContextMenuClick() {
				@Override
				public void onContextMenuClick(int resId, int pos,
						boolean isChecked, DialogInterface dialog) {	//add the actions for the new menu items
					int standardId = 0;
					int index = 0;
					planningLayer.setNewPointFlag(false);	//block new measurement point creation
					standardId = ContextMenuActions[pos - originalContextMenuItems];	//items common to both measurement point dialogs
					index = planningLayer.selectedMeasurementPointIndex;
					if(standardId == R.string.context_menu_item_clear_all_points){	//test if all measurement points should be removed
						planningLayer.selectedMeasurementPointIndex = -1;
						planningLayer.measurementPoints.clear();	//remove all points from the list
						planningLayer.setLocation(null, "");	//delete the point info text box
					}else if(standardId == R.string.context_menu_item_display_point_info){
						planningLayer.longInfoFlag = true;
						if(index >=0){
							planningLayer.displayIntermediatePointInfo(index);	//use the latLon for the point, not the menu click
							planningLayer.colourChangeIndex = index;	//point colour should change after this point
						}else{
							planningLayer.setLocation(null, "");	//delete the point info text box
						}
					}else if(standardId == R.string.context_menu_item_display_distance_only){
						planningLayer.longInfoFlag = false;
						if(index >=0){
							planningLayer.displayIntermediatePointInfo(index);	//use the latLon for the point, not the menu click
							planningLayer.colourChangeIndex = index;	//point colour should change after this point
						}else{
							planningLayer.setLocation(null, "");	//delete the point info text box
						}
					}else if(standardId == R.string.context_menu_item_edit_point_size){	//change measurement point display size
						editMeasurementPointData(1);
					}else if(standardId == R.string.context_menu_item_edit_point_selection_area){	//change measurement point selection area size
						editMeasurementPointData(2);
					}else if(standardId == R.string.context_menu_item_load_GPX_plan){	// load path from a GXP file
						dialog.dismiss();
						loadGPXFileData();
					}else if(standardId == R.string.context_menu_item_save_plan){	// save path as a GXP file
						dialog.dismiss();
						progressDlg = ProgressDialog.show(context, context.getString(R.string.saving_gpx_tracks),
								context.getString(R.string.saving_gpx_tracks), true);
						createFileNameRequestDialog(mapActivity);
					}else if(standardId == R.string.context_menu_item_planning_help){	//open help dialog
						showHelpDialog();
					}else if(standardId == R.string.context_menu_item_hide_display_zoom_buttons){	//toggle display zoom button visibility
						planningLayer.showDisplayZoomButtons();
					}else if(standardId == R.string.context_menu_item_show_display_zoom_buttons){	//toggle display zoom button visibility
						planningLayer.hideDisplayZoomButtons();
					}
					planningLayer.getPlanningView().refreshMap(true);
				}
			};
			
			if(settings.getPlanningMode()){	//Add the new menu items
				int actionsToUse =  ContextMenuActions.length;	//measurement point selected items;
				originalContextMenuItems = adapter.length();
		    	for(int j = 0; j < actionsToUse; j++){
					adapter.registerItem(ContextMenuActions[j], R.drawable.list_activities_planning, addListener, -1);	
		    	}
			}else{
		    	for(int j = 0; j < 3; j++){	//only the first 3 actions on the list are used for planning mode off
					adapter.registerItem(ContextMenuActions[j], R.drawable.list_activities_planning, addListener, -1);	
		    	}
			}
		}

		/**
		 * Method to open a menu when a measurement point or information text box is tapped.
		 */
		void createMeasurementPointMenuDialog() {
	        // Menu opened when a measurement point or information text box is tapped	    	
	    	Builder builder = new AlertDialog.Builder(mapActivity);
	    	List<String> actions = new ArrayList<String>();
	    	int actionsToUse = 0;
	    	actionsToUse =  measurementPointMenuActions.length;	//measurement point selected items;
	    	for(int j = 0; j < actionsToUse; j++){
	    		actions.add(mapActivity.getResources().getString(measurementPointMenuActions[j]));
	    	}		
	    	builder.setItems(actions.toArray(new String[actions.size()]), new DialogInterface.OnClickListener(){
	
				@Override
				public void onClick(DialogInterface dialog, int which) {
					int standardId = 0;
					int index = 0;
					planningLayer.setNewPointFlag(false);	//block new measurement point creation
					standardId = measurementPointMenuActions[which];	//items common to both measurement point dialogs
					index = planningLayer.selectedMeasurementPointIndex;
					if(standardId == R.string.context_menu_item_delete_point){	//test if current point is to be removed
						if(index >= 0){
							planningLayer.measurementPoints.remove(index);	//delete the point if one has been selected
							planningLayer.selectedMeasurementPointIndex = planningLayer.measurementPoints.size() - 1;	//point to the last point
							planningLayer.colourChangeIndex = planningLayer.selectedMeasurementPointIndex;	//point colour should change after this point							
							planningLayer.displayIntermediatePointInfo(planningLayer.colourChangeIndex);
						}
					}else if(standardId == R.string.context_menu_item_clear_all_points){	//test if all measurement points should be removed
						planningLayer.selectedMeasurementPointIndex = -1;
						planningLayer.measurementPoints.clear();	//remove all points from the list
						planningLayer.setLocation(null, "");	//delete the point info text box
					}else if(standardId == R.string.context_menu_item_insert_point){	//test if extra measurement point is to be inserted
						if(index >= 0){
							planningLayer.colourChangeIndex = index;	//point colour should change after this point
							planningLayer.measurementPointInsertionIndex = index;	//indicates an insertion should be performed before this point
							planningLayer.displayIntermediatePointInfo(index);	//use the latLon for the point, not the menu click
						}
					}else if(standardId == R.string.context_menu_item_cancel){	// close dialog with no action
					}else if(standardId == R.string.context_menu_item_display_point_info){
						planningLayer.longInfoFlag = true;
						if(index >=0){
							planningLayer.displayIntermediatePointInfo(index);	//use the latLon for the point, not the menu click
							planningLayer.colourChangeIndex = index;	//point colour should change after this point
						}else{
							planningLayer.setLocation(null, "");	//delete the point info text box
						}
					}else if(standardId == R.string.context_menu_item_display_distance_only){
						planningLayer.longInfoFlag = false;
						if(index >=0){
							planningLayer.displayIntermediatePointInfo(index);	//use the latLon for the point, not the menu click
							planningLayer.colourChangeIndex = index;	//point colour should change after this point
						}else{
							planningLayer.setLocation(null, "");	//delete the point info text box
						}
					}
					planningLayer.getPlanningView().refreshMap(true);
				}
			});
			builder.show();
		}

		/**
		 * Method to open dialog when planning map info bar is pressed
		 */
		void createMapInfoBarMenuDialog() {
	        // Menu opened when Planning/Measurement button is pressed on MapInfoMenuBar	    	
	    	Builder builder = new AlertDialog.Builder(mapActivity);
	    	List<String> actions = new ArrayList<String>();
	    	int actionsToUse = 0;
	    	actionsToUse =  mapInfoBarMenuActions.length;	//measurement point selected items;
	    	for(int j = 0; j < actionsToUse; j++){
	    		actions.add(mapActivity.getResources().getString(mapInfoBarMenuActions[j]));
	    	}		
	    	builder.setItems(actions.toArray(new String[actions.size()]), new DialogInterface.OnClickListener(){
	
				@Override
				public void onClick(DialogInterface dialog, int which) {
					int standardId = 0;
					int index = 0;
					planningLayer.setNewPointFlag(false);	//block new measurement point creation
					standardId = mapInfoBarMenuActions[which];	//items common to both measurement point dialogs
					index = planningLayer.selectedMeasurementPointIndex;
					if(standardId == R.string.context_menu_item_clear_all_points){	//test if all measurement points should be removed
						planningLayer.selectedMeasurementPointIndex = -1;
						planningLayer.measurementPoints.clear();	//remove all points from the list
						planningLayer.setLocation(null, "");	//delete the point info text box
					}else if(standardId == R.string.context_menu_item_cancel){	// close dialog with no action
					}else if(standardId == R.string.context_menu_item_display_point_info){
						planningLayer.longInfoFlag = true;
						if(index >=0){
							planningLayer.displayIntermediatePointInfo(index);	//use the latLon for the point, not the menu click
							planningLayer.colourChangeIndex = index;	//point colour should change after this point
						}else{
							planningLayer.setLocation(null, "");	//delete the point info text box
						}
					}else if(standardId == R.string.context_menu_item_display_distance_only){
						planningLayer.longInfoFlag = false;
						if(index >=0){
							planningLayer.displayIntermediatePointInfo(index);	//use the latLon for the point, not the menu click
							planningLayer.colourChangeIndex = index;	//point colour should change after this point
						}else{
							planningLayer.setLocation(null, "");	//delete the point info text box
						}
					}else if(standardId == R.string.context_menu_item_end_measurement){	// End measurement mode
						settings.setPlanningMode(false);	//turn off planning measurement mode						
					}else if(standardId == R.string.context_menu_item_edit_point_size){	//change measurement point display size
						editMeasurementPointData(1);
					}else if(standardId == R.string.context_menu_item_edit_point_selection_area){	//change measurement point selection area size
						editMeasurementPointData(2);
					}else if(standardId == R.string.context_menu_item_load_GPX_plan){	// load path from a GXP file
						dialog.dismiss();
						loadGPXFileData();
					}else if(standardId == R.string.context_menu_item_save_plan){	// save path as a GXP file
						dialog.dismiss();
						progressDlg = ProgressDialog.show(context, context.getString(R.string.saving_gpx_tracks),
								context.getString(R.string.saving_gpx_tracks), true);
						createFileNameRequestDialog(mapActivity);
					}else if(standardId == R.string.context_menu_item_planning_help){	//open help dialog
						showHelpDialog();
					}
					planningLayer.getPlanningView().refreshMap(true);
				}
	    	});
			builder.show();
		}
		    
	    protected void requestFileName(){
	    	mapActivity.showDialog(DIALOG_REQUEST_FILENAME_ID);
	    }
	    
		/**
		 * Method to open dialog to enter name of file to save data as GPX file
		 * then to save the data to the file.
		 */
	    public void createFileNameRequestDialog(final MapActivity mapActivity){
	     	Builder builder = new AlertDialog.Builder(mapActivity);
			builder.setTitle(mapActivity.getResources().getString(R.string.filename_input));
			FrameLayout parent = new FrameLayout(mapActivity);
			final EditText editText = new EditText(mapActivity);
			editText.setId(R.id.TextView);
			parent.setPadding(15, 0, 15, 0);
			parent.addView(editText);
			builder.setView(parent);
			builder.setNegativeButton(R.string.default_buttons_cancel, null);
			builder.setPositiveButton(R.string.default_buttons_ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					String fileName = editText.getText().toString();
					String warn = "";
					List<String> warnings = new ArrayList<String>();
					
					try {
						GPXFile gpx = new GPXFile();
						for (int i = 0;i < planningLayer.measurementPoints.size(); i++){
							WptPt pt = new WptPt();							
							pt.lat = planningLayer.measurementPoints.get(i).getLatitude();
							pt.lon = planningLayer.measurementPoints.get(i).getLongitude();
							pt.ele = 0;
							pt.hdop = 0;
							pt.name ="";
							pt.speed = 0;
							pt.time = 0;
							pt.desc = "";
							gpx.points.add(pt);												
						}
						File dir = settings.getExternalStorageDirectory();
						if (dir.canWrite()) {
							dir = app.getAppPath(IndexConstants.GPX_INDEX_DIR);
							dir.mkdirs();
							if (dir.exists()) {
								// save data in a GPX file
								final String f = context.getString(R.string.plan_file_name_prefix);
								File fout = new File(dir, f + ".gpx");
								if (planningLayer.measurementPoints.size() > 0) {
									if(fileName != ""){
										fileName = f + fileName + ".gpx";						
										fout = new File(dir, fileName);
									}
								}
								warn = GPXUtilities.writeGpxFile(fout, gpx, app);
								if (warn != null) {
									warnings.add(warn);
									showToast(context.getString(R.string.warning_save_failed));											
								}else{												
									showToast(context.getString(R.string.warning_saved_OK) + " " + fileName);											
								}
							}
						}
					}catch(Exception e){
						Log.e(PlatformUtil.TAG, "Error saving plan GPX file", e);
							showToast(context.getString(R.string.warning_save_failed));											
					} finally {
						if (progressDlg != null) {
							progressDlg.dismiss();
							progressDlg = null;
						}
					}
				}
			});		
			builder.show();
	    }
	    
		/**
		 * Method to open dialog to select GPX file to load, load the file
		 * then to replace the measurement track with the loaded data.
		 */
		public void loadGPXFileData(){
			selectGPXFile(new CallbackWithObject<GPXFile>() {
				@Override
				public boolean processResult(GPXFile result) {
					GPXFile toShow = result;
					if (toShow == null) {
						if(!settings.SAVE_TRACK_TO_GPX.get()){
							AccessibleToast.makeText(mapActivity, R.string.gpx_monitoring_disabled_warn, Toast.LENGTH_SHORT).show();
							return true;
						}
						Map<String, GPXFile> data = mapActivity.getSavingTrackHelper().collectRecordedData();
						if(data.isEmpty()){
							toShow = new GPXFile();						
						} else {
							toShow = data.values().iterator().next();
						}
					}
					planningLayer.showGPXPlan(result);	//plot data points
					return true;
				}
			}, true, true);
		}
		
		public void selectGPXFile(final CallbackWithObject<GPXFile> callbackWithObject, final boolean convertCloudmade,
				final boolean showCurrentGpx) {
			final List<String> list = new ArrayList<String>();
			final File dir = app.getAppPath(IndexConstants.GPX_INDEX_DIR);
			if (dir != null && dir.canRead()) {
				File[] files = dir.listFiles();
				if (files != null) {
					Arrays.sort(files, new Comparator<File>() {
						@Override
						public int compare(File object1, File object2) {
							if (object1.getName().compareTo(object2.getName()) > 0) {
								return -1;
							} else if (object1.getName().equals(object2.getName())) {
								return 0;
							}
							return 1;
						}
					});
					for (File f : files) {
						if (f.getName().endsWith(".gpx")) { //$NON-NLS-1$
							list.add(f.getName());
						}
					}
				}
			}
			
			if(list.isEmpty()){
				AccessibleToast.makeText(mapActivity, R.string.gpx_files_not_found, Toast.LENGTH_LONG).show();
			}
			if(!list.isEmpty() || showCurrentGpx){
				Builder builder = new AlertDialog.Builder(mapActivity);
				if(showCurrentGpx){
					list.add(0, getString(R.string.show_current_gpx_title));
				}
				builder.setItems(list.toArray(new String[list.size()]), new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						if(showCurrentGpx && which == 0){
							callbackWithObject.processResult(null);
						} else {
							final ProgressDialog dlg = ProgressDialog.show(mapActivity, getString(R.string.loading),
									getString(R.string.loading_data));
							final File f = new File(dir, list.get(which));
							new Thread(new Runnable() {
								@Override
								public void run() {
									final GPXFile res = GPXUtilities.loadGPXFile(app, f, convertCloudmade);
									dlg.dismiss();
									mapActivity.runOnUiThread(new Runnable() {
										@Override
										public void run() {
											if (res.warning != null) {
												AccessibleToast.makeText(mapActivity, res.warning, Toast.LENGTH_LONG).show();
											} else {
												callbackWithObject.processResult(res);
											}
										}
									});
								}
							}, "Loading gpx").start();
						}
					}
				});
				AlertDialog dlg = builder.show();
				try {
					dlg.getListView().setFastScrollEnabled(true);
				} catch(Exception e) {
					// java.lang.ClassCastException: com.android.internal.widget.RoundCornerListAdapter
					// Unknown reason but on some devices fail
				}
			}
		}
		
		/**
		 * Method to open message dialog to display help.
		 */
		public void showHelpDialog(){
	    	Builder builder = new AlertDialog.Builder(mapActivity);
			builder.setTitle(mapActivity.getResources().getText(R.string.help_Distance_Measurement_t));
			builder.setMessage(mapActivity.getResources().getString(R.string.help_Distance_Measurement));	
			builder.setCancelable(true);
			builder.setNeutralButton(mapActivity.getResources().getString(R.string.close), new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			});
			builder.show();
		}

	    private String getString(int resId) {
			return mapActivity.getString(resId);
		}

	    private void editMeasurementPointData(int type) {
	    	this.type = type;
	        Builder builder = new AlertDialog.Builder(mapActivity);
			LinearLayout linear = new LinearLayout(mapActivity); 
		    linear.setOrientation(1);
		    TextView text = new TextView(mapActivity); 
		    currentValue = new TextView(mapActivity); 
		    TextView range = new TextView(mapActivity); 
		    if(type == 1 || type == 2){
		    	if(type == 1){
		    		min = planningLayer.minMeasurementPointDisplayRadius;
		    		max = planningLayer.maxMeasurementPointDisplayRadius;
		    		current = settings.getMeasurementPointDisplayRadius();
		    		prompt = getString(R.string.plan_display_size_prompt);
		    		warning = getString(R.string.plan_display_size_warning) + " ";
			    }else if(type == 2){
		    		min = planningLayer.minMeasurementPointSelectionRadius;
		    		max = planningLayer.maxMeasurementPointSelectionRadius;
		    		current = settings.getMeasurementPointSelectionRadius();
		    		prompt = getString(R.string.plan_selection_size_prompt);
		    		warning = getString(R.string.plan_selection_size_warning) + " ";			    	
			    }
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
			            	if(getType() == 1){
				            	settings.setMeasurementPointDisplayRadius(newValue);
			            		if(settings.getMeasurementPointSelectionRadius() < newValue) settings.setMeasurementPointSelectionRadius(newValue);
			            	}else if(getType() == 2){
			            		if(newValue < settings.getMeasurementPointDisplayRadius()) newValue = settings.getMeasurementPointDisplayRadius();
				            	settings.setMeasurementPointSelectionRadius(newValue);	
			            	}
				            Toast.makeText(context, warning + String.valueOf(newValue),Toast.LENGTH_LONG).show(); 
							planningLayer.getPlanningView().refreshMap(true);
			            }
			        } 
			    });
			    
			    builder.setNegativeButton("Cancel",new DialogInterface.OnClickListener() {  
			        public void onClick(DialogInterface dialog,int id) { 
			        } 
			    }); 
			    
			    builder.show(); 
		    }   	    
	    }
	    
	    public int getType(){
	    	return type;
	    }
	}
