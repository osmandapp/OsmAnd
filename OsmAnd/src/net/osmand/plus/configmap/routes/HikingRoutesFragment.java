package net.osmand.plus.configmap.routes;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.utils.AndroidUtils;

public class HikingRoutesFragment extends MapRoutesFragment {

	public static final String TAG = HikingRoutesFragment.class.getSimpleName();

	private HikingRoutesCard hikingRoutesCard;

	@Override
	protected boolean isEnabled() {
		return routeLayersHelper.isHikingRoutesEnabled();
	}

	@Override
	protected void toggleMainPreference(@NonNull View view) {
		routeLayersHelper.toggleHikingRoutes();
	}

	protected void setupHeader(@NonNull View view) {
		super.setupHeader(view);

		boolean enabled = isEnabled();
		View container = view.findViewById(R.id.preference_container);

		TextView title = container.findViewById(R.id.title);
		title.setText(R.string.rendering_attr_hikingRoutesOSMC_name);

		TextView description = container.findViewById(R.id.description);
		description.setText(enabled ? R.string.shared_string_enabled : R.string.shared_string_disabled);

		int selectedColor = settings.getApplicationMode().getProfileColor(nightMode);
		int disabledColor = AndroidUtils.getColorFromAttr(view.getContext(), R.attr.default_icon_color);
		ImageView icon = container.findViewById(R.id.icon);
		icon.setImageDrawable(getPaintedContentIcon(R.drawable.ic_action_trekking_dark, enabled ? selectedColor : disabledColor));
	}

	@Override
	protected void setupCards(@NonNull View view) {
		super.setupCards(view);

		cardsContainer.addView(createDivider(cardsContainer, true, true));
		addCard(new HikingRoutesCard(getMapActivity()));
		cardsContainer.addView(createDivider(cardsContainer, false, true));
	}
}