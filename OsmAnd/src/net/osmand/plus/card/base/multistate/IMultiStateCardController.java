package net.osmand.plus.card.base.multistate;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

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

	void onBindStateRelatedContent(@NonNull LayoutInflater layoutInflater, @NonNull ViewGroup container);
}
