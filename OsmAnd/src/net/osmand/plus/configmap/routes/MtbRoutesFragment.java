package net.osmand.plus.configmap.routes;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.utils.AndroidUtils;

public class MtbRoutesFragment extends MapRoutesFragment {

	public static final String TAG = MtbRoutesFragment.class.getSimpleName();

	@Override
	protected boolean isEnabled() {
		return routeLayersHelper.isMtbRoutesEnabled();
	}

	@Override
	protected void toggleMainPreference(@NonNull View view) {
		routeLayersHelper.toggleMtbRoutes();
	}

	protected void setupHeader(@NonNull View view) {
		super.setupHeader(view);

		boolean enabled = isEnabled();
		View container = view.findViewById(R.id.preference_container);

		TextView title = container.findViewById(R.id.title);
		title.setText(R.string.rendering_attr_showMtbRoutes_name);

		int selectedColor = settings.getApplicationMode().getProfileColor(nightMode);
		int disabledColor = AndroidUtils.getColorFromAttr(app, R.attr.default_icon_color);
		ImageView icon = container.findViewById(R.id.icon);
		icon.setImageDrawable(getPaintedContentIcon(R.drawable.ic_action_mountain_bike, enabled ? selectedColor : disabledColor));

		AndroidUiHelper.updateVisibility(container.findViewById(R.id.description), false);
	}

	@Override
	protected void setupCards(@NonNull View view) {
		super.setupCards(view);

		cardsContainer.addView(createDivider(cardsContainer, true, true));
		addCard(new MtbRoutesCard(getMapActivity()));
		cardsContainer.addView(createDivider(cardsContainer, false, true));
	}
}