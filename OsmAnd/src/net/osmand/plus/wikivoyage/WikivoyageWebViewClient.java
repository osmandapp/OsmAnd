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

import net.osmand.IndexConstants;
import net.osmand.binary.BinaryMapDataObject;
import net.osmand.data.Amenity;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.map.OsmandRegions;
import net.osmand.map.WorldRegion;
import net.osmand.plus.GPXUtilities;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.download.DownloadActivityType;
import net.osmand.plus.download.DownloadResources;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.resources.AmenityIndexRepository;
import net.osmand.plus.wikipedia.WikipediaDialogFragment;
import net.osmand.plus.resources.AmenityIndexRepositoryBinary;
import net.osmand.plus.wikivoyage.article.WikivoyageArticleDialogFragment;
import net.osmand.plus.wikivoyage.article.WikivoyageArticleWikiLinkFragment;
import net.osmand.plus.wikivoyage.data.TravelArticle;
import net.osmand.plus.wikivoyage.explore.WikivoyageExploreDialogFragment;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static android.support.v4.app.FragmentManager.POP_BACK_STACK_INCLUSIVE;


interface RegionCallback {
	void onRegionFound(String s);
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

	private static final String PREFIX_GEO = "geo:";
	private static final String PAGE_PREFIX_HTTP = "http://";
	private static final String PAGE_PREFIX_HTTPS = "https://";
	private static final String WIKIVOAYAGE_DOMAIN = ".wikivoyage.org/wiki/";
	private static final String WIKI_DOMAIN = ".wikipedia.org/wiki/";
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
	}

	private void getWikiArticle(String name, String lang, String url) {
		List<IndexItem> items = null;
		try {
			items = DownloadResources.findIndexItemsAt(app,
					new LatLon(article.getLat(), article.getLon()), DownloadActivityType.WIKIPEDIA_FILE, true);
		} catch (IOException e) {
			e.printStackTrace();
		}
		articleSearchTask = new WikiArticleSearchTask(article, name, regionName, fragmentManager,
				lang, (MapActivity) context, nightMode, url);
		articleSearchTask.execute();

	}

	@Override
	public void onRegionFound(String s) {
		regionName = s;
	}

	public void stopRunningAsyncTasks() {
		if (articleSearchTask != null && articleSearchTask.getStatus() == AsyncTask.Status.RUNNING) {
			articleSearchTask.cancel(false);
		}
	}

	private static class WikiArticleSearchTask extends AsyncTask<Void, Void, List<Amenity>> {
		private ProgressDialog dialog;
		private String name;
		private List<String> regionDownloadNames;
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
							  String lang, MapActivity context, boolean nightMode, String url) {
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
			OsmandApplication application = applicationReference.get();
			List<Amenity> results = new ArrayList<>();
			if (application != null && !isCancelled()) {
				List<IndexItem> items = null;
				try {
					items = DownloadResources.findIndexItemsAt(application,
							new LatLon(article.getLat(), article.getLon()), DownloadActivityType.WIKIPEDIA_FILE, true);
				} catch (IOException e) {
					e.printStackTrace();
				}
				List<String> downloadedItems = new ArrayList<>();
				if (items != null) {
					Collections.sort(items, new Comparator<IndexItem>() {
						@Override
						public int compare(IndexItem indexItem, IndexItem thatItem) {
							return (int) (indexItem.getContentSize() - thatItem.getContentSize());
						}
					});
					for (IndexItem i : items) {
						if (i.isDownloaded()) {
							downloadedItems.add(i.getFileName().replace("_2", ""));
						}
					}
				}
				List<AmenityIndexRepositoryBinary> repos = new ArrayList<>();
				for (String s : downloadedItems) {
					AmenityIndexRepositoryBinary repository = application.getResourceManager()
							.getWikiRepositoryByRegionName(s);
					if (repository != null) {
						repos.add(repository);
					}
				}
				if (repos.isEmpty()) {
					regionName = (items == null || items.isEmpty()) ? "" : getRegionName(items.get(0), application.getRegions());
					return null;
				}
				for (AmenityIndexRepositoryBinary repo : repos) {
					results.addAll(repo.searchAmenitiesByName(0, 0, 0, 0,
							Integer.MAX_VALUE, Integer.MAX_VALUE, name, null));
				}
			}
			return results;
		}

		private String getRegionName(IndexItem indexItem, OsmandRegions osmandRegions) {
			if (osmandRegions != null) {
				String regionName = indexItem.getFileName().replace("_2", "")
						.replace(IndexConstants.BINARY_WIKI_MAP_INDEX_EXT, "").toLowerCase();
				int x31 = MapUtils.get31TileNumberX(article.getLat());
				int y31 = MapUtils.get31TileNumberY(article.getLon());
				List<BinaryMapDataObject> dataObjects = null;
				try {
					if (isCancelled()) {
						return null;
					}
					dataObjects = osmandRegions.query(x31, y31);
				} catch (IOException e) {
					e.printStackTrace();
				}
				if (dataObjects != null) {
					for (BinaryMapDataObject b : dataObjects) {
						if (isCancelled()) {
							break;
						}
						if(osmandRegions.contain(b, x31, y31)) {
							String downloadName = osmandRegions.getDownloadName(b);
							if (downloadName == null) {
								continue;
							}
							if (downloadName.equals(regionName)) {
								return osmandRegions.getLocaleName(downloadName, false);
							}
						}
					}
				}
			}
			return "";
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
