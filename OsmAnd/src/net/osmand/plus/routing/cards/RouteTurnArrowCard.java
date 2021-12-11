package net.osmand.plus.routing.cards;

import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;

import net.osmand.AndroidUtils;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.routepreparationmenu.cards.MapBaseCard;
import net.osmand.plus.routing.PreviewRouteLineInfo;
public class RouteTurnArrowCard extends MapBaseCard {

	private PreviewRouteLineInfo previewRouteLineInfo;
	public RouteTurnArrowCard(@NonNull MapActivity mapActivity,
	                          @NonNull PreviewRouteLineInfo previewRouteLineInfo) {
		super(mapActivity);
		this.previewRouteLineInfo = previewRouteLineInfo;
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.route_turn_arrows_card;
	}

	private void setHasTurnArrow(boolean hasTurnArrow) {
		previewRouteLineInfo.setHasTurnArrow(hasTurnArrow);
		mapActivity.refreshMap();
	}

	private boolean isHasTurnArrow() {
		return previewRouteLineInfo.isHasTurnArrow();
	}

	@Override
	protected void updateContent() {
		TextView title = view.findViewById(R.id.title);
		LinearLayout container = view.findViewById(R.id.turn_arrow_switch_container);
		SwitchCompat arrowSwitch = view.findViewById(R.id.compound_button);
		container.setBackground(AndroidUtils.getStrokedBackgroundForSwitch(app, R.color.inactive_buttons_and_links_bg_light, R.color.inactive_buttons_and_links_bg_dark, isHasTurnArrow(), nightMode));

		arrowSwitch.setChecked(isHasTurnArrow());
		title.setText(arrowSwitch.isChecked() ? app.getString(R.string.shared_string_visible) : app.getString(R.string.shared_string_hidden));

		container.setOnClickListener(view -> {
			arrowSwitch.setChecked(!arrowSwitch.isChecked());
			setHasTurnArrow(!isHasTurnArrow());
			title.setText(isHasTurnArrow() ? app.getString(R.string.shared_string_visible) : app.getString(R.string.shared_string_hidden));
			container.setBackground(AndroidUtils.getStrokedBackgroundForSwitch(app, R.color.inactive_buttons_and_links_bg_light, R.color.inactive_buttons_and_links_bg_dark, isHasTurnArrow(), nightMode));
		});
	}
}