package net.osmand.plus.routing.cards;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.card.base.multistate.IMultiStateCardController;
import net.osmand.plus.card.base.multistate.MultiStateCard;
import net.osmand.plus.settings.fragments.HeaderInfo;
import net.osmand.plus.settings.fragments.HeaderUiAdapter;

public class RouteLineColorCard extends MultiStateCard implements HeaderInfo {

	private final HeaderUiAdapter headerUiAdapter;

	public RouteLineColorCard(@NonNull FragmentActivity activity,
	                          @NonNull IMultiStateCardController cardController,
	                          @NonNull HeaderUiAdapter headerUiAdapter) {
		super(activity, cardController);
		this.headerUiAdapter = headerUiAdapter;
	}

	@Override
	public void onNeedUpdateHeader() {
		String title = controller.getMultiStateCardTitle();
		String description = controller.getMultiStateSelectorTitle();
		headerUiAdapter.onUpdateHeader(this, title, description);
	}

}