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