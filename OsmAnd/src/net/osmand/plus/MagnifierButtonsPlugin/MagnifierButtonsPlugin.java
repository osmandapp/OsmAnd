package net.osmand.plus.MagnifierButtonsPlugin;

import gnu.trove.list.array.TIntArrayList;
import java.util.ArrayList;
import java.util.List;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.FrameLayout.LayoutParams;
import android.view.Gravity;
import android.view.View;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import net.osmand.map.IMapLocationListener;
import net.osmand.plus.views.BaseMapLayer;
import net.osmand.plus.views.MapControlsLayer;
import net.osmand.plus.views.OsmandMapLayer;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.distancecalculator.DistanceCalculatorPlugin.DistanceCalculatorLayer;
import net.osmand.plus.distancecalculator.DistanceCalculatorPlugin;

/**
 * Plugin used to set up display zoom control buttons on the view.
 * Overrides some of the MapControlLayer buttons, thus allowing the 
 * plugin to be independent of the existing codebase.
 */
public class MagnifierButtonsPlugin extends OsmandPlugin{
	private static final String ID = "osmand.displayZoom";
	private Button displayZoomInButton;
	private Button displayZoomOutButton;
	private TextView displayZoomInfo;
	private OsmandApplication application;
	private MapActivity mapActivity;
	public DistanceCalculatorLayer distanceCalculatorLayer;
	public DistanceCalculatorPlugin distanceCalculatorPlugin;
	private BaseMapLayer mainLayer;
	private List<OsmandMapLayer> layers = new ArrayList<OsmandMapLayer>();
	final int[] percent = {75, 100, 150, 200, 300, 400};
	Float[] options = {0f, 0f, 0f, 0f, 0f, 0f};
	private OsmandMapTileView view;
	private MapControlsLayer mapControlsLayer;
	public IMapLocationListener locationListener;
	private int zoomLevel = 1;
	private boolean pluginEnabled = false;
	public final int ZOOM_IN_BUTTON_ID = 1000;
	public final int ZOOM_OUT_BUTTON_ID = 1001;
	public final int DISPLAY_ZOOM_IN_BUTTON_ID = 2000;
	public final int DISPLAY_ZOOM_OUT_BUTTON_ID = 2001;
	
	public MagnifierButtonsPlugin(OsmandApplication app){
		this.application = app;
		double fraction = 0f;
		for (int j = 0; j < percent.length; j++){
			if (percent[j] >= 100f) {
				fraction = Math.sqrt((percent[j] - 100f) / 100f);
			} else {
				fraction = - Math.sqrt((100f - percent[j]) / 100f);
			}
			options[j] = (float)fraction;
		}
	}
	@Override
	public void registerLayers(MapActivity activity) {
		view = activity.getMapView();
		int mapControlsLayerNumber = 0;
		for (mapControlsLayerNumber = 0; mapControlsLayerNumber < activity.getMapView().getLayers().size(); mapControlsLayerNumber++) {
			layers = activity.getMapView().getLayers();
			if (layers.get(mapControlsLayerNumber) instanceof MapControlsLayer) {
				mapControlsLayer = (MapControlsLayer)layers.get(mapControlsLayerNumber);
				break;
			}
		}
		initDisplayZoomButtons(activity.getMapView());		
		mapActivity = activity;
		mainLayer = mapControlsLayer.activity.getMapView().getMainLayer();
	}

	@Override
	public String getId() {
		return ID;
	}
	
	@Override
	public boolean init(OsmandApplication app) {
		pluginEnabled = true;	//if turning plugin back on, make buttons visible and active
		if(displayZoomInButton != null){
			displayZoomInButton.setEnabled(true);
			displayZoomInButton.setAlpha(1);
			displayZoomOutButton.setEnabled(true);
			displayZoomOutButton.setAlpha(1);
			displayZoomOutButton.setEnabled(true);
			displayZoomInfo.setAlpha(1);
			updateZoomScaleTextbox(false);
		}
		return true;
	}

	@Override
	public void disable(OsmandApplication app) {
// Hide extra buttons and text and make non-responsive to user inputs
		pluginEnabled = false;
		if(displayZoomInButton != null){
			displayZoomInButton.setEnabled(false);
			displayZoomInButton.setAlpha(0);
			displayZoomOutButton.setEnabled(false);
			displayZoomOutButton.setAlpha(0);
			displayZoomOutButton.setEnabled(false);
			displayZoomInfo.setAlpha(0);
		}
	}

	@Override
	public String getDescription() {
		return application.getString(R.string.display_zoom_plugin_description);
	}

	@Override
	public String getName() {
		return application.getString(R.string.display_zoom_plugin_name);
	}
	
