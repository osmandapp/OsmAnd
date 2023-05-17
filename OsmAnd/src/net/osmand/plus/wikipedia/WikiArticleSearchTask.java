package net.osmand.plus.wikipedia;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.view.ContextThemeWrapper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.ResultMatcher;
import net.osmand.data.Amenity;
import net.osmand.data.LatLon;
import net.osmand.map.OsmandRegions;
import net.osmand.map.WorldRegion;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.download.DownloadActivityType;
import net.osmand.plus.download.DownloadResources;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.resources.AmenityIndexRepositoryBinary;
import net.osmand.plus.wikivoyage.article.WikivoyageArticleWikiLinkFragment;
import net.osmand.util.Algorithms;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class WikiArticleSearchTask extends AsyncTask<Void, Void, List<Amenity>> {

	private static final org.apache.commons.logging.Log LOG = PlatformUtil.getLog(WikiArticleSearchTask.class);

	private ProgressDialog dialog;
	private final WeakReference<FragmentActivity> activityRef;

	private final LatLon articleLatLon;
	private String regionName;
	private final String url;
	private String lang;
	private String name;
	private final boolean isNightMode;

	WikiArticleSearchTask(
			@NonNull LatLon articleLatLon,
			@NonNull FragmentActivity activity,
			boolean nightMode,
			@NonNull String url
	) {
		this.articleLatLon = articleLatLon;
		activityRef = new WeakReference<>(activity);
		this.isNightMode = nightMode;
		this.url = url;
		dialog = createProgressDialog(activity, isNightMode);
	}

	@Override
	protected void onPreExecute() {
		lang = WikiArticleHelper.getLang(url);
		name = WikiArticleHelper.getArticleNameFromUrl(url, lang);
		if (dialog != null) {
			dialog.show();
		}
	}

	@Override
	protected List<Amenity> doInBackground(Void... voids) {
		FragmentActivity activity = activityRef.get();
		OsmandApplication application = (OsmandApplication) activity.getApplication();
		List<Amenity> results = new ArrayList<>();
		if (application != null && !isCancelled()) {
			List<WorldRegion> regions = null;
			if (articleLatLon != null) {
				try {
					regions = application.getRegions().getWorldRegionsAt(articleLatLon);
				} catch (IOException e) {
					LOG.error(e.getMessage(), e);
				}
			} else {
				return null;
			}
			if (regions != null) {
				AmenityIndexRepositoryBinary repository = getWikiRepositoryByRegions(regions, application);
				if (repository == null) {
					if (regionName == null || regionName.isEmpty()) {
						IndexItem item = null;
						try {
							item = DownloadResources.findSmallestIndexItemAt(application, articleLatLon,
									DownloadActivityType.WIKIPEDIA_FILE);
						} catch (IOException e) {
							LOG.error(e.getMessage(), e);
						}
						if (item != null) {
							regionName = getRegionName(item.getFileName(), application.getRegions());
						}
						return null;
					}

				} else {
					if (isCancelled()) {
						return null;
					}
					ResultMatcher<Amenity> matcher = new ResultMatcher<Amenity>() {
						@Override
						public boolean publish(Amenity amenity) {
							String localeName = amenity.getName();
							List<String> otherNames = amenity.getOtherNames(false);
							if (WikiArticleSearchTask.this.name.equals(localeName)) {
								results.add(amenity);
							} else {
								for (String amenityName : otherNames) {
									if (WikiArticleSearchTask.this.name.equals(amenityName)) {
										results.add(amenity);
										break;
									}
								}
							}
							return false;
						}

						@Override
						public boolean isCancelled() {
							return false;
						}
					};
					repository.searchAmenitiesByName(0, 0, 0, 0,
							Integer.MAX_VALUE, Integer.MAX_VALUE, name, matcher);
				}
			}
		}
		return results;
	}

	@Nullable
	private AmenityIndexRepositoryBinary getWikiRepositoryByRegions(@NonNull List<WorldRegion> regions, @NonNull OsmandApplication app) {
		AmenityIndexRepositoryBinary repo = null;
		for (WorldRegion reg : regions) {
			if (reg != null) {
				if (repo != null) {
					break;
				}
				repo = app.getResourceManager()
						.getAmenityRepositoryByFileName(Algorithms
								.capitalizeFirstLetterAndLowercase(reg.getRegionDownloadName()) +
								IndexConstants.BINARY_WIKI_MAP_INDEX_EXT);
			}
		}
		return repo;
	}

	@Override
	protected void onCancelled() {
		FragmentActivity activity = activityRef.get();
		if (activity != null && dialog != null) {
			dialog.dismiss();
		}
		dialog = null;
	}

	@Override
	protected void onPostExecute(List<Amenity> found) {
		FragmentActivity activity = activityRef.get();
		if (activity != null && dialog != null) {
			dialog.dismiss();
			if (found == null) {
				WikivoyageArticleWikiLinkFragment.showInstance(activity.getSupportFragmentManager(), regionName == null ?
						"" : regionName, url);
			} else if (!found.isEmpty()) {
				WikipediaDialogFragment.showInstance(activity, found.get(0), lang);
			} else {
				WikiArticleHelper.warnAboutExternalLoad(url, activityRef.get(), isNightMode);
			}
		}
	}

	private static ProgressDialog createProgressDialog(@Nullable FragmentActivity activity, boolean nightMode) {
		if (activity != null) {
			ProgressDialog dialog = new ProgressDialog(new ContextThemeWrapper(activity, nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme));
			dialog.setCancelable(false);
			dialog.setMessage(activity.getString(R.string.wiki_article_search_text));
			return dialog;
		}
		return null;
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
}
