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
import android.widget.ProgressBar;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.OsmandActionBarActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.wikivoyage.data.TravelArticle;
import net.osmand.plus.wikivoyage.data.TravelDbHelper;
import net.osmand.plus.wikivoyage.explore.travelcards.ArticleTravelCard;
import net.osmand.plus.wikivoyage.explore.travelcards.OpenBetaTravelCard;
import net.osmand.plus.wikivoyage.explore.travelcards.StartEditingTravelCard;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class ExploreTabFragment extends BaseOsmAndFragment {

	private ExploreRvAdapter adapter = new ExploreRvAdapter();
	private PopularDestinationsSearchTask popularDestinationsSearchTask;
	private StartEditingTravelCard startEditingTravelCard;
	private ProgressBar progressBar;

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		final View mainView = inflater.inflate(R.layout.fragment_explore_tab, container, false);

		adapter.setItems(generateItems());

		final RecyclerView rv = (RecyclerView) mainView.findViewById(R.id.recycler_view);
		progressBar = (ProgressBar) mainView.findViewById(R.id.progressBar);
		rv.setLayoutManager(new LinearLayoutManager(getContext()));
		rv.setAdapter(adapter);

		return mainView;
	}

	private List<Object> generateItems() {
		final List<Object> items = new ArrayList<>();
		final OsmandApplication app = getMyApplication();
		final boolean nightMode = !getSettings().isLightContent();
		startEditingTravelCard = new StartEditingTravelCard(app, nightMode);
		items.add(new OpenBetaTravelCard(app, nightMode, getFragmentManager()));
		items.add(startEditingTravelCard);
		addPopularDestinations(app, nightMode);

		return items;
	}

	private void addPopularDestinations(OsmandApplication app, boolean nightMode) {
		popularDestinationsSearchTask = new PopularDestinationsSearchTask(app.getTravelDbHelper(), getMyActivity(), adapter, nightMode, startEditingTravelCard, progressBar);
		popularDestinationsSearchTask.execute();
	}

	private static class PopularDestinationsSearchTask extends AsyncTask<ExploreRvAdapter, TravelDbHelper, List<TravelArticle>> {

		private TravelDbHelper travelDbHelper;
		private WeakReference<OsmandActionBarActivity> weakContext;
		private WeakReference<ExploreRvAdapter> weakAdapter;
		private WeakReference<StartEditingTravelCard> weakStartEditingTravelCard;
		private boolean nightMode;
		private View progressBar;

		PopularDestinationsSearchTask(TravelDbHelper travelDbHelper,
		                              OsmandActionBarActivity context, ExploreRvAdapter adapter, boolean nightMode, StartEditingTravelCard startEditingTravelCard, View progressBar) {
			this.travelDbHelper = travelDbHelper;
			weakContext = new WeakReference<>(context);
			weakAdapter = new WeakReference<>(adapter);
			weakStartEditingTravelCard = new WeakReference<>(startEditingTravelCard);
			this.nightMode = nightMode;
			this.progressBar = progressBar;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			progressBar.setVisibility(View.VISIBLE);
		}

		@Override
		protected List<TravelArticle> doInBackground(ExploreRvAdapter... exploreRvAdapters) {
			return travelDbHelper.searchPopular();
		}

		@Override
		protected void onPostExecute(List<TravelArticle> items) {
			OsmandActionBarActivity activity = weakContext.get();
			ExploreRvAdapter adapter = weakAdapter.get();

			List<Object> adapterItems = adapter.getItems();
			StartEditingTravelCard startEditingTravelCard = weakStartEditingTravelCard.get();

			adapterItems.remove(startEditingTravelCard);

			if (!items.isEmpty()) {
				if (activity != null) {
					adapterItems.add(activity.getResources().getString(R.string.popular_destinations));
					for (TravelArticle article : items) {
						adapterItems.add(new ArticleTravelCard(activity.getMyApplication(), nightMode, article, activity.getSupportFragmentManager()));
					}
				}
			}
			progressBar.setVisibility(View.GONE);
			adapterItems.add(startEditingTravelCard);
			adapter.notifyDataSetChanged();
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		if (popularDestinationsSearchTask != null && popularDestinationsSearchTask.getStatus() == AsyncTask.Status.RUNNING) {
			popularDestinationsSearchTask.cancel(false);
		}
	}
}
