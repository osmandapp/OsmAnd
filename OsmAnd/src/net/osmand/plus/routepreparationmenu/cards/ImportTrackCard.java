package net.osmand.plus.routepreparationmenu.cards;

import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.FontCache;

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
		int color = ContextCompat.getColor(app, nightMode ?
				R.color.active_color_primary_dark : R.color.active_color_primary_light);
		Typeface typeface = FontCache.getRobotoMedium(app);
		String importTrack = app.getString(R.string.plan_route_import_track);
		SpannableString spannable = UiUtilities.createCustomFontSpannable(typeface, importTrack, importTrack, importTrack);
		spannable.setSpan(new ForegroundColorSpan(color), 0, importTrack.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

		TextView title = view.findViewById(R.id.title);
		title.setText(spannable);

		ImageView icon = view.findViewById(R.id.icon);
		icon.setImageDrawable(getContentIcon(R.drawable.ic_action_import_to));

		view.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				CardListener listener = getListener();
				if (listener != null) {
					listener.onCardPressed(ImportTrackCard.this);
				}
			}
		});
	}
}