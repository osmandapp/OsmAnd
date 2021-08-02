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
import androidx.annotation.Nullable;

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
import net.osmand.render.RenderingRuleProperty;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

import static net.osmand.plus.dialogs.ConfigureMapMenu.HIKING_ROUTES_OSMC_ATTR;

public class HikingRoutesFragment extends BaseOsmAndFragment {

	public static final String TAG = HikingRoutesFragment.class.getSimpleName();

	private OsmandApplication app;
	private OsmandSettings settings;

	private CommonPreference<String> pref;
	private RenderingRuleProperty property;
	private String previousValue;

	private boolean nightMode;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = requireMyApplication();
		settings = app.getSettings();
		nightMode = app.getDaynightHelper().isNightModeForMapControls();

		pref = settings.getCustomRenderProperty(HIKING_ROUTES_OSMC_ATTR);
		property = app.getRendererRegistry().getCustomRenderingRuleProperty(HIKING_ROUTES_OSMC_ATTR);
		previousValue = isEnabled() ? pref.get() : property.getPossibleValues()[0];
	}

	private boolean isEnabled() {
		for (String value : property.getPossibleValues()) {
			if (Algorithms.stringsEqual(value, pref.get())) {
				return true;
			}
		}
		return false;
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		MapActivity mapActivity = (MapActivity) requireMyActivity();
		LayoutInflater themedInflater = UiUtilities.getInflater(mapActivity, nightMode);
		View view = themedInflater.inflate(R.layout.map_route_types_fragment, container, false);

		setupHeader(view);
		setupTypesCard(view);
		setupBottomEmptySpace(view);

		return view;
	}

	private void setupHeader(@NonNull View view) {
		View container = view.findViewById(R.id.preference_container);

		TextView title = container.findViewById(R.id.title);
		ImageView icon = container.findViewById(R.id.icon);
		TextView description = container.findViewById(R.id.description);

		boolean enabled = isEnabled();
		int selectedColor = settings.getApplicationMode().getProfileColor(nightMode);
		int disabledColor = AndroidUtils.getColorFromAttr(view.getContext(), R.attr.default_icon_color);
		String propertyName = AndroidUtils.getRenderingStringPropertyName(app, HIKING_ROUTES_OSMC_ATTR, property.getName());

		title.setText(propertyName);
		icon.setImageDrawable(getPaintedContentIcon(R.drawable.ic_action_trekking_dark, enabled ? selectedColor : disabledColor));
		description.setText(enabled ? R.string.shared_string_enabled : R.string.shared_string_disabled);

		CompoundButton button = container.findViewById(R.id.toggle_item);
		button.setClickable(false);
		button.setFocusable(false);
		button.setChecked(enabled);
		UiUtilities.setupCompoundButton(nightMode, selectedColor, button);

		container.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				pref.set(!button.isChecked() ? previousValue : "");
				setupHeader(view);
				setupTypesCard(view);
				refreshMap();
			}
		});
		AndroidUiHelper.updateVisibility(container.findViewById(R.id.divider), false);
	}

	private void setupTypesCard(@NonNull View view) {
		View container = view.findViewById(R.id.card_container);

		boolean enabled = isEnabled();
		if (enabled) {
			TextRadioItem selectedItem = null;
			List<TextRadioItem> items = new ArrayList<>();
			for (String value : property.getPossibleValues()) {
				TextRadioItem item = createRadioButton(value);
				if (Algorithms.stringsEqual(value, pref.get())) {
					selectedItem = item;
				}
				items.add(item);
			}

			TextView title = container.findViewById(R.id.title);
			TextView description = container.findViewById(R.id.description);

			title.setText(R.string.routes_color_by_type);
			description.setText(AndroidUtils.getRenderingStringPropertyDescription(app, pref.get()));

			TextToggleButton radioGroup = new TextToggleButton(app, view.findViewById(R.id.custom_radio_buttons), nightMode);
			radioGroup.setItems(items);
			radioGroup.setSelectedItem(selectedItem);
		}
		AndroidUiHelper.updateVisibility(container, enabled);
		AndroidUiHelper.updateVisibility(container.findViewById(R.id.descr), false);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.topShadowView), enabled);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.card_bottom_divider), enabled);
	}

	private TextRadioItem createRadioButton(@NonNull String value) {
		String name = AndroidUtils.getRenderingStringPropertyValue(app, value);
		TextRadioItem item = new TextRadioItem(name);
		item.setOnClickListener(new OnRadioItemClickListener() {
			@Override
			public boolean onRadioItemClick(RadioItem radioItem, View v) {
				pref.set(value);
				previousValue = value;

				View view = getView();
				if (view != null) {
					setupHeader(view);
					setupTypesCard(view);
				}
				refreshMap();
				return true;
			}
		});
		return item;
	}

	private void refreshMap() {
		MapActivity mapActivity = (MapActivity) getMyActivity();
		if (mapActivity != null) {
			mapActivity.refreshMapComplete();
			mapActivity.getMapLayers().updateLayers(mapActivity.getMapView());
		}
	}

	private void setupBottomEmptySpace(@NonNull View view) {
		int height = AndroidUtils.getScreenHeight(requireActivity()) - getResources().getDimensionPixelSize(R.dimen.dashboard_map_top_padding);
		View bottomView = view.findViewById(R.id.bottom_empty_space);
		ViewGroup.LayoutParams params = bottomView.getLayoutParams();
		params.height = height;
		bottomView.setLayoutParams(params);
	}
}