package net.osmand.plus.card.base.multistate;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;

public class MultiStateCard extends BaseCard {

	protected final IMultiStateCardController controller;

	public MultiStateCard(@NonNull FragmentActivity activity,
	                      @NonNull IMultiStateCardController cardController) {
		this(activity, cardController, true);
	}

	public MultiStateCard(@NonNull FragmentActivity activity,
	                      @NonNull IMultiStateCardController controller, boolean usedOnMap) {
		super(activity, usedOnMap);
		this.controller = controller;
		controller.bindCard(this);
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.card_multi_state;
	}

	@Override
	protected void updateContent() {
		if(controller.shouldShowMultiStateCardHeader()) {
			updateCardTitle();
			updateStateSelector();
		} else {
			View headerView = view.findViewById(R.id.header);
			AndroidUiHelper.updateVisibility(headerView, false);
		}
		bindSelectedStateContent();
	}

	public void updateSelectedCardState() {
		if (controller.shouldShowMultiStateCardHeader()) {
			updateStateSelector();
		}
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
		controller.showPopUpMenu(activity, selector, nightMode);
	}
}