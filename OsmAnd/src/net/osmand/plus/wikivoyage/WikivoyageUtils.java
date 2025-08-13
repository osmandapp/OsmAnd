package net.osmand.plus.wikivoyage;

import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.RequestCreator;

import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.PlatformUtil;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.wikipedia.WikiArticleHelper;
import net.osmand.plus.wikivoyage.article.WikivoyageArticleDialogFragment;
import net.osmand.plus.wikivoyage.data.TravelArticle.TravelArticleIdentifier;
import net.osmand.plus.wikivoyage.explore.WikivoyageExploreActivity;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;

import java.util.List;

import static net.osmand.plus.wikivoyage.article.WikivoyageArticleNavigationFragment.ARTICLE_ID_KEY;
import static net.osmand.plus.wikivoyage.article.WikivoyageArticleNavigationFragment.SELECTED_LANG_KEY;
import static net.osmand.util.MapUtils.ROUNDING_ERROR;

public class WikivoyageUtils {

	private static final Log LOG = PlatformUtil.getLog(WikivoyageUtils.class);

	private static final String GEO_PARAMS = "?lat=";
	public static final String ARTICLE_TITLE = "article_title";
	public static final String ARTICLE_LANG = "article_lang";
	public static final String EN_LANG_PREFIX = "en:";

	public static void setupNetworkPolicy(OsmandSettings settings, RequestCreator rc) {
		switch (settings.WIKI_ARTICLE_SHOW_IMAGES.get()) {
			case ON:
				break;
			case OFF:
				rc.networkPolicy(NetworkPolicy.OFFLINE);
				break;
			case WIFI:
				if (!settings.isWifiConnected()) {
					rc.networkPolicy(NetworkPolicy.OFFLINE);
				}
				break;
		}
	}

	public static WptPt findNearestPoint(@NonNull List<WptPt> points, @NonNull String coordinates) {
		double lat = Algorithms.parseDoubleSilently(coordinates.substring(0, coordinates.indexOf(",")), 0);
		double lon = Algorithms.parseDoubleSilently(coordinates.substring(coordinates.indexOf(",") + 1), 0);
		for (WptPt point : points) {
			if (MapUtils.getDistance(point.getLatitude(), point.getLongitude(), lat, lon) < ROUNDING_ERROR) {
				return point;
			}
		}
		return null;
	}

	public static void processWikivoyageDomain(@NonNull FragmentActivity activity,
	                                           @NonNull String url, boolean nightMode) {
		OsmandApplication app = (OsmandApplication) activity.getApplicationContext();
		String lang = WikiArticleHelper.getLanguageFromUrl(url);
		String articleName = WikiArticleHelper.getArticleNameFromUrl(url, lang);
		TravelArticleIdentifier articleId = app.getTravelHelper().getArticleId(articleName, lang);
		if (articleId != null) {
			openWikivoyageArticle(activity, articleId, lang);
		} else {
			WikiArticleHelper.warnAboutExternalLoad(url, activity, nightMode);
		}
	}

	public static void openWikivoyageArticle(@NonNull FragmentActivity activity,
	                                         @NonNull TravelArticleIdentifier articleId,
	                                         @NonNull String lang) {
		OsmandApplication app = (OsmandApplication) activity.getApplicationContext();
		if (activity instanceof WikivoyageExploreActivity) {
			WikivoyageArticleDialogFragment.showInstance(app, activity.getSupportFragmentManager(), articleId, lang);
		} else {
			Intent intent = new Intent(activity, WikivoyageExploreActivity.class);
			intent.putExtra(ARTICLE_ID_KEY, articleId);
			intent.putExtra(SELECTED_LANG_KEY, lang);
			activity.startActivity(intent);
		}
	}

	public static void processWikipediaDomain(@NonNull WikiArticleHelper wikiArticleHelper,
	                                          @NonNull LatLon defaultCoordinates,
	                                          @NonNull String url) {
		LatLon articleCoordinates = parseCoordinates(url);
		url = url.contains(GEO_PARAMS) ? url.substring(0, url.indexOf(GEO_PARAMS)) : url;
		wikiArticleHelper.showWikiArticle(
				articleCoordinates == null ? defaultCoordinates : articleCoordinates, url);
	}

	@Nullable
	private static LatLon parseCoordinates(@NonNull String url) {
		if (url.contains(GEO_PARAMS)) {
			String geoPart = url.substring(url.indexOf(GEO_PARAMS));
			int firstValueStart = geoPart.indexOf("=");
			int firstValueEnd = geoPart.indexOf("&");
			int secondValueStart = geoPart.indexOf("=", firstValueEnd);
			if (firstValueStart != -1 && firstValueEnd != -1 && secondValueStart != -1
					&& firstValueEnd > firstValueStart) {
				String lat = geoPart.substring(firstValueStart + 1, firstValueEnd);
				String lon = geoPart.substring(secondValueStart + 1);
				try {
					return new LatLon(Double.parseDouble(lat), Double.parseDouble(lon));
				} catch (NumberFormatException e) {
					LOG.error(e.getMessage(), e);
				}
			}
		}
		return null;
	}

	@NonNull
	public static String getTitleWithoutPrefix(@NonNull String title) {
		return title.startsWith(EN_LANG_PREFIX) ? title.substring(EN_LANG_PREFIX.length()) : title;
	}

}
