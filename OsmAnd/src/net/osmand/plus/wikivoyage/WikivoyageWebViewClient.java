package net.osmand.plus.wikivoyage;


import android.content.Intent;
import android.net.Uri;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.utils.AndroidUtils;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.wikipedia.WikiArticleHelper;
import net.osmand.plus.wikivoyage.data.TravelArticle;
import net.osmand.plus.wikivoyage.explore.WikivoyageExploreActivity;

import java.io.File;
import java.util.List;

import static net.osmand.plus.wikipedia.WikiArticleHelper.WIKIVOYAGE_DOMAIN;
import static net.osmand.plus.wikipedia.WikiArticleHelper.WIKI_DOMAIN;


/**
 * Custom WebView client to handle the internal links.
 */

public class WikivoyageWebViewClient extends WebViewClient {

	private static final String TAG = WikivoyageWebViewClient.class.getSimpleName();

	private final OsmandApplication app;
	private final FragmentManager fragmentManager;
	private final FragmentActivity activity;
	private TravelArticle article;
	private final boolean nightMode;

	private static final String PREFIX_GEO = "geo:";
	private static final String PAGE_PREFIX_HTTP = "http://";
	private static final String PAGE_PREFIX_HTTPS = "https://";
	private final WikiArticleHelper wikiArticleHelper;


	public WikivoyageWebViewClient(@NonNull FragmentActivity activity, @NonNull FragmentManager fm, boolean nightMode) {
		app = (OsmandApplication) activity.getApplication();
		fragmentManager = fm;
		this.activity = activity;
		this.nightMode = nightMode;
		wikiArticleHelper = new WikiArticleHelper(activity, nightMode);
	}

	@Override
	public boolean shouldOverrideUrlLoading(WebView view, String url) {
		url = WikiArticleHelper.normalizeFileUrl(url);
		boolean isWebPage = url.startsWith(PAGE_PREFIX_HTTP) || url.startsWith(PAGE_PREFIX_HTTPS);
		if (url.contains(WIKIVOYAGE_DOMAIN) && isWebPage) {
			WikivoyageUtils.processWikivoyageDomain(activity, url, nightMode);
			return true;
		} else if (url.contains(WIKI_DOMAIN) && isWebPage && article != null) {
			LatLon defaultCoordinates = new LatLon(article.getLat(), article.getLon());
			WikivoyageUtils.processWikipediaDomain(wikiArticleHelper, defaultCoordinates, url);
		} else if (isWebPage) {
			WikiArticleHelper.warnAboutExternalLoad(url, activity, nightMode);
		} else if (url.startsWith(PREFIX_GEO)) {
			if (article != null && article.getGpxFile() != null) {
				GpxFile gpxFile = article.getGpxFile();
				List<WptPt> points = gpxFile.getPointsList();
				String coordinates = url.replace(PREFIX_GEO, "");
				WptPt gpxPoint = WikivoyageUtils.findNearestPoint(points, coordinates);

				if (gpxPoint != null) {
					OsmandSettings settings = app.getSettings();
					settings.setMapLocationToShow(gpxPoint.getLatitude(), gpxPoint.getLongitude(),
							settings.getLastKnownMapZoom(),
							new PointDescription(PointDescription.POINT_TYPE_WPT, gpxPoint.getName()),
							false,
							gpxPoint);

					if (activity instanceof WikivoyageExploreActivity) {
						WikivoyageExploreActivity exploreActivity = (WikivoyageExploreActivity) activity;
						exploreActivity.setArticle(article);
					}

					fragmentManager.popBackStackImmediate();

					File path = app.getTravelHelper().createGpxFile(article);
					gpxFile.setPath(path.getAbsolutePath());
					app.getSelectedGpxHelper().setGpxFileToDisplay(gpxFile);
					MapActivity.launchMapActivityMoveToTop(activity);
				}
			}
		} else {
			Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
			AndroidUtils.startActivityIfSafe(activity, intent);
		}
		return true;
	}

	public void setArticle(@NonNull TravelArticle article) {
		this.article = article;
	}

	public void stopRunningAsyncTasks() {
		if (wikiArticleHelper != null) {
			wikiArticleHelper.stopSearchAsyncTask();
		}
	}
}
