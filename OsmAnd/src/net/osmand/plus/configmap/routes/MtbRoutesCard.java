package net.osmand.plus.configmap.routes;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.routepreparationmenu.cards.MapBaseCard;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.util.Algorithms;

public class MtbRoutesCard extends MapBaseCard {

	private RouteLayersHelper routeLayersHelper;

	private ViewGroup container;

	public MtbRoutesCard(@NonNull MapActivity mapActivity) {
		super(mapActivity);
		routeLayersHelper = app.getRouteLayersHelper();
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.mtb_routes_card;
	}

	@Override
	protected void updateContent() {
		container = view.findViewById(R.id.classification_properties);
		container.removeAllViews();

		for (MtbClassification classification : MtbClassification.values()) {
			boolean lastItem = classification.ordinal() != MtbClassification.values().length - 1;
			View propertyView = createRadioButton(classification, container, lastItem);
			container.addView(propertyView);
		}
		updateClassificationPreferences();
	}

	@NonNull
	private View createRadioButton(@NonNull MtbClassification classification,
			@Nullable ViewGroup container, boolean hasDivider) {
		View view = themedInflater.inflate(R.layout.item_with_radiobutton_and_descr, container, false);
		view.setTag(classification);

		TextView title = view.findViewById(R.id.title);
		TextView description = view.findViewById(R.id.description);

		title.setText(getString(classification.nameId));
		boolean hasDescription = classification.descriptionId != null;
		if (hasDescription) {
			description.setText(classification.descriptionId);
		}
		AndroidUiHelper.updateVisibility(description, hasDescription);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.divider), hasDivider);

		View button = view.findViewById(R.id.button);
		button.setOnClickListener(v -> {
			routeLayersHelper.updateSelectedMtbClassification(classification.attrName);
			updateClassificationPreferences();
			notifyCardPressed();
		});

		int color = settings.getApplicationMode().getProfileColor(nightMode);
		Drawable background = UiUtilities.getColoredSelectableDrawable(app, color, 0.3f);
		AndroidUtils.setBackground(button, background);
		return view;
	}

	private void updateClassificationPreferences() {
		String selectedValue = routeLayersHelper.getSelectedMtbClassificationId();
		for (int i = 0; i < container.getChildCount(); i++) {
			View child = container.getChildAt(i);
			if (child.getTag() instanceof MtbClassification classification) {
				CompoundButton button = child.findViewById(R.id.compound_button);
				button.setChecked(Algorithms.stringsEqual(classification.attrName, selectedValue));
			}
		}
	}
}
