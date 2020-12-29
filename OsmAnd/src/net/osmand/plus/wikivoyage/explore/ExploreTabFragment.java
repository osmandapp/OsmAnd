package net.osmand.plus.wikivoyage.explore;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.data.LatLon;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.chooseplan.ChoosePlanDialogFragment;
import net.osmand.plus.download.DownloadActivityType;
import net.osmand.plus.download.DownloadIndexesThread;
import net.osmand.plus.download.DownloadIndexesThread.DownloadEvents;
import net.osmand.plus.download.DownloadResources;
import net.osmand.plus.download.DownloadValidationManager;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.wikivoyage.data.TravelArticle;
import net.osmand.plus.wikivoyage.data.TravelHelper;
import net.osmand.plus.wikivoyage.data.TravelLocalDataHelper;
import net.osmand.plus.wikivoyage.explore.travelcards.ArticleTravelCard;
import net.osmand.plus.wikivoyage.explore.travelcards.BaseTravelCard;
import net.osmand.plus.wikivoyage.explore.travelcards.HeaderTravelCard;
import net.osmand.plus.wikivoyage.explore.travelcards.OpenBetaTravelCard;
import net.osmand.plus.wikivoyage.explore.travelcards.StartEditingTravelCard;
import net.osmand.plus.wikivoyage.explore.travelcards.TravelDownloadUpdateCard;
import net.osmand.plus.wikivoyage.explore.travelcards.TravelNeededMapsCard;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class ExploreTabFragment extends BaseOsmAndFragment implements DownloadEvents, TravelLocalDataHelper.Listener {

	private static boolean SHOW_TRAVEL_UPDATE_CARD = true;
	private static boolean SHOW_TRAVEL_NEEDED_MAPS_CARD = true;

	@Nullable
	private ExploreRvAdapter adapter = new ExploreRvAdapter();
	private boolean nightMode;

	@Nullable
	private TravelDownloadUpdateCard downloadUpdateCard;
	@Nullable
	private TravelNeededMapsCard neededMapsCard;

	@Nullable
	private DownloadValidationManager downloadManager;
	@Nullable
	private IndexItem currentDownloadingIndexItem;
	@Nullable
	private IndexItem mainIndexItem;

	private List<IndexItem> neededIndexItems = new ArrayList<>();
	private boolean waitForIndexes;

	@SuppressWarnings("RedundantCast")
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
		WikivoyageExploreActivity exploreActivity = getExploreActivity();
		if (exploreActivity != null) {
			exploreActivity.onTabFragmentResume(this);
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
	public void newDownloadIndexes() {
		if (waitForIndexes) {
			waitForIndexes = false;
			checkDownloadIndexes();
		}
	}

	@Override
	public void downloadInProgress() {
		OsmandApplication app = getMyApplication();
		if (app != null && adapter != null) {
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
					exploreActivity.populateData();
				}
			} else {
				removeRedundantCards();
			}
		}
	}

	@Override
	public void savedArticlesUpdated() {
		if (adapter != null && isAdded()) {
			adapter.notifyDataSetChanged();
		}
	}

	@Nullable
	private WikivoyageExploreActivity getExploreActivity() {
		Activity activity = getActivity();
		if (activity != null && activity instanceof WikivoyageExploreActivity) {
			return (WikivoyageExploreActivity) activity;
		} else {
			return null;
		}
	}

	public void invalidateAdapter() {
		if (adapter != null) {
			adapter.notifyDataSetChanged();
		}
	}

	public void populateData() {
		final List<BaseTravelCard> items = new ArrayList<>();
		final OsmandApplication app = getMyApplication();
		if (app != null) {
			if (adapter != null) {
				FragmentManager fm = getFragmentManager();
				if (fm != null) {
					if (!Version.isPaidVersion(app)) {
						items.add(new OpenBetaTravelCard(app, nightMode, fm));
					}
					if (app.getTravelHelper().isAnyTravelBookPresent()) {
						items.add(new HeaderTravelCard(app, nightMode, getString(R.string.popular_destinations)));

						List<TravelArticle> popularArticles = app.getTravelHelper().getPopularArticles();
						for (TravelArticle article : popularArticles) {
							items.add(new ArticleTravelCard(app, nightMode, article, fm));
						}
					}
				}
				items.add(new StartEditingTravelCard(app, getMyActivity(), nightMode));
				adapter.setItems(items);
			}
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
		if (mainIndexItem != null && mainIndexItem.isDownloaded() && !mainIndexItem.isOutdated()) {
			removeDownloadUpdateCard();
		}
		boolean allMapsDownloaded = true;
		for (IndexItem item : neededIndexItems) {
			if (!item.isDownloaded()) {
				allMapsDownloaded = false;
				break;
			}
		}
		if (allMapsDownloaded) {
			removeNeededMapsCard();
		}
	}

	private void checkDownloadIndexes() {
		new ProcessIndexItemsTask(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	private void addIndexItemCards(IndexItem mainIndexItem, List<IndexItem> neededIndexItems) {
		this.mainIndexItem = mainIndexItem;
		this.neededIndexItems.clear();
		this.neededIndexItems.addAll(neededIndexItems);
		addDownloadUpdateCard();
		addNeededMapsCard();
	}

	private void addDownloadUpdateCard() {
		final OsmandApplication app = getMyApplication();
		if (app != null && adapter != null) {
			final DownloadIndexesThread downloadThread = app.getDownloadThread();

			boolean outdated = mainIndexItem != null && mainIndexItem.isOutdated();
			boolean needsDownloading = mainIndexItem != null && !mainIndexItem.isDownloaded();

			if (!app.getTravelHelper().isAnyTravelBookPresent() || needsDownloading || (outdated && SHOW_TRAVEL_UPDATE_CARD)) {
				boolean showOtherMaps = false;
				if (needsDownloading) {
					List<IndexItem> items = downloadThread.getIndexes().getWikivoyageItems();
					showOtherMaps = items != null && items.size() > 1;
				}

				downloadUpdateCard = new TravelDownloadUpdateCard(app, nightMode, !outdated);
				downloadUpdateCard.setShowOtherMapsBtn(showOtherMaps);
				downloadUpdateCard.setListener(new TravelDownloadUpdateCard.ClickListener() {
					@Override
					public void onPrimaryButtonClick() {
						if (mainIndexItem != null && downloadManager != null && adapter != null) {
							downloadManager.startDownload(getMyActivity(), mainIndexItem);
							adapter.updateDownloadUpdateCard(false);
						}
					}

					@Override
					public void onSecondaryButtonClick() {
						if (downloadUpdateCard.isLoading() && adapter != null) {
							downloadThread.cancelDownload(mainIndexItem);
							adapter.updateDownloadUpdateCard(false);
						} else if (!downloadUpdateCard.isDownload()) {
							SHOW_TRAVEL_UPDATE_CARD = false;
							removeDownloadUpdateCard();
						} else if (downloadUpdateCard.isShowOtherMapsBtn()) {
							Activity activity = getActivity();
							if (activity != null) {
								Intent newIntent = new Intent(activity,
										getMyApplication().getAppCustomization().getDownloadActivity());
								newIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
								activity.startActivity(newIntent);
							}
						}
					}
				});
				downloadUpdateCard.setIndexItem(mainIndexItem);
				adapter.addDownloadUpdateCard(downloadUpdateCard);
			}
		}
	}

	private void addNeededMapsCard() {
		final OsmandApplication app = getMyApplication();
		if (app != null && !neededIndexItems.isEmpty() && adapter != null && SHOW_TRAVEL_NEEDED_MAPS_CARD) {
			neededMapsCard = new TravelNeededMapsCard(app, nightMode, neededIndexItems);
			neededMapsCard.setListener(new TravelNeededMapsCard.CardListener() {
				@Override
				public void onPrimaryButtonClick() {
					if (adapter != null && downloadManager != null) {
						downloadManager.startDownload(getMyActivity(), getAllItemsForDownload());
						adapter.updateNeededMapsCard(false);
					}
				}

				@Override
				public void onSecondaryButtonClick() {
					if (neededMapsCard.isDownloading()) {
						if (adapter != null) {
							app.getDownloadThread().cancelDownload(neededIndexItems);
							adapter.updateNeededMapsCard(false);
						}
					} else {
						SHOW_TRAVEL_NEEDED_MAPS_CARD = false;
						removeNeededMapsCard();
					}
				}

				@Override
				public void onIndexItemClick(IndexItem item) {
					if (item.getType() == DownloadActivityType.WIKIPEDIA_FILE && !Version.isPaidVersion(app)) {
						FragmentManager fm = getFragmentManager();
						if (fm != null) {
							ChoosePlanDialogFragment.showWikipediaInstance(fm);
						}
					} else {
						DownloadIndexesThread downloadThread = app.getDownloadThread();
						if (downloadThread.isDownloading(item)) {
							downloadThread.cancelDownload(item);
						} else if (!item.isDownloaded() && downloadManager != null) {
							downloadManager.startDownload(getMyActivity(), item);
						}
						if (adapter != null) {
							adapter.updateNeededMapsCard(false);
						}
					}
				}
			});
			adapter.addNeededMapsCard(neededMapsCard);
		}
	}

	private IndexItem[] getAllItemsForDownload() {
		boolean paidVersion = Version.isPaidVersion(getMyApplication());
		ArrayList<IndexItem> res = new ArrayList<>();
		for (IndexItem item : neededIndexItems) {
			if (!item.isDownloaded() && (paidVersion || item.getType() != DownloadActivityType.WIKIPEDIA_FILE)) {
				res.add(item);
			}
		}
		return res.toArray(new IndexItem[0]);
	}

	private void removeDownloadUpdateCard() {
		if (adapter != null) {
			adapter.removeDownloadUpdateCard();
		}
		downloadUpdateCard = null;
	}

	private void removeNeededMapsCard() {
		if (adapter != null) {
			adapter.removeNeededMapsCard();
		}
		neededMapsCard = null;
	}

	private static class ProcessIndexItemsTask extends AsyncTask<Void, Void, Pair<IndexItem, List<IndexItem>>> {

		private static DownloadActivityType[] types = new DownloadActivityType[]{
				DownloadActivityType.NORMAL_FILE,
				DownloadActivityType.WIKIPEDIA_FILE
		};

		private OsmandApplication app;
		private WeakReference<ExploreTabFragment> weakFragment;

		private String fileName;

		ProcessIndexItemsTask(ExploreTabFragment fragment) {
			app = fragment.getMyApplication();
			weakFragment = new WeakReference<>(fragment);
			fileName = app.getTravelHelper().getWikivoyageFileName();
		}

		@Override
		protected Pair<IndexItem, List<IndexItem>> doInBackground(Void... voids) {
			IndexItem mainItem = app.getDownloadThread().getIndexes().getWikivoyageItem(fileName);

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
					e.printStackTrace();
				}
			}

			return new Pair<>(mainItem, neededItems);
		}

		@Override
		protected void onPostExecute(Pair<IndexItem, List<IndexItem>> res) {
			ExploreTabFragment fragment = weakFragment.get();
			if (fragment != null && fragment.isResumed()) {
				fragment.addIndexItemCards(res.first, res.second);
			}
		}
	}
}
