package net.osmand.plus.activities;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import net.osmand.GPXUtilities;
import net.osmand.LogUtil;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.osm.LatLon;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.ProgressDialogImplementation;
import net.osmand.plus.R;
import net.osmand.plus.ResourceManager;
import net.osmand.plus.views.OsmandMapTileView;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.PointF;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

// Provide context menus and callbacks for Measurement Mode

public class MeasurementActivity{
	
	//Variables defined to support measurement mode
	public boolean measureDistanceMode = false;	//Status of distance measurement mode
	public int measurementPointInsertionIndex = -1;	//distance measurement point array insertion index
	public int selectedMeasurementPointIndex = -1;	//For distance measurement point display
	public int colourChangeIndex = -1;	//to support distance measurement point display colours
	private boolean longInfoFlag = false;	//to provide control of info provided for measurement points
	private float cumMeasuredDistance=0;	//Distance along path for distance measurement mode
	public List<LatLon> measurementPoints = new ArrayList<LatLon>();	//Path points for distance measurement
	private LatLon screenPointLatLon;		//To provide access to current point lat/lon
	private final int maxMeasurementPointRadius = 9;
	private final int minMeasurementPointRadius = 1;
	private final int maxMeasurementPointSelectionRadius = 9;
	private final int minMeasurementPointSelectionRadius = 3;
	private int measurementPointRadius = 5;
	private int measurementPointSelectionRadius = 5;
	private boolean scrollingFlag = false;		//For measurement point dragging
	private ProgressDialog progressDlg;
	private Context context;
	private LocalIndexHelper localIndexHelper;
	private String requestedName = "";

	/** Called when the activity is first created. */
	OsmandMapTileView mapView;
	
	private MapActivity mapActivity;

	private MapActivityLayers mapLayers;
	

	// constructor
	public MeasurementActivity(MapActivity mapActivity){
		this.mapActivity = mapActivity;
		mapView = mapActivity.getMapView();
		mapLayers = mapActivity.getMapLayers();
		context = (Context) mapActivity;
		localIndexHelper = new LocalIndexHelper((OsmandApplication) mapActivity.getApplication());		
	}

