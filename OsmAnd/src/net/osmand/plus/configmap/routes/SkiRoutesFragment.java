package net.osmand.plus.configmap.routes;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.routepreparationmenu.cards.BaseCard.CardListener;
import net.osmand.plus.utils.AndroidUtils;

public class SkiRoutesFragment extends MapRoutesFragment {
	private SkiRoutesCard skiRoutesCard;

	@Override
	protected boolean isEnabled() {
		return routeLayersHelper.isSkiRoutesEnabled();
	}

	@Override
	protected void toggleMainPreference(@NonNull View view) {
		routeLayersHelper.toggleSkiRoutes();
		skiRoutesCard.updateContent();
	}

	protected void setupHeader(@NonNull View view) {
		super.setupHeader(view);

		boolean enabled = isEnabled();
		View container = view.findViewById(R.id.preference_container);

		TextView title = container.findViewById(R.id.title);
		title.setText(R.string.help_article_navigation_routing_ski_routing_name);

		TextView description = container.findViewById(R.id.description);
		description.setText(enabled ? R.string.shared_string_enabled : R.string.shared_string_disabled);

		int selectedColor = settings.getApplicationMode().getProfileColor(nightMode);
		int disabledColor = AndroidUtils.getColorFromAttr(view.getContext(), R.attr.default_icon_color);
		ImageView icon = container.findViewById(R.id.icon);
		icon.setImageDrawable(getPaintedContentIcon(R.drawable.ic_action_skiing, enabled ? selectedColor : disabledColor));
	}

	@Override
	protected void createCards(@NonNull View view) {
		super.createCards(view);

		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			skiRoutesCard = new SkiRoutesCard(mapActivity);
			skiRoutesCard.setListener(new CardListener() {
				@Override
				public void onCardPressed(@NonNull BaseCard card) {
					updateFragment();
				}
			});
			addCard(skiRoutesCard);
		}
	}

	private void updateFragment() {
		View view = getView();
		if (view != null) {
			setupHeader(view);
			setupContent(view);
		}
	}
}
