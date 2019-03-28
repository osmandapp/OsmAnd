package net.osmand.plus.routepreparationmenu.cards;

import android.graphics.Typeface;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.routepreparationmenu.AddPointBottomSheetDialog;
import net.osmand.plus.routepreparationmenu.MapRouteInfoMenu.PointType;
import net.osmand.plus.wikipedia.WikipediaDialogFragment;

public class WarningCard extends BaseCard {

	public static final String OSMAND_BLOG_LINK = "https://osmand.net/blog/guideline-pt";

	public WarningCard(MapActivity mapActivity) {
		super(mapActivity);
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.warning_card;
	}

	@Override
	protected void updateContent() {
		ImageView icon = (ImageView) view.findViewById(R.id.warning_img);
		TextView warningTitle = (TextView) view.findViewById(R.id.warning_title);
		TextView warningDescr = (TextView) view.findViewById(R.id.warning_descr);

		if (app.getRoutingHelper().isPublicTransportMode()) {
			icon.setImageDrawable(getContentIcon(R.drawable.ic_action_bus_dark));
			warningTitle.setText(R.string.public_transport_warning_title);

			String text = app.getString(R.string.public_transport_warning_descr_blog);
			SpannableString spannable = new SpannableString(text);
			ClickableSpan clickableSpan = new ClickableSpan() {
				@Override
				public void updateDrawState(@NonNull TextPaint ds) {
					ds.setColor(getActiveColor());
					ds.setUnderlineText(false);
				}

				@Override
				public void onClick(@NonNull View widget) {
					WikipediaDialogFragment.showFullArticle(app, Uri.parse(OSMAND_BLOG_LINK), nightMode);
				}
			};
			int startIndex = text.lastIndexOf(" ");
			if (startIndex != -1) {
				spannable.setSpan(clickableSpan, startIndex, text.length() - 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
				spannable.setSpan(new StyleSpan(Typeface.BOLD), startIndex, text.length() - 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
				warningDescr.setMovementMethod(LinkMovementMethod.getInstance());
			}
			warningDescr.setText(spannable);
		} else {
			icon.setImageDrawable(getContentIcon(R.drawable.ic_action_waypoint));
			warningTitle.setText(R.string.route_is_too_long_v2);
			SpannableString text = new SpannableString(app.getString(R.string.add_intermediate));
			ClickableSpan clickableSpan = new ClickableSpan() {
				@Override
				public void updateDrawState(@NonNull TextPaint ds) {
					ds.setColor(getActiveColor());
					ds.setUnderlineText(false);
				}

				@Override
				public void onClick(@NonNull View widget) {
					AddPointBottomSheetDialog.showInstance(mapActivity, PointType.INTERMEDIATE);
				}
			};
			text.setSpan(clickableSpan, 0, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			text.setSpan(new StyleSpan(Typeface.BOLD), 0, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			warningDescr.setMovementMethod(LinkMovementMethod.getInstance());
			warningDescr.setTextSize(15);
			warningDescr.setText(text);
		}
	}
}