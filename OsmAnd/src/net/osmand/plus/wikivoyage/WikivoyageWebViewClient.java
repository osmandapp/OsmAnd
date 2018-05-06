package net.osmand.plus.wikivoyage;



import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import net.osmand.binary.BinaryMapDataObject;
import net.osmand.data.Amenity;
import net.osmand.data.PointDescription;
import net.osmand.map.OsmandRegions;
import net.osmand.plus.GPXUtilities;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.wikipedia.WikipediaDialogFragment;
import net.osmand.plus.resources.AmenityIndexRepositoryBinary;
import net.osmand.plus.wikivoyage.article.WikivoyageArticleDialogFragment;
import net.osmand.plus.wikivoyage.article.WikivoyageArticleWikiLinkFragment;
import net.osmand.plus.wikivoyage.data.TravelArticle;
import net.osmand.plus.wikivoyage.explore.WikivoyageExploreDialogFragment;
import net.osmand.util.MapUtils;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import static android.support.v4.app.FragmentManager.POP_BACK_STACK_INCLUSIVE;


interface RegionCallback {
	void onRegionFound(String[] s);
}
/**
 * Custom WebView client to handle the internal links.
 */

public class WikivoyageWebViewClient extends WebViewClient implements RegionCallback {

	private static final String TAG = WikivoyageWebViewClient.class.getSimpleName();

	private OsmandApplication app;
	private FragmentManager fragmentManager;
	private Context context;
	private TravelArticle article;
	private boolean nightMode;
	private String regionName;
	private String regionDownloadName;

	private static final String PREFIX_GEO = "geo:";
	private static final String PAGE_PREFIX_HTTP = "http://";
	private static final String PAGE_PREFIX_HTTPS = "https://";
	private static final String WIKIVOAYAGE_DOMAIN = ".wikivoyage.org/wiki/";
	private static final String WIKI_DOMAIN = ".wikipedia.org/wiki/";
	private FetchWikiRegion fetchRegionTask;
	private WikiArticleSearchTask articleSearchTask;


	public WikivoyageWebViewClient(FragmentActivity context, FragmentManager fm, boolean nightMode) {
		app = (OsmandApplication) context.getApplication();
		fragmentManager = fm;
		this.context = context;
		this.nightMode = nightMode;
	}

