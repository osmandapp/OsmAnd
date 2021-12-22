package net.osmand.plus.routing.cards;

import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.routepreparationmenu.cards.MapBaseCard;
import net.osmand.plus.routing.PreviewRouteLineInfo;
import net.osmand.plus.utils.UiUtilities;

public class RouteTurnArrowsCard extends MapBaseCard {

	private final PreviewRouteLineInfo routeLineInfo;

	public RouteTurnArrowsCard(@NonNull MapActivity mapActivity, @NonNull PreviewRouteLineInfo routeLineInfo) {
		super(mapActivity);
		this.routeLineInfo = routeLineInfo;
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.route_turn_arrows_card;
	}

	private void setHasTurnArrow(boolean hasTurnArrow) {
		routeLineInfo.setShowTurnArrows(hasTurnArrow);
		mapActivity.refreshMap();
	}

	private boolean shouldShowTurnArrows() {
		return routeLineInfo.shouldShowTurnArrows();
	}

	@Override
	protected void updateContent() {
		TextView title = view.findViewById(R.id.title);
		LinearLayout container = view.findViewById(R.id.turn_arrow_switch_container);
		SwitchCompat arrowSwitch = view.findViewById(R.id.compound_button);
		boolean showTurnArrows = shouldShowTurnArrows();
		container.setBackground(UiUtilities.getStrokedBackgroundForCompoundButton(app, R.color.inactive_buttons_and_links_bg_light,
				R.color.inactive_buttons_and_links_bg_dark, showTurnArrows, nightMode));
		arrowSwitch.setChecked(shouldShowTurnArrows());
		title.setText(shouldShowTurnArrows() ? app.getString(R.string.shared_string_visible) : app.getString(R.string.shared_string_hidden));

		container.setOnClickListener(view -> {
			setHasTurnArrow(!shouldShowTurnArrows());
			updateContent();
		});
	}
}