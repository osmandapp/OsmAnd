package net.osmand.plus.wikivoyage.explore;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.data.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.chooseplan.OsmAndFeature;
import net.osmand.plus.chooseplan.ChoosePlanFragment;
import net.osmand.plus.download.DownloadActivityType;
import net.osmand.plus.download.DownloadIndexesThread;
import net.osmand.plus.download.DownloadIndexesThread.DownloadEvents;
import net.osmand.plus.download.DownloadResources;
import net.osmand.plus.download.DownloadValidationManager;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.wikivoyage.data.TravelArticle;
import net.osmand.plus.wikivoyage.data.TravelGpx;
import net.osmand.plus.wikivoyage.data.TravelHelper;
import net.osmand.plus.wikivoyage.data.TravelLocalDataHelper;
import net.osmand.plus.wikivoyage.explore.travelcards.ArticleTravelCard;
import net.osmand.plus.wikivoyage.explore.travelcards.BaseTravelCard;
import net.osmand.plus.wikivoyage.explore.travelcards.HeaderTravelCard;
import net.osmand.plus.wikivoyage.explore.travelcards.OpenBetaTravelCard;
import net.osmand.plus.wikivoyage.explore.travelcards.StartEditingTravelCard;
import net.osmand.plus.wikivoyage.explore.travelcards.TravelButtonCard;
import net.osmand.plus.wikivoyage.explore.travelcards.TravelDownloadUpdateCard;
import net.osmand.plus.wikivoyage.explore.travelcards.TravelGpxCard;
import net.osmand.plus.wikivoyage.explore.travelcards.TravelNeededMapsCard;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import static net.osmand.plus.download.DownloadResources.WIKIVOYAGE_FILE_FILTER;
import static net.osmand.plus.resources.ResourceManager.DEFAULT_WIKIVOYAGE_TRAVEL_OBF;

public class ExploreTabFragment extends BaseOsmAndFragment implements DownloadEvents, TravelLocalDataHelper.Listener {

	private static boolean SHOW_TRAVEL_UPDATE_CARD = true;
	private static boolean SHOW_TRAVEL_NEEDED_MAPS_CARD = true;

	private final ExploreRvAdapter adapter = new ExploreRvAdapter();
	private boolean nightMode;

	@Nullable
	private TravelDownloadUpdateCard downloadUpdateCard;
	@Nullable
	private TravelNeededMapsCard neededMapsCard;

	@Nullable
	private DownloadValidationManager downloadManager;
	@Nullable
	private IndexItem currentDownloadingIndexItem;
	private final List<IndexItem> mainIndexItems = new ArrayList<>();
	private final List<IndexItem> neededIndexItems = new ArrayList<>();
	private boolean waitForIndexes;

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		OsmandApplication app = requireMyApplication();
		downloadManager = new DownloadValidationManager(app);
		nightMode = !app.getSettings().isLightContent();

		final View mainView = inflater.inflate(R.layout.fragment_explore_tab, container, false);
		final RecyclerView rv = (RecyclerView) mainView.findViewById(R.id.recycler_view);

		rv.setLayoutManager(new LinearLayoutManager(getContext()));
		rv.setAdapter(adapter);

