package net.osmand.plus.dialogs;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.AndroidUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.CommonPreference;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.widgets.multistatetoggle.RadioItem;
import net.osmand.plus.widgets.multistatetoggle.RadioItem.OnRadioItemClickListener;
import net.osmand.plus.widgets.multistatetoggle.TextToggleButton;
import net.osmand.plus.widgets.multistatetoggle.TextToggleButton.TextRadioItem;

import static net.osmand.plus.dialogs.ConfigureMapMenu.CYCLE_NODE_NETWORK_ROUTES_ATTR;
import static net.osmand.plus.dialogs.ConfigureMapMenu.SHOW_CYCLE_ROUTES_ATTR;

public class CycleRoutesFragment extends BaseOsmAndFragment {

	public static final String TAG = CycleRoutesFragment.class.getSimpleName();

	private OsmandApplication app;
	private OsmandSettings settings;

	private boolean nightMode;

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		app = requireMyApplication();
		settings = app.getSettings();
		nightMode = app.getDaynightHelper().isNightModeForMapControls();

		MapActivity mapActivity = (MapActivity) requireMyActivity();
		LayoutInflater themedInflater = UiUtilities.getInflater(mapActivity, nightMode);
		View view = themedInflater.inflate(R.layout.map_route_types_fragment, container, false);

		setupPreferenceHeader(view, mapActivity);
		setupTypesCard(view, mapActivity);
		setupBottomEmptySpace(view);

		return view;
	}

	private void setupPreferenceHeader(@NonNull View view, @NonNull MapActivity mapActivity) {
		CommonPreference<Boolean> pref = settings.getCustomRenderBooleanProperty(SHOW_CYCLE_ROUTES_ATTR);

		View container = view.findViewById(R.id.preference_container);

		TextView title = container.findViewById(R.id.title);
		ImageView icon = container.findViewById(R.id.icon);
		TextView description = container.findViewById(R.id.description);

		int selectedColor = settings.getApplicationMode().getProfileColor(nightMode);
		title.setText(AndroidUtils.getRenderingStringPropertyName(app, SHOW_CYCLE_ROUTES_ATTR, SHOW_CYCLE_ROUTES_ATTR));
		icon.setImageDrawable(getPaintedContentIcon(R.drawable.ic_action_bicycle_dark, selectedColor));
		description.setText(pref.get() ? R.string.shared_string_enabled : R.string.shared_string_disabled);

		CompoundButton button = container.findViewById(R.id.toggle_item);
		button.setClickable(false);
		button.setFocusable(false);
		button.setChecked(pref.get());
		UiUtilities.setupCompoundButton(nightMode, selectedColor, button);

		container.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				boolean checked = !pref.get();
				pref.set(checked);
				button.setChecked(checked);
				description.setText(checked ? R.string.shared_string_enabled : R.string.shared_string_disabled);

				mapActivity.refreshMapComplete();
				mapActivity.getMapLayers().updateLayers(mapActivity.getMapView());
			}
		});
		AndroidUiHelper.updateVisibility(container.findViewById(R.id.divider), false);
	}

	private void setupTypesCard(@NonNull View view, @NonNull MapActivity mapActivity) {
		CommonPreference<Boolean> pref = settings.getCustomRenderBooleanProperty(CYCLE_NODE_NETWORK_ROUTES_ATTR);

		View container = view.findViewById(R.id.card_container);
		TextView title = container.findViewById(R.id.title);
		TextView type = container.findViewById(R.id.descr);
		TextView description = container.findViewById(R.id.description);

		title.setText(R.string.routes_color_by_type);
		type.setText(pref.get() ? R.string.rendering_value_walkingRoutesOSMCNodes_name : R.string.layer_route);
		description.setText(pref.get() ? R.string.rendering_value_walkingRoutesOSMCNodes_description : R.string.walking_route_osmc_description);

		TextRadioItem relation = createRadioButton(mapActivity, pref, false, R.string.layer_route);
		TextRadioItem nodeNetworks = createRadioButton(mapActivity, pref, true, R.string.rendering_value_walkingRoutesOSMCNodes_name);

		TextToggleButton radioGroup = new TextToggleButton(app, view.findViewById(R.id.custom_radio_buttons), nightMode);
		radioGroup.setItems(relation, nodeNetworks);
		radioGroup.setSelectedItem(pref.get() ? nodeNetworks : relation);
	}

	private TextRadioItem createRadioButton(@NonNull MapActivity mapActivity, @NonNull CommonPreference<Boolean> pref,
											boolean enabled, int titleId) {
		TextRadioItem item = new TextRadioItem(getString(titleId));
		item.setOnClickListener(new OnRadioItemClickListener() {
			@Override
			public boolean onRadioItemClick(RadioItem radioItem, View view) {
				pref.set(enabled);
				View mainView = getView();
				if (mainView != null) {
					setupTypesCard(mainView, mapActivity);
				}
				mapActivity.refreshMapComplete();
				mapActivity.getMapLayers().updateLayers(mapActivity.getMapView());
				return true;
			}
		});
		return item;
	}

	private void setupBottomEmptySpace(@NonNull View view) {
		View bottomView = view.findViewById(R.id.bottom_empty_space);
		int height = AndroidUtils.getScreenHeight(requireActivity()) - getResources().getDimensionPixelSize(R.dimen.dashboard_map_top_padding);
		ViewGroup.LayoutParams params = bottomView.getLayoutParams();
		params.height = height;
		bottomView.setLayoutParams(params);
	}
}