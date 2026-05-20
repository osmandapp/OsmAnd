package net.osmand.plus.plugins.srtm.building;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseFullScreenFragment;
import net.osmand.plus.base.ProgressHelper;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.srtm.SRTMPlugin;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.InsetTarget;
import net.osmand.plus.utils.InsetTarget.Type;
import net.osmand.plus.utils.InsetTargetsCollection;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.TextViewEx;
import net.osmand.plus.widgets.multistatetoggle.IconToggleButton;

public class Buildings3DFragment extends BaseFullScreenFragment {

	public static final String TAG = Buildings3DFragment.class.getSimpleName();

	private final SRTMPlugin plugin = PluginsHelper.requirePlugin(SRTMPlugin.class);

	private ViewGroup contentContainer;

	private View view;
	private IconToggleButton detailsLevelToggleButton;
	private IconToggleButton viewDistanceToggleButton;

	@Override
	protected boolean isUsedOnMap() {
		return true;
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
	                         @Nullable Bundle savedInstanceState) {
		updateNightMode();
		view = inflate(R.layout.fragment_3d_buildings, container, false);
		showHideTopShadow();
		setupMainToggle();
		setupContent(view);
		updateUiMode();
		return view;
	}

	private void setupMainToggle() {
		View button = view.findViewById(R.id.main_toggle);
		boolean enabled = isEnabled();
		int profileColor = getAppModeColor(nightMode);

		TextView tvTitle = button.findViewById(R.id.title_tv);
		tvTitle.setText(R.string.enable_3d_objects);
		updateUiMode();

		CompoundButton cb = button.findViewById(R.id.switch_compat);
		cb.setChecked(enabled);
		cb.setVisibility(View.VISIBLE);
		UiUtilities.setupCompoundButton(nightMode, profileColor, cb);

		cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
			cb.setChecked(isChecked);
			plugin.ENABLE_3D_MAP_OBJECTS.set(isChecked);
			refreshMap();
			updateUiMode();
		});

		button.setOnClickListener(v -> {
			boolean newState = !cb.isChecked();
			cb.setChecked(newState);
		});

		updateUiMode();
		setupSelectableBackground(button);
	}

	private void updateUiMode() {
		boolean enabled = isEnabled();
		View button = view.findViewById(R.id.main_toggle);
		int defIconColor = ColorUtilities.getSecondaryIconColor(app, nightMode);
		int profileColor = getAppModeColor(nightMode);

		ImageView ivIcon = button.findViewById(R.id.icon_iv);
		ivIcon.setImageResource(R.drawable.ic_action_3d_buildings);
		ivIcon.setColorFilter(enabled ? profileColor : defIconColor);

		TextView tvSummary = button.findViewById(R.id.state_tv);
		tvSummary.setText(enabled ? R.string.shared_string_on : R.string.shared_string_off);

		AndroidUiHelper.updateVisibility(contentContainer, enabled);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.title_divider), !enabled);
	}

	private boolean isEnabled() {
		return plugin.ENABLE_3D_MAP_OBJECTS.get();
	}

	private void showHideTopShadow() {
		boolean portrait = AndroidUiHelper.isOrientationPortrait(requireActivity());
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.shadow_on_map), portrait);
	}

	protected void setupContent(@NonNull View view) {
		contentContainer = view.findViewById(R.id.content_container);

		setupAppearance(contentContainer);
		setupPerformance(contentContainer);

		setupSunParameters(contentContainer);
	}

	private void setupAppearance(@NonNull ViewGroup container) {
		View appearanceContainer = container.findViewById(R.id.appearance_container);
		AndroidUtils.setBackgroundColor(appearanceContainer.getContext(), appearanceContainer, ColorUtilities.getListBgColorId(nightMode));

		TextViewEx title = appearanceContainer.findViewById(R.id.title);
		title.setText(R.string.shared_string_appearance);
		float alpha = plugin.BUILDINGS_3D_ALPHA.get();
		int progress = ProgressHelper.normalizeProgressPercent((int) (alpha * 100));

		TextView visibilityTv = container.findViewById(R.id.opacity_value);
		visibilityTv.setText(String.format("%s%%", progress));
		TextView colorStyleTv = container.findViewById(R.id.color_scheme_name);
		colorStyleTv.setText(Buildings3DColorType.Companion.getById(plugin.BUILDINGS_3D_COLOR_STYLE.get()).getLabelId());

		View btnColor = container.findViewById(R.id.color_container);
		btnColor.setOnClickListener((v) -> callMapActivity(mapActivity -> {
			mapActivity.getDashboard().hideDashboard();
			Buildings3DColorScreenController.showDialog(mapActivity.getSupportFragmentManager(), app);
		}));
		setupSelectableBackground(btnColor);

		View btnVisibility = container.findViewById(R.id.opacity_container);
		btnVisibility.setOnClickListener((v) -> callMapActivity(mapActivity -> {
			mapActivity.getDashboard().hideDashboard();
			Buildings3DVisibilityController.showDialog(app, mapActivity.getSupportFragmentManager());
		}));
		setupSelectableBackground(btnVisibility);
	}

	private void setupPerformance(@NonNull ViewGroup container) {
		View performanceContainer = container.findViewById(R.id.performance_container);
		AndroidUtils.setBackgroundColor(performanceContainer.getContext(), performanceContainer, ColorUtilities.getListBgColorId(nightMode));
		TextViewEx title = performanceContainer.findViewById(R.id.title);
		title.setText(R.string.performance);

		setupDetailsLevelToggleButtons(performanceContainer.findViewById(R.id.details_lvl_container));
		setupViewDistanceToggleButtons(performanceContainer.findViewById(R.id.view_distance_container));
	}

	private void setupSunParameters(@NonNull ViewGroup container) {
		View sunContainer = container.findViewById(R.id.sun_container);
		AndroidUtils.setBackgroundColor(sunContainer.getContext(), sunContainer, ColorUtilities.getListBgColorId(nightMode));

		sunContainer.setOnClickListener((v) -> callMapActivity(mapActivity -> {
			mapActivity.getDashboard().hideDashboard();
			SunParametersController.showDialog(app, mapActivity.getSupportFragmentManager());
		}));
		setupSelectableBackground(sunContainer);

		boolean visible = PluginsHelper.isDevelopment();
		AndroidUiHelper.updateVisibility(container.findViewById(R.id.sun_divider), visible);
		AndroidUiHelper.updateVisibility(container.findViewById(R.id.sun_parameters_container), visible);
	}

	private void setupDetailsLevelToggleButtons(@NonNull View view) {
		IconToggleButton.IconRadioItem low = new IconToggleButton.IconRadioItem(R.drawable.ic_action_3d_buildings_level_of_detail_1);
		low.setOnClickListener((radioItem, v) -> {
			onDetailsLevelChanged(low, false);
			return true;
		});
		low.setContentDescription(app.getString(R.string.building_3d_low_details_content_desc));

		IconToggleButton.IconRadioItem high = new IconToggleButton.IconRadioItem(R.drawable.ic_action_3d_buildings_level_of_detail_2);
		high.setOnClickListener((radioItem, v) -> {
			onDetailsLevelChanged(high, true);
			return true;
		});
		high.setContentDescription(app.getString(R.string.building_3d_high_details_content_desc));

		LinearLayout container = view.findViewById(R.id.custom_radio_buttons);
		detailsLevelToggleButton = new IconToggleButton(app, container, nightMode);
		detailsLevelToggleButton.setItems(low, high);
		detailsLevelToggleButton.setSelectedItem(plugin.BUILDINGS_3D_DETAIL_LEVEL.get() ? high : low);
	}

	private void setupViewDistanceToggleButtons(@NonNull View view) {
		int level = plugin.BUILDINGS_3D_VIEW_DISTANCE.get();
		IconToggleButton.IconRadioItem low = new IconToggleButton.IconRadioItem(R.drawable.ic_action_view_distance_low);
		low.setOnClickListener((radioItem, v) -> {
			onViewDistanceChanged(low, 1);
			return true;
		});
		low.setContentDescription(app.getString(R.string.building_3d_near_distance_content_desc));

		IconToggleButton.IconRadioItem high = new IconToggleButton.IconRadioItem(R.drawable.ic_action_view_distance_high);
		high.setOnClickListener((radioItem, v) -> {
			onViewDistanceChanged(high, 2);
			return true;
		});
		high.setContentDescription(app.getString(R.string.building_3d_far_distance_content_desc));

		LinearLayout container = view.findViewById(R.id.custom_radio_buttons);
		viewDistanceToggleButton = new IconToggleButton(app, container, nightMode);
		viewDistanceToggleButton.setItems(low, high);
		viewDistanceToggleButton.setSelectedItem(level == 2 ? high : low);
	}

	private void onDetailsLevelChanged(@NonNull IconToggleButton.IconRadioItem selectItem, boolean enable) {
		plugin.BUILDINGS_3D_DETAIL_LEVEL.set(enable);
		MapActivity activity = requireMapActivity();
		activity.refreshMapComplete();
		activity.updateLayers();
		detailsLevelToggleButton.setSelectedItem(selectItem);
	}

	private void onViewDistanceChanged(@NonNull IconToggleButton.IconRadioItem selectItem, int level) {
		plugin.BUILDINGS_3D_VIEW_DISTANCE.set(level);
		plugin.apply3DBuildingsDetalization();
		refreshMap();
		viewDistanceToggleButton.setSelectedItem(selectItem);
	}

	protected void refreshMap() {
		callMapActivity(MapActivity::refreshMap);
	}

	@Override
	public InsetTargetsCollection getInsetTargets() {
		InsetTargetsCollection collection = super.getInsetTargets();
		collection.replace(InsetTarget.createBottomContainer(R.id.main_container).landscapeLeftSided(true));
		collection.removeType(Type.ROOT_INSET);
		return collection;
	}

	private void setupSelectableBackground(@NonNull View view) {
		UiUtilities.setupListItemBackground(view.getContext(), view, getAppModeColor(nightMode));
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			fragmentManager.beginTransaction()
					.replace(R.id.content, new Buildings3DFragment(), TAG)
					.commitAllowingStateLoss();
		}
	}
}