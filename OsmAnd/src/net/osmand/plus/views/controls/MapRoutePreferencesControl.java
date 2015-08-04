package net.osmand.plus.views.controls;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.osmand.CallbackWithObject;
import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.GPXUtilities;
import net.osmand.plus.GPXUtilities.GPXFile;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.OsmandSettings.CommonPreference;
import net.osmand.plus.R;
import net.osmand.plus.TargetPointsHelper;
import net.osmand.plus.TargetPointsHelper.TargetPoint;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.SettingsBaseActivity;
import net.osmand.plus.activities.SettingsNavigationActivity;
import net.osmand.plus.activities.actions.AppModeDialog;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.GpxUiHelper;
import net.osmand.plus.routing.RouteProvider.GPXRouteParamsBuilder;
import net.osmand.plus.routing.RouteProvider.RouteService;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.views.MapControlsLayer;
import net.osmand.router.GeneralRouter;
import net.osmand.router.GeneralRouter.RoutingParameter;
import net.osmand.router.GeneralRouter.RoutingParameterType;
import net.osmand.util.MapUtils;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.support.v7.widget.PopupMenu;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class MapRoutePreferencesControl {
	private OsmandSettings settings;
	private Dialog dialog;
	private ArrayAdapter<LocalRoutingParameter> listAdapter;
	private MapActivity mapActivity;
	private MapControlsLayer controlsLayer;

	public MapRoutePreferencesControl(MapActivity mapActivity, MapControlsLayer controlsLayer) {
		this.mapActivity = mapActivity;
		this.controlsLayer = controlsLayer;
		settings = mapActivity.getMyApplication().getSettings();
	}

	private static class LocalRoutingParameter {

		public RoutingParameter routingParameter;

		public String getText(MapActivity mapActivity) {
			return SettingsBaseActivity.getRoutingStringPropertyName(mapActivity, routingParameter.getId(),
					routingParameter.getName());
		}

		public boolean isSelected(OsmandSettings settings) {
			final CommonPreference<Boolean> property = settings.getCustomRoutingBooleanProperty(routingParameter
					.getId());
			return property.get();
		}

		public void setSelected(OsmandSettings settings, boolean isChecked) {
			final CommonPreference<Boolean> property = settings.getCustomRoutingBooleanProperty(routingParameter
					.getId());
			property.set(isChecked);
		}

	}
	
	private static class GpxLocalRoutingParameter extends LocalRoutingParameter {
		
	}
	
	private static class OtherSettingsRoutingParameter extends LocalRoutingParameter {
		
	}

	private static class OtherLocalRoutingParameter extends LocalRoutingParameter {
		public String text;
		public boolean selected;
		public int id;

		public OtherLocalRoutingParameter(int id, String text, boolean selected) {
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

	public void showAndHideDialog() {
		if (dialog != null) {
			hideDialog();
		} else {
			dialog = showDialog(controlsLayer, mapActivity, createLayout(), new OnDismissListener() {
				@Override
				public void onDismiss(DialogInterface d) {
					dialog = null;				
				}
			});
		}
	}

	public static class RoutePrepareDialog extends Dialog {

		private OnDismissListener listener;

		public RoutePrepareDialog(Context context) {
			super(context);
		}
		
		public OnDismissListener getListener() {
			return listener;
		}
		
		@Override
		public void setOnDismissListener(OnDismissListener l) {
			this.listener = l;
			super.setOnDismissListener(new OnDismissListener() {
				
				@Override
				public void onDismiss(DialogInterface dialog) {
					if(listener != null) {
						listener.onDismiss(dialog);
					}
				}
			});
		}

		public void cancelDismissListener() {
			this.listener = null;
		}
		
	}
	public static Dialog showDialog(final MapControlsLayer controlsLayer, final MapActivity mapActivity, final View ll,
			final OnDismissListener dismiss) {
		final boolean switched = controlsLayer.switchToRoutePlanningLayout();
		final RoutePrepareDialog dialog = new RoutePrepareDialog(mapActivity);
		WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
		final int maxHeight ;
		boolean portrait = AndroidUiHelper.isOrientationPortrait(mapActivity);
		if(portrait) {
			maxHeight = (int) mapActivity.getResources().getDimension(R.dimen.map_route_planning_max_height);
			lp.width = WindowManager.LayoutParams.MATCH_PARENT;
			lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
			lp.gravity = Gravity.BOTTOM;
		} else {
			maxHeight = -1;
			lp.height = WindowManager.LayoutParams.MATCH_PARENT;
			lp.width = (int) mapActivity.getResources().getDimension(R.dimen.map_route_planning_land_width);
			lp.gravity = Gravity.LEFT;
		}
		dialog.getContext().setTheme(R.style.Dialog_Fullscreen);
		dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
		dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
		dialog.setContentView(ll, new LayoutParams(WindowManager.LayoutParams.MATCH_PARENT,
				portrait ? WindowManager.LayoutParams.WRAP_CONTENT : WindowManager.LayoutParams.MATCH_PARENT));
		dialog.setCanceledOnTouchOutside(true);
		dialog.getWindow().setAttributes(lp);
		if (maxHeight != -1) {
			ll.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
				boolean wrap = true;

				@Override
				public void onGlobalLayout() {
					if (ll.getHeight() > maxHeight) {
						dialog.setContentView(ll, new LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, maxHeight));
						wrap = false;
					} else if (ll.getHeight() < maxHeight && !wrap) {
						dialog.setContentView(ll,
								new LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, ll.getHeight()));
						wrap = true;
					}
				}
			});
		}
		if(!portrait) {
			mapActivity.getMapView().setMapPositionX(1);
			mapActivity.getMapView().refreshMap();
		}
		dialog.show();
		if(!AndroidUiHelper.isXLargeDevice(mapActivity)) {
			AndroidUiHelper.updateVisibility(mapActivity.findViewById(R.id.map_right_widgets_panel), false);
			AndroidUiHelper.updateVisibility(mapActivity.findViewById(R.id.map_left_widgets_panel), false);
		}
		if(!portrait) {
			AndroidUiHelper.updateVisibility(mapActivity.findViewById(R.id.map_route_land_left_margin), true);
		}
		OnDismissListener dismissList = new OnDismissListener() {
			@Override
			public void onDismiss(DialogInterface dlg) {
				mapActivity.getMapView().setMapPositionX(0);
				mapActivity.getMapView().refreshMap();
				AndroidUiHelper.updateVisibility(mapActivity.findViewById(R.id.map_route_land_left_margin), false);
				AndroidUiHelper.updateVisibility(mapActivity.findViewById(R.id.map_right_widgets_panel), true);
				AndroidUiHelper.updateVisibility(mapActivity.findViewById(R.id.map_left_widgets_panel), true);
				if(switched) {
					controlsLayer.switchToRouteFollowingLayout();
				}
				if(dismiss != null){
					dismiss.onDismiss(dialog);
				}
			}
		};
		dialog.setOnDismissListener(dismissList);
		return dialog;
	}
	

	private void updateGpxRoutingParameter(OtherLocalRoutingParameter gpxParam) {
		GPXRouteParamsBuilder rp = mapActivity.getRoutingHelper().getCurrentGPXRoute();
		boolean selected = gpxParam.isSelected(settings);
		if (rp != null) {
			if (gpxParam.id == R.string.gpx_option_reverse_route) {
				rp.setReverse(selected);
				TargetPointsHelper tg = mapActivity.getMyApplication().getTargetPointsHelper();
				List<Location> ps = rp.getPoints();
				if (ps.size() > 0) {
					Location first = ps.get(0);
					Location end = ps.get(ps.size() - 1);
					TargetPoint pn = tg.getPointToNavigate();
					boolean update = false;
					if (pn == null
							|| MapUtils.getDistance(pn.point, new LatLon(first.getLatitude(), first.getLongitude())) < 10) {
						tg.navigateToPoint(new LatLon(end.getLatitude(), end.getLongitude()), false, -1);
						update = true;
					}
					if (tg.getPointToStart() == null
							|| MapUtils.getDistance(tg.getPointToStart().point,
									new LatLon(end.getLatitude(), end.getLongitude())) < 10) {
						tg.setStartPoint(new LatLon(first.getLatitude(), first.getLongitude()), false, null);
						update = true;
					}
					if (update) {
						tg.updateRouteAndReferesh(true);
					}
				}
			} else if (gpxParam.id == R.string.gpx_option_calculate_first_last_segment) {
				rp.setCalculateOsmAndRouteParts(selected);
				settings.GPX_ROUTE_CALC_OSMAND_PARTS.set(selected);
			} else if (gpxParam.id == R.string.gpx_option_from_start_point) {
				rp.setPassWholeRoute(selected);
			} else if (gpxParam.id == R.string.use_points_as_intermediates) {
				settings.GPX_CALCULATE_RTEPT.set(selected);
				rp.setUseIntermediatePointsRTE(selected);
			} else if (gpxParam.id == R.string.calculate_osmand_route_gpx) {
				settings.GPX_ROUTE_CALC.set(selected);
				rp.setCalculateOsmAndRoute(selected);
				updateParameters();
			}
		}
		if (gpxParam.id == R.string.calculate_osmand_route_without_internet) {
			settings.GPX_ROUTE_CALC_OSMAND_PARTS.set(selected);
		}
		if (gpxParam.id == R.string.fast_route_mode) {
			settings.FAST_ROUTE_MODE.set(selected);
		}
		if (gpxParam.id == R.string.speak_favorites) {
			settings.ANNOUNCE_NEARBY_FAVORITES.set(selected);
		}
	}
	
	private List<LocalRoutingParameter> getRoutingParameters(ApplicationMode am) {
		List<LocalRoutingParameter> list = getRoutingParametersInner(am);
		list.add(new GpxLocalRoutingParameter());
		list.add(new OtherSettingsRoutingParameter());
		return list;
	}

	private List<LocalRoutingParameter> getRoutingParametersInner(ApplicationMode am) {
		List<LocalRoutingParameter> list = new ArrayList<LocalRoutingParameter>();
		GPXRouteParamsBuilder rparams = mapActivity.getRoutingHelper().getCurrentGPXRoute();
		boolean osmandRouter = settings.ROUTER_SERVICE.get() == RouteService.OSMAND;
		if (!osmandRouter) {
			list.add(new OtherLocalRoutingParameter(R.string.calculate_osmand_route_without_internet,
					getString(R.string.calculate_osmand_route_without_internet), settings.GPX_ROUTE_CALC_OSMAND_PARTS
							.get()));
			list.add(new OtherLocalRoutingParameter(R.string.fast_route_mode, getString(R.string.fast_route_mode),
					settings.FAST_ROUTE_MODE.get()));
			return list;
		}
		if (rparams != null) {
			GPXFile fl = rparams.getFile();
			if (fl.hasRtePt()) {
				list.add(new OtherLocalRoutingParameter(R.string.use_points_as_intermediates,
						getString(R.string.use_points_as_intermediates), rparams.isUseIntermediatePointsRTE()));
			}
			list.add(new OtherLocalRoutingParameter(R.string.gpx_option_reverse_route,
					getString(R.string.gpx_option_reverse_route), rparams.isReverse()));
			if (!rparams.isUseIntermediatePointsRTE()) {
				list.add(new OtherLocalRoutingParameter(R.string.gpx_option_from_start_point,
						getString(R.string.gpx_option_from_start_point), rparams.isPassWholeRoute()));
				list.add(new OtherLocalRoutingParameter(R.string.gpx_option_calculate_first_last_segment,
						getString(R.string.gpx_option_calculate_first_last_segment), rparams
								.isCalculateOsmAndRouteParts()));
			}
			// list.add(new OtherLocalRoutingParameter(R.string.announce_gpx_waypoints,
			// getString(R.string.announce_gpx_waypoints), rparams.isAnnounceWaypoints()));
			// Temporary disabled
			// list.add(new GPXLocalRoutingParameter(R.string.calculate_osmand_route_gpx,
			// getString(R.string.calculate_osmand_route_gpx), rparams.isCalculateOsmAndRoute()));
		}
		GeneralRouter rm = SettingsNavigationActivity.getRouter(mapActivity.getMyApplication()
				.getDefaultRoutingConfig(), am);
		if (rm == null || (rparams != null && !rparams.isCalculateOsmAndRoute()) && !rparams.getFile().hasRtePt()) {
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
		ImageView muteBtn = (ImageView) settingsDlg.findViewById(R.id.mute);
		setMuteBtn(muteBtn);
		
		ImageView avoidRoads = (ImageView) settingsDlg.findViewById(R.id.avoid_roads);
		setAvoidRoads(avoidRoads);

		
		setupListParameters(settingsDlg, ctx);
		setupApplicationModes(settingsDlg);
		controlsLayer.updateRouteButtons(settingsDlg, false);
		
		return settingsDlg;
	}

	private void setupListParameters(View settingsDlg, Context ctx) {
		final ListView lv = (ListView) settingsDlg.findViewById(android.R.id.list);
		lv.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				if(listAdapter.getItem(position) instanceof OtherSettingsRoutingParameter) {
					final Intent settings = new Intent(mapActivity, SettingsNavigationActivity.class);
					settings.putExtra(SettingsNavigationActivity.INTENT_SKIP_DIALOG, true);
					mapActivity.startActivity(settings);
				} else if(view.findViewById(R.id.GPXRouteSpinner) != null) {
					showOptionsMenu((TextView) view.findViewById(R.id.GPXRouteSpinner));
				} else {
					CheckBox ch = (CheckBox) view.findViewById(R.id.check_item);
					if (ch != null) {
						ch.setChecked(!ch.isChecked());
					}
				}
			}
		});
		listAdapter = new ArrayAdapter<LocalRoutingParameter>(ctx, R.layout.layers_list_activity_item, R.id.title,
				getRoutingParameters(settings.APPLICATION_MODE.get())) {
			@Override
			public View getView(final int position, View convertView, ViewGroup parent) {
				LocalRoutingParameter parameter = getItem(position);
				if(parameter instanceof GpxLocalRoutingParameter) {
					View v = mapActivity.getLayoutInflater().inflate(R.layout.plan_route_gpx, null);
					final TextView gpxSpinner = (TextView) v.findViewById(R.id.GPXRouteSpinner);
					updateSpinnerItems(gpxSpinner);
					return v;
				}
				if(parameter instanceof OtherSettingsRoutingParameter) {
					View v = mapActivity.getLayoutInflater().inflate(R.layout.layers_list_activity_item, null);
					final ImageView icon = (ImageView) v.findViewById(R.id.icon);
					icon.setImageDrawable(mapActivity.getMyApplication().getIconsCache().getContentIcon(R.drawable.ic_action_gsettings_dark));
					icon.setVisibility(View.VISIBLE);
					((TextView)v.findViewById(R.id.title)).setText(R.string.routing_settings_2);
					v.findViewById(R.id.check_item).setVisibility(View.GONE);
					return v;
				}
				return inflateRoutingParameter(position);
			}

			private View inflateRoutingParameter(final int position) {
				View v = mapActivity.getLayoutInflater().inflate(R.layout.layers_list_activity_item, null);
				final TextView tv = (TextView) v.findViewById(R.id.title);
				final CheckBox ch = ((CheckBox) v.findViewById(R.id.check_item));
				final LocalRoutingParameter rp = getItem(position);
				tv.setText(rp.getText(mapActivity));
				ch.setOnCheckedChangeListener(null);
				if (rp.routingParameter != null && rp.routingParameter.getId().equals("short_way")) {
					// if short route settings - it should be inverse of fast_route_mode
					ch.setChecked(!settings.FAST_ROUTE_MODE.get());
				} else {
					ch.setChecked(rp.isSelected(settings));
				}
				ch.setVisibility(View.VISIBLE);
				ch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
						// if short way that it should set valut to fast mode opposite of current
						if (rp.routingParameter != null && rp.routingParameter.getId().equals("short_way")) {
							settings.FAST_ROUTE_MODE.set(!isChecked);
						}
						rp.setSelected(settings, isChecked);

						if (rp instanceof OtherLocalRoutingParameter) {
							updateGpxRoutingParameter((OtherLocalRoutingParameter) rp);
						}
						mapActivity.getRoutingHelper().recalculateRouteDueToSettingsChange();
					}
				});
				return v;
			}
		};
		lv.setAdapter(listAdapter);
	}

	private void setupApplicationModes(View settingsDlg) {
		final OsmandSettings settings = mapActivity.getMyApplication().getSettings();
		final Set<ApplicationMode> selected = new HashSet<ApplicationMode>();
		selected.add(settings.APPLICATION_MODE.get());
		AppModeDialog.prepareAppModeView(mapActivity, selected, false,
				(ViewGroup) settingsDlg.findViewById(R.id.app_modes), true, new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						if (selected.size() > 0) {
							ApplicationMode next = selected.iterator().next();
							settings.APPLICATION_MODE.set(next);
							mapActivity.getRoutingHelper().recalculateRouteDueToSettingsChange();
							updateParameters();
						}
					}

				});
	}
	

	private void setAvoidRoads(ImageView avoidRoads) {
		avoidRoads.setContentDescription(mapActivity.getString(R.string.impassable_road));
		avoidRoads.setImageDrawable(mapActivity.getMyApplication().getIconsCache().getContentIcon(R.drawable.ic_action_road_works_dark));
		avoidRoads.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				hideDialog();
				mapActivity.getMyApplication().getAvoidSpecificRoads().showDialog(mapActivity);
			}
		});		
	}

	private void setMuteBtn(final ImageView muteBtn) {
		final RoutingHelper routingHelper = mapActivity.getMyApplication().getRoutingHelper();
		boolean mute = routingHelper.getVoiceRouter().isMute();
		int t = mute ? R.string.menu_mute_on : R.string.menu_mute_off;
		int icon;
		if(mute) {
			icon = R.drawable.ic_action_volume_off;
		} else{
			icon = R.drawable.ic_action_volume_up;
		}
		muteBtn.setContentDescription(mapActivity.getString(t));
		muteBtn.setImageDrawable(mapActivity.getMyApplication().getIconsCache().getContentIcon(icon));
		muteBtn.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				boolean mt = !routingHelper.getVoiceRouter().isMute();
				mapActivity.getMyApplication().getSettings().VOICE_MUTE.set(mt);
				routingHelper.getVoiceRouter().setMute(mt);
				setMuteBtn(muteBtn);
			}
		});
	}

	private void updateParameters() {
		ApplicationMode am = settings.APPLICATION_MODE.get();
		listAdapter.setNotifyOnChange(false);
		listAdapter.clear();
		for (LocalRoutingParameter r : getRoutingParameters(am)) {
			listAdapter.add(r);
		}
		listAdapter.notifyDataSetChanged();
	}


	protected void openGPXFileSelection(final TextView gpxSpinner) {
		GpxUiHelper.selectGPXFile(mapActivity, false, false, new CallbackWithObject<GPXUtilities.GPXFile[]>() {

			@Override
			public boolean processResult(GPXFile[] result) {
				mapActivity.getMapActions().setGPXRouteParams(result[0]);
				mapActivity.getMyApplication().getTargetPointsHelper().updateRouteAndReferesh(true);
				updateSpinnerItems(gpxSpinner);
				updateParameters();
				mapActivity.getRoutingHelper().recalculateRouteDueToSettingsChange();
				return true;
			}
		});
	}

	private void updateSpinnerItems(final TextView gpxSpinner) {
		GPXRouteParamsBuilder rp = mapActivity.getRoutingHelper().getCurrentGPXRoute();
		gpxSpinner.setText(rp == null ? mapActivity.getString(R.string.shared_string_none) : 
			new File(rp.getFile().path).getName());
	}

	private void showOptionsMenu(final TextView gpxSpinner) {
		GPXRouteParamsBuilder rp = mapActivity.getRoutingHelper().getCurrentGPXRoute();
		final PopupMenu optionsMenu = new PopupMenu(gpxSpinner.getContext(), gpxSpinner);
		MenuItem item = optionsMenu.getMenu().add(
				mapActivity.getString(R.string.shared_string_none));
		item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				if (mapActivity.getRoutingHelper().getCurrentGPXRoute() != null) {
					mapActivity.getRoutingHelper().setGpxParams(null);
					settings.FOLLOW_THE_GPX_ROUTE.set(null);
					mapActivity.getRoutingHelper().recalculateRouteDueToSettingsChange();
				}
				updateParameters();
				return true;
			}
		});
		item = optionsMenu.getMenu().add(mapActivity.getString(R.string.select_gpx));
		item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				openGPXFileSelection(gpxSpinner);
				return true;
			}
		});
		if(rp != null) {
			item = optionsMenu.getMenu().add(new File(rp.getFile().path).getName());
			item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(MenuItem item) {
					// nothing to change
					return true;
				}
			});
		}
		optionsMenu.show();
	}
	
	public boolean isDialogVisible() {
		return dialog != null && dialog.isShowing();
	}

	public void hideDialog() {
		Dialog dialog = this.dialog;
		if(dialog != null) {
			if(dialog instanceof RoutePrepareDialog && 
					((RoutePrepareDialog) dialog).getListener() != null) {
					((RoutePrepareDialog) dialog).getListener().onDismiss(dialog);
					((RoutePrepareDialog) dialog).cancelDismissListener();
			}
			dialog.dismiss();
			this.dialog = null;
		}
	}

}
