package net.osmand.plus.track.cards;

import static net.osmand.plus.track.cards.AuthorCard.*;
import static net.osmand.plus.track.cards.AuthorCard.NO_ICON;

import androidx.annotation.NonNull;

import net.osmand.gpx.GPXFile;
import net.osmand.gpx.GPXUtilities;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.routepreparationmenu.cards.MapBaseCard;
import net.osmand.util.Algorithms;

public class CopyrightCard extends MapBaseCard {

	private final GPXFile gpxFile;
	private final boolean nightMode;

	public CopyrightCard(@NonNull MapActivity mapActivity, @NonNull GPXFile gpxFile, boolean nightMode) {
		super(mapActivity);
		this.gpxFile = gpxFile;
		this.nightMode = nightMode;
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.copyright_card;
	}

	@Override
	public void updateContent() {
		GPXUtilities.Copyright copyright = gpxFile.metadata.copyright;

		String author = copyright.author;
		String year = copyright.year;
		String license = copyright.license;

		updateVisibility(!Algorithms.isEmpty(author) || !Algorithms.isEmpty(year) || !Algorithms.isEmpty(license));
		OsmandApplication app = mapActivity.getMyApplication();

		if (!Algorithms.isEmpty(author)) {
			fillCardItems(app, view, nightMode, R.id.author_container, NO_ICON, R.string.shared_string_name, author, false, true);
			AndroidUiHelper.updateVisibility(view.findViewById(R.id.author_container), true);
		}
		if (!Algorithms.isEmpty(year)) {
			fillCardItems(app, view, nightMode, R.id.year_container, NO_ICON, R.string.year, year, false, true);
			AndroidUiHelper.updateVisibility(view.findViewById(R.id.year_container), true);
		}
		if (!Algorithms.isEmpty(license)) {
			fillCardItems(app, view, nightMode, R.id.license_container, NO_ICON, R.string.shared_string_license, license, false, true);
			AndroidUiHelper.updateVisibility(view.findViewById(R.id.license_container), true);
		}
	}
}