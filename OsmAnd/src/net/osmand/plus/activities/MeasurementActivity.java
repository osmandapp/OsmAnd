package net.osmand.plus.activities;

import java.util.ArrayList;
import java.util.List;
import net.osmand.plus.R;
import net.osmand.plus.views.OsmandMapTileView;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.res.Resources;

// Provide context menus and callbacks for Measurement Mode

public class MeasurementActivity{

	/** Called when the activity is first created. */
	OsmandMapTileView mapView;
	
	private MapActivity mapActivity;

	private MapActivityLayers mapLayers;	// = new MapActivityLayers(mapActivity);

	// constructor
	public MeasurementActivity(MapActivity mapActivity){
		this.mapActivity = mapActivity;
	}

    public void createMeasurementMenu(int menuId){
		mapView = (OsmandMapTileView) mapActivity.findViewById(R.id.MapView);
		mapLayers=mapActivity.getMapLayers();
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
    			R.string.context_menu_item_end_measurement,
    			R.string.context_menu_item_increase_point_size,
    			R.string.context_menu_item_decrease_point_size,
    			R.string.context_menu_item_increase_point_selection_area,
    			R.string.context_menu_item_decrease_point_selection_area,
    	};	
    	//Measurement point text box clicked menu options
    	final int[] measurementPointTextBoxSelectedMenuActions = new int[]{
    			R.string.context_menu_item_clear_all_points,
    			R.string.context_menu_item_end_measurement,
    			R.string.context_menu_item_cancel,
    			R.string.context_menu_item_increase_point_size,
    			R.string.context_menu_item_decrease_point_size,
    			R.string.context_menu_item_increase_point_selection_area,
    			R.string.context_menu_item_decrease_point_selection_area,
    	};

    	int actionsToUse = 0;
    	if(mapView.getSelectedMeasurementPointIndex() >= 0){	//Select different menu items for measurement mode
    		actionsToUse =  measurementPointSelectedMenuActions.length;	//measurement point selected items;
        	for(int j = 0; j < actionsToUse; j++){
        		actions.add(resources.getString(measurementPointSelectedMenuActions[j]));
        	}
        }else if(mapView.getSelectedMeasurementPointIndex() < 0){
    		actionsToUse =  measurementPointTextBoxSelectedMenuActions.length;	//measurement point text box selected items
        	for(int j = 0; j < actionsToUse; j++){
        		actions.add(resources.getString(measurementPointTextBoxSelectedMenuActions[j]));
        	}
        }else{
    	}
    	
    	builder.setItems(actions.toArray(new String[actions.size()]), new DialogInterface.OnClickListener(){

			@Override
			public void onClick(DialogInterface dialog, int which) {
	// Measurement point menu callbacks
				int standardId = 0;
				int index = 0;
				if(mapView.getMeasureDistanceMode()){
					mapLayers.getContextMenuLayer().setNewPointFlag(false);	//block new measurement point creation
					if(mapView.getSelectedMeasurementPointIndex() >= 0){
						standardId = measurementPointSelectedMenuActions[which];	//items common to both measurement point dialogs
					}else{
						standardId = measurementPointTextBoxSelectedMenuActions[which];	//items common to both measurement point dialogs					
					}
					index = mapView.getSelectedMeasurementPointIndex();
					if(standardId == R.string.context_menu_item_delete_point){	//test if current point is to be removed
						if(index >= 0){
							mapView.measurementPoints.remove(index);	//delete the point if one has been selected
							mapView.setSelectedMeasurementPointIndex(-1);
							mapLayers.getContextMenuLayer().setLocation(null, "");	//delete the point info text box
						}
					}else if(standardId == R.string.context_menu_item_clear_all_points){	//test if all measurement points should be removed
						mapView.setSelectedMeasurementPointIndex(-1);
						mapView.measurementPoints.clear();	//remove all points from the list
						mapLayers.getContextMenuLayer().setLocation(null, "");	//delete the point info text box
					}else if(standardId == R.string.context_menu_item_insert_point){	//test if extra measurement point is to be inserted
						if(index >= 0){
							mapView.setColourChangeIndex(index);	//point colour should change after this point
							mapView.setMeasurementPointInsertionIndex(index);	//indicates an insertion should be performed before this point
							mapLayers.getContextMenuLayer().displayIntermediatePointInfo(index);	//use the latLon for the point, not the menu click
						}
					}else if(standardId == R.string.context_menu_item_cancel){	// close dialog with no action
					}else if(standardId == R.string.context_menu_item_display_point_info){
						mapView.setLongInfoFlag(true);
						if(index >=0){
							mapLayers.getContextMenuLayer().displayIntermediatePointInfo(index);	//use the latLon for the point, not the menu click
							mapView.setColourChangeIndex(index);	//point colour should change after this point
						}else{
							mapLayers.getContextMenuLayer().setLocation(null, "");	//delete the point info text box
						}
					}else if(standardId == R.string.context_menu_item_display_distance_only){
						mapView.setLongInfoFlag(false);
						if(index >=0){
							mapLayers.getContextMenuLayer().displayIntermediatePointInfo(index);	//use the latLon for the point, not the menu click
							mapView.setColourChangeIndex(index);	//point colour should change after this point
						}else{
							mapLayers.getContextMenuLayer().setLocation(null, "");	//delete the point info text box
						}
					}else if(standardId == R.string.context_menu_item_end_measurement){	// End measurement mode
						mapView.setMeasureDistanceMode(false);	//turn off measurement mode						
						mapView.measurementPoints.clear();	//remove all points from the list
						mapLayers.getContextMenuLayer().setLocation(null, "");	//delete the point info text box
					}else if(standardId == R.string.context_menu_item_increase_point_size){	//change measurement point display size
						if(mapView.checkMeasurementPointSize(mapView.getMeasurementPointRadius() + 2)) {
							mapView.setMeasurementPointRadius(mapView.getMeasurementPointRadius() + 2);
						}
					}else if(standardId == R.string.context_menu_item_decrease_point_size){	//change measurement point display size
						if(mapView.checkMeasurementPointSize(mapView.getMeasurementPointRadius() - 2)) {
							mapView.setMeasurementPointRadius(mapView.getMeasurementPointRadius() - 2);
						}
					}else if(standardId == R.string.context_menu_item_increase_point_selection_area){	//change measurement point display size
						if(mapView.checkMeasurementPointSelectionSize(mapView.getMeasurementPointSelectionRadius() + 1)) {
							mapView.setMeasurementPointSelectionRadius(mapView.getMeasurementPointSelectionRadius() + 1);
						}
					}else if(standardId == R.string.context_menu_item_decrease_point_selection_area){	//change measurement point display size
						if(mapView.checkMeasurementPointSelectionSize(mapView.getMeasurementPointSelectionRadius() - 1)) {
							mapView.setMeasurementPointSelectionRadius(mapView.getMeasurementPointSelectionRadius() - 1);
						}
					}
					mapView.refreshMap();
				}
			}
		});
		builder.create().show();
    }
        
}
