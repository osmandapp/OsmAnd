package net.osmand.plus.wikivoyage.menu;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.Metadata;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.controllers.WptPtMenuController;
import net.osmand.plus.wikivoyage.article.WikivoyageArticleDialogFragment;
import net.osmand.plus.wikivoyage.data.TravelArticle;
import net.osmand.plus.wikivoyage.data.TravelArticle.TravelArticleIdentifier;

public class WikivoyageWptPtMenuController extends WptPtMenuController {

	private WikivoyageWptPtMenuController(@NonNull MapActivity mapActivity, @NonNull PointDescription pointDescription, @NonNull WptPt wpt, @NonNull TravelArticle article) {
		super(new WikivoyageWptPtMenuBuilder(mapActivity, wpt), mapActivity, pointDescription, wpt);
		final TravelArticleIdentifier articleId = article.generateIdentifier();
		final String lang = article.getLang();
		leftTitleButtonController = new TitleButtonController() {
			@Override
			public void buttonPressed() {
				MapActivity mapActivity = getMapActivity();
				if (mapActivity != null) {
					WikivoyageArticleDialogFragment.showInstance(mapActivity.getMyApplication(),
							mapActivity.getSupportFragmentManager(), articleId, lang);
				}
			}
		};
		leftTitleButtonController.caption = mapActivity.getString(R.string.context_menu_read_article);
		leftTitleButtonController.startIconId = R.drawable.ic_action_read_text;
	}

	private static TravelArticle getTravelArticle(@NonNull MapActivity mapActivity, @NonNull WptPt wpt) {
		OsmandApplication app = mapActivity.getMyApplication();
		SelectedGpxFile selectedGpxFile = app.getSelectedGpxHelper().getSelectedGPXFile(wpt);
		GPXFile gpxFile = selectedGpxFile != null ? selectedGpxFile.getGpxFile() : null;
		Metadata metadata = gpxFile != null ? gpxFile.metadata : null;
		String title = metadata != null ? metadata.getArticleTitle() : null;
		String lang = metadata != null ? metadata.getArticleLang() : null;
		if (!TextUtils.isEmpty(title) && !TextUtils.isEmpty(lang)) {
			return app.getTravelHelper().getArticleByTitle(title, new LatLon(wpt.lat, wpt.lon), lang, false, null);
		}
		return null;
	}

	public static WptPtMenuController getInstance(@NonNull MapActivity mapActivity, @NonNull PointDescription pointDescription, @NonNull WptPt wpt) {
		TravelArticle travelArticle = getTravelArticle(mapActivity, wpt);
		if (travelArticle != null) {
			return new WikivoyageWptPtMenuController(mapActivity, pointDescription, wpt, travelArticle);
		}
		return null;
	}
}
