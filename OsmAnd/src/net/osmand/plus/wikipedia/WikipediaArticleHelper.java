package net.osmand.plus.wikipedia;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;

import net.osmand.CallbackWithObject;
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


public class WikipediaArticleHelper {

	private static final String TAG = WikipediaArticleHelper.class.getSimpleName();
	private static final String ZIP_EXT = ".zip";
	private static final String PAGE_PREFIX_HTTP = "http://";
	private static final String PAGE_PREFIX_HTTPS = "https://";
	private static final String WIKIVOAYAGE_DOMAIN = ".wikivoyage.org/wiki/";
	private static final String WIKI_DOMAIN = ".wikipedia.org/wiki/";

	private WikiArticleSearchTask articleSearchTask;
	private MapActivity mapActivity;

	private String regionName;
	private boolean nightMode;

	public WikipediaArticleHelper(MapActivity mapActivity, boolean nightMode) {
		this.mapActivity = mapActivity;
		this.nightMode = nightMode;
	}

	public static class WikiArticleSearchTask extends AsyncTask<Void, Void, List<Amenity>> {

		private ProgressDialog dialog;
		private CallbackWithObject<String> callback;
		private WeakReference<MapActivity> weakMapActivity;

		private Object article;
		private String regionName;
		private String url;
		private String lang;
		private String name;
		private boolean isNightMode;

		WikiArticleSearchTask(Object article,
		                      String regionName,
		                      MapActivity mapActivity,
		                      boolean nightMode,
		                      String url,
		                      final CallbackWithObject<String> callback) {
			this.article = article;
			this.regionName = regionName;
			weakMapActivity = new WeakReference<>(mapActivity);
			this.isNightMode = nightMode;
			this.url = url;
			this.callback = callback;
			dialog = createProgressDialog(mapActivity);
		}

		@Override
		protected void onPreExecute() {
			lang = getLang(url);
			name = getArticleNameFromUrl(url, lang);
			if (dialog != null) {
				dialog.show();
			}
		}

		@Override
		protected List<Amenity> doInBackground(Void... voids) {
			MapActivity activity = weakMapActivity.get();
			OsmandApplication application = activity.getMyApplication();
			List<Amenity> results = new ArrayList<>();
			if (application != null && !isCancelled()) {
				IndexItem item = null;
				try {
					if (article != null) {
						if (article instanceof TravelArticle) {
							TravelArticle travelArticle = (TravelArticle) article;
							item = DownloadResources.findSmallestIndexItemAt(application,
									new LatLon(travelArticle.getLat(), travelArticle.getLon()), DownloadActivityType.WIKIPEDIA_FILE);
						} else if (article instanceof Amenity) {
							Amenity amenityArticle = (Amenity) article;
							item = DownloadResources.findSmallestIndexItemAt(application,
									amenityArticle.getLocation(), DownloadActivityType.WIKIPEDIA_FILE);
						}
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
						callback.processResult(regionName);
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
			MapActivity activity = weakMapActivity.get();
			if (activity != null && !activity.isActivityDestroyed() && dialog != null) {
				dialog.dismiss();
			}
			dialog = null;
			callback = null;
		}

		@Override
		protected void onPostExecute(List<Amenity> found) {
			MapActivity activity = weakMapActivity.get();
			if (activity != null && !activity.isActivityDestroyed() && dialog != null) {
				dialog.dismiss();
				if (found == null) {
					WikivoyageArticleWikiLinkFragment.showInstance(activity.getSupportFragmentManager(), regionName == null ?
							"" : regionName, url);
				} else if (!found.isEmpty()) {
					WikipediaDialogFragment.showInstance(activity, found.get(0), lang);
				} else {
					warnAboutExternalLoad(url, weakMapActivity.get(), isNightMode);
				}
			}
		}
	}

	private static String getFilenameFromIndex(String fileName) {
		return fileName
				.replace("_" + IndexConstants.BINARY_MAP_VERSION, "")
				.replace(ZIP_EXT, "");
	}

	private static String getRegionName(String filename, OsmandRegions osmandRegions) {
		if (osmandRegions != null && filename != null) {
			String regionName = filename
					.replace("_" + IndexConstants.BINARY_MAP_VERSION, "")
					.replace(IndexConstants.BINARY_WIKI_MAP_INDEX_EXT_ZIP, "")
					.toLowerCase();
			return osmandRegions.getLocaleName(regionName, false);
		}
		return "";
	}

	private static ProgressDialog createProgressDialog(MapActivity activity) {
		if (activity != null && !activity.isActivityDestroyed()) {
			ProgressDialog dialog = new ProgressDialog(activity);
			dialog.setCancelable(false);
			dialog.setMessage(activity.getString(R.string.wiki_article_search_text));
			return dialog;
		}
		return null;
	}

	public void getWikiArticle(Object article, String url) {
		if (article == null) {
			return;
		}
		articleSearchTask = new WikiArticleSearchTask(article, regionName, mapActivity, nightMode, url, new CallbackWithObject<String>() {
			@Override
			public boolean processResult(String s) {
				regionName = s;
				return true;
			}
		});
		articleSearchTask.execute();
	}

	@NonNull
	public static String getLang(String url) {
		if (url.startsWith(PAGE_PREFIX_HTTP)) {
			return url.substring(url.startsWith(PAGE_PREFIX_HTTP) ? PAGE_PREFIX_HTTP.length() : 0, url.indexOf("."));
		} else if (url.startsWith(PAGE_PREFIX_HTTPS)) {
			return url.substring(url.startsWith(PAGE_PREFIX_HTTPS) ? PAGE_PREFIX_HTTPS.length() : 0, url.indexOf("."));
		}
		return "";
	}

	public static String getArticleNameFromUrl(String url, String lang) {
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