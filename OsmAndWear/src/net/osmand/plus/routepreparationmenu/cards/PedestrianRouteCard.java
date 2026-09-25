package net.osmand.plus.routepreparationmenu.cards;

import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;

public class PedestrianRouteCard extends MapBaseCard {

	private final int approxPedestrianTime;

	public PedestrianRouteCard(@NonNull MapActivity mapActivity, int approxPedestrianTime) {
		super(mapActivity);
		this.approxPedestrianTime = approxPedestrianTime;
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.route_ped_info;
	}

	@Override
	protected void updateContent() {
		TextView titleView = view.findViewById(R.id.title);
		String text = app.getString(R.string.public_transport_ped_route_title);
		String formattedDuration = OsmAndFormatter.getFormattedDuration(approxPedestrianTime, app);
		int start = text.indexOf("%1$s");
		int end = start + formattedDuration.length();
		text = text.replace("%1$s", formattedDuration);
		SpannableString spannable = new SpannableString(text);
		spannable.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		titleView.setText(spannable);

		FrameLayout button = view.findViewById(R.id.button);
		View buttonDescr = view.findViewById(R.id.button_descr);
		button.setOnClickListener(v -> notifyButtonPressed(0));
		AndroidUtils.setBackground(app, button, nightMode, R.drawable.btn_border_light, R.drawable.btn_border_dark);
		AndroidUtils.setBackground(app, buttonDescr, nightMode, R.drawable.ripple_light, R.drawable.ripple_dark);

		Drawable icPedestrian = app.getUIUtilities().getIcon(
				R.drawable.ic_action_pedestrian_dark,
				R.color.icon_color_default_light);
		((ImageView) view.findViewById(R.id.image)).setImageDrawable(
				AndroidUtils.getDrawableForDirection(app, icPedestrian));
		view.findViewById(R.id.card_divider).setVisibility(View.VISIBLE);
		view.findViewById(R.id.top_divider).setVisibility(View.GONE);
	}
}
