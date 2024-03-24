package net.osmand.plus.card.base.multistate;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.widgets.popup.OnPopUpMenuItemClickListener;
import net.osmand.plus.widgets.popup.PopUpMenu;
import net.osmand.plus.widgets.popup.PopUpMenuDisplayData;
import net.osmand.plus.widgets.popup.PopUpMenuItem;

import java.util.List;

public abstract class BaseMultiStateCardController implements IMultiStateCardController {

	protected final OsmandApplication app;
	protected IMultiStateCard cardInstance;

	public BaseMultiStateCardController(@NonNull OsmandApplication app) {
		this.app = app;
	}

	@Override
	public void bindComponent(@NonNull IMultiStateCard cardInstance) {
		this.cardInstance = cardInstance;
	}

	@Override
	public void onSelectorButtonClicked(@NonNull View selectorView) {
		FragmentActivity activity = cardInstance.getActivity();
		boolean nightMode = cardInstance.isNightMode();

		OnPopUpMenuItemClickListener onItemClickListener =
				item -> onPopUpMenuItemSelected(activity, selectorView, item);
		PopUpMenuDisplayData displayData = new PopUpMenuDisplayData();
		displayData.anchorView = selectorView;
		displayData.menuItems = getPopUpMenuItems();
		displayData.nightMode = nightMode;
		displayData.onItemClickListener = onItemClickListener;
		PopUpMenu.show(displayData);
	}


	@NonNull
	public abstract List<PopUpMenuItem> getPopUpMenuItems();

	public abstract void onPopUpMenuItemSelected(@NonNull FragmentActivity activity,
	                                             @NonNull View view, @NonNull PopUpMenuItem item);
}
