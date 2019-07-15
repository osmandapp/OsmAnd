package net.osmand.plus.wikivoyage.menu;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import net.osmand.GPXUtilities;
import net.osmand.data.PointDescription;
import net.osmand.plus.GpxSelectionHelper;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.MenuBuilder;
import net.osmand.plus.mapcontextmenu.controllers.WptPtMenuController;
import net.osmand.plus.wikivoyage.article.WikivoyageArticleDialogFragment;
import net.osmand.plus.wikivoyage.data.TravelArticle;

public class WikivoyageWptPtMenuController extends WptPtMenuController {
	public WikivoyageWptPtMenuController(@NonNull MenuBuilder menuBuilder, @NonNull final MapActivity mapActivity, @NonNull PointDescription pointDescription, @NonNull GPXUtilities.WptPt wpt) {
		super(menuBuilder, mapActivity, pointDescription, wpt);

		final OsmandApplication app = mapActivity.getMyApplication();
		GpxSelectionHelper.SelectedGpxFile selectedGpxFile = app.getSelectedGpxHelper().getSelectedGPXFile(wpt);
		GPXUtilities.GPXFile gpxFile = selectedGpxFile != null ? selectedGpxFile.getGpxFile() : null;
		GPXUtilities.Metadata metadata = gpxFile != null ? gpxFile.metadata : null;
		final TravelArticle article = metadata != null ? getTravelArticle(metadata) : null;
		if (article != null) {
			leftTitleButtonController = new TitleButtonController() {
				@Override
				public void buttonPressed() {
					WikivoyageArticleDialogFragment.showInstance(app, mapActivity.getSupportFragmentManager(), article.getTripId(), article.getLang());
				}
			};
			leftTitleButtonController.caption = mapActivity.getString(R.string.context_menu_read_article);
			leftTitleButtonController.leftIconId = R.drawable.ic_action_read_text;
		}
	}

	private TravelArticle getTravelArticle(@NonNull GPXUtilities.Metadata metadata) {
		String title = metadata.getArticleTitle();
		String lang = metadata.getArticleLang();
		if (!TextUtils.isEmpty(title) && !TextUtils.isEmpty(lang)) {
			return getMapActivity().getMyApplication().getTravelDbHelper().getArticle(title, lang);
		}
		return null;
	}
}
