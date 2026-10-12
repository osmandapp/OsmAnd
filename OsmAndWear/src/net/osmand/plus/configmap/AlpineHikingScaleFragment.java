package net.osmand.plus.configmap;

import static net.osmand.osm.OsmRouteType.ALPINE;
import static net.osmand.plus.configmap.ConfigureMapMenu.ALPINE_HIKING_SCALE_SCHEME_ATTR;

import android.graphics.drawable.Drawable;
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

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.configmap.routes.RouteLayersHelper;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.render.RendererRegistry;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.render.RenderingRuleProperty;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class AlpineHikingScaleFragment extends BaseOsmAndFragment {

	public static final String TAG = AlpineHikingScaleFragment.class.getSimpleName();

	private RouteLayersHelper routeLayersHelper;
	private final List<View> itemsViews = new ArrayList<>();
	private ImageView headerIcon;
	private TextView headerDescription;
	private CompoundButton compoundButton;

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
		View view = themedInflater.inflate(R.layout.fragment_alpine_difficult_classification, container, false);

		setupHeader(view);
		updateScreenMode(view, routeLayersHelper.isAlpineHikingRoutesEnabled());
		setupClassifications(view);
		updateClassificationPreferences();

		return view;
	}

	private void setupHeader(@NonNull View view) {
		View container = view.findViewById(R.id.preference_container);

		TextView headerTitle = container.findViewById(R.id.title);
		headerIcon = container.findViewById(R.id.icon);
		headerDescription = container.findViewById(R.id.description);

		headerTitle.setText(getString(R.string.rendering_attr_alpineHiking_name));

		compoundButton = container.findViewById(R.id.toggle_item);
		compoundButton.setClickable(false);
		compoundButton.setFocusable(false);

		container.setOnClickListener(v -> {
			routeLayersHelper.toggleAlpineHikingRoutes();
			updateHeader();
			updateScreenMode(view, routeLayersHelper.isAlpineHikingRoutesEnabled());
			refreshMap();
		});

		updateHeader();
		AndroidUiHelper.updateVisibility(container.findViewById(R.id.divider), false);
		AndroidUiHelper.updateVisibility(container.findViewById(R.id.secondary_icon), false);
	}

	private void updateHeader() {
		boolean enabled = routeLayersHelper.isAlpineHikingRoutesEnabled();
		int selectedColor = settings.getApplicationMode().getProfileColor(nightMode);
		int disabledColor = AndroidUtils.getColorFromAttr(app, R.attr.default_icon_color);
		headerIcon.setImageDrawable(getPaintedContentIcon(R.drawable.ic_action_trekking_dark, enabled ? selectedColor : disabledColor));

		headerDescription.setText(getDifficultyClassificationDescription(app));
		AndroidUiHelper.updateVisibility(headerDescription, enabled);

		compoundButton.setChecked(enabled);
		UiUtilities.setupCompoundButton(nightMode, selectedColor, compoundButton);
	}

	@NonNull
	public static String getDifficultyClassificationDescription(@NonNull OsmandApplication app) {
		OsmandSettings settings = app.getSettings();
		if (!settings.getCustomRenderBooleanProperty(ALPINE.getRenderingPropertyAttr()).get()) {
			return app.getString(R.string.shared_string_disabled);
		}
		String value = settings.getCustomRenderProperty(ALPINE_HIKING_SCALE_SCHEME_ATTR).get();
		if (Algorithms.isEmpty(value)) {
			RendererRegistry registry = app.getRendererRegistry();
			RenderingRuleProperty property = registry.getCustomRenderingRuleProperty(ALPINE_HIKING_SCALE_SCHEME_ATTR);
			if (property != null) {
				value = property.getPossibleValues()[0];
			} else {
				return app.getString(R.string.shared_string_disabled);
			}
		}
		return app.getString(R.string.ltr_or_rtl_combine_via_comma, app.getString(R.string.shared_string_on),
				AndroidUtils.getRenderingStringPropertyValue(app, value));
	}

	private void setupClassifications(@NonNull View view) {
		ViewGroup container = view.findViewById(R.id.classification_properties);
		LayoutInflater inflater = UiUtilities.getInflater(getContext(), nightMode);
		itemsViews.clear();

		RenderingRuleProperty property = app.getRendererRegistry().getCustomRenderingRuleProperty(ALPINE_HIKING_SCALE_SCHEME_ATTR);
		if (property != null) {
			String[] possibleValues = property.getPossibleValues();
			for (int i = 0; i < possibleValues.length; i++) {
				boolean hasDivider = i != possibleValues.length - 1;
				View propertyView = createRadioButton(possibleValues[i], inflater, container, hasDivider);
				container.addView(propertyView);
			}
		}
	}

	@NonNull
	private View createRadioButton(@NonNull String value, @NonNull LayoutInflater inflater, @Nullable ViewGroup container, boolean hasDivider) {
		View view = inflater.inflate(R.layout.item_with_radiobutton_and_descr, container, false);
		view.setTag(value);
		itemsViews.add(view);

		TextView title = view.findViewById(R.id.title);
		TextView description = view.findViewById(R.id.description);

		title.setText(AndroidUtils.getRenderingStringPropertyValue(app, value));
		description.setText(AndroidUtils.getRenderingStringPropertyDescription(app, value));

		AndroidUiHelper.updateVisibility(description, true);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.divider), hasDivider);

		View button = view.findViewById(R.id.button);
		button.setOnClickListener(v -> {
			routeLayersHelper.updateAlpineHikingScaleScheme(value);
			updateClassificationPreferences();
			updateHeader();
			refreshMap();
		});

		Drawable background = UiUtilities.getColoredSelectableDrawable(app, settings.getApplicationMode().getProfileColor(nightMode), 0.3f);
		AndroidUtils.setBackground(button, background);
		return view;
	}

	private void updateClassificationPreferences() {
		String selectedValue = routeLayersHelper.getSelectedAlpineHikingScaleScheme();
		for (View itemView : itemsViews) {
			String itemValue = (String) itemView.getTag();
			boolean selected = Objects.equals(selectedValue, itemValue);
			CompoundButton button = itemView.findViewById(R.id.compound_button);
			button.setChecked(selected);
		}
	}

	private void updateScreenMode(@NonNull View view, boolean enabled) {
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.empty_screen), !enabled);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.normal_screen), enabled);
	}

	private void refreshMap() {
		MapActivity mapActivity = (MapActivity) getMyActivity();
		if (mapActivity != null) {
			mapActivity.refreshMapComplete();
			mapActivity.updateLayers();
		}
	}

	public static void showInstance(@NonNull FragmentManager manager) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			manager.beginTransaction()
					.replace(R.id.content, new AlpineHikingScaleFragment(), TAG)
					.commitAllowingStateLoss();
		}
	}
}
