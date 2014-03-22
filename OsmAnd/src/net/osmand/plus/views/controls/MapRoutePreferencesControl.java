package net.osmand.plus.views.controls;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.osmand.CallbackWithObject;
import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.GPXUtilities;
import net.osmand.plus.GPXUtilities.GPXFile;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.OsmandSettings.CommonPreference;
import net.osmand.plus.R;
import net.osmand.plus.TargetPointsHelper;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.SettingsBaseActivity;
import net.osmand.plus.activities.SettingsNavigationActivity;
import net.osmand.plus.activities.actions.AppModeDialog;
import net.osmand.plus.helpers.GpxUiHelper;
import net.osmand.plus.routing.RouteProvider.GPXRouteParamsBuilder;
import net.osmand.plus.views.OsmandMapLayer.DrawSettings;
import net.osmand.router.GeneralRouter;
import net.osmand.router.GeneralRouter.RoutingParameter;
import net.osmand.router.GeneralRouter.RoutingParameterType;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.graphics.Canvas;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

public class MapRoutePreferencesControl extends MapControls {
	private ImageButton settingsAppModeButton;
	private OsmandSettings settings;
	private int cachedId;
	private Dialog dialog;
	private ArrayAdapter<LocalRoutingParameter> listAdapter;
	
	public MapRoutePreferencesControl(MapActivity mapActivity, Handler showUIHandler, float scaleCoefficient) {
		super(mapActivity, showUIHandler, scaleCoefficient);
		settings = mapActivity.getMyApplication().getSettings();
	}
	
	private static class LocalRoutingParameter {
		
		public RoutingParameter routingParameter;

		public String getText(MapActivity mapActivity) {
			return SettingsBaseActivity.getRoutingStringPropertyName(mapActivity, routingParameter.getId(), routingParameter.getName());
		}

		public boolean isSelected(OsmandSettings settings) {
			final CommonPreference<Boolean> property = settings.getCustomRoutingBooleanProperty(routingParameter.getId());
			return property.get();
		}

		public void setSelected(OsmandSettings settings, boolean isChecked) {
			final CommonPreference<Boolean> property = settings.getCustomRoutingBooleanProperty(routingParameter.getId());
			property.set(isChecked);			
		}
		
	}
	
	private static class GPXLocalRoutingParameter extends LocalRoutingParameter {
		public String text;
		public boolean selected;
		public int id;
		
		public GPXLocalRoutingParameter(int id, String text, boolean selected) {
			this.text = text;
			this.selected = selected;
			this.id = id;
		}
		
		@Override
		public String getText(MapActivity mapActivity) {
			return text;
		}
		
		@Override
		public boolean isSelected(OsmandSettings settings) {
			return selected;
		}
		
		@Override
		public void setSelected(OsmandSettings settings, boolean isChecked) {
			selected = isChecked;
		}
	}
	
