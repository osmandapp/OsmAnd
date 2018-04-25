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
		startEditingTravelCard = new StartEditingTravelCard(app, nightMode);
		items.add(new OpenBetaTravelCard(app, nightMode, getFragmentManager()));
		items.add(startEditingTravelCard);
		addPopularDestinations(app, nightMode);

		return items;
	}

	private void addPopularDestinations(OsmandApplication app, boolean nightMode) {
		popularDestinationsSearchTask = new PopularDestinationsSearchTask(app.getTravelDbHelper(), getMyActivity(), adapter, nightMode, startEditingTravelCard);
		popularDestinationsSearchTask.execute();
	}

	private static class PopularDestinationsSearchTask extends AsyncTask<ExploreRvAdapter, TravelDbHelper, List<Object>> {

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
		protected List<Object> doInBackground(ExploreRvAdapter... exploreRvAdapters) {
			List<TravelArticle> articles = travelDbHelper.searchPopular();
			List<Object> items = new ArrayList<>();
			if (!articles.isEmpty()) {
				OsmandActionBarActivity activity = weakContext.get();
				if (activity != null) {
					for (TravelArticle article : articles) {
						items.add(new ArticleTravelCard(activity.getMyApplication(), nightMode, article, activity.getSupportFragmentManager()));
					}
				}
			}
			return items;
		}

		@Override
		protected void onPostExecute(List<Object> items) {
			OsmandActionBarActivity activity = weakContext.get();
			ExploreRvAdapter adapter = weakAdapter.get();

			List<Object> adapterItems = adapter.getItems();
			StartEditingTravelCard startEditingTravelCard = weakStartEditingTravelCard.get();

			adapterItems.remove(startEditingTravelCard);
			adapterItems.add(activity.getResources().getString(R.string.popular_destinations));
			adapterItems.addAll(items);
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
