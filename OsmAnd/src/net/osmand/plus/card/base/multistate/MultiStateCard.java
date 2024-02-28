package net.osmand.plus.card.base.multistate;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.R;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.popup.OnPopUpMenuItemClickListener;
import net.osmand.plus.widgets.popup.PopUpMenu;
import net.osmand.plus.widgets.popup.PopUpMenuDisplayData;
import net.osmand.plus.widgets.popup.PopUpMenuItem;

import java.util.List;

public class MultiStateCard extends BaseCard {

	private final IMultiStateCardController controller;

	public MultiStateCard(@NonNull FragmentActivity activity,
	                      @NonNull IMultiStateCardController cardController) {
		this(activity, cardController, true);
	}

	public MultiStateCard(@NonNull FragmentActivity activity,
	                      @NonNull IMultiStateCardController controller, boolean usedOnMap) {
		super(activity, usedOnMap);
		this.controller = controller;
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.card_multi_state;
	}

	@Override
	protected void updateContent() {
		updateCardTitle();
		updateStateSelector();
		bindSelectedStateContent();
	}

	private void updateCardTitle() {
		TextView tvTitle = view.findViewById(R.id.card_title);
		tvTitle.setText(controller.getMultiStateCardTitle());
	}

	private void updateStateSelector() {
		View selector = view.findViewById(R.id.card_selector);
		selector.setOnClickListener(v -> showPopUpMenu());
		updateStateSelectorTitle();
	}

	private void updateStateSelectorTitle() {
		View selector = view.findViewById(R.id.card_selector);
		TextView tvTitle = selector.findViewById(R.id.title);
		tvTitle.setText(controller.getMultiStateSelectorTitle());
	}

	private void bindSelectedStateContent() {
		ViewGroup contentContainer = view.findViewById(R.id.content);
		controller.onBindMultiStateCardContent(activity, contentContainer, nightMode);
	}

	private void showPopUpMenu() {
		View selector = view.findViewById(R.id.card_selector);
		List<PopUpMenuItem> menuItems = controller.getMultiSateMenuItems();

		OnPopUpMenuItemClickListener onItemClickListener = item -> {
			if (controller.onMultiStateMenuItemSelected(activity, selector, item)) {
				// Update selected state only if controller
				// has processed user's selection and returned 'true'
				updateStateSelector();
				bindSelectedStateContent();
			}
		};
		PopUpMenuDisplayData displayData = new PopUpMenuDisplayData();
		displayData.anchorView = selector;
		displayData.menuItems = menuItems;
		displayData.nightMode = nightMode;
		displayData.onItemClickListener = onItemClickListener;
		PopUpMenu.show(displayData);
	}
}