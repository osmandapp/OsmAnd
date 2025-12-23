package net.osmand.plus.plugins.srtm;

import static net.osmand.plus.configmap.ConfigureMapMenu.createRenderingProperty;
import static net.osmand.plus.plugins.srtm.SRTMPlugin.BUILDINGS_3D;
import static net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem.INVALID_ID;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.SeekBar;
import android.widget.RadioGroup;
import android.widget.RadioButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseFullScreenFragment;
import net.osmand.plus.configmap.ConfigureMapUtils;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.development.OsmandDevelopmentPlugin;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.InsetTarget;
import net.osmand.plus.utils.InsetTarget.Type;
import net.osmand.plus.utils.InsetTargetsCollection;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.utils.UiUtilities.CompoundButtonType;
import net.osmand.plus.widgets.ctxmenu.ViewCreator;
import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem;
import net.osmand.render.RenderingRuleProperty;
import net.osmand.core.android.MapRendererContext;
import net.osmand.core.android.MapRendererView;
import net.osmand.plus.views.corenative.NativeCoreContext;

import java.util.ArrayList;
import java.util.List;

public class Buildings3DFragment extends BaseFullScreenFragment {

	public static final String TAG = Buildings3DFragment.class.getSimpleName();

	private final OsmandDevelopmentPlugin plugin = PluginsHelper.requirePlugin(OsmandDevelopmentPlugin.class);

	private TextView stateTv;
	private CompoundButton compoundButton;
	private ImageView iconIv;
	private LinearLayout contentContainer;
	private View titleDivider;
	private View transparencyRow;
	private SeekBar transparencySeekBar;
	private TextView transparencyValueTv;
	private View detailRow;
	private RadioGroup detailRadioGroup;
	private RadioButton mediumDetailRb;
	private RadioButton highDetailRb;
	private int profileColor;

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
		profileColor = settings.getApplicationMode().getProfileColor(nightMode);

		setupHeader(view);
		setupContent(view);

		updateUiMode();

		if (plugin.ENABLE_3D_MAP_OBJECTS.get()) {
			float alpha = plugin.BUILDINGS_3D_ALPHA.get();
			if (alpha <= 0f || alpha > 1f) {
				alpha = 0.7f;
			}
			int detailLevel = plugin.BUILDINGS_3D_DETAIL_LEVEL.get();
			if (detailLevel != 1 && detailLevel != 2) {
				detailLevel = 1;
			}
			apply3DBuildingsAlpha(alpha);
			apply3DBuildingsDetalization(detailLevel);
		}
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
		contentContainer.removeAllViews();

		contentContainer.addView(inflate(R.layout.list_item_divider));

		MapActivity mapActivity = requireMapActivity();
		ViewCreator viewCreator = new ViewCreator(mapActivity, nightMode);
		viewCreator.setDefaultLayoutId(R.layout.list_item_icon_and_menu);
		viewCreator.setCustomControlsColor(appMode.getProfileColor(nightMode));

		List<RenderingRuleProperty> rules = get3DBuildingsRules(app);
		for (int i = 0; i < rules.size(); i++) {
			RenderingRuleProperty property = rules.get(i);
			ContextMenuItem item = createRenderingProperty(mapActivity, INVALID_ID, property,
					BUILDINGS_3D + property.getName(), nightMode);

			boolean lastItem = i == rules.size() - 1;
			item.setHideDivider(lastItem);

			View itemView = viewCreator.getView(item, null);
			contentContainer.addView(itemView);

			if (lastItem) {
				contentContainer.addView(inflate(R.layout.card_bottom_divider));
			}
		}

		if (rules.isEmpty()) {
			contentContainer.addView(inflate(R.layout.card_bottom_divider));
		}

		setup3DBuildingsControls(view);
	}

	private void setup3DBuildingsControls(@NonNull View view) {
		transparencyRow = view.findViewById(R.id.transparency_row);
		detailRow = view.findViewById(R.id.detail_row);

		transparencySeekBar = view.findViewById(R.id.seekbar_transparency);
		transparencyValueTv = view.findViewById(R.id.value_transparency);

		detailRadioGroup = view.findViewById(R.id.radio_group_detail);
		mediumDetailRb = view.findViewById(R.id.radio_detail_medium);
		highDetailRb = view.findViewById(R.id.radio_detail_high);

		float alpha = plugin.BUILDINGS_3D_ALPHA.get();
		if (alpha <= 0f || alpha > 1f) {
			alpha = 0.7f;
		}
		int progress = Math.round(alpha * 100f);
		transparencySeekBar.setProgress(progress);
		updateTransparencyValueText(progress);

		transparencySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				float value = progress / 100f;
				plugin.BUILDINGS_3D_ALPHA.set(value);
				updateTransparencyValueText(progress);
				apply3DBuildingsAlpha(value);
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}
		});

		int detailLevel = plugin.BUILDINGS_3D_DETAIL_LEVEL.get();
		if (detailLevel != 1 && detailLevel != 2) {
			detailLevel = 1;
		}
		if (detailLevel == 1) {
			mediumDetailRb.setChecked(true);
		} else {
			highDetailRb.setChecked(true);
		}

		detailRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
			int level = checkedId == R.id.radio_detail_high ? 2 : 1;
			plugin.BUILDINGS_3D_DETAIL_LEVEL.set(level);
			apply3DBuildingsDetalization(level);
		});
	}

	private void updateTransparencyValueText(int progress) {
		if (transparencyValueTv != null) {
			transparencyValueTv.setText(progress + "%");
		}
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
			iconIv.setImageDrawable(getPaintedIcon(R.drawable.ic_action_3d, profileColor));
			stateTv.setText(R.string.shared_string_on);
		} else {
			iconIv.setImageDrawable(getIcon(R.drawable.ic_action_3d, ColorUtilities.getSecondaryIconColorId(nightMode)));
			stateTv.setText(R.string.shared_string_off);
		}
		AndroidUiHelper.updateVisibility(contentContainer, enabled);
		AndroidUiHelper.updateVisibility(titleDivider, !enabled);
		if (transparencyRow != null) {
			AndroidUiHelper.updateVisibility(transparencyRow, enabled);
			if (transparencySeekBar != null) {
				transparencySeekBar.setEnabled(enabled);
			}
		}
		if (detailRow != null) {
			AndroidUiHelper.updateVisibility(detailRow, enabled);
			if (detailRadioGroup != null) {
				for (int i = 0; i < detailRadioGroup.getChildCount(); i++) {
					View child = detailRadioGroup.getChildAt(i);
					child.setEnabled(enabled);
				}
			}
		}
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