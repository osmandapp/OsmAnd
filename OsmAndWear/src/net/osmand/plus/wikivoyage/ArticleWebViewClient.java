package net.osmand.plus.wikivoyage;

import static net.osmand.plus.wikipedia.WikiArticleHelper.WIKIVOYAGE_DOMAIN;
import static net.osmand.plus.wikipedia.WikiArticleHelper.WIKI_DOMAIN;

import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.track.fragments.ReadDescriptionFragment;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.wikipedia.WikiArticleHelper;
import net.osmand.shared.data.KQuadRect;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.shared.gpx.primitives.WptPt;

public class ArticleWebViewClient extends WebViewClient {

	private static final String PAGE_PREFIX_HTTP = "http://";
	private static final String PAGE_PREFIX_HTTPS = "https://";

	private static final String PREFIX_GEO = "geo:";
	private static final String PREFIX_TEL = "tel:";

	private final OsmandApplication app;
	private final ReadDescriptionFragment fragment;
	private final FragmentActivity activity;
	private final GpxFile gpxFile;
	private final View view;
	private final boolean usedOnMap;
	private final WikiArticleHelper wikiArticleHelper;

	public ArticleWebViewClient(@NonNull ReadDescriptionFragment fragment,
	                            @NonNull FragmentActivity activity,
	                            @NonNull GpxFile gpxFile,
	                            @NonNull View view,
	                            boolean usedOnMap) {
		this.fragment = fragment;
		this.activity = activity;
		this.gpxFile = gpxFile;
		this.view = view;
		this.usedOnMap = usedOnMap;
		this.app = (OsmandApplication) activity.getApplicationContext();
		wikiArticleHelper = new WikiArticleHelper(activity, isNightMode());
	}

	@Override
	public void onPageCommitVisible(WebView webView, String url) {
		super.onPageCommitVisible(webView, url);
		if (fragment.getActivity() != null) {
			fragment.setupDependentViews(view);
		}
	}

	@Override
	public boolean shouldOverrideUrlLoading(WebView view, String url) {
		boolean isWebPage = url.startsWith(PAGE_PREFIX_HTTP) || url.startsWith(PAGE_PREFIX_HTTPS);
		if (url.contains(WIKIVOYAGE_DOMAIN) && isWebPage) {
			WikivoyageUtils.processWikivoyageDomain(activity, url, isNightMode());
			fragment.dismiss();
		} else if (url.contains(WIKI_DOMAIN) && isWebPage) {
			KQuadRect rect = gpxFile.getRect();
			LatLon defaultCoordinates = new LatLon(rect.centerY(), rect.centerX());
			WikivoyageUtils.processWikipediaDomain(wikiArticleHelper, defaultCoordinates, url);
		} else if (url.contains(PREFIX_TEL)) {
			Intent intent = new Intent(Intent.ACTION_DIAL);
			intent.setData(Uri.parse(url));
			return AndroidUtils.startActivityIfSafe(activity, intent);
		} else if (url.contains(PREFIX_GEO)) {
			fragment.closeAll();
			String coordinates = url.replace(PREFIX_GEO, "");
			WptPt gpxPoint = WikivoyageUtils.findNearestPoint(gpxFile.getPointsList(), coordinates);
			if (gpxPoint != null) {
				OsmandSettings settings = app.getSettings();
				settings.setMapLocationToShow(gpxPoint.getLatitude(), gpxPoint.getLongitude(),
						settings.getLastKnownMapZoom(),
						new PointDescription(PointDescription.POINT_TYPE_WPT, gpxPoint.getName()),
						false,
						gpxPoint);

				MapActivity.launchMapActivityMoveToTop(activity);
			}
		} else if (isWebPage) {
			WikiArticleHelper.warnAboutExternalLoad(url, activity, isNightMode());
		} else {
			Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
			return AndroidUtils.startActivityIfSafe(activity, intent);
		}
		return true;
	}

	protected boolean isNightMode() {
		return app.getDaynightHelper().isNightMode(usedOnMap);
	}
}