	public void initDisplayZoomButtons(final OsmandMapTileView view) {
		FrameLayout parent = (FrameLayout)view.getParent();
		int minimumWidth = view.getResources().getDrawable(R.drawable.map_scale_up_button).getMinimumWidth();		
		android.widget.FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT,
					Gravity.BOTTOM);
		params.setMargins(0, 0, 0, 0);
		displayZoomInButton = new Button(view.getContext());
		displayZoomInButton.setBackgroundResource(R.drawable.map_scale_up_button);
		displayZoomInButton.getBackground().setAlpha(120);
		params = new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
					Gravity.LEFT);
		params.setMargins(0, 4*minimumWidth, 0, 0);
		parent.addView(displayZoomInButton, params);	
		displayZoomOutButton = new Button(view.getContext());
		displayZoomOutButton.setBackgroundResource(R.drawable.map_scale_down_button);
		displayZoomOutButton.getBackground().setAlpha(120);
		displayZoomInButton.setId(DISPLAY_ZOOM_IN_BUTTON_ID);
		displayZoomOutButton.setId(DISPLAY_ZOOM_OUT_BUTTON_ID);
		params = new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
					Gravity.LEFT);
		params.setMargins(0, 5*minimumWidth , 0, 0);
		parent.addView(displayZoomOutButton, params);

		displayZoomInButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				 setDisplayScaleFactor(true);
				if(getDistanceCalculatorLayer()){
					distanceCalculatorLayer.updateTextSize();
				}
				view.refreshMap(true);
				updateZoomScaleTextbox(false);
			}
		});		
		displayZoomOutButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				 setDisplayScaleFactor(false);
				if(getDistanceCalculatorLayer()){
					distanceCalculatorLayer.updateTextSize();
				}
				view.refreshMap(true);
				updateZoomScaleTextbox(false);
			}
		});	
		
		final View.OnClickListener listener = getOnClickMagnifierListener(view);
		final View.OnLongClickListener longClickListener = getOnLongClickMagnifierListener(view);
		displayZoomOutButton.setOnLongClickListener(longClickListener);
		displayZoomInButton.setOnLongClickListener(longClickListener);
		mapControlsLayer.getzoomInButton().setOnClickListener(listener);
		mapControlsLayer.getzoomInButton().setOnLongClickListener(longClickListener);
		mapControlsLayer.getzoomOutButton().setOnClickListener(listener);
		mapControlsLayer.getzoomOutButton().setOnLongClickListener(longClickListener);
		mapControlsLayer.getzoomInButton().setId(ZOOM_IN_BUTTON_ID);
		mapControlsLayer.getzoomOutButton().setId(ZOOM_OUT_BUTTON_ID);

		displayZoomInfo = new TextView(view.getContext());
		LayoutParams lp = new LayoutParams(80, 200);
		displayZoomInfo.setLayoutParams(lp);
		displayZoomInfo.setTextColor(Color.BLACK);
		displayZoomInfo.setTextSize(22f);
		displayZoomInfo.setMinLines(2);
		displayZoomInfo.setGravity(Gravity.CENTER_HORIZONTAL);
		params = new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
					Gravity.LEFT);
		params.setMargins(10, 6*minimumWidth , 0, 0);
		parent.addView(displayZoomInfo, params);
		zoomLevel = view.getSettings().getLastKnownMapZoom();
		updateZoomScaleTextbox(true);
	}
	
	final View.OnLongClickListener getOnLongClickMagnifierListener(final OsmandMapTileView mapView) {
		final View.OnLongClickListener longClickListener = new View.OnLongClickListener() {

			@Override
			public boolean onLongClick(View v) {
// This overrides the MaControlLayer use of long clicks to open magnification menu, Ideally use of long
// click on MapControlsLayer is changed to open map layer selection dialog.
				if(pluginEnabled){
					if (v.getId() == DISPLAY_ZOOM_IN_BUTTON_ID || v.getId() == DISPLAY_ZOOM_OUT_BUTTON_ID){
						showDialog(mapActivity);
					}
					if(v.getId() == ZOOM_IN_BUTTON_ID || v.getId() == ZOOM_OUT_BUTTON_ID){
						showZoomLayerSelector();
					}
				}else{
					showDialog(mapActivity);
				}
			return true;
			}
		};
		return longClickListener;
	}
	final View.OnClickListener getOnClickMagnifierListener(final OsmandMapTileView mapView) {
		final View.OnClickListener listener = new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				if (v.getId() == ZOOM_IN_BUTTON_ID) {
					zoomLevel = view.getZoom() + (view.getZoom() <  mainLayer.getMaximumShownMapZoom() ? 1 : 0);
					mapActivity.changeZoom(1);
				} else if (v.getId() ==ZOOM_OUT_BUTTON_ID){
					zoomLevel = view.getZoom() - (view.getZoom() >  1 ? 1 : 0);
					mapActivity.changeZoom(-1);
				}else{
					
				}
				updateZoomScaleTextbox(true);
			}
		};	
		return listener;
	}
	
	public void setDisplayScaleFactor(boolean magnify) {
		int index = magnificationFactorToPercent();
		if(index < 0) return;
		if(index == 0 && !magnify) return;
		if(index >= options.length - 1 && magnify) return;
		if (magnify){
			index++;
		}else{
			index--;
		}
		application.getSettings().MAP_ZOOM_SCALE_BY_DENSITY.set(options[index]);
		view.getAnimatedDraggingThread().startZooming(view.getZoom(), options[index], false);
		updateZoomScaleTextbox(false);
	}
	
	public void updateZoomScaleTextbox(boolean changePending){
		String magnification = null;
		int index = magnificationFactorToPercent();
		if(index >= 0) magnification = Integer.toString(percent[index]) + "%";
		String zoom = Integer.toString(view.getZoom());
		if(changePending) zoom = Integer.toString(zoomLevel);	//set target level, not current level
		displayZoomInfo.setText(magnification + "\nL" + zoom);
		displayZoomInfo.layout(0, 0,100,80);
	}
	
	public int magnificationFactorToPercent(){
		int index = 0;
		float p = application.getSettings().MAP_ZOOM_SCALE_BY_DENSITY.get();
		if(p < -0.10){
			index = 0;
		}else if(p  >+ -0.1 && p < 0.2){
			index = 1;
		}else if(p  >+ 0.5 && p < 0.8){
			index = 2;
		}else if(p  >+ 0.9 && p < 1.1){
			index = 3;
		}else if(p  >+ 1.3 && p < 1.6){
			index = 4;
		}else if(p  > 1.6){
			index = 5;
		}else{
			index = -1;		
		}
		if(index == -1) return -1;
		return index;
	}
	
	private void showDialog(final MapActivity activity) {
		final OsmandSettings.OsmandPreference<Float> zoomScale = activity.getMapView().getSettings().MAP_ZOOM_SCALE_BY_DENSITY;
		final AlertDialog.Builder bld = new AlertDialog.Builder(activity.getMapView().getContext());
		float scale = activity.getMapView().getZoomScale();
		int p = (int) ((scale > 0 ? 1 : -1) * Math.round(scale * scale * 100)) + 100;
		final TIntArrayList tlist = new TIntArrayList(percent);
		final List<String> values = new ArrayList<String>();
		int i = -1;
		for (int k = 0; k <= tlist.size(); k++) {
			final boolean end = k == tlist.size();
			if (i == -1) {
				if ((end || p < tlist.get(k))) {
					values.add(p + "%");
					i = k;
				} else if (p == tlist.get(k)) {
					i = k;
				}
			}
			if (k < tlist.size()) {
				values.add(tlist.get(k) + "%");
			}
		}
		if (values.size() != tlist.size()) {
			tlist.insert(i, p);
		}
	
		bld.setTitle(R.string.map_magnifier);
		bld.setSingleChoiceItems(values.toArray(new String[values.size()]), i,
				new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				int p = tlist.get(which);
				float newScale;
				if (p >= 100) {
					newScale = (float) Math.sqrt((tlist.get(which) - 100f) / 100f);
				} else {
					newScale = -(float) Math.sqrt((100f - tlist.get(which)) / 100f);
				}
				if(getDistanceCalculatorLayer()){
					distanceCalculatorLayer.updateTextSize();
				}
				zoomScale.set(newScale - (float) Math.sqrt(Math.max(activity.getMapView().getDensity() - 1, 0)));
				activity.getMapView().getAnimatedDraggingThread().startZooming(activity.getMapView().getZoom(),
						activity.getMapView().getSettingsZoomScale(), false);
				dialog.dismiss();
				updateZoomScaleTextbox(false);
				distanceCalculatorLayer.updateTextSize();
			}
		});
		bld.show();
	}
	
	public void showZoomLayerSelector() {
		final AlertDialog.Builder bld = new AlertDialog.Builder(mapActivity.getMapView().getContext());
		bld.setTitle(R.string.map_zoom_level);
		String[] items = new String[mainLayer.getMaximumShownMapZoom()];
		for (int i = 0; i <= mainLayer.getMaximumShownMapZoom() - mainLayer.getMinimumShownMapZoom(); i++) {
			items[i] = Integer.toString(i + mainLayer.getMinimumShownMapZoom());
		}
		bld.setSingleChoiceItems(items, view.getZoom() - 1, new DialogInterface.OnClickListener(){
			@Override
			public void onClick(DialogInterface dialog, int which) {
				zoomLevel = which + mainLayer.getMinimumShownMapZoom();
				int delta = zoomLevel - view.getZoom();
				if(view.isZooming()) delta++;
				mapControlsLayer.activity.changeZoom(delta);
				dialog.dismiss();
				updateZoomScaleTextbox(true);
			}
		});
		bld.show();
	}

	public boolean getDistanceCalculatorLayer(){
		layers = mapActivity.getMapView().getLayers();
		for(int j = 0; j < layers.size(); j++){
			if(layers.get(j).getClass() == DistanceCalculatorLayer.class){
				distanceCalculatorLayer = (DistanceCalculatorLayer) layers.get(j);
				return true;
			}
		}
		return false;
	}	
}
