package net.osmand.plus.wikipedia;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.Log;

import net.osmand.IndexConstants;
import net.osmand.data.Amenity;
import net.osmand.data.LatLon;
import net.osmand.map.OsmandRegions;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.download.DownloadActivityType;
import net.osmand.plus.download.DownloadResources;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.resources.AmenityIndexRepositoryBinary;
import net.osmand.plus.wikivoyage.article.WikivoyageArticleWikiLinkFragment;
import net.osmand.plus.wikivoyage.data.TravelArticle;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

interface RegionCallback {
	void onRegionFound(String s);
}

public class WikipediaArticleHelper implements RegionCallback {

	private static final String TAG = WikipediaArticleHelper.class.getSimpleName();
	private static final String ZIP_EXT = ".zip";
	private static final String PAGE_PREFIX_HTTP = "http://";
	private static final String PAGE_PREFIX_HTTPS = "https://";
	private static final String WIKIVOAYAGE_DOMAIN = ".wikivoyage.org/wiki/";
	private static final String WIKI_DOMAIN = ".wikipedia.org/wiki/";

	private WikiArticleSearchTask articleSearchTask;
	private FragmentManager fragmentManager;
	private Context context;

	private String regionName;
	private boolean nightMode;

	public WikipediaArticleHelper(FragmentActivity context, FragmentManager fm, boolean nightMode) {
		fragmentManager = fm;
		this.context = context;
		this.nightMode = nightMode;
	}

	@Override
	public void onRegionFound(String s) {
		regionName = s;
	}

	public static class WikiArticleSearchTask extends AsyncTask<Void, Void, List<Amenity>> {
		private ProgressDialog dialog;
		private RegionCallback callback;
		private String name;
		private String regionName;
		private WeakReference<MapActivity> weakContext;
		private WeakReference<OsmandApplication> applicationReference;
		private boolean isNightMode;
		private String url;
		private String lang;
		private TravelArticle travelArticle;
		private Amenity amenityArticle;
		private FragmentManager fragmentManager;

		WikiArticleSearchTask(RegionCallback callback, TravelArticle travelArticle, String articleName,
		                      String regionName, FragmentManager fragmentManager,
		                      String lang, MapActivity context, boolean nightMode, String url) {
			this.fragmentManager = fragmentManager;
			this.regionName = regionName;
			name = articleName;
			this.lang = lang;
			weakContext = new WeakReference<>(context);
			OsmandApplication app = (OsmandApplication) context.getApplication();
			applicationReference = new WeakReference<>(app);
			dialog = createProgressDialog();
			this.isNightMode = nightMode;
			this.url = url;
			this.travelArticle = travelArticle;
			this.callback = callback;
		}

		WikiArticleSearchTask(RegionCallback callback, Amenity amenityArticle, String articleName,
		                      String regionName, FragmentManager fragmentManager,
		                      String lang, MapActivity context, boolean nightMode, String url) {
			this.fragmentManager = fragmentManager;
			this.regionName = regionName;
			name = articleName;
			this.lang = lang;
			weakContext = new WeakReference<>(context);
			OsmandApplication app = (OsmandApplication) context.getApplication();
			applicationReference = new WeakReference<>(app);
			dialog = createProgressDialog();
			this.isNightMode = nightMode;
			this.url = url;
			this.amenityArticle = amenityArticle;
			this.callback = callback;
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
				IndexItem item = null;
				try {
					if (travelArticle != null) {
						item = DownloadResources.findSmallestIndexItemAt(application,
								new LatLon(travelArticle.getLat(), travelArticle.getLon()), DownloadActivityType.WIKIPEDIA_FILE);
					} else if (amenityArticle != null) {
						item = DownloadResources.findSmallestIndexItemAt(application,
								amenityArticle.getLocation(), DownloadActivityType.WIKIPEDIA_FILE);
					}
				} catch (IOException e) {
					Log.e(TAG, e.getMessage(), e);
				}
				String filename = null;
				if (item != null && item.isDownloaded()) {
					filename = getFilenameFromIndex(item.getFileName());
				}
				AmenityIndexRepositoryBinary repository = application.getResourceManager()
						.getAmenityRepositoryByFileName(filename == null ? "" : filename);
				if (repository == null) {
					if ((regionName == null || regionName.isEmpty()) && item != null) {
						regionName = (getRegionName(item.getFileName(), application.getRegions()));
						callback.onRegionFound(regionName);
					}
					return null;
				} else {
					if (isCancelled()) {
						return null;
					}
					results.addAll(repository.searchAmenitiesByName(0, 0, 0, 0,
							Integer.MAX_VALUE, Integer.MAX_VALUE, name, null));
				}
			}
			return results;
		}

