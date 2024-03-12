package net.osmand.plus.routepreparationmenu;

import static net.osmand.IndexConstants.GPX_FILE_EXT;
import static net.osmand.plus.measurementtool.MeasurementToolFragment.ATTACH_ROADS_MODE;
import static net.osmand.plus.measurementtool.MeasurementToolFragment.CALCULATE_HEIGHTMAP_MODE;
import static net.osmand.plus.measurementtool.MeasurementToolFragment.CALCULATE_SRTM_MODE;
import static net.osmand.plus.measurementtool.MeasurementToolFragment.FOLLOW_TRACK_MODE;
import static net.osmand.plus.routepreparationmenu.RoutingOptionsHelper.DRIVING_STYLE;
import static net.osmand.plus.settings.fragments.BaseSettingsFragment.APP_MODE_KEY;
import static net.osmand.plus.settings.fragments.RouteParametersFragment.RELIEF_SMOOTHNESS_FACTOR;
import static net.osmand.plus.settings.fragments.RouteParametersFragment.getRoutingParameterTitle;
import static net.osmand.plus.settings.fragments.RouteParametersFragment.isRoutingParameterSelected;
import static net.osmand.router.GeneralRouter.USE_HEIGHT_OBSTACLES;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.gpx.GPXFile;
import net.osmand.PlatformUtil;
import net.osmand.StateChangedListener;
import net.osmand.plus.avoidroads.AvoidRoadsBottomSheetDialogFragment;
import net.osmand.plus.simulation.OsmAndLocationSimulation;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithCompoundButton;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerStartItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.track.helpers.GpxUiHelper;
import net.osmand.plus.measurementtool.MeasurementToolFragment;
import net.osmand.plus.routepreparationmenu.RoutingOptionsHelper.AvoidPTTypesRoutingParameter;
import net.osmand.plus.routepreparationmenu.RoutingOptionsHelper.AvoidRoadsRoutingParameter;
import net.osmand.plus.routepreparationmenu.RoutingOptionsHelper.CalculateAltitudeItem;
import net.osmand.plus.routepreparationmenu.RoutingOptionsHelper.CustomizeRouteLineRoutingParameter;
import net.osmand.plus.routepreparationmenu.RoutingOptionsHelper.DividerItem;
import net.osmand.plus.routepreparationmenu.RoutingOptionsHelper.GpxLocalRoutingParameter;
import net.osmand.plus.routepreparationmenu.RoutingOptionsHelper.LocalRoutingParameter;
import net.osmand.plus.routepreparationmenu.RoutingOptionsHelper.LocalRoutingParameterGroup;
import net.osmand.plus.routepreparationmenu.RoutingOptionsHelper.MuteSoundRoutingParameter;
import net.osmand.plus.routepreparationmenu.RoutingOptionsHelper.OtherSettingsRoutingParameter;
import net.osmand.plus.routepreparationmenu.RoutingOptionsHelper.RouteSimulationItem;
import net.osmand.plus.routepreparationmenu.RoutingOptionsHelper.ShowAlongTheRouteItem;
import net.osmand.plus.routepreparationmenu.RoutingOptionsHelper.TimeConditionalRoutingItem;
import net.osmand.plus.routing.GPXRouteParams.GPXRouteParamsBuilder;
import net.osmand.plus.routing.RouteService;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.routing.RoutingHelperUtils;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmAndAppCustomization;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.settings.bottomsheets.ElevationDateBottomSheet;
import net.osmand.plus.settings.fragments.BaseSettingsFragment;
import net.osmand.plus.settings.fragments.SettingsScreenType;
import net.osmand.plus.settings.fragments.voice.VoiceLanguageBottomSheetFragment;
import net.osmand.plus.track.fragments.TrackAltitudeBottomSheet;
import net.osmand.plus.track.fragments.TrackAltitudeBottomSheet.CalculateAltitudeListener;
import net.osmand.plus.track.helpers.save.SaveGpxHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.FileUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.router.GeneralRouter;
import net.osmand.router.GeneralRouter.RoutingParameter;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class RouteOptionsBottomSheet extends MenuBottomSheetDialogFragment implements CalculateAltitudeListener {

	public static final String TAG = RouteOptionsBottomSheet.class.getSimpleName();
	private static final Log LOG = PlatformUtil.getLog(RouteOptionsBottomSheet.class);
	public static final String DIALOG_MODE_KEY = "DIALOG_MODE_KEY";

	private OsmandApplication app;
	private OsmandSettings settings;
	private RoutingHelper routingHelper;
	private RoutingOptionsHelper routingOptionsHelper;
	private ApplicationMode applicationMode;
	@ColorInt
	private int selectedModeColor;
	private boolean currentMuteState;
	private boolean currentUseHeightState;
	private MapActivity mapActivity;
	private CommonPreference<Boolean> useHeightPref;
	private StateChangedListener<Boolean> voiceMuteChangeListener;
	private StateChangedListener<Boolean> useHeightChangeListener;
	private List<RoutingParameter> reliefParameters = new ArrayList<>();
	private DialogMode dialogMode;

	public enum DialogMode {
		DIRECTIONS(),
		PLAN_ROUTE(MuteSoundRoutingParameter.class,
				RouteSimulationItem.class,
				GpxLocalRoutingParameter.class,
				ShowAlongTheRouteItem.class);

		private final Class<? extends LocalRoutingParameter>[] excludeParameters;

		@SafeVarargs
		DialogMode(Class<? extends LocalRoutingParameter>... excludeParameters) {
			this.excludeParameters = excludeParameters;
		}

		public boolean isAvailableParameter(LocalRoutingParameter parameter) {
			for (Class<? extends LocalRoutingParameter> c : excludeParameters) {
				if (Algorithms.objectEquals(parameter.getClass(), c)) {
					return false;
				}
			}
			return true;
		}

		public static DialogMode getModeByName(String modeName) {
			if (modeName != null) {
				return valueOf(modeName);
			}
			return DIRECTIONS;
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Bundle args = getArguments();
		if (args != null) {
			String appMode = args.getString(APP_MODE_KEY, null);
			if (appMode != null) {
				applicationMode = ApplicationMode.valueOfStringKey(appMode, null);
			}
			String dialogModeName = args.getString(DIALOG_MODE_KEY, null);
			dialogMode = DialogMode.getModeByName(dialogModeName);
		}
		app = requiredMyApplication();
		settings = app.getSettings();
		routingHelper = app.getRoutingHelper();
		routingOptionsHelper = app.getRoutingOptionsHelper();
		mapActivity = getMapActivity();
		if (applicationMode == null) {
			applicationMode = routingHelper.getAppMode();
		}
		if (dialogMode == null) {
			dialogMode = DialogMode.DIRECTIONS;
		}
		selectedModeColor = applicationMode.getProfileColor(nightMode);
		voiceMuteChangeListener = new StateChangedListener<Boolean>() {
			@Override
			public void stateChanged(Boolean change) {
				app.runInUIThread(() -> updateWhenMuteChanged());
			}
		};
		useHeightChangeListener = new StateChangedListener<Boolean>() {
			@Override
			public void stateChanged(Boolean change) {
				app.runInUIThread(() -> updateWhenUseHeightChanged());
			}
		};
		useHeightPref = settings.getCustomRoutingBooleanProperty(USE_HEIGHT_OBSTACLES, false);
		reliefParameters = getReliefParameters();
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		items.add(new TitleItem(app.getString(R.string.shared_string_settings), ColorUtilities.getActiveColorId(nightMode)));

		OsmAndAppCustomization customization = app.getAppCustomization();
		List<LocalRoutingParameter> list = getRoutingParameters(applicationMode);
		for (LocalRoutingParameter optionsItem : list) {
			if (!dialogMode.isAvailableParameter(optionsItem) || !customization.isFeatureEnabled(optionsItem.getKey())) {
				continue;
			}

			if (optionsItem instanceof DividerItem) {
				if (isDividerRequired()) {
					items.add(new DividerStartItem(app));
				}
			} else if (optionsItem instanceof MuteSoundRoutingParameter) {
				items.add(createMuteSoundItem(optionsItem));
			} else if (optionsItem instanceof ShowAlongTheRouteItem) {
				items.add(createShowAlongTheRouteItem(optionsItem));
			} else if (optionsItem instanceof RouteSimulationItem) {
				items.add(createRouteSimulationItem(optionsItem));
			} else if (optionsItem instanceof AvoidPTTypesRoutingParameter) {
				items.add(createAvoidPTTypesItem(optionsItem));
			} else if (optionsItem instanceof AvoidRoadsRoutingParameter) {
				items.add(createAvoidRoadsItem(optionsItem));
			} else if (optionsItem instanceof GpxLocalRoutingParameter) {
				items.add(createGpxRoutingItem(optionsItem));
			} else if (optionsItem instanceof TimeConditionalRoutingItem) {
				items.add(createTimeConditionalRoutingItem(optionsItem));
			} else if (optionsItem instanceof OtherSettingsRoutingParameter) {
				items.add(createOtherSettingsRoutingItem(optionsItem));
			} else if (optionsItem instanceof CustomizeRouteLineRoutingParameter) {
				items.add(createCustomizeRouteLineRoutingItem(optionsItem));
			} else if (optionsItem instanceof CalculateAltitudeItem) {
				items.add(createCalculateAltitudeItem(optionsItem));
			} else if (USE_HEIGHT_OBSTACLES.equals(optionsItem.getKey()) && hasReliefParameters()) {
				items.add(inflateElevationParameter(optionsItem));
			} else {
				inflateRoutingParameter(optionsItem);
			}
		}
	}

	private boolean isDividerRequired() {
		// do not show two dividers at once
		return items.size() > 1 && !(items.get(items.size() - 1) instanceof DividerStartItem);
	}

	@Override
	public void onResume() {
		super.onResume();
		for (BaseBottomSheetItem item : items) {
			if (item instanceof BottomSheetItemWithCompoundButton) {
				BottomSheetItemWithCompoundButton itemWithCompoundButton = (BottomSheetItemWithCompoundButton) item;
				itemWithCompoundButton.setChecked(itemWithCompoundButton.isChecked());
			}
		}
		currentUseHeightState = useHeightPref.getModeValue(applicationMode);
		currentMuteState = app.getSettings().VOICE_MUTE.getModeValue(applicationMode);

		useHeightPref.addListener(useHeightChangeListener);
		app.getSettings().VOICE_MUTE.addListener(voiceMuteChangeListener);
	}

	@Override
	public void onPause() {
		super.onPause();
		useHeightPref.removeListener(useHeightChangeListener);
		app.getSettings().VOICE_MUTE.removeListener(voiceMuteChangeListener);
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_close;
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == AvoidRoadsBottomSheetDialogFragment.REQUEST_CODE
				&& resultCode == AvoidRoadsBottomSheetDialogFragment.OPEN_AVOID_ROADS_DIALOG_REQUEST_CODE) {
			dismiss();
		}
		if (requestCode == ShowAlongTheRouteBottomSheet.REQUEST_CODE
				&& resultCode == ShowAlongTheRouteBottomSheet.SHOW_CONTENT_ITEM_REQUEST_CODE) {
			mapActivity.getMapRouteInfoMenu().hide();
			dismiss();
		}
	}

	public void updateWhenMuteChanged() {
		boolean changedState = app.getSettings().VOICE_MUTE.getModeValue(applicationMode);
		if (changedState != currentMuteState) {
			currentMuteState = changedState;
			updateMenuItems();
			updateMenu();
		}
	}

	public void updateWhenUseHeightChanged() {
		boolean changedState = useHeightPref.getModeValue(applicationMode);
		if (changedState != currentUseHeightState) {
			currentUseHeightState = changedState;
			updateMenuItems();
			updateMenu();
		}
	}

	private BaseBottomSheetItem createMuteSoundItem(LocalRoutingParameter optionsItem) {
		boolean active = !routingHelper.getVoiceRouter().isMuteForMode(applicationMode);
		View itemView = UiUtilities.getInflater(app, nightMode).inflate(
				R.layout.bottom_sheet_item_with_descr_switch_and_additional_button_56dp, null, false);
		ImageView icon = itemView.findViewById(R.id.icon);
		TextView tvTitle = itemView.findViewById(R.id.title);
		TextView tvDescription = itemView.findViewById(R.id.description);
		View basicItem = itemView.findViewById(R.id.basic_item_body);
		CompoundButton cb = itemView.findViewById(R.id.compound_button);
		View voicePromptsBtn = itemView.findViewById(R.id.additional_button);
		ImageView voicePromptsBtnImage = itemView.findViewById(R.id.additional_button_icon);

		tvTitle.setText(getString(R.string.shared_string_sound));
		tvDescription.setText(getString(R.string.voice_announcements));
		icon.setImageDrawable(getContentIcon(active ?
				optionsItem.getActiveIconId() : optionsItem.getDisabledIconId()));
		cb.setChecked(active);
		cb.setFocusable(false);
		UiUtilities.setupCompoundButton(nightMode, selectedModeColor, cb);

		basicItem.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				routingOptionsHelper.addNewRouteMenuParameter(applicationMode, optionsItem);
				boolean active = !routingHelper.getVoiceRouter().isMuteForMode(applicationMode);
				routingHelper.getVoiceRouter().setMuteForMode(applicationMode, active);
				String voiceProvider = app.getSettings().VOICE_PROVIDER.getModeValue(applicationMode);
				if (voiceProvider == null || OsmandSettings.VOICE_PROVIDER_NOT_USE.equals(voiceProvider)) {
					VoiceLanguageBottomSheetFragment.showInstance(mapActivity.getSupportFragmentManager(),
							RouteOptionsBottomSheet.this, applicationMode, usedOnMap);
				} else {
					cb.setChecked(!active);
					icon.setImageDrawable(getContentIcon(!active ? optionsItem.getActiveIconId() : optionsItem.getDisabledIconId()));
				}
				updateMenu();
			}
		});

		Drawable drawable = app.getUIUtilities().getIcon(R.drawable.ic_action_settings,
				nightMode ? R.color.icon_color_default_dark : R.color.icon_color_default_light);
		if (Build.VERSION.SDK_INT >= 21) {
			Drawable activeDrawable = app.getUIUtilities().getPaintedIcon(R.drawable.ic_action_settings, selectedModeColor);
			drawable = AndroidUtils.createPressedStateListDrawable(drawable, activeDrawable);
		}
		voicePromptsBtnImage.setImageDrawable(drawable);

		voicePromptsBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				BaseSettingsFragment.showInstance(mapActivity, SettingsScreenType.VOICE_ANNOUNCES, applicationMode);
				dismiss();
			}
		});

		return new BaseBottomSheetItem.Builder()
				.setCustomView(itemView)
				.create();
	}

	private BaseBottomSheetItem inflateElevationParameter(LocalRoutingParameter parameter) {
		BottomSheetItemWithCompoundButton[] item = new BottomSheetItemWithCompoundButton[1];
		boolean active = !useHeightPref.getModeValue(applicationMode);
		View itemView = UiUtilities.getInflater(app, nightMode).inflate(
				R.layout.bottom_sheet_item_with_switch_and_dialog, null, false);
		SwitchCompat switchButton = itemView.findViewById(R.id.compound_button);
		View itemsContainer = itemView.findViewById(R.id.selectable_list_item);
		itemsContainer.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (USE_HEIGHT_OBSTACLES.equals(parameter.getKey()) && hasReliefParameters()) {
					FragmentManager fm = getFragmentManager();
					if (fm != null) {
						ElevationDateBottomSheet.showInstance(fm, applicationMode, RouteOptionsBottomSheet.this, false);
					}
				}
			}
		});

		switchButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				applyParameter(item[0], parameter);
				item[0].setDescription(getElevationDescription(parameter));
				switchButton.setChecked(parameter.isSelected(settings));
			}
		});

		item[0] = (BottomSheetItemWithCompoundButton) new BottomSheetItemWithCompoundButton.Builder()
				.setChecked(!active)
				.setCompoundButtonColor(selectedModeColor)
				.setDescription(getElevationDescription(parameter))
				.setIcon(getContentIcon(active ? parameter.getActiveIconId() : parameter.getDisabledIconId()))
				.setTitle(getString(R.string.routing_attr_height_obstacles_name))
				.setCustomView(itemView)
				.create();

		return item[0];
	}

	private String getElevationDescription(LocalRoutingParameter parameter) {
		String description;
		if (parameter.isSelected(settings)) {
			description = getString(R.string.shared_string_enabled);
			for (RoutingParameter routingParameter : reliefParameters) {
				if (isRoutingParameterSelected(settings, applicationMode, routingParameter)) {
					description = getString(R.string.ltr_or_rtl_combine_via_comma, description,
							getRoutingParameterTitle(app, routingParameter));
				}
			}
		} else {
			description = getString(R.string.shared_string_disabled);
		}
		return description;
	}

	private BaseBottomSheetItem createTimeConditionalRoutingItem(LocalRoutingParameter optionsItem) {
		BottomSheetItemWithCompoundButton[] timeConditionalRoutingItem = new BottomSheetItemWithCompoundButton[1];
		timeConditionalRoutingItem[0] = (BottomSheetItemWithCompoundButton) new BottomSheetItemWithCompoundButton.Builder()
				.setCompoundButtonColor(selectedModeColor)
				.setChecked(settings.ENABLE_TIME_CONDITIONAL_ROUTING.getModeValue(applicationMode))
				.setIcon(getContentIcon((optionsItem.getActiveIconId())))
				.setTitle(getString(R.string.temporary_conditional_routing))
				.setLayoutId(R.layout.bottom_sheet_item_with_switch_56dp)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						boolean enabled = !settings.ENABLE_TIME_CONDITIONAL_ROUTING.getModeValue(applicationMode);
						settings.ENABLE_TIME_CONDITIONAL_ROUTING.setModeValue(applicationMode, enabled);
						timeConditionalRoutingItem[0].setChecked(enabled);
						app.getRoutingHelper().onSettingsChanged(applicationMode, true);
					}
				})
				.create();
		return timeConditionalRoutingItem[0];
	}

	private BaseBottomSheetItem createShowAlongTheRouteItem(LocalRoutingParameter optionsItem) {
		return new SimpleBottomSheetItem.Builder()
				.setIcon(getContentIcon((optionsItem.getActiveIconId())))
				.setTitle(getString(R.string.show_along_the_route))
				.setLayoutId(R.layout.bottom_sheet_item_simple_56dp)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						routingOptionsHelper.addNewRouteMenuParameter(applicationMode, optionsItem);
						FragmentManager fm = getFragmentManager();
						if (fm == null) {
							return;
						}
						Bundle args = new Bundle();
						ShowAlongTheRouteBottomSheet fragment = new ShowAlongTheRouteBottomSheet();
						fragment.setUsedOnMap(true);
						fragment.setArguments(args);
						fragment.setTargetFragment(RouteOptionsBottomSheet.this, ShowAlongTheRouteBottomSheet.REQUEST_CODE);
						fragment.show(fm, ShowAlongTheRouteBottomSheet.TAG);
						updateMenu();
					}
				}).create();
	}

	private BaseBottomSheetItem createRouteSimulationItem(LocalRoutingParameter optionsItem) {

		View itemView = UiUtilities.getInflater(app, nightMode).inflate(
				R.layout.bottom_sheet_item_with_descr_switch_and_additional_button_56dp, null, false);
		ImageView icon = itemView.findViewById(R.id.icon);
		TextView tvTitle = itemView.findViewById(R.id.title);
		itemView.findViewById(R.id.description).setVisibility(View.GONE);
		View basicItem = itemView.findViewById(R.id.basic_item_body);
		CompoundButton cb = itemView.findViewById(R.id.compound_button);
		View settingBtn = itemView.findViewById(R.id.additional_button);
		ImageView settingBtnImage = itemView.findViewById(R.id.additional_button_icon);

		tvTitle.setText(getString(R.string.simulate_navigation));
		icon.setImageDrawable(getContentIcon(R.drawable.ic_action_start_navigation));
		cb.setChecked(settings.simulateNavigation);
		cb.setFocusable(false);
		UiUtilities.setupCompoundButton(nightMode, selectedModeColor, cb);

		basicItem.setOnClickListener(v -> {
			boolean enabled = !settings.simulateNavigation;
			settings.simulateNavigation = enabled;
			cb.setChecked(enabled);
			OsmAndLocationSimulation sim = app.getLocationProvider().getLocationSimulation();
			if (sim.isRouteAnimating()) {
				sim.startStopRouteAnimation(getActivity());
			} else if (routingHelper.isFollowingMode() && routingHelper.isRouteCalculated() && !routingHelper.isRouteBeingCalculated()) {
				sim.startStopRouteAnimation(getActivity());
			}
		});

		Drawable drawable = app.getUIUtilities().getIcon(R.drawable.ic_action_settings,
				nightMode ? R.color.icon_color_default_dark : R.color.icon_color_default_light);
		Drawable activeDrawable = app.getUIUtilities().getPaintedIcon(R.drawable.ic_action_settings, selectedModeColor);
		drawable = AndroidUtils.createPressedStateListDrawable(drawable, activeDrawable);
		settingBtnImage.setImageDrawable(drawable);
		settingBtn.setOnClickListener(v -> {
			BaseSettingsFragment.showInstance(mapActivity, SettingsScreenType.SIMULATION_NAVIGATION, applicationMode);
			dismiss();
		});

		return new BaseBottomSheetItem.Builder()
				.setCustomView(itemView)
				.create();
	}


	private BaseBottomSheetItem createAvoidRoadsItem(LocalRoutingParameter optionsItem) {
		return new SimpleBottomSheetItem.Builder()
				.setIcon(getContentIcon((optionsItem.getActiveIconId())))
				.setTitle(getString(R.string.impassable_road))
				.setLayoutId(R.layout.bottom_sheet_item_simple_56dp)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						routingOptionsHelper.addNewRouteMenuParameter(applicationMode, optionsItem);
						AvoidRoadsBottomSheetDialogFragment avoidRoadsFragment = new AvoidRoadsBottomSheetDialogFragment();
						avoidRoadsFragment.setTargetFragment(RouteOptionsBottomSheet.this, AvoidRoadsBottomSheetDialogFragment.REQUEST_CODE);
						avoidRoadsFragment.setCompoundButtonColor(selectedModeColor);
						avoidRoadsFragment.setApplicationMode(applicationMode);
						avoidRoadsFragment.show(mapActivity.getSupportFragmentManager(), AvoidRoadsBottomSheetDialogFragment.TAG);
						updateMenu();
					}
				})
				.create();
	}


	private BaseBottomSheetItem createAvoidPTTypesItem(LocalRoutingParameter optionsItem) {
		return new SimpleBottomSheetItem.Builder()
				.setIcon(getContentIcon((optionsItem.getActiveIconId())))
				.setTitle(getString(R.string.avoid_pt_types))
				.setLayoutId(R.layout.bottom_sheet_item_simple_56dp)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						routingOptionsHelper.addNewRouteMenuParameter(applicationMode, optionsItem);
						AvoidRoadsBottomSheetDialogFragment avoidRoadsFragment = new AvoidRoadsBottomSheetDialogFragment();
						avoidRoadsFragment.setHideImpassableRoads(true);
						avoidRoadsFragment.setTargetFragment(RouteOptionsBottomSheet.this, AvoidRoadsBottomSheetDialogFragment.REQUEST_CODE);
						avoidRoadsFragment.setCompoundButtonColor(selectedModeColor);
						avoidRoadsFragment.show(mapActivity.getSupportFragmentManager(), AvoidRoadsBottomSheetDialogFragment.TAG);
						updateMenu();
					}
				})
				.create();
	}

	private BaseBottomSheetItem createGpxRoutingItem(LocalRoutingParameter optionsItem) {
		GPXRouteParamsBuilder routeParamsBuilder = mapActivity.getRoutingHelper().getCurrentGPXRoute();
		String description = null;
		int descriptionColorId;
		if (routeParamsBuilder == null) {
			descriptionColorId = ColorUtilities.getSecondaryTextColorId(nightMode);
			description = mapActivity.getString(R.string.follow_track_descr);
		} else {
			descriptionColorId = ColorUtilities.getActiveColorId(nightMode);
			GPXFile gpxFile = routeParamsBuilder.getFile();
			if (!Algorithms.isEmpty(gpxFile.path)) {
				description = new File(gpxFile.path).getName();
			} else if (!Algorithms.isEmpty(gpxFile.tracks)) {
				description = gpxFile.tracks.get(0).name;
			}
		}

		return new BottomSheetItemWithDescription.Builder()
				.setDescription(description)
				.setDescriptionColorId(descriptionColorId)
				.setIcon(getContentIcon(optionsItem.getActiveIconId()))
				.setTitle(getString(R.string.follow_track))
				.setLayoutId(R.layout.bottom_sheet_item_with_descr_56dp)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						MapRouteInfoMenu mapRouteInfoMenu = mapActivity.getMapRouteInfoMenu();
						mapRouteInfoMenu.hide();
						mapRouteInfoMenu.chooseAndShowFollowTrack();
						dismiss();
					}
				})
				.create();
	}

	private BaseBottomSheetItem createOtherSettingsRoutingItem(LocalRoutingParameter optionsItem) {
		return new SimpleBottomSheetItem.Builder()
				.setIcon(getContentIcon(optionsItem.getActiveIconId()))
				.setTitle(getString(R.string.routing_settings_2))
				.setLayoutId(R.layout.bottom_sheet_item_simple_56dp)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						dismiss();

						if (dialogMode == DialogMode.PLAN_ROUTE) {
							Fragment fragment = getTargetFragment();
							if (fragment instanceof MeasurementToolFragment) {
								((MeasurementToolFragment) fragment).getOnBackPressedCallback().setEnabled(false);
							}
						}

						Bundle args = new Bundle();
						args.putString(DIALOG_MODE_KEY, dialogMode.name());
						BaseSettingsFragment.showInstance(mapActivity,
								SettingsScreenType.NAVIGATION, applicationMode, args, null);
					}
				})
				.create();
	}

	private BaseBottomSheetItem createCustomizeRouteLineRoutingItem(LocalRoutingParameter parameter) {
		return new SimpleBottomSheetItem.Builder()
				.setIcon(getContentIcon(parameter.getActiveIconId()))
				.setTitle(getString(R.string.customize_route_line))
				.setLayoutId(R.layout.bottom_sheet_item_simple_56dp)
				.setOnClickListener(v -> {
					if (mapActivity != null) {
						MapRouteInfoMenu mapRouteInfoMenu = mapActivity.getMapRouteInfoMenu();
						mapRouteInfoMenu.hide();
						mapRouteInfoMenu.customizeRouteLine();
						dismiss();
					}
				}).create();
	}

	private void inflateRoutingParameter(LocalRoutingParameter parameter) {
		if (parameter != null) {
			BottomSheetItemWithCompoundButton[] item = new BottomSheetItemWithCompoundButton[1];
			BottomSheetItemWithCompoundButton.Builder builder = new BottomSheetItemWithCompoundButton.Builder();
			builder.setCompoundButtonColor(selectedModeColor);
			int iconId = -1;
			if (parameter.routingParameter != null || parameter instanceof RoutingOptionsHelper.OtherLocalRoutingParameter) {
				builder.setTitle(parameter.getText(mapActivity));
				iconId = parameter.isSelected(settings) ? parameter.getActiveIconId() : parameter.getDisabledIconId();
			}
			if (parameter instanceof LocalRoutingParameterGroup) {
				LocalRoutingParameterGroup group = (LocalRoutingParameterGroup) parameter;
				LocalRoutingParameter selected = group.getSelected(settings);
				iconId = selected != null ? parameter.getActiveIconId() : parameter.getDisabledIconId();
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
								updateMenu();
							}
						});
					}
				});
			} else {
				builder.setLayoutId(R.layout.bottom_sheet_item_with_switch_56dp);
				if (parameter.routingParameter != null && parameter.routingParameter.getId().equals(GeneralRouter.USE_SHORTEST_WAY)) {
					// if short route settings - it should be inverse of fast_route_mode
					builder.setChecked(!settings.FAST_ROUTE_MODE.getModeValue(applicationMode));
				} else {
					builder.setChecked(parameter.isSelected(settings));
				}
				builder.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						applyParameter(item[0], parameter);
					}
				});
			}
			if (iconId != -1) {
				builder.setIcon(getContentIcon(iconId));
			}
			item[0] = builder.create();
			items.add(item[0]);
		}
	}

	private BaseBottomSheetItem createCalculateAltitudeItem(LocalRoutingParameter parameter) {
		return new SimpleBottomSheetItem.Builder()
				.setTitle(getString(R.string.get_altitude_information))
				.setLayoutId(R.layout.bottom_sheet_item_simple_56dp)
				.setOnClickListener(v -> {
					if (mapActivity != null) {
						int segmentIndex = settings.GPX_SEGMENT_INDEX.get();
						FragmentManager manager = mapActivity.getSupportFragmentManager();
						TrackAltitudeBottomSheet.showInstance(manager, this, segmentIndex);
					}
				}).create();
	}

	@Override
	public void attachToRoadsSelected(int segmentIndex) {
		GPXFile gpxFile = GpxUiHelper.makeGpxFromRoute(routingHelper.getRoute(), app);
		openPlanRoute(gpxFile, segmentIndex, ATTACH_ROADS_MODE | FOLLOW_TRACK_MODE);
	}

	@Override
	public void calculateOnlineSelected(int segmentIndex) {
		GPXFile gpxFile = GpxUiHelper.makeGpxFromRoute(routingHelper.getRoute(), app);
		gpxFile.path = FileUtils.getTempDir(app).getAbsolutePath() + "/route" + GPX_FILE_EXT;
		SaveGpxHelper.saveGpx(gpxFile, errorMessage -> {
			if (errorMessage == null) {
				openPlanRoute(gpxFile, segmentIndex, CALCULATE_SRTM_MODE | FOLLOW_TRACK_MODE);
			}
		});
	}

	@Override
	public void calculateOfflineSelected(int segmentIndex) {
		GPXFile gpxFile = GpxUiHelper.makeGpxFromRoute(routingHelper.getRoute(), app);
		gpxFile.path = FileUtils.getTempDir(app).getAbsolutePath() + "/route" + GPX_FILE_EXT;
		SaveGpxHelper.saveGpx(gpxFile, errorMessage -> {
			if (errorMessage == null) {
				openPlanRoute(gpxFile, segmentIndex, CALCULATE_HEIGHTMAP_MODE | FOLLOW_TRACK_MODE);
			}
		});
	}

	public void openPlanRoute(@NonNull GPXFile gpxFile, int segmentIndex, int mode) {
		if (mapActivity != null) {
			MeasurementToolFragment.showInstance(mapActivity, gpxFile, segmentIndex, mode);
			mapActivity.getMapRouteInfoMenu().hide();
			dismiss();
		}
	}

	private boolean hasReliefParameters() {
		return !Algorithms.isEmpty(reliefParameters);
	}

	private List<RoutingParameter> getReliefParameters() {
		List<RoutingParameter> reliefFactorParameters = new ArrayList<>();
		GeneralRouter router = app.getRouter(applicationMode);
		if (router != null) {
			Map<String, RoutingParameter> parameters = RoutingHelperUtils.getParametersForDerivedProfile(applicationMode, router);
			for (Map.Entry<String, RoutingParameter> entry : parameters.entrySet()) {
				RoutingParameter routingParameter = entry.getValue();
				if (RELIEF_SMOOTHNESS_FACTOR.equals(routingParameter.getGroup())) {
					reliefFactorParameters.add(routingParameter);
				}
			}
		}
		return reliefFactorParameters;
	}

	private void applyParameter(BottomSheetItemWithCompoundButton bottomSheetItem, LocalRoutingParameter parameter) {
		routingOptionsHelper.addNewRouteMenuParameter(applicationMode, parameter);
		boolean selected = !parameter.isSelected(settings);
		routingOptionsHelper.applyRoutingParameter(parameter, selected);
		bottomSheetItem.setChecked(selected);
		int iconId = selected ? parameter.getActiveIconId() : parameter.getDisabledIconId();
		if (iconId != -1) {
			bottomSheetItem.setIcon(getContentIcon(iconId));
		}
		updateMenu();
	}

	private void updateMenu() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.getMapRouteInfoMenu().updateMenu();
		}
	}

	public List<LocalRoutingParameter> getRoutingParameters(ApplicationMode applicationMode) {
		List<String> routingParameters = new ArrayList<>();

		boolean osmandRouter = applicationMode.getRouteService() == RouteService.OSMAND;
		if (!osmandRouter) {
			if (applicationMode.getRouteService() == RouteService.STRAIGHT) {
				routingParameters.addAll(AppModeOptions.STRAIGHT.routingParameters);
			} else if (applicationMode.getRouteService() == RouteService.DIRECT_TO) {
				routingParameters.addAll(AppModeOptions.DIRECT_TO.routingParameters);
			} else {
				routingParameters.addAll(AppModeOptions.OTHER.routingParameters);
			}
		} else if (applicationMode.isDerivedRoutingFrom(ApplicationMode.CAR)) {
			routingParameters.addAll(AppModeOptions.CAR.routingParameters);
		} else if (applicationMode.isDerivedRoutingFrom(ApplicationMode.BICYCLE)) {
			routingParameters.addAll(AppModeOptions.BICYCLE.routingParameters);
		} else if (applicationMode.isDerivedRoutingFrom(ApplicationMode.PEDESTRIAN)) {
			routingParameters.addAll(AppModeOptions.PEDESTRIAN.routingParameters);
		} else if (applicationMode.isDerivedRoutingFrom(ApplicationMode.PUBLIC_TRANSPORT)) {
			routingParameters.addAll(AppModeOptions.PUBLIC_TRANSPORT.routingParameters);
		} else {
			routingParameters.addAll(AppModeOptions.OTHER.routingParameters);
		}
		return routingOptionsHelper.getRoutingParameters(applicationMode, routingParameters);
	}

	private MapActivity getMapActivity() {
		return (MapActivity) getActivity();
	}

	public static void showInstance(@NonNull MapActivity mapActivity) {
		showInstance(mapActivity, null, DialogMode.DIRECTIONS, null);
	}

	public static void showInstance(@NonNull MapActivity mapActivity,
	                                @Nullable Fragment targetFragment,
	                                @NonNull DialogMode dialogMode,
	                                @Nullable String appModeKey) {
		try {
			FragmentManager fm = mapActivity.getSupportFragmentManager();
			if (!fm.isStateSaved()) {
				RouteOptionsBottomSheet fragment = new RouteOptionsBottomSheet();
				Bundle args = new Bundle();
				args.putString(APP_MODE_KEY, appModeKey);
				args.putString(DIALOG_MODE_KEY, dialogMode.name());
				fragment.setArguments(args);
				fragment.setTargetFragment(targetFragment, 0);
				fragment.show(fm, TAG);
			}
		} catch (RuntimeException e) {
			LOG.error("showInstance", e);
		}
	}

	public enum AppModeOptions {

		CAR(MuteSoundRoutingParameter.KEY,
				DividerItem.KEY,
				AvoidRoadsRoutingParameter.KEY,
				ShowAlongTheRouteItem.KEY,
				DividerItem.KEY,
				GpxLocalRoutingParameter.KEY,
				DividerItem.KEY,
				GeneralRouter.ALLOW_PRIVATE,
				GeneralRouter.ALLOW_PRIVATE_FOR_TRUCK,
				GeneralRouter.USE_SHORTEST_WAY,
				TimeConditionalRoutingItem.KEY,
				DividerItem.KEY,
				OtherSettingsRoutingParameter.KEY,
				CustomizeRouteLineRoutingParameter.KEY,
				RouteSimulationItem.KEY),

		BICYCLE(MuteSoundRoutingParameter.KEY,
				DRIVING_STYLE,
				GeneralRouter.USE_HEIGHT_OBSTACLES,
				DividerItem.KEY,
				GeneralRouter.ALLOW_MOTORWAYS,
				AvoidRoadsRoutingParameter.KEY,
				ShowAlongTheRouteItem.KEY,
				GpxLocalRoutingParameter.KEY,
				TimeConditionalRoutingItem.KEY,
				DividerItem.KEY,
				OtherSettingsRoutingParameter.KEY,
				CustomizeRouteLineRoutingParameter.KEY,
				RouteSimulationItem.KEY),

		PEDESTRIAN(MuteSoundRoutingParameter.KEY,
				GeneralRouter.USE_HEIGHT_OBSTACLES,
				DividerItem.KEY,
				AvoidRoadsRoutingParameter.KEY,
				ShowAlongTheRouteItem.KEY,
				GpxLocalRoutingParameter.KEY,
				TimeConditionalRoutingItem.KEY,
				DividerItem.KEY,
				OtherSettingsRoutingParameter.KEY,
				CustomizeRouteLineRoutingParameter.KEY,
				RouteSimulationItem.KEY),

		PUBLIC_TRANSPORT(MuteSoundRoutingParameter.KEY,
				DividerItem.KEY,
				AvoidPTTypesRoutingParameter.KEY,
				// ShowAlongTheRouteItem.KEY,
				// DividerItem.KEY,
				TimeConditionalRoutingItem.KEY,
				OtherSettingsRoutingParameter.KEY),

		OTHER(MuteSoundRoutingParameter.KEY,
				DividerItem.KEY,
				AvoidRoadsRoutingParameter.KEY,
				ShowAlongTheRouteItem.KEY,
				GpxLocalRoutingParameter.KEY,
				TimeConditionalRoutingItem.KEY,
				DividerItem.KEY,
				OtherSettingsRoutingParameter.KEY,
				CustomizeRouteLineRoutingParameter.KEY,
				RouteSimulationItem.KEY),

		STRAIGHT(MuteSoundRoutingParameter.KEY,
				DividerItem.KEY,
				ShowAlongTheRouteItem.KEY,
				DividerItem.KEY,
				GpxLocalRoutingParameter.KEY,
				OtherSettingsRoutingParameter.KEY,
				CustomizeRouteLineRoutingParameter.KEY,
				RouteSimulationItem.KEY),

		DIRECT_TO(MuteSoundRoutingParameter.KEY,
				DividerItem.KEY,
				ShowAlongTheRouteItem.KEY,
				DividerItem.KEY,
				GpxLocalRoutingParameter.KEY,
				OtherSettingsRoutingParameter.KEY,
				CustomizeRouteLineRoutingParameter.KEY,
				RouteSimulationItem.KEY);


		List<String> routingParameters;

		AppModeOptions(String... routingParameters) {
			this.routingParameters = Arrays.asList(routingParameters);
		}
	}
}