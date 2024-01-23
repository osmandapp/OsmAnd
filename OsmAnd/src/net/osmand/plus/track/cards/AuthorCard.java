package net.osmand.plus.track.cards;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.CONTEXT_MENU_LINKS_ID;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import net.osmand.gpx.GPXUtilities.Author;
import net.osmand.gpx.GPXUtilities.Metadata;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.util.Algorithms;

public class AuthorCard extends BaseMetadataCard {

	public AuthorCard(@NonNull MapActivity mapActivity, @NonNull Metadata metadata) {
		super(mapActivity, metadata);
	}

	@Override
	@StringRes
	protected int getTitleId() {
		return R.string.shared_string_author;
	}

	@Override
	public void updateContent() {
		super.updateContent();

		Author author = metadata.author;
		boolean visible = author != null && (!Algorithms.isEmpty(author.name)
				|| !Algorithms.isEmpty(author.email) || !Algorithms.isEmpty(author.link));

		updateVisibility(visible);

		if (visible) {
			if (!Algorithms.isEmpty(author.name)) {
				createItemRow(getString(R.string.shared_string_name), author.name, null);
			}
			if (!Algorithms.isEmpty(author.email)) {
				createItemRow(getString(R.string.shared_string_email_address), author.email, null);
			}
			addLinkRow(author.link);
		}
	}

	private void addLinkRow(@Nullable String link) {
		if (!Algorithms.isEmpty(link)) {
			Drawable icon = getContentIcon(R.drawable.ic_world_globe_dark);
			View view = createItemRow(getString(R.string.shared_string_link), link, icon);

			TextView description = view.findViewById(R.id.description);
			description.setTextColor(ColorUtilities.getActiveColor(app, nightMode));

			view.setOnClickListener(v -> {
				if (app.getAppCustomization().isFeatureEnabled(CONTEXT_MENU_LINKS_ID)) {
					Intent intent = new Intent(Intent.ACTION_VIEW);
					intent.setData(Uri.parse(link));
					AndroidUtils.startActivityIfSafe(v.getContext(), intent);
				}
			});
		}
	}
}