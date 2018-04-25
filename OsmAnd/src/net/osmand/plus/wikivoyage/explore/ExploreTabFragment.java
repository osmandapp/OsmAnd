package net.osmand.plus.wikivoyage.explore;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.wikivoyage.article.WikivoyageArticleDialogFragment;
import net.osmand.plus.wikivoyage.data.TravelArticle;
import net.osmand.plus.wikivoyage.explore.travelcards.ArticleTravelCard;
import net.osmand.plus.wikivoyage.explore.travelcards.OpenBetaTravelCard;
import net.osmand.plus.wikivoyage.explore.travelcards.StartEditingTravelCard;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class ExploreTabFragment extends BaseOsmAndFragment {

	boolean nightMode;

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		final OsmandApplication app = getMyApplication();
		nightMode = !getSettings().isLightContent();
		final View mainView = inflater.inflate(R.layout.fragment_explore_tab, container, false);


		ExploreRvAdapter adapter = new ExploreRvAdapter(app);

		final RecyclerView rv = (RecyclerView) mainView.findViewById(R.id.recycler_view);
		rv.setLayoutManager(new LinearLayoutManager(getContext()));
		adapter.setItems(getItems());
		rv.setAdapter(adapter);
		return mainView;
	}

	private List<Object> getItems() {
		List<Object> items = new LinkedList<>();
		List<TravelArticle> savedArticles = getMyApplication().getTravelDbHelper().searchPopular();
		if (!savedArticles.isEmpty()) {
			items.add(new OpenBetaTravelCard(getMyApplication(), nightMode, getFragmentManager()));
			items.add(new StartEditingTravelCard(getMyApplication(), nightMode));
			items.add(getString(R.string.popular_destinations));

			for (TravelArticle article : savedArticles) {
				items.add(new ArticleTravelCard(getMyApplication(), nightMode, article, getFragmentManager()));
			}
		}
		return items;
	}
}
