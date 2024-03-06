package net.osmand.plus.card.base.multistate;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.widgets.popup.OnPopUpMenuItemClickListener;
import net.osmand.plus.widgets.popup.PopUpMenu;
import net.osmand.plus.widgets.popup.PopUpMenuDisplayData;
import net.osmand.plus.widgets.popup.PopUpMenuItem;

import java.util.List;

public interface IMultiStateCardController {

	default void showPopUpMenu(@NonNull FragmentActivity activity,
	                           @NonNull View selector, boolean nightMode) {
		OnPopUpMenuItemClickListener onItemClickListener =
				item -> onMultiStateMenuItemSelected(activity, selector, item);
		PopUpMenuDisplayData displayData = new PopUpMenuDisplayData();
		displayData.anchorView = selector;
		displayData.menuItems = getMultiSateMenuItems();
		displayData.nightMode = nightMode;
		displayData.onItemClickListener = onItemClickListener;
		PopUpMenu.show(displayData);
	}

	void bindCard(@NonNull MultiStateCard card);

	@NonNull
	String getMultiStateCardTitle();

	@NonNull
	String getMultiStateSelectorTitle();

	@ColorInt
	int getMultiStateSelectorAccentColor(boolean nightMode);

	@NonNull
	List<PopUpMenuItem> getMultiSateMenuItems();

	void onMultiStateMenuItemSelected(@NonNull FragmentActivity activity,
	                                  @NonNull View view, @NonNull PopUpMenuItem item);

	void onBindMultiStateCardContent(@NonNull FragmentActivity activity,
	                                 @NonNull ViewGroup container, boolean nightMode);

	boolean shouldShowMultiStateCardHeader();
}
