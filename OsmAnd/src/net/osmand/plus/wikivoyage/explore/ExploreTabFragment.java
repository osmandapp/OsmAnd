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

import java.util.ArrayList;
import java.util.List;

public class ExploreTabFragment extends BaseOsmAndFragment {

	private static final int DOWNLOAD_UPDATE_CARD_POSITION = 0;

	private ExploreRvAdapter adapter = new ExploreRvAdapter();

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

	private void addDownloadUpdateCard(final boolean nightMode) {
		final OsmandApplication app = getMyApplication();
		new CheckWorldWikivoyageTask(app, new CheckWorldWikivoyageTask.Callback() {
			@Override
			public void onCheckFinished(boolean worldWikivoyageDownloaded) {
				if (!worldWikivoyageDownloaded && adapter != null) {
					TravelDownloadUpdateCard card = new TravelDownloadUpdateCard(app, nightMode, true);
					if (adapter.addItem(DOWNLOAD_UPDATE_CARD_POSITION, card)) {
						adapter.notifyDataSetChanged();
					}
				}
			}
		}).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
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

	private static class CheckWorldWikivoyageTask extends AsyncTask<Void, Void, Boolean> {

		private OsmandApplication app;
		private Callback callback;

		CheckWorldWikivoyageTask(OsmandApplication app, Callback callback) {
			this.app = app;
			this.callback = callback;
		}

		@Override
		protected Boolean doInBackground(Void... voids) {
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
			return worldWikivoyageDownloaded[0];
		}

		@Override
		protected void onPostExecute(Boolean worldWikivoyageDownloaded) {
			if (callback != null) {
				callback.onCheckFinished(worldWikivoyageDownloaded);
			}
			callback = null;
		}

		interface Callback {
			void onCheckFinished(boolean worldWikivoyageDownloaded);
		}
	}
}
