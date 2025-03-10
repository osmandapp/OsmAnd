package net.osmand.plus.card.base.multistate;

import android.view.View;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;

public interface IMultiStateCard {

	FragmentActivity getActivity();
	TextView getCardTitleView();
	View getSelectorView();
	boolean isNightMode();
	void updateSelectedCardState();

}
