package net.osmand.plus.track;

import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.fragment.app.Fragment;

import net.osmand.AndroidUtils;
import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.routepreparationmenu.cards.MapBaseCard;
import net.osmand.plus.widgets.TextViewEx;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Objects;

public class InfoCard extends MapBaseCard {

	private final Fragment targetFragment;
	private final GPXFile gpxFile;

	public InfoCard(@NonNull MapActivity mapActivity,
	                       @NonNull Fragment targetFragment,
	                       @NonNull GPXFile gpxFile) {
		super(mapActivity);
		this.gpxFile = gpxFile;
		this.targetFragment = targetFragment;
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.gpx_info_card;
	}

	@Override
	protected void updateContent() {
		if (gpxFile.metadata == null) {
			gpxFile.metadata = new GPXUtilities.Metadata();
		}

		String trackSize = "";
		String trackLocation = "";
		if (!gpxFile.showCurrentTrack){
			File file = new File(gpxFile.path);
			trackSize = AndroidUtils.formatSize(app, file.length());
			String directory = Objects.requireNonNull(file.getParentFile()).getName();
			trackLocation = directory.substring(0,1).toUpperCase().concat(directory.substring(1));
		}
		String trackDate = new SimpleDateFormat("dd MMM yyyy", Locale.US).format(gpxFile.metadata.time);

		LinearLayout sizeContainer = view.findViewById(R.id.size_container);
		AppCompatImageView sizeIcon = sizeContainer.findViewById(R.id.icon);
		TextViewEx sizeTitle = sizeContainer.findViewById(R.id.title);
		TextViewEx sizeDescription = sizeContainer.findViewById(R.id.description);
		sizeIcon.setImageResource(R.drawable.ic_sdcard);
		sizeTitle.setText(R.string.shared_string_size);
		sizeDescription.setText(trackSize);

		LinearLayout createdOnContainer = view.findViewById(R.id.created_on_container);
		AppCompatImageView createdOnIcon = createdOnContainer.findViewById(R.id.icon);
		TextViewEx createdOnTitle = createdOnContainer.findViewById(R.id.title);
		TextViewEx createdOnDescription = createdOnContainer.findViewById(R.id.description);
		createdOnIcon.setImageResource(R.drawable.ic_action_data);
		createdOnTitle.setText(R.string.created_on);
		createdOnDescription.setText(trackDate);

		LinearLayout locationContainer = view.findViewById(R.id.location_container);
		AppCompatImageView locationIcon = locationContainer.findViewById(R.id.icon);
		TextViewEx locationTitle = locationContainer.findViewById(R.id.title);
		TextViewEx locationDescription = locationContainer.findViewById(R.id.description);
		locationIcon.setImageResource(R.drawable.ic_action_folder);
		locationTitle.setText(R.string.shared_string_location);
		locationDescription.setText(trackLocation);

		if (gpxFile.metadata.time != 0) createdOnContainer.setVisibility(View.VISIBLE); // check if track create time is 0, to hide the createOn field

	}
}
