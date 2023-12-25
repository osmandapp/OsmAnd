package net.osmand.plus.track.cards;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.CONTEXT_MENU_LINKS_ID;

import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import net.osmand.plus.utils.AndroidUtils;
import net.osmand.gpx.GPXFile;
import net.osmand.IndexConstants;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.routepreparationmenu.cards.MapBaseCard;
import net.osmand.util.Algorithms;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class GpxInfoCard extends MapBaseCard {

	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("d MMM yyyy", Locale.getDefault());

	private final GPXFile gpxFile;

	public GpxInfoCard(@NonNull MapActivity mapActivity, @NonNull GPXFile gpxFile) {
		super(mapActivity);
		this.gpxFile = gpxFile;
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.gpx_info_card;
	}

	@Override
	public void updateContent() {
		if (gpxFile.showCurrentTrack) {
			updateVisibility(false);
			return;
		}
		updateVisibility(true);

		File file = new File(gpxFile.path);
		String trackSize = AndroidUtils.formatSize(app, file.length());
		String trackDate = DATE_FORMAT.format(gpxFile.metadata.time);
		String trackLocation = "";
		String link = gpxFile.metadata.link;
		String description = gpxFile.metadata.desc;

		File dir = file.getParentFile();
		if (dir != null) {
			if (dir.equals(app.getAppPath(IndexConstants.GPX_INDEX_DIR))) {
				trackLocation = app.getString(R.string.shared_string_tracks);
			} else {
				trackLocation = Algorithms.capitalizeFirstLetter(dir.getName());
			}
		}
		fillCardItems(R.id.size_container, R.drawable.ic_sdcard, R.string.shared_string_size, trackSize);
		fillCardItems(R.id.created_on_container, R.drawable.ic_action_data, R.string.created_on, trackDate);
		fillCardItems(R.id.location_container, R.drawable.ic_action_folder, R.string.shared_string_location, trackLocation);

		if (!Algorithms.isEmpty(link)) {
			View linkContainer = view.findViewById(R.id.link_container);
			TextView linkTextView = linkContainer.findViewById(R.id.description);
			int linkTextColor = ContextCompat.getColor(view.getContext(), !nightMode ? R.color.active_color_primary_light : R.color.active_color_primary_dark);
			linkTextView.setTextColor(linkTextColor);

			fillCardItems(R.id.link_container, R.drawable.ic_world_globe_dark, R.string.shared_string_link, link);
			AndroidUiHelper.updateVisibility(view.findViewById(R.id.link_container), true);
			linkContainer.setBackgroundResource(AndroidUtils.resolveAttribute(view.getContext(), android.R.attr.selectableItemBackground));

			linkContainer.setOnClickListener(v -> {
				if (app.getAppCustomization().isFeatureEnabled(CONTEXT_MENU_LINKS_ID)) {
					Intent intent = new Intent(Intent.ACTION_VIEW);
					intent.setData(Uri.parse(link));
					AndroidUtils.startActivityIfSafe(v.getContext(), intent);
				}
			});
		}
		if (!Algorithms.isEmpty(description)) {
			fillCardItems(R.id.description_container, R.drawable.ic_action_description, R.string.shared_string_description, description);
			AndroidUiHelper.updateVisibility(view.findViewById(R.id.description_container), true);
		}
	}

	private void fillCardItems(int containerId, int iconId, int titleId, String descriptionText) {
		LinearLayout container = view.findViewById(containerId);
		ImageView icon = container.findViewById(R.id.icon);
		TextView title = container.findViewById(R.id.title);
		TextView description = container.findViewById(R.id.description);
		title.setText(titleId);
		icon.setImageResource(iconId);
		description.setText(descriptionText);

		if (containerId == R.id.created_on_container) {
			AndroidUiHelper.updateVisibility(container, gpxFile.metadata.time > 0);
		}
	}
}