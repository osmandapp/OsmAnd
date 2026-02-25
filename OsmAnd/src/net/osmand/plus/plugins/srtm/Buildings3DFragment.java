package net.osmand.plus.plugins.srtm;

import static net.osmand.plus.plugins.srtm.SRTMPlugin.BUILDINGS_3D;

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

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseFullScreenFragment;
import net.osmand.plus.base.ProgressHelper;
import net.osmand.plus.configmap.ConfigureMapUtils;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.InsetTarget;
import net.osmand.plus.utils.InsetTarget.Type;
import net.osmand.plus.utils.InsetTargetsCollection;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.utils.UiUtilities.CompoundButtonType;
import net.osmand.plus.widgets.TextViewEx;
import net.osmand.plus.widgets.multistatetoggle.IconToggleButton;
import net.osmand.render.RenderingRuleProperty;

import java.util.ArrayList;
import java.util.List;

public class Buildings3DFragment extends BaseFullScreenFragment {

	public static final String TAG = Buildings3DFragment.class.getSimpleName();

	private final SRTMPlugin plugin = PluginsHelper.requirePlugin(SRTMPlugin.class);

	private ViewGroup contentContainer;


	private TextView stateTv;
	private CompoundButton compoundButton;
	private ImageView iconIv;
	private View titleDivider;
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
		View view = inflate(R.layout.fragment_3d_buildings, container, false);

		setupHeader(view);
		setupContent(view);

		updateUiMode();

		return view;
	}

	protected void setupHeader(@NonNull View view) {
		stateTv = view.findViewById(R.id.state_tv);
		iconIv = view.findViewById(R.id.icon_iv);
		titleDivider = view.findViewById(R.id.title_divider);

		TextView titleTv = view.findViewById(R.id.title_tv);
		titleTv.setText(R.string.enable_3d_objects);

		compoundButton = view.findViewById(R.id.switch_compat);
		compoundButton.setClickable(false);
		compoundButton.setFocusable(false);
		compoundButton.setChecked(plugin.ENABLE_3D_MAP_OBJECTS.get());

		view.findViewById(R.id.header_container).setOnClickListener(v -> {
			boolean enabled = !plugin.ENABLE_3D_MAP_OBJECTS.get();
			compoundButton.setChecked(enabled);
			plugin.ENABLE_3D_MAP_OBJECTS.set(enabled);
			refreshMap();
			updateUiMode();
		});
		showHideTopShadow(view);
		UiUtilities.setupCompoundButton(compoundButton, nightMode, CompoundButtonType.PROFILE_DEPENDENT);
	}

	protected void setupContent(@NonNull View view) {
		contentContainer = view.findViewById(R.id.content_container);

		setupAppearance(contentContainer);
		setupPerformance(contentContainer);
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
		container.findViewById(R.id.color_container).setOnClickListener((v) -> callMapActivity(mapActivity -> {
			mapActivity.getDashboard().hideDashboard();
			Buildings3DColorFragment.showInstance(mapActivity.getSupportFragmentManager());
		}));
		container.findViewById(R.id.opacity_container).setOnClickListener((v) -> callMapActivity(mapActivity -> {
			mapActivity.getDashboard().hideDashboard();
			Buildings3DVisibilityFragment.showInstance(mapActivity.getSupportFragmentManager());
		}));
	}

	private void setupPerformance(@NonNull ViewGroup container) {
		View performanceContainer = container.findViewById(R.id.performance_container);
		AndroidUtils.setBackgroundColor(performanceContainer.getContext(), performanceContainer, ColorUtilities.getListBgColorId(nightMode));
		TextViewEx title = performanceContainer.findViewById(R.id.title);
		title.setText(R.string.performance);

		setupDetailsLevelToggleButtons(performanceContainer.findViewById(R.id.details_lvl_container));
		setupViewDistanceToggleButtons(performanceContainer.findViewById(R.id.view_distance_container));
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

	private void showHideTopShadow(@NonNull View view) {
		boolean portrait = AndroidUiHelper.isOrientationPortrait(requireActivity());
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.shadow_on_map), portrait);
	}

	private void updateUiMode() {
		boolean enabled = plugin.ENABLE_3D_MAP_OBJECTS.get();
		if (enabled) {
			int profileColor = settings.getApplicationMode().getProfileColor(nightMode);
			iconIv.setImageDrawable(getPaintedIcon(R.drawable.ic_action_3d_buildings, profileColor));
			stateTv.setText(R.string.shared_string_on);
		} else {
			iconIv.setImageDrawable(getIcon(R.drawable.ic_action_3d, ColorUtilities.getSecondaryIconColorId(nightMode)));
			stateTv.setText(R.string.shared_string_off);
		}
		AndroidUiHelper.updateVisibility(contentContainer, enabled);
		AndroidUiHelper.updateVisibility(titleDivider, !enabled);
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

	public static void showInstance(@NonNull FragmentManager fragmentManager) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			fragmentManager.beginTransaction()
					.replace(R.id.content, new Buildings3DFragment(), TAG)
					.commitAllowingStateLoss();
		}
	}
}