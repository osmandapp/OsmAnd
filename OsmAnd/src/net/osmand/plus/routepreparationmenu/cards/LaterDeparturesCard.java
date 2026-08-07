package net.osmand.plus.routepreparationmenu.cards;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.utils.AndroidUtils;

public class LaterDeparturesCard extends MapBaseCard {

	private final long laterTimeMillis;

	public LaterDeparturesCard(@NonNull MapActivity mapActivity, long laterTimeMillis) {
		super(mapActivity);
		this.laterTimeMillis = laterTimeMillis;
	}

	public long getLaterTimeMillis() {
		return laterTimeMillis;
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.route_ped_info;
	}

	@Override
	protected void updateContent() {
		view.findViewById(R.id.title).setVisibility(View.GONE);

		TextView buttonDescr = view.findViewById(R.id.button_descr);
		buttonDescr.setText(R.string.transit_later_departures);

		FrameLayout button = view.findViewById(R.id.button);
		button.setOnClickListener(v -> notifyButtonPressed(0));
		AndroidUtils.setBackground(app, button, nightMode, R.drawable.btn_border_light, R.drawable.btn_border_dark);
		AndroidUtils.setBackground(app, buttonDescr, nightMode, R.drawable.ripple_light, R.drawable.ripple_dark);

		Drawable icon = app.getUIUtilities().getIcon(R.drawable.ic_action_time, R.color.icon_color_default_light);
		((ImageView) view.findViewById(R.id.image)).setImageDrawable(icon);

		view.findViewById(R.id.card_divider).setVisibility(View.VISIBLE);
		view.findViewById(R.id.top_divider).setVisibility(View.GONE);
	}
}
