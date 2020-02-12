package net.osmand.plus.routepreparationmenu.cards;

import android.graphics.Typeface;
import android.os.Build;
import android.support.annotation.NonNull;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import net.osmand.AndroidUtils;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;

public class PedestrianRouteCard extends BaseCard {

	private int approxPedestrianTime;

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
		TextView titleView = (TextView) view.findViewById(R.id.title);
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
		button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				CardListener listener = getListener();
				if (listener != null) {
					listener.onCardButtonPressed(PedestrianRouteCard.this, 0);
				}
			}
		});
		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
			AndroidUtils.setBackground(app, button, nightMode, R.drawable.btn_border_light, R.drawable.btn_border_dark);
			AndroidUtils.setBackground(app, buttonDescr, nightMode, R.drawable.ripple_light, R.drawable.ripple_dark);
		} else {
			AndroidUtils.setBackground(app, buttonDescr, nightMode, R.drawable.btn_border_trans_light, R.drawable.btn_border_trans_dark);
		}
		view.findViewById(R.id.card_divider).setVisibility(View.VISIBLE);
		view.findViewById(R.id.top_divider).setVisibility(View.GONE);
	}
}
