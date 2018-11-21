package net.osmand.plus.routepreparationmenu;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import net.osmand.AndroidUtils;
import net.osmand.CallbackWithObject;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuItem;
import net.osmand.plus.GPXUtilities;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.SettingsBaseActivity;
import net.osmand.plus.activities.SettingsNavigationActivity;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithCompoundButton;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerHalfItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.dashboard.DashboardOnMap;
import net.osmand.plus.helpers.GpxUiHelper;
import net.osmand.plus.routepreparationmenu.RoutingOptionsHelper.AvoidRoadsRoutingParameter;
import net.osmand.plus.routepreparationmenu.RoutingOptionsHelper.AvoidRoadsTypesRoutingParameter;
import net.osmand.plus.routepreparationmenu.RoutingOptionsHelper.DividerItem;
import net.osmand.plus.routepreparationmenu.RoutingOptionsHelper.GpxLocalRoutingParameter;
import net.osmand.plus.routepreparationmenu.RoutingOptionsHelper.LocalRoutingParameter;
import net.osmand.plus.routepreparationmenu.RoutingOptionsHelper.LocalRoutingParameterGroup;
import net.osmand.plus.routepreparationmenu.RoutingOptionsHelper.MuteSoundRoutingParameter;
import net.osmand.plus.routepreparationmenu.RoutingOptionsHelper.OtherSettingsRoutingParameter;
import net.osmand.plus.routepreparationmenu.RoutingOptionsHelper.RouteSimulationItem;
import net.osmand.plus.routepreparationmenu.RoutingOptionsHelper.ShowAlongTheRouteItem;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.views.MapControlsLayer;
import net.osmand.router.GeneralRouter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static net.osmand.plus.routepreparationmenu.RoutingOptionsHelper.DRIVING_STYLE;

public class RouteOptionsBottomSheet extends MenuBottomSheetDialogFragment {

	public static final String TAG = "RouteOptionsBottomSheet";

	private OsmandSettings settings;
	private OsmandApplication app;
	private MapActivity mapActivity;
	private MapControlsLayer controlsLayer;
	private RoutingHelper routingHelper;
	private RoutingOptionsHelper routingOptionsHelper;
	private ApplicationMode applicationMode;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = getMyApplication();
		settings = app.getSettings();
		routingHelper = app.getRoutingHelper();
		routingOptionsHelper = app.getRoutingOptionsHelper();
		mapActivity = getMapActivity();
		controlsLayer = mapActivity.getMapLayers().getMapControlsLayer();
		applicationMode = routingHelper.getAppMode();
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		items.add(new TitleItem(app.getString(R.string.shared_string_settings), nightMode ? R.color.active_buttons_and_links_dark : R.color.active_buttons_and_links_light));

		List<LocalRoutingParameter> list = new ArrayList<>();
		if (applicationMode.equals(ApplicationMode.CAR)) {
			list = routingOptionsHelper.getRoutingParameters(applicationMode, AppModeOptions.CAR.routingParameters);
		} else if (applicationMode.equals(ApplicationMode.BICYCLE)) {
			list = routingOptionsHelper.getRoutingParameters(applicationMode, AppModeOptions.BICYCLE.routingParameters);
		} else if (applicationMode.equals(ApplicationMode.PEDESTRIAN)) {
			list = routingOptionsHelper.getRoutingParameters(applicationMode, AppModeOptions.PEDESTRIAN.routingParameters);
		}

