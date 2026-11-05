package net.osmand.plus.card.base.multistate;

import android.view.View;

import androidx.fragment.app.FragmentActivity;

public interface IMultiStateCard {

	FragmentActivity getActivity();
	View getSelectorView();
	boolean isNightMode();
	void updateSelectedCardState();

}
