package net.osmand.plus.routepreparationmenu.cards;


import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.FontCache;
import net.osmand.plus.utils.UiUtilities;


public class ImportTrackCard extends MapBaseCard {

	public ImportTrackCard(@NonNull MapActivity mapActivity) {
		super(mapActivity);
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.import_track_card;
	}

	@Override
	protected void updateContent() {
		int color = ContextCompat.getColor(app, ColorUtilities.getActiveColorId(nightMode));
		String importTrack = app.getString(R.string.plan_route_import_track);
		SpannableString spannable = UiUtilities.createCustomFontSpannable(FontCache.getMediumFont(), importTrack, importTrack, importTrack);
		spannable.setSpan(new ForegroundColorSpan(color), 0, importTrack.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

		TextView title = view.findViewById(R.id.title);
		title.setText(spannable);

		ImageView icon = view.findViewById(R.id.icon);
		icon.setImageDrawable(getContentIcon(R.drawable.ic_action_import_to));

		view.setOnClickListener(v -> notifyCardPressed());
	}
}