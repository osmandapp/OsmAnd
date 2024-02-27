package net.osmand.plus.card.base.multistate;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.widgets.popup.PopUpMenuItem;

import java.util.List;

public interface IMultiStateCardController {

	@NonNull
	String getMultiStateCardTitle();

	@NonNull
	String getMultiStateSelectorTitle();

	@ColorInt
	int getMultiStateSelectorAccentColor(boolean nightMode);

	@NonNull
	List<PopUpMenuItem> getMultiSateMenuItems();

	boolean onMultiStateMenuItemSelected(@NonNull FragmentActivity activity, @NonNull View view,
	                                     @NonNull PopUpMenuItem item);

	void onBindMultiStateCardContent(@NonNull FragmentActivity activity, @NonNull ViewGroup container, boolean nightMode);
}