    public void createMeasurementMenu(int menuId){
		final Resources resources = mapActivity.getResources();
    	Builder builder = new AlertDialog.Builder(mapActivity);
    	List<String> actions = new ArrayList<String>();
    	
    	//Measurement point clicked menu options
    	final int[] measurementPointSelectedMenuActions = new int[]{
    			R.string.context_menu_item_delete_point,
    			R.string.context_menu_item_insert_point,
    			R.string.context_menu_item_display_point_info,
    			R.string.context_menu_item_display_distance_only,
    			R.string.context_menu_item_clear_all_points,
    			R.string.context_menu_item_cancel,
    			R.string.context_menu_item_save_plan,
    			R.string.context_menu_item_load_GPX_plan,
    			R.string.context_menu_item_end_measurement,
    			R.string.context_menu_item_increase_point_size,
    			R.string.context_menu_item_decrease_point_size,
    			R.string.context_menu_item_increase_point_selection_area,
    			R.string.context_menu_item_decrease_point_selection_area,
    	};	
    	//Measurement point text box clicked menu options
    	final int[] measurementPointTextBoxSelectedMenuActions = new int[]{
    			R.string.context_menu_item_display_point_info,
    			R.string.context_menu_item_display_distance_only,
    			R.string.context_menu_item_clear_all_points,
    			R.string.context_menu_item_end_measurement,
    			R.string.context_menu_item_cancel,
    			R.string.context_menu_item_save_plan,
    			R.string.context_menu_item_load_GPX_plan,
    			R.string.context_menu_item_increase_point_size,
    			R.string.context_menu_item_decrease_point_size,
    			R.string.context_menu_item_increase_point_selection_area,
    			R.string.context_menu_item_decrease_point_selection_area,
    	};

    	int actionsToUse = 0;
    	if(selectedMeasurementPointIndex >= 0){	//Select different menu items for measurement mode
    		actionsToUse =  measurementPointSelectedMenuActions.length;	//measurement point selected items;
        	for(int j = 0; j < actionsToUse; j++){
        		actions.add(resources.getString(measurementPointSelectedMenuActions[j]));
        	}
        }else if(selectedMeasurementPointIndex < 0){
    		actionsToUse =  measurementPointTextBoxSelectedMenuActions.length;	//measurement point text box selected items
        	for(int j = 0; j < actionsToUse; j++){
        		actions.add(resources.getString(measurementPointTextBoxSelectedMenuActions[j]));
        	}
        }else{
    	}
    	
    	builder.setItems(actions.toArray(new String[actions.size()]), new DialogInterface.OnClickListener(){

			@Override
			public void onClick(DialogInterface dialog, int which) {
				int standardId = 0;
				int index = 0;
				if(measureDistanceMode){
					mapLayers.getPlanningLayer().setNewPointFlag(false);	//block new measurement point creation
					if(selectedMeasurementPointIndex >= 0){
						standardId = measurementPointSelectedMenuActions[which];	//items common to both measurement point dialogs
					}else{
						standardId = measurementPointTextBoxSelectedMenuActions[which];	//items common to both measurement point dialogs					
					}
					index = selectedMeasurementPointIndex;
					if(standardId == R.string.context_menu_item_delete_point){	//test if current point is to be removed
						if(index >= 0){
							measurementPoints.remove(index);	//delete the point if one has been selected
							selectedMeasurementPointIndex = measurementPoints.size() - 1;	//point to the last point
							colourChangeIndex = selectedMeasurementPointIndex;	//point colour should change after this point							
							mapLayers.getPlanningLayer().displayIntermediatePointInfo(colourChangeIndex);
						}
					}else if(standardId == R.string.context_menu_item_clear_all_points){	//test if all measurement points should be removed
						selectedMeasurementPointIndex = -1;
						measurementPoints.clear();	//remove all points from the list
						mapLayers.getPlanningLayer().setLocation(null, "");	//delete the point info text box
					}else if(standardId == R.string.context_menu_item_insert_point){	//test if extra measurement point is to be inserted
						if(index >= 0){
							colourChangeIndex = index;	//point colour should change after this point
							measurementPointInsertionIndex = index;	//indicates an insertion should be performed before this point
							mapLayers.getPlanningLayer().displayIntermediatePointInfo(index);	//use the latLon for the point, not the menu click
						}
					}else if(standardId == R.string.context_menu_item_cancel){	// close dialog with no action
					}else if(standardId == R.string.context_menu_item_display_point_info){
						longInfoFlag = true;
						if(index >=0){
							mapLayers.getPlanningLayer().displayIntermediatePointInfo(index);	//use the latLon for the point, not the menu click
							colourChangeIndex = index;	//point colour should change after this point
						}else{
							mapLayers.getPlanningLayer().setLocation(null, "");	//delete the point info text box
						}
					}else if(standardId == R.string.context_menu_item_display_distance_only){
						longInfoFlag = false;
						if(index >=0){
							mapLayers.getPlanningLayer().displayIntermediatePointInfo(index);	//use the latLon for the point, not the menu click
							colourChangeIndex = index;	//point colour should change after this point
						}else{
							mapLayers.getPlanningLayer().setLocation(null, "");	//delete the point info text box
						}
					}else if(standardId == R.string.context_menu_item_end_measurement){	// End measurement mode
						measureDistanceMode = false;	//turn off measurement mode						
						measurementPoints.clear();	//remove all points from the list
						mapLayers.getPlanningLayer().setLocation(null, "");	//delete the point info text box
					}else if(standardId == R.string.context_menu_item_increase_point_size){	//change measurement point display size
						if(checkMeasurementPointSize(measurementPointRadius + 2)) {
							measurementPointRadius = measurementPointRadius + 2;
						}
					}else if(standardId == R.string.context_menu_item_decrease_point_size){	//change measurement point display size
						if(checkMeasurementPointSize(measurementPointRadius - 2)) {
							measurementPointRadius = measurementPointRadius - 2;
						}
					}else if(standardId == R.string.context_menu_item_increase_point_selection_area){	//change measurement point selection area size
						if(checkMeasurementPointSelectionSize(measurementPointSelectionRadius + 1)) {
							measurementPointSelectionRadius = measurementPointSelectionRadius + 1;
						}
					}else if(standardId == R.string.context_menu_item_decrease_point_selection_area){	//change measurement point selection area size
						if(checkMeasurementPointSelectionSize(measurementPointSelectionRadius - 1)) {
							measurementPointSelectionRadius = measurementPointSelectionRadius - 1;
						}
					}else if(standardId == R.string.context_menu_item_load_GPX_plan){	// load path from a GXP file
						mapLayers.showGPXFileLayer(mapView);
					}else if(standardId == R.string.context_menu_item_save_plan){	// save path as a GXP file
						progressDlg = ProgressDialog.show(context, context.getString(R.string.saving_gpx_tracks), context.getString(R.string.saving_gpx_tracks), true);
						final ProgressDialogImplementation impl = new ProgressDialogImplementation(progressDlg);
						requestFileName();

						impl.setRunnable("SavingGPX", new Runnable() { //$NON-NLS-1$
								@Override
								public void run() {
									String warn = "";
									try {
										GPXFile gpx = new GPXFile();
										for (int i = 0;i < measurementPoints.size(); i++){
											WptPt pt = new WptPt();
											
											pt.lat = measurementPoints.get(i).getLatitude();
											pt.lon = measurementPoints.get(i).getLongitude();
											pt.ele = 0;
											pt.hdop = 0;
											pt.name ="";
											pt.speed = 0;
											pt.time = 0;
											pt.desc = "";
											gpx.points.add(pt);												
										}
										List<String> warnings = new ArrayList<String>();
										String fileName = "";
										File dir = OsmandSettings.getOsmandSettings(context).getExternalStorageDirectory();
										if (dir.canWrite()) {
											dir = new File(dir, ResourceManager.GPX_PATH);
											dir.mkdirs();
											if (dir.exists()) {

												// save file
												final String f = context.getString(R.string.plan_file_name_prefix);
												File fout = new File(dir, f + ".gpx"); //$NON-NLS-1$
												if (measurementPoints.size() > 0) {
													if(requestedName != ""){
														fileName = f + requestedName + ".gpx"; //$NON-NLS-1$						
														fout = new File(dir, fileName); //$NON-NLS-1$
													}
												}

												warn = GPXUtilities.writeGpxFile(fout, gpx, context);
												if (warn != null) {
													warnings.add(warn);
													showToast(context.getString(R.string.warning_save_failed));											
												}else{												
													showToast(context.getString(R.string.warning_saved_OK) + fileName);											
												}
											}
										}
									}catch(Exception e){
										Log.e(LogUtil.TAG, "Error saving plan GPX file", e); //$NON-NLS-1$
											showToast(context.getString(R.string.warning_save_failed));											
									} finally {
										if (progressDlg != null) {
											progressDlg.dismiss();
											progressDlg = null;
										}
									}
								}
							});
						impl.run();					
					}
					mapView.refreshMap();
				}
			}
		});
		builder.create().show();
    }

