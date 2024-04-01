package net.osmand.plus.card.base.multistate;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

public interface IMultiStateCardController {

	void bindComponent(@NonNull IMultiStateCard cardInstance);

	@NonNull
	String getCardTitle();

	@NonNull
	String getCardStateSelectorTitle();

	void onSelectorButtonClicked(@NonNull View selectorView);

	void onBindCardContent(@NonNull FragmentActivity activity,
	                       @NonNull ViewGroup container, boolean nightMode);

	default boolean shouldShowCardHeader() {
		return true;
	}
}
