package net.osmand.plus.plugins.srtm;

import static net.osmand.plus.configmap.ConfigureMapMenu.createRenderingProperty;
import static net.osmand.plus.plugins.srtm.SRTMPlugin.BUILDINGS_3D;
import static net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem.INVALID_ID;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.slider.Slider;

import net.osmand.core.android.MapRendererContext;
import net.osmand.core.android.MapRendererView;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseFullScreenFragment;
import net.osmand.plus.base.ProgressHelper;
import net.osmand.plus.configmap.ConfigureMapUtils;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.development.OsmandDevelopmentPlugin;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.FontCache;
import net.osmand.plus.utils.InsetTarget;
import net.osmand.plus.utils.InsetTarget.Type;
import net.osmand.plus.utils.InsetTargetsCollection;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.utils.UiUtilities.CompoundButtonType;
import net.osmand.plus.views.corenative.NativeCoreContext;
import net.osmand.plus.widgets.ctxmenu.ViewCreator;
import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem;
import net.osmand.plus.widgets.multistatetoggle.TextToggleButton;
import net.osmand.plus.widgets.multistatetoggle.TextToggleButton.TextRadioItem;
import net.osmand.render.RenderingRuleProperty;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

public class Buildings3DFragment extends BaseFullScreenFragment {

	public static final String TAG = Buildings3DFragment.class.getSimpleName();

	private final OsmandDevelopmentPlugin plugin = PluginsHelper.requirePlugin(OsmandDevelopmentPlugin.class);

	private ViewGroup contentContainer;


	private TextView stateTv;
	private CompoundButton compoundButton;
	private ImageView iconIv;
	private View titleDivider;

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

		setupSlider(contentContainer);
		setupDetalization(contentContainer);
		setupRenderingRules(contentContainer);
	}

	@SuppressLint("SetTextI18n")
	private void setupSlider(@NonNull ViewGroup container) {
		View view = container.findViewById(R.id.transparency_container);
		AndroidUtils.setBackgroundColor(view.getContext(), view, ColorUtilities.getListBgColorId(nightMode));
		Slider visibilitySlider = view.findViewById(R.id.transparency_slider);
		TextView visibilityTv = view.findViewById(R.id.transparency_value_tv);
		TextView title = view.findViewById(R.id.title);
		title.setText(R.string.gpx_visibility_txt);

		float alpha = plugin.BUILDINGS_3D_ALPHA.get();
		int progress = ProgressHelper.normalizeProgressPercent((int) (alpha * 100));

		visibilityTv.setText(progress + "%");

		visibilitySlider.addOnChangeListener((slider, value, fromUser) -> {
			if (fromUser) {
				float newValue = value / 100f;
				plugin.BUILDINGS_3D_ALPHA.set(newValue);
				visibilityTv.setText(ProgressHelper.normalizeProgressPercent((int) value) + "%");
				apply3DBuildingsAlpha(newValue);
			}
		});
		visibilitySlider.setValueTo(100);
		visibilitySlider.setValueFrom(0);
		visibilitySlider.setValue(progress);
		int profileColor = settings.getApplicationMode().getProfileColor(nightMode);
		UiUtilities.setupSlider(visibilitySlider, nightMode, profileColor);
	}

	private void apply3DBuildingsAlpha(float alpha) {
		MapRendererContext ctx = NativeCoreContext.getMapRendererContext();
		if (ctx != null) {
			MapRendererView rendererView = ctx.getMapRendererView();
			if (rendererView != null) {
				rendererView.set3DBuildingsAlpha(alpha);
				refreshMap();
			}
		}
	}

	private void setupRenderingRules(@NonNull View view) {
		ViewGroup container = view.findViewById(R.id.rendering_rules_container);
		container.removeAllViews();

		MapActivity mapActivity = requireMapActivity();
		ViewCreator viewCreator = new ViewCreator(mapActivity, nightMode);
		viewCreator.setDefaultLayoutId(R.layout.list_item_icon_and_menu);
		viewCreator.setCustomControlsColor(appMode.getProfileColor(nightMode));

		List<RenderingRuleProperty> rules = get3DBuildingsRules(app);
		if (!Algorithms.isEmpty(rules)) {
			container.addView(inflate(R.layout.list_item_divider));
		}
		for (int i = 0; i < rules.size(); i++) {
			RenderingRuleProperty property = rules.get(i);
			ContextMenuItem item = createRenderingProperty(mapActivity, INVALID_ID, property,
					BUILDINGS_3D + property.getName(), nightMode);

			boolean lastItem = i == rules.size() - 1;
			item.setHideDivider(lastItem);

			View itemView = viewCreator.getView(item, null);
			container.addView(itemView);

			if (lastItem) {
				container.addView(inflate(R.layout.card_bottom_divider));
			}
		}
		if (rules.isEmpty()) {
			container.addView(inflate(R.layout.card_bottom_divider));
		}
	}

	private void setupDetalization(@NonNull View view) {
		ViewGroup container = view.findViewById(R.id.detalization_container);

		TextView title = container.findViewById(R.id.title);
		title.setText(R.string.buildings_3d_detail_level);
		title.setTypeface(FontCache.getNormalFont());

		int level = plugin.BUILDINGS_3D_DETAIL_LEVEL.get();
		TextRadioItem medium = createRadioButton(R.string.rendering_value_medium_name, 1);
		TextRadioItem high = createRadioButton(R.string.rendering_value_high_name, 2);

		TextToggleButton radioGroup = new TextToggleButton(app, container.findViewById(R.id.custom_radio_buttons), nightMode);
		radioGroup.setItems(medium, high);
		radioGroup.setSelectedItemByTag(level);

		AndroidUiHelper.updateVisibility(container.findViewById(R.id.descr), false);
		AndroidUiHelper.updateVisibility(container.findViewById(R.id.description), false);
	}

	@NonNull
	private TextRadioItem createRadioButton(@StringRes int titleId, int level) {
		TextRadioItem item = new TextRadioItem(getString(titleId));
		item.setTag(level);
		item.setOnClickListener((radioItem, view) -> {
			plugin.BUILDINGS_3D_DETAIL_LEVEL.set(level);
			apply3DBuildingsDetalization(level);
			return true;
		});
		return item;
	}

	private void apply3DBuildingsDetalization(int level) {
		MapRendererContext ctx = NativeCoreContext.getMapRendererContext();
		if (ctx != null) {
			MapRendererView rendererView = ctx.getMapRendererView();
			if (rendererView != null) {
				rendererView.set3DBuildingsDetalization(level);
				refreshMap();
			}
		}
	}

	private void showHideTopShadow(@NonNull View view) {
		boolean portrait = AndroidUiHelper.isOrientationPortrait(requireActivity());
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.shadow_on_map), portrait);
	}

	private void updateUiMode() {
		boolean enabled = plugin.ENABLE_3D_MAP_OBJECTS.get();
		if (enabled) {
			int profileColor = settings.getApplicationMode().getProfileColor(nightMode);
			iconIv.setImageDrawable(getPaintedIcon(R.drawable.ic_action_3d, profileColor));
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

	@NonNull
	public static List<RenderingRuleProperty> get3DBuildingsRules(@NonNull OsmandApplication app) {
		List<RenderingRuleProperty> rules = new ArrayList<>();
		for (RenderingRuleProperty property : ConfigureMapUtils.getCustomRules(app)) {
			if (BUILDINGS_3D.equals(property.getCategory())) {
				rules.add(property);
			}
		}
		return rules;
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			fragmentManager.beginTransaction()
					.replace(R.id.content, new Buildings3DFragment(), TAG)
					.commitAllowingStateLoss();
		}
	}
}