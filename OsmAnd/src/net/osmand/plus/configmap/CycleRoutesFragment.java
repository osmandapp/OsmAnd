package net.osmand.plus.configmap;

import static net.osmand.osm.OsmRouteType.BICYCLE;
import static net.osmand.plus.configmap.routes.RouteUtils.CYCLE_NODE_NETWORK_ROUTES_ATTR;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.configmap.routes.RouteLayersHelper;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.multistatetoggle.TextToggleButton;
import net.osmand.plus.widgets.multistatetoggle.TextToggleButton.TextRadioItem;

public class CycleRoutesFragment extends BaseOsmAndFragment {

	public static final String TAG = CycleRoutesFragment.class.getSimpleName();

	private RouteLayersHelper routeLayersHelper;

	@Override
	protected boolean isUsedOnMap() {
		return true;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		routeLayersHelper = app.getRouteLayersHelper();
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		updateNightMode();
		View view = themedInflater.inflate(R.layout.map_route_types_fragment, container, false);

		showHideTopShadow(view);
		setupHeader(view);
		setupTypesCard(view);

		return view;
	}

	private void showHideTopShadow(@NonNull View view) {
		boolean portrait = AndroidUiHelper.isOrientationPortrait(requireActivity());
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.shadow_on_map), portrait);
	}

	private void setupHeader(@NonNull View view) {
		boolean enabled = routeLayersHelper.isCycleRoutesEnabled();
		View container = view.findViewById(R.id.preference_container);

		TextView title = container.findViewById(R.id.title);
		ImageView icon = container.findViewById(R.id.icon);
		TextView description = container.findViewById(R.id.description);

		int selectedColor = settings.getApplicationMode().getProfileColor(nightMode);
		int disabledColor = AndroidUtils.getColorFromAttr(view.getContext(), R.attr.default_icon_color);
		String propertyAttr = BICYCLE.getRenderingPropertyAttr();
		title.setText(AndroidUtils.getRenderingStringPropertyName(app, propertyAttr, propertyAttr));
		icon.setImageDrawable(getPaintedContentIcon(R.drawable.ic_action_bicycle_dark, enabled ? selectedColor : disabledColor));
		description.setText(enabled ? R.string.shared_string_enabled : R.string.shared_string_disabled);

		CompoundButton button = container.findViewById(R.id.toggle_item);
		button.setClickable(false);
		button.setFocusable(false);
		button.setChecked(enabled);
		UiUtilities.setupCompoundButton(nightMode, selectedColor, button);

		container.setOnClickListener(v -> {
			routeLayersHelper.toggleCycleRoutes();
			View mainView = getView();
			if (mainView != null) {
				setupHeader(mainView);
				setupTypesCard(mainView);
			}
			MapActivity mapActivity = (MapActivity) getMyActivity();
			if (mapActivity != null) {
				mapActivity.refreshMapComplete();
				mapActivity.updateLayers();
			}
		});
		AndroidUiHelper.updateVisibility(container.findViewById(R.id.divider), false);
		AndroidUiHelper.updateVisibility(container.findViewById(R.id.secondary_icon), false);
	}

	private void setupTypesCard(@NonNull View view) {
		View container = view.findViewById(R.id.card_container);
		TextView title = container.findViewById(R.id.title);
		TextView description = container.findViewById(R.id.description);

		boolean nodeNetworkEnabled = routeLayersHelper.isCycleRoutesNodeNetworkEnabled();
		title.setText(R.string.routes_color_by_type);
		description.setText(nodeNetworkEnabled ? R.string.rendering_value_walkingRoutesOSMCNodes_description : R.string.walking_route_osmc_description);

		TextRadioItem relation = createRadioButton(false, R.string.layer_route);
		TextRadioItem nodeNetworks = createRadioButton(true, R.string.rendering_value_walkingRoutesOSMCNodes_name);

		TextToggleButton radioGroup = new TextToggleButton(app, view.findViewById(R.id.custom_radio_buttons), nightMode);
		radioGroup.setItems(relation, nodeNetworks);
		radioGroup.setSelectedItem(nodeNetworkEnabled ? nodeNetworks : relation);

		boolean enabled = routeLayersHelper.isCycleRoutesEnabled();
		AndroidUiHelper.updateVisibility(container, enabled);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.topShadowView), enabled);
		AndroidUiHelper.updateVisibility(container.findViewById(R.id.descr), false);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.card_bottom_divider), enabled);
	}

	private TextRadioItem createRadioButton(boolean enabled, int titleId) {
		TextRadioItem item = new TextRadioItem(getString(titleId));
		item.setOnClickListener((radioItem, view) -> {
			routeLayersHelper.toggleCycleRoutesNodeNetwork(enabled);
			View mainView = getView();
			if (mainView != null) {
				setupTypesCard(mainView);
			}
			MapActivity mapActivity = (MapActivity) getMyActivity();
			if (mapActivity != null) {
				mapActivity.refreshMapComplete();
				mapActivity.getMapLayers().updateLayers(mapActivity);
			}
			return true;
		});
		return item;
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			fragmentManager.beginTransaction()
					.replace(R.id.content, new CycleRoutesFragment(), TAG)
					.commitAllowingStateLoss();
		}
	}
}