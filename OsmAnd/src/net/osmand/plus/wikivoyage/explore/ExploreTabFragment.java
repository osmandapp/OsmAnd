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

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.LocalIndexHelper;
import net.osmand.plus.activities.LocalIndexInfo;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.download.DownloadResources;
import net.osmand.plus.download.ui.AbstractLoadLocalIndexTask;
import net.osmand.plus.wikivoyage.data.TravelArticle;
import net.osmand.plus.wikivoyage.explore.travelcards.ArticleTravelCard;
import net.osmand.plus.wikivoyage.explore.travelcards.OpenBetaTravelCard;
import net.osmand.plus.wikivoyage.explore.travelcards.StartEditingTravelCard;
import net.osmand.plus.wikivoyage.explore.travelcards.TravelDownloadUpdateCard;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class ExploreTabFragment extends BaseOsmAndFragment {

	private static final int DOWNLOAD_UPDATE_CARD_POSITION = 0;

	private ExploreRvAdapter adapter = new ExploreRvAdapter();

	private AddDownloadUpdateCardTask addDownloadUpdateCardTask;

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		final View mainView = inflater.inflate(R.layout.fragment_explore_tab, container, false);

		adapter.setItems(generateItems());

		final RecyclerView rv = (RecyclerView) mainView.findViewById(R.id.recycler_view);
		rv.setLayoutManager(new LinearLayoutManager(getContext()));
		rv.setAdapter(adapter);

		return mainView;
	}

	@Override
	public void onDestroyView() {
		cancelAddDownloadUpdateCardTask();
		super.onDestroyView();
	}

	private void cancelAddDownloadUpdateCardTask() {
		if (addDownloadUpdateCardTask != null) {
			addDownloadUpdateCardTask.cancel(true);
			addDownloadUpdateCardTask = null;
		}
	}

	private List<Object> generateItems() {
		final List<Object> items = new ArrayList<>();
		final OsmandApplication app = getMyApplication();
		final boolean nightMode = !getSettings().isLightContent();

		addDownloadUpdateCard(nightMode);
		items.add(new OpenBetaTravelCard(app, nightMode, getFragmentManager()));
		items.add(new StartEditingTravelCard(app, nightMode));
		addPopularDestinations(items, nightMode);

		return items;
	}

	private void addDownloadUpdateCard(boolean nightMode) {
		addDownloadUpdateCardTask = new AddDownloadUpdateCardTask(getMyApplication(), adapter, nightMode);
		addDownloadUpdateCardTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	private void addPopularDestinations(@NonNull List<Object> items, boolean nightMode) {
		OsmandApplication app = getMyApplication();
		List<TravelArticle> savedArticles = app.getTravelDbHelper().searchPopular();
		if (!savedArticles.isEmpty()) {
			items.add(getString(R.string.popular_destinations));
			for (TravelArticle article : savedArticles) {
				items.add(new ArticleTravelCard(app, nightMode, article, getFragmentManager()));
			}
		}
	}

	private static class AddDownloadUpdateCardTask extends AsyncTask<Void, Void, TravelDownloadUpdateCard> {

		private OsmandApplication app;
		private WeakReference<ExploreRvAdapter> adapterWr;

		private boolean nightMode;

		AddDownloadUpdateCardTask(OsmandApplication app, ExploreRvAdapter adapter, boolean nightMode) {
			this.app = app;
			this.adapterWr = new WeakReference<>(adapter);
			this.nightMode = nightMode;
		}

		@Override
		protected TravelDownloadUpdateCard doInBackground(Void... voids) {
			final boolean[] worldWikivoyageDownloaded = new boolean[1];

			new LocalIndexHelper(app).getLocalTravelFiles(new AbstractLoadLocalIndexTask() {
				@Override
				public void loadFile(LocalIndexInfo... loaded) {
					for (LocalIndexInfo lii : loaded) {
						if (lii.getBaseName().toLowerCase().equals(DownloadResources.WORLD_WIKIVOYAGE_NAME)) {
							worldWikivoyageDownloaded[0] = true;
						}
					}
				}
			});

			if (!worldWikivoyageDownloaded[0] && !isCancelled()) {
				TravelDownloadUpdateCard card = new TravelDownloadUpdateCard(app, nightMode, true);
				return card;
			}

			return null;
		}

		@Override
		protected void onPostExecute(TravelDownloadUpdateCard card) {
			if (!isCancelled() && card != null) {
				ExploreRvAdapter adapter = adapterWr.get();
				if (adapter != null) {
					if (adapter.addItem(DOWNLOAD_UPDATE_CARD_POSITION, card)) {
						adapter.notifyItemInserted(DOWNLOAD_UPDATE_CARD_POSITION);
					}
				}
			}
		}
	}
}
