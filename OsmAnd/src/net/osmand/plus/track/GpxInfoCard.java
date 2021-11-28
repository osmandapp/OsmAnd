package net.osmand.plus.track;

import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;

import net.osmand.AndroidUtils;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.IndexConstants;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.routepreparationmenu.cards.MapBaseCard;
import net.osmand.plus.widgets.TextViewEx;
import net.osmand.util.Algorithms;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class GpxInfoCard extends MapBaseCard {

	private final GPXFile gpxFile;
	private static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

	public GpxInfoCard(@NonNull MapActivity mapActivity,
	                   @NonNull GPXFile gpxFile) {
		super(mapActivity);
		this.gpxFile = gpxFile;
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.gpx_info_card;
	}

	@Override
	protected void updateContent() {

		if ( gpxFile.showCurrentTrack) view.setVisibility(View.GONE);

		File file = new File(gpxFile.path);
		String trackSize = AndroidUtils.formatSize(app, file.length());
		String trackDate = SIMPLE_DATE_FORMAT.format(gpxFile.metadata.time);
		String trackLocation = "";

		if (file.getParentFile() != null){
			if (file.getParentFile().equals(app.getAppPath(IndexConstants.GPX_INDEX_DIR))){
				trackLocation = app.getString(R.string.shared_string_tracks);
			} else trackLocation = Algorithms.capitalizeFirstLetter(file.getParentFile().getName());

		}

		fillCardItems(R.id.size_container, R.drawable.ic_sdcard, R.string.shared_string_size, trackSize);
		fillCardItems(R.id.created_on_container, R.drawable.ic_action_data, R.string.created_on, trackDate);
		fillCardItems(R.id.location_container, R.drawable.ic_action_folder, R.string.shared_string_location, trackLocation);
	}

	private void fillCardItems(int containerId, int iconId, int titleId, String descriptionText) {
		LinearLayout container = view.findViewById(containerId);
		AppCompatImageView icon = container.findViewById(R.id.icon);
		TextViewEx title = container.findViewById(R.id.title);
		TextViewEx description = container.findViewById(R.id.description);
		icon.setImageResource(iconId);
		title.setText(titleId);
		description.setText(descriptionText);

		if (containerId == R.id.created_on_container) AndroidUiHelper.updateVisibility(container, gpxFile.metadata.time > 0);
	}
}