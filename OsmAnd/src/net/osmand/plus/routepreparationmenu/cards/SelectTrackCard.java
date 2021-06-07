package net.osmand.plus.routepreparationmenu.cards;

import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.FontCache;

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
		Typeface typeface = FontCache.getRobotoMedium(app);
		String title = app.getString(R.string.select_another_track);
		SpannableString spannable = UiUtilities.createCustomFontSpannable(typeface, title, title, title);
		ForegroundColorSpan colorSpan = new ForegroundColorSpan(getActiveColor());
		spannable.setSpan(colorSpan, 0, title.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

		TextView titleTv = view.findViewById(R.id.title);
		titleTv.setText(spannable);

		ImageView icon = view.findViewById(R.id.icon);
		icon.setImageDrawable(getActiveIcon(R.drawable.ic_action_folder));

		int minHeight = app.getResources().getDimensionPixelSize(R.dimen.route_info_list_text_padding);
		view.setMinimumHeight(minHeight);
		view.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				CardListener listener = getListener();
				if (listener != null) {
					listener.onCardPressed(SelectTrackCard.this);
				}
			}
		});
	}
}