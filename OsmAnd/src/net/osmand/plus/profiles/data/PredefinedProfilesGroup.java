package net.osmand.plus.profiles.data;

import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import net.osmand.AndroidUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.helpers.FontCache;

import java.util.List;

public class PredefinedProfilesGroup extends ProfilesGroup {

	private String type;

	public PredefinedProfilesGroup(@NonNull String title,
	                               @NonNull String type,
	                               @NonNull List<ProfileDataObject> profiles) {
		super(title, profiles);
		this.type = type;
	}

	public String getType() {
		return type;
	}

	@Override
	public CharSequence getDescription(@NonNull final OsmandApplication ctx, boolean nightMode) {
		String fullDescription = ctx.getString(R.string.provided_by, description);
		int color = ContextCompat.getColor(ctx, nightMode ?
				R.color.active_color_primary_dark : R.color.active_color_primary_light);
		Typeface typeface = FontCache.getRobotoMedium(ctx);
		final String url = description.toString();
		SpannableString spannable = UiUtilities.createCustomFontSpannable(typeface, fullDescription, url);
		int startInd = fullDescription.indexOf(url);
		spannable.setSpan(new ForegroundColorSpan(color), startInd, startInd + url.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

		ClickableSpan clickableSpan = new ClickableSpan() {
			@Override
			public void onClick(@NonNull View widget) {
				Intent i = new Intent(Intent.ACTION_VIEW);
				i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				i.setData(Uri.parse(url));
				if (AndroidUtils.isIntentSafe(ctx, i)) {
					ctx.startActivity(i);
				}
			}

			@Override
			public void updateDrawState(@NonNull TextPaint ds) {
				super.updateDrawState(ds);
				ds.setUnderlineText(false);
			}
		};
		spannable.setSpan(clickableSpan, startInd, startInd + url.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

		return spannable;
	}
}
