package net.osmand.plus.wikivoyage.explore;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
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
import java.util.ArrayList;
import java.util.List;

public class ExploreTabFragment extends BaseOsmAndFragment implements DownloadIndexesThread.DownloadEvents {

	private static final String WORLD_WIKIVOYAGE_FILE_NAME = "World_wikivoyage.sqlite";

	private ExploreRvAdapter adapter = new ExploreRvAdapter();

	private TravelDownloadUpdateCard downloadUpdateCard;

	private boolean nightMode;

	private IndexItem indexItem;

	private boolean downloadIndexesRequested;

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		nightMode = !getMyApplication().getSettings().isLightContent();

		final View mainView = inflater.inflate(R.layout.fragment_explore_tab, container, false);
		final RecyclerView rv = (RecyclerView) mainView.findViewById(R.id.recycler_view);

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
			if (downloadUpdateCard == null) {
				addDownloadUpdateCard(false);
			}
		}
	}

	@Override
	public void downloadInProgress() {
		DownloadIndexesThread downloadThread = getMyApplication().getDownloadThread();
		IndexItem current = downloadThread.getCurrentDownloadingItem();
		indexItem = downloadThread.getIndexes().getWikivoyageItem(getWikivoyageFileName());
		if (current != null && downloadUpdateCard != null
				&& indexItem != null
				&& current == indexItem
				&& (!current.isDownloaded() || current.isOutdated())) {
			downloadUpdateCard.setProgress(downloadThread.getCurrentDownloadingItemProgress());
			adapter.updateDownloadUpdateCard();
		}
	}

	@Override
	public void downloadHasFinished() {
		final OsmandApplication app = getMyApplication();
		File targetFile = indexItem.getTargetFile(app);
		if (downloadUpdateCard != null && indexItem != null && targetFile.exists()) {
			downloadUpdateCard.setLoadingInProgress(false);
			removeDownloadUpdateCard();
			TravelDbHelper travelDbHelper = app.getTravelDbHelper();
			travelDbHelper.initTravelBooks();
			travelDbHelper.selectTravelBook(targetFile);
			Fragment parent = getParentFragment();
			if (parent != null && parent instanceof WikivoyageExploreDialogFragment) {
				((WikivoyageExploreDialogFragment) parent).populateData();
			}
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

		addOpenBetaTravelCard(items, nightMode);
		if (app.getTravelDbHelper().getSelectedTravelBook() != null) {
			items.add(new HeaderTravelCard(app, nightMode, getString(R.string.popular_destinations)));
			List<TravelArticle> popularArticles = app.getTravelDbHelper().getPopularArticles();
			FragmentActivity activity = getActivity();
			if (activity != null) {
				for (TravelArticle article : popularArticles) {
					items.add(new ArticleTravelCard(getMyApplication(), nightMode, article,
							activity.getSupportFragmentManager()));
				}
			}
		}
		items.add(new StartEditingTravelCard(app, nightMode));
		adapter.setItems(items);

		checkToAddDownloadTravelCard();
	}

	private void checkToAddDownloadTravelCard() {
		final OsmandApplication app = getMyApplication();
		final DownloadIndexesThread downloadThread = app.getDownloadThread();

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

	private void addDownloadUpdateCard(boolean loadingInProgress) {
		final OsmandApplication app = getMyApplication();

		boolean outdated = indexItem != null && indexItem.isOutdated();
		File selectedTravelBook = app.getTravelDbHelper().getSelectedTravelBook();
		if (selectedTravelBook == null || outdated) {
			boolean showOtherMaps = false;
			if (selectedTravelBook == null) {
				List<IndexItem> items = app.getDownloadThread().getIndexes().getWikivoyageItems();
				showOtherMaps = items != null && items.size() > 1;
			}
			downloadUpdateCard = new TravelDownloadUpdateCard(app, nightMode, !outdated);
			downloadUpdateCard.setShowOtherMapsBtn(showOtherMaps);
			downloadUpdateCard.setLoadingInProgress(loadingInProgress);
			downloadUpdateCard.setListener(new TravelDownloadUpdateCard.ClickListener() {
				@Override
				public void onPrimaryButtonClick() {
					if (indexItem != null) {
						new DownloadValidationManager(app).startDownload(getMyActivity(), indexItem);
						downloadUpdateCard.setLoadingInProgress(true);
						adapter.updateDownloadUpdateCard();
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
			downloadUpdateCard.setIndexItem(indexItem);
			adapter.setDownloadUpdateCard(downloadUpdateCard);
		}
	}

	@NonNull
	private String getWikivoyageFileName() {
		File selectedTravelBook = getMyApplication().getTravelDbHelper().getSelectedTravelBook();
		return selectedTravelBook == null ? WORLD_WIKIVOYAGE_FILE_NAME : selectedTravelBook.getName();
	}

	private void removeDownloadUpdateCard() {
		adapter.removeDownloadUpdateCard();
		downloadUpdateCard = null;
	}

	private void addOpenBetaTravelCard(List<BaseTravelCard> items, final boolean nightMode) {
		final OsmandApplication app = getMyApplication();
		if (!Version.isPaidVersion(app)) {
			items.add(new OpenBetaTravelCard(app, nightMode, getFragmentManager()));
		}
	}
}
