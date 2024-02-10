package net.osmand.plus.card.base.multistate;

import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.widgets.popup.PopUpMenuItem;

import java.util.List;

public interface IMultiStateCardController {

	@NonNull
	String getCardTitle();

	@NonNull
	String getMenuButtonTitle();

	boolean shouldShowMenuButton();

	@NonNull
	List<PopUpMenuItem> getMenuItems();

	boolean onMenuItemSelected(@NonNull PopUpMenuItem item);

	void onBindContentView(@NonNull FragmentActivity activity, @NonNull ViewGroup container);
}
