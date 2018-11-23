package net.osmand.plus.routepreparationmenu;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import net.osmand.AndroidUtils;
import net.osmand.CallbackWithObject;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.GPXUtilities;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
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
import net.osmand.plus.development.OsmandDevelopmentPlugin;
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
		applicationMode = routingHelper.getAppMode();
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		controlsLayer = mapActivity.getMapLayers().getMapControlsLayer();

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
						.setIcon(getContentIcon(R.drawable.ic_action_show_along_route))
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
				if (OsmandPlugin.getEnabledPlugin(OsmandDevelopmentPlugin.class) == null) {
					continue;
				}
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
								AvoidRoadsBottomSheetDialogFragment avoidRoadsFragment = new AvoidRoadsBottomSheetDialogFragment();
								avoidRoadsFragment.setTargetFragment(RouteOptionsBottomSheet.this, AvoidRoadsBottomSheetDialogFragment.REQUEST_CODE);
								avoidRoadsFragment.show(mapActivity.getSupportFragmentManager(), AvoidRoadsBottomSheetDialogFragment.TAG);
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
								AvoidRoadsBottomSheetDialogFragment avoidRoadsFragment = new AvoidRoadsBottomSheetDialogFragment();
								avoidRoadsFragment.setTargetFragment(RouteOptionsBottomSheet.this, AvoidRoadsBottomSheetDialogFragment.REQUEST_CODE);
								avoidRoadsFragment.show(mapActivity.getSupportFragmentManager(), AvoidRoadsBottomSheetDialogFragment.TAG);
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

	private void inflateRoutingParameter(final LocalRoutingParameter optionsItem) {
		if (optionsItem != null) {
			final LocalRoutingParameter parameter = (LocalRoutingParameter) optionsItem;
			final BottomSheetItemWithCompoundButton[] item = new BottomSheetItemWithCompoundButton[1];
			BottomSheetItemWithCompoundButton.Builder builder = new BottomSheetItemWithCompoundButton.Builder();
			builder.setIcon(getContentIcon(R.drawable.ic_action_fuel));
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
						routingOptionsHelper.showLocalRoutingParameterGroupDialog(group, mapActivity, new RoutingOptionsHelper.OnClickListener() {
							@Override
							public void onClick() {
								LocalRoutingParameter selected = group.getSelected(settings);
								if (selected != null) {
									item[0].setDescription(selected.getText(mapActivity));
								}
							}
						});
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

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == AvoidRoadsBottomSheetDialogFragment.REQUEST_CODE
				&& resultCode == AvoidRoadsBottomSheetDialogFragment.OPEN_AVOID_ROADS_DIALOG_REQUEST_CODE) {
			dismiss();
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