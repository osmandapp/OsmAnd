package net.osmand.plus.track.cards;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.CONTEXT_MENU_LINKS_ID;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import net.osmand.gpx.GPXFile;
import net.osmand.gpx.GPXUtilities;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.mapcontextmenu.other.ShareMenu;
import net.osmand.plus.routepreparationmenu.cards.MapBaseCard;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.util.Algorithms;

public class AuthorCard extends MapBaseCard {
	public static final int NO_ICON = -1;

	private final GPXFile gpxFile;
	private final boolean nightMode;

	public AuthorCard(@NonNull MapActivity mapActivity, @NonNull GPXFile gpxFile, boolean nightMode) {
		super(mapActivity);
		this.gpxFile = gpxFile;
		this.nightMode = nightMode;
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.author_card;
	}

	@Override
	public void updateContent() {
		GPXUtilities.Author author = gpxFile.metadata.author;

		String name = author.name;
		String email = author.email;
		String link = author.link;

		updateVisibility(!Algorithms.isEmpty(name) || !Algorithms.isEmpty(email) || !Algorithms.isEmpty(link));
		OsmandApplication app = mapActivity.getMyApplication();
		if (!Algorithms.isEmpty(name)) {
			fillCardItems(app, view, nightMode, R.id.name_container, NO_ICON, R.string.shared_string_name, name, false, false);
			AndroidUiHelper.updateVisibility(view.findViewById(R.id.name_container), true);
		}
		if (!Algorithms.isEmpty(email)) {
			fillCardItems(app, view, nightMode, R.id.email_container, NO_ICON, R.string.shared_string_email_address, email, false, true);
			AndroidUiHelper.updateVisibility(view.findViewById(R.id.email_container), true);
		}
		if (!Algorithms.isEmpty(link)) {
			fillCardItems(app, view, nightMode, R.id.link_container, NO_ICON, R.string.shared_string_link, link, true, true);
			AndroidUiHelper.updateVisibility(view.findViewById(R.id.link_container), true);
		}
	}

	public static void fillCardItems(OsmandApplication app, View view, boolean nightMode, int containerId, int iconId, int titleId, String descriptionText, boolean isUrl, boolean copyOnLongClick) {
		LinearLayout container = view.findViewById(containerId);
		fillCardItems(app, view, nightMode, container, iconId, titleId, descriptionText, isUrl, copyOnLongClick);
	}

	public static void fillCardItems(OsmandApplication app, View view, boolean nightMode, LinearLayout container, int iconId, int titleId, String descriptionText, boolean isUrl, boolean copyOnLongClick) {
		fillCardItems(app, view, nightMode, container, iconId, view.getContext().getString(titleId), descriptionText, isUrl, copyOnLongClick);
	}

	public static void fillCardItems(OsmandApplication app, View view, boolean nightMode, LinearLayout container, int iconId, String titleText, String descriptionText, boolean isUrl, boolean copyOnLongClick) {
		ImageView icon = container.findViewById(R.id.icon);
		TextView title = container.findViewById(R.id.title);
		TextView description = container.findViewById(R.id.description);

		title.setText(titleText);
		if (iconId == NO_ICON) {
			icon.setVisibility(View.GONE);
		} else {
			icon.setImageResource(iconId);
		}
		description.setText(descriptionText);


		if (isUrl) {
			container.setBackgroundResource(AndroidUtils.resolveAttribute(view.getContext(), android.R.attr.selectableItemBackground));
			int linkTextColor = ContextCompat.getColor(view.getContext(), !nightMode ? R.color.active_color_primary_light : R.color.active_color_primary_dark);
			description.setTextColor(linkTextColor);
			container.setOnClickListener(v -> {
				if (app.getAppCustomization().isFeatureEnabled(CONTEXT_MENU_LINKS_ID)) {
					Intent intent = new Intent(Intent.ACTION_VIEW);
					intent.setData(Uri.parse(descriptionText));
					AndroidUtils.startActivityIfSafe(v.getContext(), intent);
				}
			});
		}
		if (copyOnLongClick) {
			container.setBackgroundResource(AndroidUtils.resolveAttribute(view.getContext(), android.R.attr.selectableItemBackground));
			container.setOnLongClickListener(v -> {
				copyToClipboard(descriptionText, view.getContext());
				return true;
			});
		}
	}

	protected static void copyToClipboard(String text, Context ctx) {
		ShareMenu.copyToClipboardWithToast(ctx, text, Toast.LENGTH_SHORT);
	}
}