	@Override
	public void showControls(FrameLayout parent) {
		settingsAppModeButton = addImageButton(parent, R.string.route_preferences, R.drawable.map_btn_plain);
		cachedId = 0;
		settingsAppModeButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if(dialog != null) {
					dialog.hide();
					dialog = null;
					settingsAppModeButton.setBackgroundResource(R.drawable.map_btn_plain);
				} else {
					dialog = showDialog();
					dialog.show();
					settingsAppModeButton.setBackgroundResource(R.drawable.map_btn_plain_p);
					dialog.setOnDismissListener(new OnDismissListener() {
						@Override
						public void onDismiss(DialogInterface dlg) {
							settingsAppModeButton.setBackgroundResource(R.drawable.map_btn_plain);
							dialog = null;
						}
					});
				}
			}
		});
	}
	
	private Dialog showDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mapActivity);        
        View ll = createLayout();
        builder.setView(ll);
        //builder.setTitle(R.string.route_preferences);
        Dialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(true);
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(dialog.getWindow().getAttributes());
        lp.width = WindowManager.LayoutParams.WRAP_CONTENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        lp.gravity = Gravity.BOTTOM;
        lp.y = (int) (settingsAppModeButton.getBottom() - settingsAppModeButton.getTop() + scaleCoefficient * 5); 
        dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setAttributes(lp);
        return dialog;
	}
	
	
	private void updateGpxRoutingParameter(GPXLocalRoutingParameter gpxParam) {
		GPXRouteParamsBuilder rp = mapActivity.getRoutingHelper().getCurrentGPXRoute();
		if (rp != null) {
			boolean selected = gpxParam.isSelected(settings);
			if (gpxParam.id == R.string.gpx_option_reverse_route) {
				rp.setReverse(selected);
			} else if (gpxParam.id == R.string.gpx_option_from_start_point) {
				rp.setPassWholeRoute(selected);
			} else if (gpxParam.id == R.string.announce_gpx_waypoints) {
				settings.SPEAK_GPX_WPT.set(selected);
				rp.setAnnounceWaypoints(selected);
			} else if (gpxParam.id == R.string.calculate_osmand_route_gpx) {
				settings.CALC_GPX_ROUTE.set(selected);
				rp.setCalculateOsmAndRoute(selected);
				updateParameters();
			}
		}
	}

	
	private List<LocalRoutingParameter> getRoutingParameters(ApplicationMode am) {
		List<LocalRoutingParameter> list = new ArrayList<LocalRoutingParameter>();
		GeneralRouter rm = SettingsNavigationActivity.getRouter(mapActivity.getMyApplication().getDefaultRoutingConfig(), am);
		GPXRouteParamsBuilder rparams = mapActivity.getRoutingHelper().getCurrentGPXRoute();
		if(rparams != null) {
			list.add(new GPXLocalRoutingParameter(R.string.gpx_option_reverse_route, 
					getString(R.string.gpx_option_reverse_route), rparams.isReverse()));
			list.add(new GPXLocalRoutingParameter(R.string.gpx_option_from_start_point, 
					getString(R.string.gpx_option_from_start_point), rparams.isPassWholeRoute()));
			list.add(new GPXLocalRoutingParameter(R.string.announce_gpx_waypoints, 
					getString(R.string.announce_gpx_waypoints), rparams.isAnnounceWaypoints()));
			list.add(new GPXLocalRoutingParameter(R.string.calculate_osmand_route_gpx, 
					getString(R.string.calculate_osmand_route_gpx), rparams.isCalculateOsmAndRoute()));
		}
		if(rm == null || (rparams != null && !rparams.isCalculateOsmAndRoute())) {
			return list;
		}
		for (RoutingParameter r : rm.getParameters().values()) {
			if (r.getType() == RoutingParameterType.BOOLEAN) {
				LocalRoutingParameter rp = new LocalRoutingParameter();
				rp.routingParameter = r;
				list.add(rp);
			}
		}
		return list;
	}
	private String getString(int id) {
		return mapActivity.getString(id);
	}

	private View createLayout() {
		View settingsDlg = View.inflate(mapActivity, R.layout.plan_route_settings, null);
		Context ctx = mapActivity;
		final OsmandSettings settings = mapActivity.getMyApplication().getSettings();
		ApplicationMode am = settings.APPLICATION_MODE.get();
		final ListView lv = (ListView) settingsDlg.findViewById(android.R.id.list);
		final Set<ApplicationMode> selected = new HashSet<ApplicationMode>();
		selected.add(am);
		
		setupSpinner(settingsDlg);
		
		
		listAdapter = new ArrayAdapter<LocalRoutingParameter>(ctx, 
				R.layout.layers_list_activity_item, R.id.title, getRoutingParameters(am)) {
			@Override
			public View getView(final int position, View convertView, ViewGroup parent) {
				View v = mapActivity.getLayoutInflater().inflate(R.layout.layers_list_activity_item, null);
				final TextView tv = (TextView) v.findViewById(R.id.title);
				final CheckBox ch = ((CheckBox) v.findViewById(R.id.check_item));
				final LocalRoutingParameter rp = getItem(position);
				tv.setText(rp.getText(mapActivity));
				tv.setPadding((int) (5 * scaleCoefficient), 0, 0, 0);
				ch.setChecked(rp.isSelected(settings));
				ch.setVisibility(View.VISIBLE);
				ch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
						rp.setSelected(settings, isChecked);
						if(rp instanceof GPXLocalRoutingParameter) {
							updateGpxRoutingParameter((GPXLocalRoutingParameter) rp);
						}
						mapActivity.getRoutingHelper().recalculateRouteDueToSettingsChange();
					}
				});
				return v;
			}
		};
		
		AppModeDialog.prepareAppModeView(mapActivity, selected, false, 
				(ViewGroup) settingsDlg.findViewById(R.id.TopBar), true, 
				new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if(selected.size() > 0) {
					ApplicationMode next = selected.iterator().next();
					settings.APPLICATION_MODE.set(next);
					mapActivity.getRoutingHelper().recalculateRouteDueToSettingsChange();
					updateParameters();
				}
			}

			
		});
		lv.setAdapter(listAdapter);
		return settingsDlg;
	}
	
	private void updateParameters() {
		ApplicationMode am = settings.APPLICATION_MODE.get();
		listAdapter.setNotifyOnChange(false);
		listAdapter.clear();
		for(LocalRoutingParameter r : getRoutingParameters(am)) {
			listAdapter.add(r);
		}
		listAdapter.notifyDataSetChanged();
	}

	private void setupSpinner(View settingsDlg) {
		final Spinner gpxSpinner = (Spinner) settingsDlg.findViewById(R.id.GPXRouteSpinner);
		updateSpinnerItems(gpxSpinner);
		gpxSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				if(position == 0) {
					mapActivity.getRoutingHelper().setGpxParams(null);
					mapActivity.getRoutingHelper().recalculateRouteDueToSettingsChange();
					updateParameters();
				} else if(position == 1) {
					openGPXFileSelection(gpxSpinner);
				} else if(position == 2) {
					// nothing to change 
				}				
				
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});
	}

	protected void openGPXFileSelection(final Spinner gpxSpinner) {
		GpxUiHelper.selectGPXFile(mapActivity, false, false, new CallbackWithObject<GPXUtilities.GPXFile>() {
			
			@Override
			public boolean processResult(GPXFile result) {
				GPXRouteParamsBuilder params = new GPXRouteParamsBuilder(result, mapActivity.getMyApplication().getSettings());
				params.setAnnounceWaypoints(settings.SPEAK_GPX_WPT.get());
				params.setCalculateOsmAndRoute(settings.CALC_GPX_ROUTE.get());
				List<Location> ps = params.getPoints();
				
				if(!ps.isEmpty()) {
					Location loc = ps.get(ps.size() -1);
					TargetPointsHelper tg = mapActivity.getMyApplication().getTargetPointsHelper();	
					tg.navigateToPoint(new LatLon(loc.getLatitude(), loc.getLongitude()), false, -1);
					if(tg.getPointToStart() == null) {
						loc = ps.get(0);
						tg.setStartPoint(new LatLon(loc.getLatitude(), loc.getLongitude()), false, null);
					}
				}
				mapActivity.getRoutingHelper().setGpxParams(params);
				mapActivity.getRoutingHelper().recalculateRouteDueToSettingsChange();
				updateSpinnerItems(gpxSpinner);
				updateParameters();
				return true;
			}
		});
	}

	private void updateSpinnerItems(Spinner gpxSpinner) {
		ArrayList<String> gpxActions = new ArrayList<String>();
		gpxActions.add(mapActivity.getString(R.string.default_none));
		gpxActions.add(mapActivity.getString(R.string.select_gpx));
		GPXRouteParamsBuilder rp = mapActivity.getRoutingHelper().getCurrentGPXRoute();
		if(rp != null) {
			gpxActions.add(new File(rp.getFile().path).getName());
		}
		
		ArrayAdapter<String> gpxAdapter = new ArrayAdapter<String>(mapActivity, 
				android.R.layout.simple_spinner_item, 
				gpxActions
				);
		gpxAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		gpxSpinner.setAdapter(gpxAdapter);
		if(rp != null) {
			gpxSpinner.setSelection(2);
		} else {
			gpxSpinner.setSelection(0);
		}
	}

	@Override
	public void hideControls(FrameLayout layout) {
		removeButton(layout, settingsAppModeButton);
		layout.removeView(settingsAppModeButton);
		mapActivity.accessibleContent.remove(settingsAppModeButton);
	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings nightMode) {
		int id = settings.getApplicationMode().getSmallIcon(false); // settingsAppModeButton.isPressed() || dialog != null
		if(cachedId != id && settingsAppModeButton.getLeft() > 0) {
			cachedId = id;
			settingsAppModeButton.setImageResource(id);
		}
	}
}
