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

		BaseTravelCard openBeta = new BaseTravelCard.Builder()
				.setLayoutId(R.layout.wikivoyage_base_card)
				.setBackgroundImage(getIconsCache().getIcon(R.drawable.img_help_wikivoyage_articles))
				.setLeftButtonText(getString(R.string.get_unlimited_access))
				.setDescription(getString(R.string.welcome_to_open_beta_description))
				.setTitle(getString(R.string.welcome_to_open_beta))
				.create();
		items.add(openBeta);

		BaseTravelCard startEditing = new BaseTravelCard.Builder()
				.setLayoutId(R.layout.wikivoyage_start_editing_card)
				.setBackgroundImage(getIconsCache().getIcon(R.drawable.img_help_wikivoyage_contribute))
				.setLeftButtonText(getString(R.string.start_editing))
				.setDescription(getString(R.string.start_editing_card_description))
				.create();
		items.add(startEditing);

		for (BaseTravelCard item : items) {
			item.inflate(app, linearLayout, nightMode);
		}

		return mainView;
	}
}