		for (final LocalRoutingParameter optionsItem : list) {

			if (optionsItem instanceof DividerItem) {
				items.add(new DividerHalfItem(app));
			} else if (optionsItem instanceof MuteSoundRoutingParameter) {
				final BottomSheetItemWithCompoundButton[] muteSoundRoutingParameter = new BottomSheetItemWithCompoundButton[1];
				muteSoundRoutingParameter[0] = (BottomSheetItemWithCompoundButton) new BottomSheetItemWithCompoundButton.Builder()
						.setChecked(!routingHelper.getVoiceRouter().isMute())
						.setDescription(getString(R.string.voice_announcements))
						.setIcon(getContentIcon(R.drawable.ic_action_volume_up))
						.setTitle(getString(R.string.shared_string_sound))
						.setLayoutId(R.layout.bottom_sheet_item_with_descr_and_switch_56dp)
						.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View v) {
								routingOptionsHelper.addNewRouteMenuParameter(applicationMode, optionsItem);
								boolean mt = !routingHelper.getVoiceRouter().isMute();
								settings.VOICE_MUTE.set(mt);
								routingHelper.getVoiceRouter().setMute(mt);
								muteSoundRoutingParameter[0].setChecked(!routingHelper.getVoiceRouter().isMute());
							}
						})
						.create();
				items.add(muteSoundRoutingParameter[0]);

			} else if (optionsItem instanceof ShowAlongTheRouteItem) {
				BaseBottomSheetItem showAlongTheRouteItem = new SimpleBottomSheetItem.Builder()
						.setIcon(getContentIcon(R.drawable.ic_action_snap_to_road))
						.setTitle(getString(R.string.show_along_the_route))
						.setLayoutId(R.layout.bottom_sheet_item_simple)
						.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View view) {
								routingOptionsHelper.addNewRouteMenuParameter(applicationMode, optionsItem);
								Toast.makeText(app, getText(R.string.show_along_the_route), Toast.LENGTH_LONG).show();
							}
						})
						.create();
				items.add(showAlongTheRouteItem);
			} else if (optionsItem instanceof RouteSimulationItem) {
				BaseBottomSheetItem routeSimulationItem = new SimpleBottomSheetItem.Builder()
						.setIcon(getContentIcon(R.drawable.ic_action_start_navigation))
						.setTitle(getString(R.string.simulate_navigation))
						.setLayoutId(R.layout.bottom_sheet_item_simple)
						.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View view) {
								final OsmAndLocationProvider loc = app.getLocationProvider();
								loc.getLocationSimulation().startStopRouteAnimation(getActivity());
								dismiss();
							}
						})
						.create();
				items.add(routeSimulationItem);
			} else if (optionsItem instanceof AvoidRoadsTypesRoutingParameter) {
				BaseBottomSheetItem avoidRoadsRoutingParameter = new SimpleBottomSheetItem.Builder()
						.setIcon(getContentIcon(R.drawable.ic_action_road_works_dark))
						.setTitle(getString(R.string.impassable_road))
						.setLayoutId(R.layout.bottom_sheet_item_simple)
						.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View view) {
								routingOptionsHelper.addNewRouteMenuParameter(applicationMode, optionsItem);
								List<GeneralRouter.RoutingParameter> avoidParameters = routingOptionsHelper.getAvoidRoutingPrefsForAppMode(applicationMode);
								String[] vals = new String[avoidParameters.size()];
								OsmandSettings.OsmandPreference[] bls = new OsmandSettings.OsmandPreference[avoidParameters.size()];
								for (int i = 0; i < avoidParameters.size(); i++) {
									GeneralRouter.RoutingParameter p = avoidParameters.get(i);
									vals[i] = SettingsBaseActivity.getRoutingStringPropertyName(app, p.getId(), p.getName());
									bls[i] = settings.getCustomRoutingBooleanProperty(p.getId(), p.getDefaultBoolean());
								}
								showBooleanSettings(vals, bls, getString(R.string.impassable_road), mapActivity);
							}
						})
						.create();
				items.add(avoidRoadsRoutingParameter);
			} else if (optionsItem instanceof AvoidRoadsRoutingParameter) {
				BaseBottomSheetItem avoidRoadsRoutingParameter = new SimpleBottomSheetItem.Builder()
						.setIcon(getContentIcon(R.drawable.ic_action_road_works_dark))
						.setTitle(getString(R.string.impassable_road))
						.setLayoutId(R.layout.bottom_sheet_item_simple)
						.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View view) {
								routingOptionsHelper.addNewRouteMenuParameter(applicationMode, optionsItem);
								mapActivity.getDashboard().setDashboardVisibility(false, DashboardOnMap.DashboardType.ROUTE_PREFERENCES);
								controlsLayer.getMapRouteInfoMenu().hide();
								app.getAvoidSpecificRoads().showDialog(mapActivity);
								dismiss();
							}
						})
						.create();
				items.add(avoidRoadsRoutingParameter);

			} else if (optionsItem instanceof GpxLocalRoutingParameter) {
				View v = mapActivity.getLayoutInflater().inflate(R.layout.plan_route_gpx, null);
				AndroidUtils.setListItemBackground(mapActivity, v, nightMode);
				AndroidUtils.setTextPrimaryColor(mapActivity, (TextView) v.findViewById(R.id.GPXRouteTitle), nightMode);
				final TextView gpxSpinner = (TextView) v.findViewById(R.id.GPXRouteSpinner);
				AndroidUtils.setTextPrimaryColor(mapActivity, gpxSpinner, nightMode);
				((ImageView) v.findViewById(R.id.dropDownIcon))
						.setImageDrawable(app.getUIUtilities().getIcon(R.drawable.ic_action_arrow_drop_down, !nightMode));

				BaseBottomSheetItem gpxLocalRoutingParameter = new BottomSheetItemWithDescription.Builder()
						.setDescription(getString(R.string.choose_track_file_to_follow))
						.setIcon(getContentIcon(R.drawable.ic_action_polygom_dark))
						.setTitle(getString(R.string.shared_string_gpx_route))
						.setLayoutId(R.layout.bottom_sheet_item_with_descr_56dp)
						.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View view) {
								openGPXFileSelection();
							}
						})
						.create();
				items.add(gpxLocalRoutingParameter);

			} else if (optionsItem instanceof OtherSettingsRoutingParameter) {
				BaseBottomSheetItem otherSettingsRoutingParameter = new SimpleBottomSheetItem.Builder()
						.setIcon(getContentIcon(R.drawable.map_action_settings))
						.setTitle(getString(R.string.routing_settings_2))
						.setLayoutId(R.layout.bottom_sheet_item_simple)
						.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View view) {
								final Intent settings = new Intent(mapActivity, SettingsNavigationActivity.class);
								settings.putExtra(SettingsNavigationActivity.INTENT_SKIP_DIALOG, true);
								settings.putExtra(SettingsBaseActivity.INTENT_APP_MODE, routingHelper.getAppMode().getStringKey());
								mapActivity.startActivity(settings);
							}
						})
						.create();
				items.add(otherSettingsRoutingParameter);

			} else {
				inflateRoutingParameter(optionsItem);
			}
		}
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_close;
	}


	public static AlertDialog showBooleanSettings(String[] vals, final OsmandSettings.OsmandPreference<Boolean>[] prefs, final CharSequence title, MapActivity mapActivity) {
		AlertDialog.Builder bld = new AlertDialog.Builder(mapActivity);
		boolean[] checkedItems = new boolean[prefs.length];
		for (int i = 0; i < prefs.length; i++) {
			checkedItems[i] = prefs[i].get();
		}

		final boolean[] tempPrefs = new boolean[prefs.length];
		for (int i = 0; i < prefs.length; i++) {
			tempPrefs[i] = prefs[i].get();
		}

		bld.setMultiChoiceItems(vals, checkedItems, new DialogInterface.OnMultiChoiceClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which, boolean isChecked) {
				tempPrefs[which] = isChecked;
			}
		});

		bld.setTitle(title);

		bld.setNegativeButton(R.string.shared_string_cancel, null);

		bld.setPositiveButton(R.string.shared_string_ok, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				for (int i = 0; i < prefs.length; i++) {
					prefs[i].set(tempPrefs[i]);
				}
			}
		});

		return bld.show();
	}

	private void inflateRoutingParameter(final LocalRoutingParameter optionsItem) {
		if (optionsItem != null) {
			final LocalRoutingParameter parameter = (LocalRoutingParameter) optionsItem;
			final BottomSheetItemWithCompoundButton[] item = new BottomSheetItemWithCompoundButton[1];
			BottomSheetItemWithCompoundButton.Builder builder = new BottomSheetItemWithCompoundButton.Builder();
			builder.setIcon(getContentIcon(R.drawable.mx_amenity_fuel));
			if (parameter.routingParameter != null) {
				builder.setTitle(parameter.getText(mapActivity));
			}
			if (parameter instanceof LocalRoutingParameterGroup) {
				final LocalRoutingParameterGroup group = (LocalRoutingParameterGroup) parameter;
				LocalRoutingParameter selected = group.getSelected(settings);
				if (selected != null) {
					builder.setTitle(group.getText(mapActivity));
					builder.setDescription(selected.getText(mapActivity));
				}
				builder.setLayoutId(R.layout.bottom_sheet_item_with_descr_56dp);
				builder.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						routingOptionsHelper.addNewRouteMenuParameter(applicationMode, parameter);

						final ContextMenuAdapter adapter = new ContextMenuAdapter();
						int i = 0;
						int selectedIndex = -1;
						for (LocalRoutingParameter p : group.getRoutingParameters()) {
							adapter.addItem(ContextMenuItem.createBuilder(p.getText(mapActivity))
									.setSelected(false).createItem());
							if (p.isSelected(settings)) {
								selectedIndex = i;
							}
							i++;
						}
						if (selectedIndex == -1) {
							selectedIndex = 0;
						}

						AlertDialog.Builder builder = new AlertDialog.Builder(mapActivity);
						final int layout = R.layout.list_menu_item_native_singlechoice;

						final ArrayAdapter<String> listAdapter = new ArrayAdapter<String>(mapActivity, layout, R.id.text1,
								adapter.getItemNames()) {
							@NonNull
							@Override
							public View getView(final int position, View convertView, ViewGroup parent) {
								// User super class to create the View
								View v = convertView;
								if (v == null) {
									v = mapActivity.getLayoutInflater().inflate(layout, null);
								}
								final ContextMenuItem item = adapter.getItem(position);
								TextView tv = (TextView) v.findViewById(R.id.text1);
								tv.setText(item.getTitle());
								tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f);

								return v;
							}
						};

						final int[] selectedPosition = {selectedIndex};
						builder.setSingleChoiceItems(listAdapter, selectedIndex, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int position) {
								selectedPosition[0] = position;
							}
						});
						builder.setTitle(group.getText(mapActivity))
								.setPositiveButton(R.string.shared_string_ok, new DialogInterface.OnClickListener() {

									@Override
									public void onClick(DialogInterface dialog, int which) {
										int position = selectedPosition[0];
										if (position >= 0 && position < group.getRoutingParameters().size()) {
											for (int i = 0; i < group.getRoutingParameters().size(); i++) {
												LocalRoutingParameter rp = group.getRoutingParameters().get(i);
												rp.setSelected(settings, i == position);
											}
											mapActivity.getRoutingHelper().recalculateRouteDueToSettingsChange();
											LocalRoutingParameter selected = group.getSelected(settings);
											if (selected != null) {
												item[0].setDescription(selected.getText(mapActivity));
											}
										}
									}
								})
								.setNegativeButton(R.string.shared_string_cancel, null);

						builder.create().show();
					}
				});
			} else {
				builder.setLayoutId(R.layout.bottom_sheet_item_with_switch);
				if (parameter.routingParameter != null) {
					if (parameter.routingParameter.getId().equals("short_way")) {
						// if short route settings - it should be inverse of fast_route_mode
						builder.setChecked(!settings.FAST_ROUTE_MODE.getModeValue(routingHelper.getAppMode()));
					} else {
						builder.setChecked(parameter.isSelected(settings));
					}
				}
				builder.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						routingOptionsHelper.addNewRouteMenuParameter(applicationMode, parameter);

						boolean selected = parameter.isSelected(settings);
						routingOptionsHelper.applyRoutingParameter(parameter, !selected);
						item[0].setChecked(!selected);
					}
				});
			}
			item[0] = builder.create();
			items.add(item[0]);
		}
	}

	private MapActivity getMapActivity() {
		return (MapActivity) getActivity();
	}

	public static void showInstance(FragmentManager fragmentManager) {
		RouteOptionsBottomSheet f = new RouteOptionsBottomSheet();
		f.show(fragmentManager, RouteOptionsBottomSheet.TAG);
	}

	protected void openGPXFileSelection() {
		GpxUiHelper.selectGPXFile(mapActivity, false, false, new CallbackWithObject<GPXUtilities.GPXFile[]>() {

			@Override
			public boolean processResult(GPXUtilities.GPXFile[] result) {
				mapActivity.getMapActions().setGPXRouteParams(result[0]);
				app.getTargetPointsHelper().updateRouteAndRefresh(true);
				routingHelper.recalculateRouteDueToSettingsChange();
				return true;
			}
		});
	}

	public enum AppModeOptions {

		CAR(MuteSoundRoutingParameter.KEY,
				DividerItem.KEY,
				AvoidRoadsRoutingParameter.KEY,
				ShowAlongTheRouteItem.KEY,
				GeneralRouter.ALLOW_PRIVATE,
				GeneralRouter.USE_SHORTEST_WAY,
				DividerItem.KEY,
				GpxLocalRoutingParameter.KEY,
				OtherSettingsRoutingParameter.KEY,
				RouteSimulationItem.KEY),

		BICYCLE(MuteSoundRoutingParameter.KEY,
				DRIVING_STYLE,
				GeneralRouter.USE_HEIGHT_OBSTACLES,
				DividerItem.KEY,
				AvoidRoadsTypesRoutingParameter.KEY,
				ShowAlongTheRouteItem.KEY,
				DividerItem.KEY,
				GpxLocalRoutingParameter.KEY,
				OtherSettingsRoutingParameter.KEY,
				RouteSimulationItem.KEY),

		PEDESTRIAN(MuteSoundRoutingParameter.KEY,
				GeneralRouter.USE_HEIGHT_OBSTACLES,
				DividerItem.KEY,
				AvoidRoadsTypesRoutingParameter.KEY,
				ShowAlongTheRouteItem.KEY,
				DividerItem.KEY,
				GpxLocalRoutingParameter.KEY,
				OtherSettingsRoutingParameter.KEY,
				RouteSimulationItem.KEY);


		List<String> routingParameters;

		AppModeOptions(String... routingParameters) {
			this.routingParameters = Arrays.asList(routingParameters);
		}
	}
}