    protected void showToast(final String msg){
    	mapActivity.runOnUiThread(new Runnable(){
			@Override
			public void run() {
				Toast.makeText(mapActivity, msg, Toast.LENGTH_LONG).show();
			}
    	});
    }
    
    protected void requestFileName(){
    	String name = "Enter File Name";
     	Builder builder = new AlertDialog.Builder(mapActivity);
		builder.setTitle(name);
		FrameLayout parent = new FrameLayout(mapActivity);
		final EditText editText = new EditText(mapActivity);
		editText.setId(R.id.TextView);
		parent.setPadding(15, 0, 15, 0);
		parent.addView(editText);
		builder.setView(parent);
		builder.setNegativeButton(R.string.default_buttons_cancel, null);
		builder.setPositiveButton(R.string.default_buttons_ok, new DialogInterface.OnClickListener() {
			@Override
			public  void onClick(DialogInterface dialog, int which) {
				String fileName = editText.getText().toString();
				requestedName =  fileName;
				}
		});
		builder.show();
    }
    public void showGPXPlan(GPXFile data){
		LatLon latLon = null;
		if(data != null){
	    	measurementPoints.clear();
	    	for (int i = 0; i < data.points.size(); i++){
	    		latLon = new LatLon(data.points.get(i).lat, data.points.get(i).lon);
	    		measurementPoints.add(latLon);
	    	}
			measureDistanceMode = true;
			colourChangeIndex = measurementPoints.size() - 1;
			selectedMeasurementPointIndex = colourChangeIndex;
			mapLayers.getPlanningLayer().displayIntermediatePointInfo(colourChangeIndex);
		}
    }

	public void setCumMeasuredDistance(float dist){	//distance measured
		cumMeasuredDistance=dist;
	}

