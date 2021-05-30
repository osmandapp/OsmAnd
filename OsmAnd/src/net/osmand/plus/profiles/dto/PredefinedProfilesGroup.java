package net.osmand.plus.profiles.dto;

import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.helpers.FontCache;

import java.util.List;

public class PredefinedProfilesGroup extends ProfilesGroup {

	public PredefinedProfilesGroup(@NonNull String title,
	                               @NonNull List<ProfileDataObject> profiles) {
		super(title, profiles);
	}

	@Override
	public CharSequence getDescription(@NonNull OsmandApplication ctx, boolean nightMode) {
		String fullDescription = ctx.getString(R.string.provided_by, description);
		int color = ContextCompat.getColor(ctx, nightMode ?
				R.color.active_color_primary_dark : R.color.active_color_primary_light);
		Typeface typeface = FontCache.getRobotoMedium(ctx);
		String url = description.toString();
		SpannableString spannable = UiUtilities.createCustomFontSpannable(typeface, fullDescription, url);
		int startInd = fullDescription.indexOf(url);
		spannable.setSpan(new ForegroundColorSpan(color), startInd, startInd + url.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		return spannable;
	}
}
