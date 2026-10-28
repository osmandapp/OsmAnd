package net.osmand.plus.routepreparationmenu.cards;


import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.utils.FontCache;
import net.osmand.plus.utils.UiUtilities;


public class SelectTrackCard extends MapBaseCard {

	public SelectTrackCard(@NonNull MapActivity mapActivity) {
		super(mapActivity);
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.bottom_sheet_item_simple;
	}

	@Override
	protected void updateContent() {
		String title = app.getString(R.string.select_another_track);
		SpannableString spannable = UiUtilities.createCustomFontSpannable(FontCache.getMediumFont(), title, title, title);
		ForegroundColorSpan colorSpan = new ForegroundColorSpan(getActiveColor());
		spannable.setSpan(colorSpan, 0, title.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

		TextView titleTv = view.findViewById(R.id.title);
		titleTv.setText(spannable);

		ImageView icon = view.findViewById(R.id.icon);
		icon.setImageDrawable(getActiveIcon(R.drawable.ic_action_folder));

		int minHeight = getDimen(R.dimen.route_info_list_text_padding);
		view.setMinimumHeight(minHeight);
		view.setOnClickListener(v -> notifyCardPressed());
	}
}