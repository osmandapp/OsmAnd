package net.osmand.plus.configmap.routes;

import static net.osmand.osm.OsmRouteType.ALPINE;
import static net.osmand.plus.configmap.ConfigureMapMenu.ALPINE_HIKING_SCALE_SCHEME_ATTR;

import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.render.RendererRegistry;
import net.osmand.plus.routepreparationmenu.cards.MapBaseCard;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.render.RenderingRuleProperty;
import net.osmand.util.Algorithms;

public class AlpineHikingCard extends MapBaseCard {

	private RouteLayersHelper routeLayersHelper;

	private ViewGroup container;

	public AlpineHikingCard(@NonNull MapActivity mapActivity) {
		super(mapActivity);
		routeLayersHelper = app.getRouteLayersHelper();
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.alpine_hiking_scale_card;
	}

	@Override
	protected void updateContent() {
		container = view.findViewById(R.id.classification_properties);
		container.removeAllViews();

		RenderingRuleProperty property = app.getRendererRegistry().getCustomRenderingRuleProperty(ALPINE_HIKING_SCALE_SCHEME_ATTR);
		if (property != null) {
			String[] possibleValues = property.getPossibleValues();
			for (int i = 0; i < possibleValues.length; i++) {
				boolean hasDivider = i != possibleValues.length - 1;
				View propertyView = createRadioButton(possibleValues[i], themedInflater, container, hasDivider);
				container.addView(propertyView);
			}
		}
		updateClassificationPreferences();
	}

	@NonNull
	private View createRadioButton(@NonNull String value, @NonNull LayoutInflater inflater,
			@Nullable ViewGroup container, boolean hasDivider) {
		View view = inflater.inflate(R.layout.item_with_radiobutton_and_descr, container, false);
		view.setTag(value);

		TextView title = view.findViewById(R.id.title);
		TextView description = view.findViewById(R.id.description);

		title.setText(AndroidUtils.getRenderingStringPropertyValue(app, value));
		description.setText(AndroidUtils.getRenderingStringPropertyDescription(app, value));

		AndroidUiHelper.updateVisibility(description, true);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.divider), hasDivider);

		View button = view.findViewById(R.id.button);
		button.setOnClickListener(v -> {
			routeLayersHelper.updateAlpineHikingScaleScheme(value);
			notifyCardPressed();
			updateClassificationPreferences();
		});

		Drawable background = UiUtilities.getColoredSelectableDrawable(app, settings.getApplicationMode().getProfileColor(nightMode), 0.3f);
		AndroidUtils.setBackground(button, background);
		return view;
	}

	private void updateClassificationPreferences() {
		String selectedValue = routeLayersHelper.getSelectedAlpineHikingScaleScheme();

		for (int i = 0; i < container.getChildCount(); i++) {
			View child = container.getChildAt(i);
			if (child.getTag() instanceof String value) {
				CompoundButton button = child.findViewById(R.id.compound_button);
				button.setChecked(Algorithms.stringsEqual(selectedValue, value));
			}
		}
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
}