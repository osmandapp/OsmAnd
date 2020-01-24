package net.osmand.plus.routepreparationmenu.cards;

import android.net.Uri;
import android.support.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.wikipedia.WikipediaDialogFragment;

public class PublicTransportBetaWarningCard extends WarningCard {

	private static final String OSMAND_BLOG_LINK = "https://osmand.net/blog/guideline-pt";

	public PublicTransportBetaWarningCard(@NonNull MapActivity mapActivity) {
		super(mapActivity);
		imageId = R.drawable.ic_action_bus;
		title = mapActivity.getString(R.string.public_transport_warning_title);
		linkText = mapActivity.getString(R.string.public_transport_warning_descr_blog);
		startLinkIndex = linkText.lastIndexOf(" ");
		endLinkIndex = linkText.length() - 1;
	}

	@Override
	protected void onLinkClicked() {
		WikipediaDialogFragment.showFullArticle(mapActivity, Uri.parse(OSMAND_BLOG_LINK), nightMode);
	}
}