	@Override
	public boolean shouldOverrideUrlLoading(WebView view, String url) {
		boolean isWebPage = url.startsWith(PAGE_PREFIX_HTTP) || url.startsWith(PAGE_PREFIX_HTTPS);
		if (url.contains(WIKIVOAYAGE_DOMAIN) && isWebPage) {
			String lang = getLang(url);
			String articleName = getArticleNameFromUrl(url, lang);
			long articleId = app.getTravelDbHelper().getArticleId(articleName, lang);
			if (articleId != 0) {
				WikivoyageArticleDialogFragment.showInstance(app, fragmentManager, articleId, lang);
			} else {
				warnAboutExternalLoad(url, context, nightMode);
			}
			return true;
		} else if (url.contains(WIKI_DOMAIN) && isWebPage) {
			String lang = getLang(url);
			String articleName = getArticleNameFromUrl(url, lang);
			getWikiArticle(articleName, lang, url);
		} else if (isWebPage) {
			warnAboutExternalLoad(url, context, nightMode);
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

	private static void warnAboutExternalLoad(final String url, final Context context, final boolean nightMode) {
		if (context == null) {
			return;
		}
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
		if (this.article != null && app != null) {
			fetchRegionTask = new FetchWikiRegion(this, app.getRegions(), article.getLat(), article.getLon());
			fetchRegionTask.execute();
		}
	}

	private void getWikiArticle(String name, String lang, String url) {
		articleSearchTask = new WikiArticleSearchTask(article, name, regionName, fragmentManager,
				lang, regionDownloadName, (MapActivity) context, nightMode, url);
		articleSearchTask.execute();

	}

	@Override
	public void onRegionFound(String[] s) {
		if (s != null) {
			regionDownloadName = s[0];
			regionName = s[1];
		}
	}

	private static String getDownloadName(double lat, double lon, OsmandRegions regions) {
		String downloadName = null;
		if (regions != null) {
			int x31 = MapUtils.get31TileNumberX(lon);
			int y31 = MapUtils.get31TileNumberY(lat);
			List<BinaryMapDataObject> dataObjects = null;
			try {
				dataObjects = regions.query(x31, y31);
			} catch (IOException e) {
				e.printStackTrace();
			}
			if (dataObjects != null) {
				for (BinaryMapDataObject b : dataObjects) {
					if (regions.contain(b, x31, y31)) {
						downloadName = regions.getDownloadName(b);
						if (downloadName != null && !downloadName.isEmpty()) {
							break;
						}
					}
				}
			}
		}
		return downloadName;
	}

	public void stopRunningAsyncTasks() {
		if (articleSearchTask != null && articleSearchTask.getStatus() == AsyncTask.Status.RUNNING) {
			articleSearchTask.cancel(false);
		}
		if (fetchRegionTask != null && fetchRegionTask.getStatus() == AsyncTask.Status.RUNNING) {
			fetchRegionTask.cancel(false);
		}
	}

	private static class FetchWikiRegion extends AsyncTask<Void, Void, String[]> {

		private RegionCallback callback;
		private OsmandRegions osmandRegions;
		private double lat;
		private double lon;

		FetchWikiRegion(RegionCallback callback, OsmandRegions osmandRegions, double lat, double lon) {
			this.callback = callback;
			this.osmandRegions = osmandRegions;
			this.lat = lat;
			this.lon = lon;
		}

		@Override
		protected String[] doInBackground(Void... voids) {
			String downloadName = getDownloadName(lat, lon, osmandRegions);
			downloadName = downloadName == null ? "" : downloadName;
			return new String[]{downloadName,
					osmandRegions.getLocaleName(downloadName, false)};
		}

		@Override
		protected void onCancelled(){
			callback = null;
		}

		@Override
		protected void onPostExecute(String[] result) {
			if (callback != null) {
				callback.onRegionFound(result);
			}
		}
	}

	private static class WikiArticleSearchTask extends AsyncTask<Void, Void, List<Amenity>> {
		private ProgressDialog dialog;
		private String name;
		private String regionDownloadName;
		private String regionName;
		private OsmandRegions regions;
		private WeakReference<MapActivity> weakContext;
		private WeakReference<OsmandApplication> applicationReference;
		private boolean isNightMode;
		private String url;
		private String lang;
		private TravelArticle article;
		private FragmentManager fragmentManager;

		WikiArticleSearchTask(TravelArticle article, String articleName, String regionName, FragmentManager fragmentManager,
							  String lang, String regionDownloadName,
							  MapActivity context, boolean nightMode, String url) {
			this.fragmentManager = fragmentManager;
			this.regionName = regionName;
			name = articleName;
			this.lang = lang;
			weakContext = new WeakReference<>(context);
			OsmandApplication app = (OsmandApplication) context.getApplication();
			applicationReference = new WeakReference<>(app);
			regions = app.getRegions();
			dialog = createProgressDialog();
			this.isNightMode = nightMode;
			this.url = url;
			this.regionDownloadName = regionDownloadName;
			this.article = article;
		}

		@Override
		protected void onPreExecute() {
			if (dialog != null) {
				dialog.show();
			}
		}

		@Override
		protected List<Amenity> doInBackground(Void... voids) {
			if (regionDownloadName == null && article != null && !isCancelled()) {
				regionDownloadName = getDownloadName(article.getLat(), article.getLon(), regions);
			}
			OsmandApplication application = applicationReference.get();
			if (application != null && !isCancelled()) {
				AmenityIndexRepositoryBinary index = application.getResourceManager()
						.getWikiRepositoryByRegionName(regionDownloadName);
				if (index != null && !isCancelled()) {
					return new ArrayList<>(index.searchAmenitiesByName(0, 0, 0, 0,
							Integer.MAX_VALUE, Integer.MAX_VALUE, name, null));
				}
			}
			return null;
		}

		@Override
		protected void onCancelled(){
			dialog = null;
		}

		@Override
		protected void onPostExecute(List<Amenity> found) {
			MapActivity activity = weakContext.get();
			if (activity != null && !activity.isActivityDestroyed() && dialog != null) {
				dialog.dismiss();
				if (found == null) {
					WikivoyageArticleWikiLinkFragment.showInstance(fragmentManager, regionName == null ?
							"" : regionName, url);
				} else if (!found.isEmpty()) {
					WikipediaDialogFragment.showInstance(activity, found.get(0), lang);
				} else {
					warnAboutExternalLoad(url, weakContext.get(), isNightMode);
				}
			}
		}

		private ProgressDialog createProgressDialog() {
			MapActivity activity = weakContext.get();
			if (activity != null && !activity.isActivityDestroyed()) {
				ProgressDialog dialog = new ProgressDialog(activity);
				dialog.setCancelable(false);
				dialog.setMessage(activity.getString(R.string.wiki_article_search_text));
				return dialog;
			}
			return null;
		}
	}
}