	public float getCumMeasuredDistance(){	//access distance measured
		return cumMeasuredDistance;
	}

	public LatLon getScreenPointLatLon(){	//accessor for local point lat lon storage
		return screenPointLatLon;
	}
	
	public void setMeasurementPointRadius(int radius){	//change measurement point display size
		measurementPointRadius = radius;
	}
	
	public int getMeasurementPointRadius(){	//access measurement point display size
		return measurementPointRadius;
	}
	
	public void setMeasurementPointSelectionRadius(int radius){	//change measurement point display size
		measurementPointSelectionRadius = radius;
	}
	
	public int getMeasurementPointSelectionRadius(){	//access measurement point selection area size
		return measurementPointSelectionRadius;
	}
	
	public void setScreenPointLatLon(LatLon point) {	//Save current screen point values
		screenPointLatLon = point;
	}
        
	public void saveScreenPoint() {	//Save current screen point
		screenPointLatLon = measurementPoints.get(selectedMeasurementPointIndex);
	}
        
	public boolean checkMeasurementPointSize(int radius){	//check if proposed change to measurement point display size is within defined range
		if(radius <= maxMeasurementPointRadius && radius >= minMeasurementPointRadius){
			return true;
		}else{
			return false;
		}
	}
	
	public boolean checkMeasurementPointSelectionSize(int radius){	//Check if proposed change to measurement point selection area is within defined range
		if(radius <= maxMeasurementPointSelectionRadius && radius >= minMeasurementPointSelectionRadius){
			return true;
		}else{
			return false;
		}
	}

	public int isMeasurementPointSelected(PointF point){	//test if point on map is a point in measurement set
		int locationX = 0;
		int locationY = 0;
		int index = -1;	//indicates no match found
		int size = measurementPoints.size();
		if(size > 0){
			for (int i = 0;i < size; i++){
				locationX = mapView.getMapXForPoint(measurementPoints.get(i).getLongitude());
				locationY = mapView.getMapYForPoint(measurementPoints.get(i).getLatitude());
				if(Math.abs(locationX - point.x) < measurementPointSelectionRadius &&
						Math.abs(locationY - point.y) < measurementPointSelectionRadius){
					index = i;	//a point has been found in the detection area
					break;
				}
			}
		}
		return index;
	}
	
	public boolean dragMeasurementPoint(MotionEvent e1){
		if (!measureDistanceMode) return false;	//Check if a measurement point has been selected
		if(scrollingFlag) return true;	//delay activity until scrolling has finished
			PointF point = new PointF(e1.getX(), e1.getY());
			if(isMeasurementPointSelected(point) >= 0){
				selectedMeasurementPointIndex = isMeasurementPointSelected(point);	//save index of point selected	
				scrollingFlag = true;
				return true;
			}
			return false;
	}
	
	public int getSelectedMeasurementPointIndex(){	//Save measurement point selected
		return selectedMeasurementPointIndex;
	}
	
	public void setSelectedMeasurementPointIndex(int index){	//Access selected measurement point index
		selectedMeasurementPointIndex = index;
	}
	
	public void setLongInfoFlag(boolean status){	//Support distance measuring info length selection
		longInfoFlag = status;
	}

	public boolean getLongInfoFlag(){	//Support distance measuring info length selection
		return longInfoFlag;
	}

	public int getColourChangeIndex(){	//Indicate measurement point where display colour should change
		return colourChangeIndex;
	}
	
	public void setColourChangeIndex(int index){	//indicate measurement point where display colour should change
		colourChangeIndex = index;
	}
	
	public boolean getMeasureDistanceMode() {	//to check for distance measuring mode
		return measureDistanceMode;
	}

	public void setMeasureDistanceMode(boolean state) {	//Set distance measuring mode
		measureDistanceMode=state;
		measurementPointInsertionIndex = -1;	//clear insertion point index
	}

	public int getMeasurementPointInsertionIndex() {	//Access distance measurement point insertion
		return measurementPointInsertionIndex;
	}

	public void setMeasurementPointInsertionIndex(int index) {	//Set distance measurement point insertion
		measurementPointInsertionIndex = index;
	}

	public void setScrollingFlag(boolean status){	//Support distance measuring point dragging
		scrollingFlag = status;
	}

	public boolean getScrollingFlag(){	//Support distance measuring point dragging
		return scrollingFlag;
	}
}
