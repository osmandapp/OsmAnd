package net.osmand.plus.wikivoyage.explore;

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
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.wikivoyage.data.TravelArticle;
import net.osmand.plus.wikivoyage.explore.travelcards.ArticleTravelCard;
import net.osmand.plus.wikivoyage.explore.travelcards.OpenBetaTravelCard;
import net.osmand.plus.wikivoyage.explore.travelcards.StartEditingTravelCard;

import java.util.ArrayList;
import java.util.List;

public class ExploreTabFragment extends BaseOsmAndFragment {

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

	/**
	 * Cards order:
	 * - Download/Update
	 * - Open Beta
	 * - Edit Wiki
	 * - Popular Destinations
	 * - Maps you need
	 *
	 * @return list of generated items.
	 */
	private List<Object> generateItems() {
		final List<Object> items = new ArrayList<>();
		final OsmandApplication app = getMyApplication();
		final boolean nightMode = !getSettings().isLightContent();

		items.add(new OpenBetaTravelCard(app, nightMode, getFragmentManager()));
		items.add(new StartEditingTravelCard(app, nightMode));
		addPopularDestinations(items, nightMode);

		return items;
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
}