		@Override
		protected void onCancelled() {
			MapActivity activity = weakContext.get();
			if (activity != null && !activity.isActivityDestroyed() && dialog != null) {
				dialog.dismiss();
			}
			dialog = null;
			callback = null;
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

		private String getFilenameFromIndex(String fileName) {
			return fileName
					.replace("_" + IndexConstants.BINARY_MAP_VERSION, "")
					.replace(ZIP_EXT, "");
		}

		private String getRegionName(String filename, OsmandRegions osmandRegions) {
			if (osmandRegions != null && filename != null) {
				String regionName = filename
						.replace("_" + IndexConstants.BINARY_MAP_VERSION, "")
						.replace(IndexConstants.BINARY_WIKI_MAP_INDEX_EXT_ZIP, "")
						.toLowerCase();
				return osmandRegions.getLocaleName(regionName, false);
			}
			return "";
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

	public void getWikiArticle(TravelArticle article, String url) {
		if (article == null) {
			return;
		}
		String lang = getLang(url);
		String articleName = getArticleNameFromUrl(url, lang);
		articleSearchTask = new WikiArticleSearchTask(this, article, articleName, regionName, fragmentManager,
				lang, (MapActivity) context, nightMode, url);
		articleSearchTask.execute();
	}

	public void getWikiArticle(Amenity article, String url) {
		if (article == null) {
			return;
		}
		String lang = getLang(url);
		String articleName = getArticleNameFromUrl(url, lang);
		articleSearchTask = new WikiArticleSearchTask(this, article, articleName, regionName, fragmentManager,
				lang, (MapActivity) context, nightMode, url);
		articleSearchTask.execute();
	}

	@NonNull
	public String getLang(String url) {
		if (url.startsWith(PAGE_PREFIX_HTTP)) {
			return url.substring(url.startsWith(PAGE_PREFIX_HTTP) ? PAGE_PREFIX_HTTP.length() : 0, url.indexOf("."));
		} else if (url.startsWith(PAGE_PREFIX_HTTPS)) {
			return url.substring(url.startsWith(PAGE_PREFIX_HTTPS) ? PAGE_PREFIX_HTTPS.length() : 0, url.indexOf("."));
		}
		return "";
	}

	public String getArticleNameFromUrl(String url, String lang) {
		String domain = url.contains(WIKIVOAYAGE_DOMAIN) ? WIKIVOAYAGE_DOMAIN : WIKI_DOMAIN;
		String articleName = "";

		if (url.startsWith(PAGE_PREFIX_HTTP)) {
			articleName = url.replace(PAGE_PREFIX_HTTP + lang + domain, "")
					.replaceAll("_", " ");
		} else if (url.startsWith(PAGE_PREFIX_HTTPS)) {
			articleName = url.replace(PAGE_PREFIX_HTTPS + lang + domain, "")
					.replaceAll("_", " ");
		}
		try {
			articleName = URLDecoder.decode(articleName, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			Log.w(TAG, e.getMessage(), e);
		}
		return articleName;
	}

	public void stopSearchAsyncTask() {
		if (articleSearchTask != null && articleSearchTask.getStatus() == AsyncTask.Status.RUNNING) {
			articleSearchTask.cancel(false);
		}
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
}