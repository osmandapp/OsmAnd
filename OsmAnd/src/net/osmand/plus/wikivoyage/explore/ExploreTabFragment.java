package net.osmand.plus.wikivoyage.explore;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.wikivoyage.explore.travelcards.BaseTravelCard;
import net.osmand.plus.wikivoyage.explore.travelcards.OpenBetaTravelCard;
import net.osmand.plus.wikivoyage.explore.travelcards.StartEditingTravelCard;

import java.util.ArrayList;

public class ExploreTabFragment extends BaseOsmAndFragment {

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		OsmandApplication app = getMyApplication();
		boolean nightMode = !getSettings().isLightContent();

		final View mainView = inflater.inflate(R.layout.fragment_explore_tab, container, false);

		ArrayList<BaseTravelCard> items = new ArrayList<>();

		OpenBetaTravelCard openBetaTravelCard = new OpenBetaTravelCard(app, nightMode, getFragmentManager());
		StartEditingTravelCard startEditingTravelCard = new StartEditingTravelCard(app, nightMode);
		items.add(openBetaTravelCard);
		items.add(startEditingTravelCard);

		final RecyclerView rv = (RecyclerView) mainView.findViewById(R.id.recycler_view);
		rv.setLayoutManager(new LinearLayoutManager(getContext()));
		return mainView;
	}
}
