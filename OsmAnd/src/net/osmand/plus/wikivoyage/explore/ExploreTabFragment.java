package net.osmand.plus.wikivoyage.explore;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
		boolean nightMode = app.getDaynightHelper().isNightMode();

		final View mainView = inflater.inflate(R.layout.fragment_explore_tab, container, false);

		LinearLayout linearLayout = (LinearLayout) mainView.findViewById(R.id.cards_list);
		ArrayList<BaseTravelCard> items = new ArrayList<>();

		BaseTravelCard openBetaTravelCard = new OpenBetaTravelCard(getMyActivity());
		BaseTravelCard startEditingTravelCard = new StartEditingTravelCard(getMyActivity());
		items.add(openBetaTravelCard);
		items.add(startEditingTravelCard);

		for (BaseTravelCard item : items) {
			item.inflate(app, linearLayout, nightMode);
		}

		return mainView;
	}
}
