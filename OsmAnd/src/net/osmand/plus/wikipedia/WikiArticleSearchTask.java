package net.osmand.plus.wikipedia;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

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
import net.osmand.plus.resources.ResourceManager;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.wikivoyage.article.WikivoyageArticleWikiLinkFragment;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WikiArticleSearchTask extends AsyncTask<Void, Void, List<Amenity>> {

	private static final Log LOG = PlatformUtil.getLog(WikiArticleSearchTask.class);

	private final OsmandApplication app;
	private final OsmandRegions osmandRegions;
	private final WeakReference<FragmentActivity> activityRef;

	private final String url;
	private final List<LatLon> articlePossibleLocations;

	private String articleName;
	private String articleLanguage;

	private final List<String> regionsToDownload = new ArrayList<>();

	private ProgressDialog dialog;
	private final boolean nightMode;

	WikiArticleSearchTask(
			@NonNull List<LatLon> articlePossibleLocations, @NonNull String url,
			@NonNull FragmentActivity activity, boolean nightMode
	) {
		this.articlePossibleLocations = articlePossibleLocations;
		this.nightMode = nightMode;
		this.url = url;
		activityRef = new WeakReference<>(activity);
		app = (OsmandApplication) activity.getApplicationContext();
		osmandRegions = app.getRegions();
		dialog = createProgressDialog(activity, this.nightMode);
	}

	@Override
	protected void onPreExecute() {
		articleLanguage = WikiArticleHelper.getLanguageFromUrl(url);
		articleName = WikiArticleHelper.getArticleNameFromUrl(url, articleLanguage);
		if (dialog != null) {
			dialog.show();
		}
	}

	@Override
	protected List<Amenity> doInBackground(Void... voids) {
		if (isCancelled()) {
			return Collections.emptyList();
		}
		return searchArticleInLocations(articlePossibleLocations);
	}

	private List<Amenity> searchArticleInLocations(@NonNull List<LatLon> locations) {
		Map<LatLon, List<WorldRegion>> regionsByLatLon = collectUniqueRegions(locations);
		if (Algorithms.isEmpty(regionsByLatLon)) {
			return Collections.emptyList();
		}
		List<Amenity> results = null;
		for (LatLon location : regionsByLatLon.keySet()) {
			List<WorldRegion> regions = regionsByLatLon.get(location);
			if (regions != null) {
				results = searchArticleInRegions(location, regions);
				if (isCancelled()) {
					return null;
				}
				if (!Algorithms.isEmpty(results)) {
					break;
				}
			}
		}
		return results;
	}

	private List<Amenity> searchArticleInRegions(@NonNull LatLon location,
	                                             @NonNull List<WorldRegion> regions) {
		List<Amenity> results = new ArrayList<>();
		AmenityIndexRepositoryBinary repository = findWikiRepository(regions);
		if (repository == null) {
			IndexItem item = null;
			try {
				item = DownloadResources.findSmallestIndexItemAt(app, location, DownloadActivityType.WIKIPEDIA_FILE);
			} catch (IOException e) {
				LOG.error(e.getMessage(), e);
			}
			if (item != null) {
				regionsToDownload.add(parseRegionName(item.getFileName()));
			}
		} else if (!isCancelled()) {
			repository.searchAmenitiesByName(0, 0, 0, 0,
					Integer.MAX_VALUE, Integer.MAX_VALUE, articleName, createResultMatcher(results));
		}
		return results;
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
			if (!Algorithms.isEmpty(found)) {
				WikipediaDialogFragment.showInstance(activity, found.get(0), articleLanguage);
			} else if (Algorithms.isEmpty(regionsToDownload)) {
				WikiArticleHelper.warnAboutExternalLoad(url, activityRef.get(), nightMode);
			} else {
				FragmentManager fragmentManager = activity.getSupportFragmentManager();
				String regionName = Algorithms.isEmpty(regionsToDownload) ? "" : regionsToDownload.get(0);
				WikivoyageArticleWikiLinkFragment.showInstance(fragmentManager, regionName, url);
			}
		}
	}

	@NonNull
	private Map<LatLon, List<WorldRegion>> collectUniqueRegions(@NonNull List<LatLon> locations) {
		Map<LatLon, List<WorldRegion>> regionsByLocation = new HashMap<>();
		for (LatLon location : locations) {
			if (!isUniqueLocation(location, regionsByLocation)) {
				continue;
			}
			try {
				List<WorldRegion> regionsAtLocation = osmandRegions.getWorldRegionsAt(location);
				List<WorldRegion> uniqueRegions = new ArrayList<>();
				for (WorldRegion region : regionsAtLocation) {
					if (!isRegionAdded(region, regionsByLocation)) {
						uniqueRegions.add(region);
					}
				}
				regionsByLocation.put(location, uniqueRegions);
			} catch (IOException e) {
				LOG.error(e.getMessage(), e);
			}
		}
		return regionsByLocation;
	}

	private boolean isUniqueLocation(
			@NonNull LatLon location,
			@NonNull Map<LatLon, List<WorldRegion>> regionsByLatLon
	) {
		for (List<WorldRegion> regions : regionsByLatLon.values()) {
			boolean containsInAll = true;
			for (WorldRegion region : regions) {
				if (!region.containsPoint(location)) {
					containsInAll = false;
					break;
				}
			}
			if (containsInAll) {
				return false;
			}
		}
		return true;
	}

	private boolean isRegionAdded(
			@NonNull WorldRegion region,
			@NonNull Map<LatLon, List<WorldRegion>> regionsByLatLon
	) {
		for (List<WorldRegion> regionsInMap : regionsByLatLon.values()) {
			if (regionsInMap.contains(region)) {
				return true;
			}
		}
		return false;
	}

	@Nullable
	private AmenityIndexRepositoryBinary findWikiRepository(
			@NonNull List<WorldRegion> regions
	) {
		ResourceManager resourceManager = app.getResourceManager();
		AmenityIndexRepositoryBinary repository = null;
		for (WorldRegion region : regions) {
			if (region == null) {
				continue;
			}
			String fileName = region.getRegionDownloadName();
			fileName = Algorithms.capitalizeFirstLetterAndLowercase(fileName);
			fileName = fileName + IndexConstants.BINARY_WIKI_MAP_INDEX_EXT;

			repository = resourceManager.getAmenityRepositoryByFileName(fileName);
			if (repository != null) {
				break;
			}
		}
		return repository;
	}

	private ResultMatcher<Amenity> createResultMatcher(@NonNull List<Amenity> results) {
		return new ResultMatcher<Amenity>() {
			@Override
			public boolean publish(Amenity amenity) {
				String localeName = amenity.getName();
				List<String> otherNames = amenity.getOtherNames(false);
				if (articleName.equals(localeName)) {
					results.add(amenity);
				} else {
					for (String amenityName : otherNames) {
						if (articleName.equals(amenityName)) {
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
	}

	private String parseRegionName(@Nullable String filename) {
		if (osmandRegions != null && filename != null) {
			String regionName = filename
					.replace("_" + IndexConstants.BINARY_MAP_VERSION, "")
					.replace(IndexConstants.BINARY_WIKI_MAP_INDEX_EXT_ZIP, "")
					.toLowerCase();
			return osmandRegions.getLocaleName(regionName, false);
		}
		return "";
	}

	private static ProgressDialog createProgressDialog(@NonNull FragmentActivity activity, boolean nightMode) {
		Context context = UiUtilities.getThemedContext(activity, nightMode);
		ProgressDialog dialog = new ProgressDialog(context);
		dialog.setCancelable(false);
		String message = context.getString(R.string.wiki_article_search_text);
		dialog.setMessage(message);
		return dialog;
	}
}
