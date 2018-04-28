package net.osmand.plus.wikivoyage.explore;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.activities.OsmandActionBarActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.download.DownloadIndexesThread;
import net.osmand.plus.download.DownloadValidationManager;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.wikivoyage.data.TravelArticle;
import net.osmand.plus.wikivoyage.data.TravelDbHelper;
import net.osmand.plus.wikivoyage.explore.travelcards.ArticleTravelCard;
import net.osmand.plus.wikivoyage.explore.travelcards.BaseTravelCard;
import net.osmand.plus.wikivoyage.explore.travelcards.HeaderTravelCard;
import net.osmand.plus.wikivoyage.explore.travelcards.OpenBetaTravelCard;
import net.osmand.plus.wikivoyage.explore.travelcards.StartEditingTravelCard;
import net.osmand.plus.wikivoyage.explore.travelcards.TravelDownloadUpdateCard;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class ExploreTabFragment extends BaseOsmAndFragment implements DownloadIndexesThread.DownloadEvents {

	private static final String WORLD_WIKIVOYAGE_FILE_NAME = "World_wikivoyage.sqlite";

	private ExploreRvAdapter adapter = new ExploreRvAdapter();

	private StartEditingTravelCard startEditingTravelCard;
	private TravelDownloadUpdateCard downloadUpdateCard;

	private boolean nightMode;

	private IndexItem indexItem;

	private File selectedTravelBook;
	private boolean downloadIndexesRequested;
	private boolean downloadUpdateCardAdded;

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		nightMode = !getMyApplication().getSettings().isLightContent();

		final View mainView = inflater.inflate(R.layout.fragment_explore_tab, container, false);
		final RecyclerView rv = (RecyclerView) mainView.findViewById(R.id.recycler_view);

		adapter.setItems(generateItems(), false);

		rv.setLayoutManager(new LinearLayoutManager(getContext()));
		rv.setAdapter(adapter);

		return mainView;
	}

	@Override
	public void newDownloadIndexes() {
		if (downloadIndexesRequested) {
			downloadIndexesRequested = false;
			indexItem = getMyApplication().getDownloadThread().getIndexes()
					.getWikivoyageItem(getWikivoyageFileName());
			addDownloadUpdateCard(false);
		}
	}

	@Override
	public void downloadInProgress() {
		DownloadIndexesThread downloadThread = getMyApplication().getDownloadThread();
		IndexItem current = downloadThread.getCurrentDownloadingItem();
		indexItem = downloadThread.getIndexes().getWikivoyageItem(getWikivoyageFileName());
		if (current != null
				&& indexItem != null
				&& current == indexItem
				&& (!current.isDownloaded() || current.isOutdated())) {
			addDownloadUpdateCard(true);
			downloadUpdateCard.setProgress(downloadThread.getCurrentDownloadingItemProgress());
			adapter.updateDownloadUpdateCard();
		}
	}

	@Override
	public void downloadHasFinished() {
		IndexItem current = getMyApplication().getDownloadThread().getCurrentDownloadingItem();
		if (downloadUpdateCard != null && current != null && indexItem != null && current == indexItem) {
			downloadUpdateCard.setLoadingInProgress(false);
			removeDownloadUpdateCard();
		}
	}

	private void addDownloadUpdateCard(boolean loadingInProgress) {
		if (downloadUpdateCardAdded) {
			return;
		}

		final OsmandApplication app = getMyApplication();

		boolean outdated = indexItem != null && indexItem.isOutdated();

		if (selectedTravelBook == null || outdated) {
			downloadUpdateCard = new TravelDownloadUpdateCard(app, nightMode, !outdated);
			downloadUpdateCard.setLoadingInProgress(loadingInProgress);
			downloadUpdateCard.setListener(new TravelDownloadUpdateCard.ClickListener() {
				@Override
				public void onPrimaryButtonClick() {
					if (app.getSettings().isInternetConnectionAvailable()) {
						new DownloadValidationManager(app).startDownload(getMyActivity(), indexItem);
						downloadUpdateCard.setLoadingInProgress(true);
						adapter.updateDownloadUpdateCard();
					} else {
						Toast.makeText(app, app.getString(R.string.no_index_file_to_download), Toast.LENGTH_SHORT).show();
					}
				}

				@Override
				public void onSecondaryButtonClick() {
					if (downloadUpdateCard.isLoadingInProgress()) {
						app.getDownloadThread().cancelDownload(indexItem);
						downloadUpdateCard.setLoadingInProgress(false);
						adapter.updateDownloadUpdateCard();
					} else if (!downloadUpdateCard.isDownload()) {
						removeDownloadUpdateCard();
					}
				}
			});
			downloadUpdateCard.setIndexItem(indexItem);
			adapter.setDownloadUpdateCard(downloadUpdateCard);
			downloadUpdateCardAdded = true;
		}
	}

	@NonNull
	private String getWikivoyageFileName() {
		return selectedTravelBook == null ? WORLD_WIKIVOYAGE_FILE_NAME : selectedTravelBook.getName();
	}

	private void removeDownloadUpdateCard() {
		adapter.removeDownloadUpdateCard();
		downloadUpdateCardAdded = false;
	}

	private List<BaseTravelCard> generateItems() {
		final List<BaseTravelCard> items = new ArrayList<>();
		final OsmandApplication app = getMyApplication();

		checkSelectedTravelBook();
		startEditingTravelCard = new StartEditingTravelCard(app, nightMode);
		addOpenBetaTravelCard(items, nightMode);
		items.add(startEditingTravelCard);

		if (app.getTravelDbHelper().getSelectedTravelBook() != null) {
			items.add(new HeaderTravelCard(app, nightMode, getString(R.string.popular_destinations)));
			addPopularDestinations(app);
		}

		return items;
	}

	private void checkSelectedTravelBook() {
		final OsmandApplication app = getMyApplication();
		final DownloadIndexesThread downloadThread = app.getDownloadThread();
		selectedTravelBook = app.getTravelDbHelper().getSelectedTravelBook();

		if (!downloadThread.getIndexes().isDownloadedFromInternet) {
			downloadIndexesRequested = true;
			downloadThread.runReloadIndexFilesSilent();
		} else {
			indexItem = downloadThread.getIndexes().getWikivoyageItem(getWikivoyageFileName());
			IndexItem current = downloadThread.getCurrentDownloadingItem();
			boolean loadingInProgress = current != null && indexItem != null && current == indexItem;
			addDownloadUpdateCard(loadingInProgress);
		}
	}

	private void addPopularDestinations(OsmandApplication app) {
		PopularDestinationsSearchTask popularDestinationsSearchTask = new PopularDestinationsSearchTask(
				app.getTravelDbHelper(), getMyActivity(), adapter, nightMode, startEditingTravelCard);
		popularDestinationsSearchTask.execute();
	}

	private void addOpenBetaTravelCard(List<BaseTravelCard> items, final boolean nightMode) {
		final OsmandApplication app = getMyApplication();
		if (!Version.isPaidVersion(app)) {
			items.add(new OpenBetaTravelCard(app, nightMode, getFragmentManager()));
		}
	}

	private static class PopularDestinationsSearchTask extends AsyncTask<Void, TravelDbHelper, List<TravelArticle>> {

		private TravelDbHelper travelDbHelper;
		private WeakReference<OsmandActionBarActivity> weakContext;
		private WeakReference<ExploreRvAdapter> weakAdapter;
		private WeakReference<StartEditingTravelCard> weakStartEditingTravelCard;
		private boolean nightMode;

		PopularDestinationsSearchTask(TravelDbHelper travelDbHelper,
									  OsmandActionBarActivity context, ExploreRvAdapter adapter, boolean nightMode, StartEditingTravelCard startEditingTravelCard) {
			this.travelDbHelper = travelDbHelper;
			weakContext = new WeakReference<>(context);
			weakAdapter = new WeakReference<>(adapter);
			weakStartEditingTravelCard = new WeakReference<>(startEditingTravelCard);
			this.nightMode = nightMode;
		}

		@Override
		protected List<TravelArticle> doInBackground(Void... voids) {
			return travelDbHelper.searchPopular();
		}

		@Override
		protected void onPostExecute(List<TravelArticle> items) {
			OsmandActionBarActivity activity = weakContext.get();
			ExploreRvAdapter adapter = weakAdapter.get();
			StartEditingTravelCard startEditingTravelCard = weakStartEditingTravelCard.get();

			if (activity != null && adapter != null && startEditingTravelCard != null) {
				List<BaseTravelCard> adapterItems = adapter.getItems();

				if (adapterItems.contains(startEditingTravelCard)) {
					adapterItems.remove(startEditingTravelCard);
				}
				for (TravelArticle article : items) {
					adapterItems.add(new ArticleTravelCard(activity.getMyApplication(), nightMode, article, activity.getSupportFragmentManager()));
				}

				adapterItems.add(startEditingTravelCard);
				adapter.notifyDataSetChanged();
			}
		}
	}
}