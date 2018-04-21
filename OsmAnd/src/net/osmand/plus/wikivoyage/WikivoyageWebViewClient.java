package net.osmand.plus.wikivoyage;


import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import net.osmand.binary.BinaryMapDataObject;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.data.Amenity;
import net.osmand.data.PointDescription;
import net.osmand.data.QuadRect;
import net.osmand.map.OsmandRegions;
import net.osmand.osm.PoiCategory;
import net.osmand.plus.GPXUtilities;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.WikipediaDialogFragment;
import net.osmand.plus.resources.AmenityIndexRepository;
import net.osmand.plus.resources.AmenityIndexRepositoryBinary;
import net.osmand.plus.wikivoyage.article.WikivoyageArticleDialogFragment;
import net.osmand.plus.wikivoyage.article.WikivoyageArticleWikiLinkFragment;
import net.osmand.plus.wikivoyage.data.TravelArticle;
import net.osmand.plus.wikivoyage.explore.WikivoyageExploreDialogFragment;
import net.osmand.util.MapUtils;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static android.support.v4.app.FragmentManager.POP_BACK_STACK_INCLUSIVE;

/**
 * Custom WebView client to handle the internal links.
 */

public class WikivoyageWebViewClient extends WebViewClient {

	private static final String TAG = WikivoyageWebViewClient.class.getSimpleName();
	private static final String REGIONS_OCBF = "regions.ocbf";

	private OsmandApplication app;
	private FragmentManager fragmentManager;
	private Context context;
	private TravelArticle article;
	private boolean nightMode;

	private static final String PREFIX_GEO = "geo:";
	private static final String PAGE_PREFIX_HTTP = "http://";
	private static final String PAGE_PREFIX_HTTPS = "https://";
	private static final String WIKIVOAYAGE_DOMAIN = ".wikivoyage.com/wiki/";
	private static final String WIKI_DOMAIN = ".wikipedia.org/wiki/";

	public WikivoyageWebViewClient(FragmentActivity context, FragmentManager fm, boolean nightMode) {
		app = (OsmandApplication) context.getApplication();
		fragmentManager = fm;
		this.context = context;
		this.nightMode = nightMode;
	}

	@Override
	public boolean shouldOverrideUrlLoading(WebView view, String url) {
		if (url.contains(WIKIVOAYAGE_DOMAIN)) {
			String lang = getLang(url);
			String articleName = getArticleNameFromUrl(url, lang);
			long articleId = app.getTravelDbHelper().getArticleId(articleName, lang);
			if (articleId != 0) {
				WikivoyageArticleDialogFragment.showInstance(app, fragmentManager, articleId, lang);
			} else {
				warnAboutExternalLoad(url);
			}
			return true;
		} else if (url.contains(WIKI_DOMAIN)) {
			String articleName = getArticleNameFromUrl(url, getLang(url));
			getWikiArticle(articleName, url);
		} else if (url.startsWith(PAGE_PREFIX_HTTP) || url.startsWith(PAGE_PREFIX_HTTPS)) {
			warnAboutExternalLoad(url);
		} else if (url.startsWith(PREFIX_GEO)) {
			if (article != null) {
				List<GPXUtilities.WptPt> points = article.getGpxFile().getPoints();
				GPXUtilities.WptPt gpxPoint = null;
				String coordinates = url.replace(PREFIX_GEO, "");
				double lat;
				double lon;
				try {
					lat = Double.valueOf(coordinates.substring(0, coordinates.indexOf(",")));
					lon = Double.valueOf(coordinates.substring(coordinates.indexOf(",") + 1,
							coordinates.length()));
				} catch (NumberFormatException e) {
					Log.w(TAG, e.getMessage(), e);
					return true;
				}
				for (GPXUtilities.WptPt point : points) {
					if (point.getLatitude() == lat && point.getLongitude() == lon) {
						gpxPoint = point;
						break;
					}
				}
				if (gpxPoint != null) {
					final OsmandSettings settings = app.getSettings();
					settings.setMapLocationToShow(lat, lon,	settings.getLastKnownMapZoom(),
							new PointDescription(PointDescription.POINT_TYPE_WPT, gpxPoint.name),
							false,
							gpxPoint);
					fragmentManager.popBackStackImmediate(WikivoyageExploreDialogFragment.TAG,
							POP_BACK_STACK_INCLUSIVE);

					File path = app.getTravelDbHelper().createGpxFile(article);
					GPXUtilities.GPXFile gpxFile = article.getGpxFile();
					gpxFile.path = path.getAbsolutePath();
					app.getSelectedGpxHelper().setGpxFileToDisplay(gpxFile);
					MapActivity.launchMapActivityMoveToTop(context);
				}
			}
		} else {
			Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
			context.startActivity(i);
		}
		return true;
	}

	@NonNull
	private String getLang(String url) {
		return url.substring(url.startsWith(PAGE_PREFIX_HTTPS) ? PAGE_PREFIX_HTTPS.length() : 0, url.indexOf("."));
	}

	private String getArticleNameFromUrl(String url, String lang) {
		String domain = url.contains(WIKIVOAYAGE_DOMAIN) ? WIKIVOAYAGE_DOMAIN : WIKI_DOMAIN;
		String articleName = url.replace(PAGE_PREFIX_HTTPS + lang + domain, "")
				.replaceAll("_", " ");
		try {
			articleName = URLDecoder.decode(articleName, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			Log.w(TAG, e.getMessage(), e);
		}
		return articleName;
	}

	private void warnAboutExternalLoad(final String url) {
		new AlertDialog.Builder(context)
				.setTitle(url)
				.setMessage(R.string.online_webpage_warning)
				.setPositiveButton(R.string.shared_string_ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						WikipediaDialogFragment.showFullArticle(context, Uri.parse(url), nightMode);
					}
				})
				.setNegativeButton(R.string.shared_string_cancel, null)
				.show();
	}

	public void setArticle(TravelArticle article) {
		this.article = article;
	}

	protected void getWikiArticle(String name, String url) {
		List<Amenity> found = new ArrayList<>();
		List<AmenityIndexRepositoryBinary> indexes = app.getResourceManager()
				.getWikiAmenityRepository(article.getLat(), article.getLon());
		if (indexes.isEmpty()) {
			String reg = "";
			try {
				reg = getRegion(article.getLat(), article.getLon());
			} catch (IOException e) {
				Log.e(TAG, e.getMessage(), e);
			}
			WikivoyageArticleWikiLinkFragment.showInstance(fragmentManager, reg, url);
		} else {
			for (AmenityIndexRepositoryBinary repo : indexes) {
				found.addAll(repo.searchAmenitiesByName(0, 0, 0, 0,
						Integer.MAX_VALUE, Integer.MAX_VALUE, name, null));
			}
			WikipediaDialogFragment.showInstance((AppCompatActivity) context, found.get(0));
		}

	}

	private String getRegion(double lat, double lon) throws IOException {
		OsmandRegions regions = new OsmandRegions();
		regions.prepareFile(app.getAppPath(REGIONS_OCBF).getAbsolutePath());
		regions.cacheAllCountries();
		int x31 = MapUtils.get31TileNumberX(lon);
		int y31 = MapUtils.get31TileNumberY(lat);
		List<BinaryMapDataObject> cs = regions.query(x31, y31);
		for (BinaryMapDataObject b : cs) {
			if(regions.contain(b, x31, y31)) {
				return regions.getFullName(b);
			}
		}
		return "";
	}
}
