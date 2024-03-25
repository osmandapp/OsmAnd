package net.osmand.plus.card.base.multistate;

import androidx.fragment.app.FragmentActivity;

public interface IMultiStateCard {

	FragmentActivity getActivity();
	boolean isNightMode();
	void updateSelectedCardState();

}
