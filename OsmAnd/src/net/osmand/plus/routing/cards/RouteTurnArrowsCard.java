package net.osmand.plus.routing.cards;

import android.graphics.drawable.Drawable;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.SwitchCompat;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.routepreparationmenu.cards.MapBaseCard;
import net.osmand.plus.routing.PreviewRouteLineInfo;

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

	private boolean showTurnArrows() {
		return routeLineInfo.showTurnArrows();
	}

	@Override
	protected void updateContent() {
		TextView title = view.findViewById(R.id.title);
		LinearLayout container = view.findViewById(R.id.turn_arrow_switch_container);
		SwitchCompat arrowSwitch = view.findViewById(R.id.compound_button);
		Drawable active = AppCompatResources.getDrawable(getMapActivity(), nightMode ? R.drawable.btn_background_inactive_dark : R.drawable.btn_background_inactive_light);
		Drawable inActive = AppCompatResources.getDrawable(getMapActivity(), nightMode ? R.drawable.btn_border_dark : R.drawable.btn_border_light);

		boolean showTurnArrows = showTurnArrows();
		arrowSwitch.setChecked(showTurnArrows);
		title.setText(showTurnArrows ? app.getString(R.string.shared_string_visible) : app.getString(R.string.shared_string_hidden));
		container.setBackground(showTurnArrows ? active : inActive);

		container.setOnClickListener(view -> {
			setHasTurnArrow(!showTurnArrows());
			updateContent();
		});
	}
}