		return mainView;
	}

	@Override
	public void onResume() {
		super.onResume();
		OsmandApplication app = getMyApplication();
		if (app != null) {
			app.getTravelHelper().getBookmarksHelper().addListener(this);
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		OsmandApplication app = getMyApplication();
		if (app != null) {
			app.getTravelHelper().getBookmarksHelper().removeListener(this);
		}
	}

	@Override
	public void onUpdatedIndexesList() {
		if (waitForIndexes) {
			waitForIndexes = false;
			checkDownloadIndexes();
		}
	}

	@Override
	public void downloadInProgress() {
		OsmandApplication app = getMyApplication();
		if (app != null) {
			IndexItem current = app.getDownloadThread().getCurrentDownloadingItem();
			if (current != null && current != currentDownloadingIndexItem) {
				currentDownloadingIndexItem = current;
				removeRedundantCards();
			}
			adapter.updateDownloadUpdateCard(true);
			adapter.updateNeededMapsCard(true);
		}
	}

	@Override
	public void downloadHasFinished() {
		OsmandApplication app = getMyApplication();
		if (app != null) {
			TravelHelper travelHelper = app.getTravelHelper();
			if (travelHelper.isAnyTravelBookPresent()) {
				app.getTravelHelper().initializeDataOnAppStartup();
				WikivoyageExploreActivity exploreActivity = getExploreActivity();
				if (exploreActivity != null) {
					exploreActivity.populateData(true);
				}
			} else {
				removeRedundantCards();
			}
		}
	}

	@Override
	public void savedArticlesUpdated() {
		OsmandApplication app = getMyApplication();
		if (app != null) {
			DownloadIndexesThread downloadThread = app.getDownloadThread();
			if (!downloadThread.getIndexes().isDownloadedFromInternet) {
				waitForIndexes = true;
				downloadThread.runReloadIndexFilesSilent();
			} else {
				checkDownloadIndexes();
			}
		}
	}

	@Nullable
	private WikivoyageExploreActivity getExploreActivity() {
		Activity activity = getActivity();
		if (activity instanceof WikivoyageExploreActivity) {
			return (WikivoyageExploreActivity) activity;
		} else {
			return null;
		}
	}

	public void invalidateAdapter() {
		adapter.notifyDataSetChanged();
	}

	public void populateData() {
		final List<BaseTravelCard> items = new ArrayList<>();
		final FragmentActivity activity = getActivity();
		final OsmandApplication app = activity != null ? (OsmandApplication) activity.getApplication() : null;
		if (app != null) {
			if (!Version.isPaidVersion(app) && !OpenBetaTravelCard.isClosed()) {
				items.add(new OpenBetaTravelCard(activity, nightMode));
			}
			final List<TravelArticle> popularArticles = app.getTravelHelper().getPopularArticles();
			if (!popularArticles.isEmpty()) {
				items.add(new HeaderTravelCard(app, nightMode, getString(R.string.popular_destinations)));
				for (TravelArticle article : popularArticles) {
					if (article instanceof TravelGpx) {
						items.add(new TravelGpxCard(app, nightMode, (TravelGpx) article, activity));
					} else {
						items.add(new ArticleTravelCard(app, nightMode, article, activity.getSupportFragmentManager()));
					}
				}
			}
			if (!isOnlyDefaultTravelBookPresent()) {
				TravelButtonCard travelButtonCard = new TravelButtonCard(app, nightMode);
				travelButtonCard.setListener(new TravelNeededMapsCard.CardListener() {
					@Override
					public void onPrimaryButtonClick() {
						WikivoyageExploreActivity exploreActivity = getExploreActivity();
						if (exploreActivity != null) {
							exploreActivity.populateData(false);
						}
					}

					@Override
					public void onSecondaryButtonClick() {

					}

					@Override
					public void onIndexItemClick(IndexItem item) {

					}
				});
				items.add(travelButtonCard);
			}
			items.add(new StartEditingTravelCard(activity, nightMode));
			adapter.setItems(items);
			final DownloadIndexesThread downloadThread = app.getDownloadThread();
			if (!downloadThread.getIndexes().isDownloadedFromInternet) {
				waitForIndexes = true;
				downloadThread.runReloadIndexFilesSilent();
			} else {
				checkDownloadIndexes();
			}
		}
	}

	private void removeRedundantCards() {
		boolean allTravelGuideDownloaded = true;
		for (IndexItem item : mainIndexItems) {
			if (!item.isDownloaded()) {
				allTravelGuideDownloaded = false;
				break;
			}
		}
		if (allTravelGuideDownloaded) {
			removeDownloadUpdateCard();
		}
		boolean neededMapsDownloaded = true;
		for (IndexItem item : neededIndexItems) {
			if (!item.isDownloaded()) {
				neededMapsDownloaded = false;
				break;
			}
		}
		if (neededMapsDownloaded) {
			removeNeededMapsCard();
		}
	}

	private void checkDownloadIndexes() {
		new ProcessIndexItemsTask(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	private void addIndexItemCards(List<IndexItem> mainIndexItem, List<IndexItem> neededIndexItems) {
		if (isOnlyDefaultTravelBookPresent()) {
			this.mainIndexItems.clear();
			this.mainIndexItems.addAll(mainIndexItem);
			addDownloadUpdateCard();
		}
		this.neededIndexItems.clear();
		this.neededIndexItems.addAll(neededIndexItems);
		addNeededMapsCard();
	}

	private boolean isOnlyDefaultTravelBookPresent() {
		OsmandApplication app = getMyApplication();
		if (app != null && !app.isApplicationInitializing()) {
			return app.getResourceManager().isOnlyDefaultTravelBookPresent();

		}
		return true;
	}

	private void addDownloadUpdateCard() {
		final OsmandApplication app = getMyApplication();
		if (app != null && !mainIndexItems.isEmpty() && SHOW_TRAVEL_UPDATE_CARD) {
			boolean outdated = isMapsOutdated();
			downloadUpdateCard = new TravelDownloadUpdateCard(app, nightMode, mainIndexItems, !outdated);
			downloadUpdateCard.setListener(new TravelDownloadUpdateCard.CardListener() {
				@Override
				public void onPrimaryButtonClick() {
					if (downloadManager != null) {
						downloadManager.startDownload(getMyActivity(), getAllItemsForDownload(mainIndexItems));
						adapter.updateDownloadUpdateCard(false);
					}
				}

				@Override
				public void onSecondaryButtonClick() {
					if (downloadUpdateCard.isDownloading()) {
						app.getDownloadThread().cancelDownload(mainIndexItems);
						adapter.updateDownloadUpdateCard(false);
					} else {
						SHOW_TRAVEL_UPDATE_CARD = false;
						removeDownloadUpdateCard();
					}
				}

				@Override
				public void onIndexItemClick(IndexItem item) {
					if ((item.getType() == DownloadActivityType.WIKIPEDIA_FILE
							|| item.getType() == DownloadActivityType.TRAVEL_FILE) && !Version.isPaidVersion(app)) {
						FragmentActivity activity = getActivity();
						if (activity != null) {
							ChoosePlanFragment.showInstance(activity, OsmAndFeature.WIKIPEDIA);
						}
					} else {
						DownloadIndexesThread downloadThread = app.getDownloadThread();
						if (downloadThread.isDownloading(item)) {
							downloadThread.cancelDownload(item);
						} else if (!item.isDownloaded() && downloadManager != null) {
							downloadManager.startDownload(getMyActivity(), item);
						}
						adapter.updateDownloadUpdateCard(false);
					}
				}
			});
			adapter.addDownloadUpdateCard(downloadUpdateCard);
		}
	}

	private boolean isMapsOutdated() {
		for (IndexItem indexItem : mainIndexItems) {
			if (indexItem.isOutdated()) {
				return true;
			}
		}
		return false;
	}

	private void addNeededMapsCard() {
		final OsmandApplication app = getMyApplication();
		if (app != null && !neededIndexItems.isEmpty() && SHOW_TRAVEL_NEEDED_MAPS_CARD) {
			neededMapsCard = new TravelNeededMapsCard(app, nightMode, neededIndexItems);
			neededMapsCard.setListener(new TravelNeededMapsCard.CardListener() {
				@Override
				public void onPrimaryButtonClick() {
					if (downloadManager != null) {
						downloadManager.startDownload(getMyActivity(), getAllItemsForDownload(neededIndexItems));
						adapter.updateNeededMapsCard(false);
					}
				}

				@Override
				public void onSecondaryButtonClick() {
					if (neededMapsCard.isDownloading()) {
						app.getDownloadThread().cancelDownload(neededIndexItems);
						adapter.updateNeededMapsCard(false);
					} else {
						SHOW_TRAVEL_NEEDED_MAPS_CARD = false;
						removeNeededMapsCard();
					}
				}

				@Override
				public void onIndexItemClick(IndexItem item) {
					if ((item.getType() == DownloadActivityType.WIKIPEDIA_FILE
							|| item.getType() == DownloadActivityType.TRAVEL_FILE) && !Version.isPaidVersion(app)) {
						FragmentActivity activity = getActivity();
						if (activity != null) {
							ChoosePlanFragment.showInstance(activity, OsmAndFeature.WIKIPEDIA);
						}
					} else {
						DownloadIndexesThread downloadThread = app.getDownloadThread();
						if (downloadThread.isDownloading(item)) {
							downloadThread.cancelDownload(item);
						} else if (!item.isDownloaded() && downloadManager != null) {
							downloadManager.startDownload(getMyActivity(), item);
						}
						adapter.updateNeededMapsCard(false);
					}
				}
			});
			adapter.addNeededMapsCard(neededMapsCard);
		}
	}

	private IndexItem[] getAllItemsForDownload(List<IndexItem> indexItems) {
		boolean paidVersion = Version.isPaidVersion(getMyApplication());
		ArrayList<IndexItem> res = new ArrayList<>();
		for (IndexItem item : indexItems) {
			if (!item.isDownloaded() && (paidVersion || item.getType() != DownloadActivityType.WIKIPEDIA_FILE)) {
				res.add(item);
			}
		}
		return res.toArray(new IndexItem[0]);
	}

	private void removeDownloadUpdateCard() {
		adapter.removeDownloadUpdateCard();
		downloadUpdateCard = null;
	}

	private void removeNeededMapsCard() {
		adapter.removeNeededMapsCard();
		neededMapsCard = null;
	}

	private static class ProcessIndexItemsTask extends AsyncTask<Void, Void, Pair<List<IndexItem>, List<IndexItem>>> {

		private static final DownloadActivityType[] types = new DownloadActivityType[]{
				DownloadActivityType.NORMAL_FILE,
				DownloadActivityType.WIKIPEDIA_FILE
		};

		private final OsmandApplication app;
		private final WeakReference<ExploreTabFragment> weakFragment;

		ProcessIndexItemsTask(ExploreTabFragment fragment) {
			app = fragment.getMyApplication();
			weakFragment = new WeakReference<>(fragment);
		}

		@Override
		protected Pair<List<IndexItem>, List<IndexItem>> doInBackground(Void... voids) {
			List<IndexItem> mainItems = new ArrayList<>();
			List<IndexItem> allWikivoyageItems = app.getDownloadThread().getIndexes().getWikivoyageItems();
			if (allWikivoyageItems != null) {
				for (IndexItem item : allWikivoyageItems) {
					if (!item.isDownloaded()
							&& !mainItems.contains(item)
							&& item.getFileName().contains(WIKIVOYAGE_FILE_FILTER)) {
						mainItems.add(item);
					}
				}
			}
			List<IndexItem> neededItems = new ArrayList<>();
			for (TravelArticle article : app.getTravelHelper().getBookmarksHelper().getSavedArticles()) {
				LatLon latLon = new LatLon(article.getLat(), article.getLon());
				try {
					for (DownloadActivityType type : types) {
						IndexItem item = DownloadResources.findSmallestIndexItemAt(app, latLon, type);
						if (item != null && !item.isDownloaded() && !neededItems.contains(item)) {
							neededItems.add(item);
						}
					}
				} catch (IOException e) {
					// ignore
				}
			}
			return new Pair<>(mainItems, neededItems);
		}

		@Override
		protected void onPostExecute(Pair<List<IndexItem>, List<IndexItem>> res) {
			ExploreTabFragment fragment = weakFragment.get();
			if (res != null && fragment != null && fragment.isAdded()) {
				fragment.addIndexItemCards(res.first, res.second);
				fragment.removeRedundantCards();
				if (!fragment.isResumed()) {
					fragment.invalidateAdapter();
				}
			}
		}
	}
}
