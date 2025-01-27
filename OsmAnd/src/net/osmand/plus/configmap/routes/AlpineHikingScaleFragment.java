package net.osmand.plus.configmap.routes;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.utils.AndroidUtils;

public class AlpineHikingScaleFragment extends MapRoutesFragment {

	public static final String TAG = AlpineHikingScaleFragment.class.getSimpleName();

	@Override
	protected boolean isEnabled() {
		return routeLayersHelper.isAlpineHikingRoutesEnabled();
	}

	@Override
	protected void toggleMainPreference(@NonNull View view) {
		routeLayersHelper.toggleAlpineHikingRoutes();
	}

	protected void setupHeader(@NonNull View view) {
		super.setupHeader(view);

		boolean enabled = isEnabled();
		View container = view.findViewById(R.id.preference_container);

		TextView title = container.findViewById(R.id.title);
		title.setText(R.string.rendering_attr_alpineHiking_name);

		TextView description = container.findViewById(R.id.description);
		description.setText(AlpineHikingCard.getDifficultyClassificationDescription(app));
		AndroidUiHelper.updateVisibility(description, enabled);

		int selectedColor = settings.getApplicationMode().getProfileColor(nightMode);
		int disabledColor = AndroidUtils.getColorFromAttr(app, R.attr.default_icon_color);
		ImageView icon = container.findViewById(R.id.icon);
		icon.setImageDrawable(getPaintedContentIcon(R.drawable.ic_action_trekking_dark, enabled ? selectedColor : disabledColor));
	}

	@Override
	protected void setupCards(@NonNull View view) {
		super.setupCards(view);

		cardsContainer.addView(createDivider(cardsContainer, true, true));
		addCard(new AlpineHikingCard(getMapActivity()));
		cardsContainer.addView(createDivider(cardsContainer, false, true));
	}
}
