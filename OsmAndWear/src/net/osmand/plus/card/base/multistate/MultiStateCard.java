package net.osmand.plus.card.base.multistate;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;

public class MultiStateCard extends BaseCard implements IMultiStateCard {

	protected final IMultiStateCardController controller;

	public MultiStateCard(@NonNull FragmentActivity activity, @NonNull IMultiStateCardController controller) {
		this(activity, controller, true);
	}

	public MultiStateCard(@NonNull FragmentActivity activity, @NonNull IMultiStateCardController controller, boolean usedOnMap) {
		super(activity, usedOnMap);
		this.controller = controller;
		controller.bindComponent(this);
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.card_multi_state;
	}

	@Override
	protected void updateContent() {
		if (controller.shouldShowCardHeader()) {
			updateCardTitle();
			updateStateSelector();
		} else {
			View headerView = view.findViewById(R.id.header);
			AndroidUiHelper.updateVisibility(headerView, false);
		}
		bindSelectedStateContent();
	}

	@Override
	public void updateSelectedCardState() {
		if (controller.shouldShowCardHeader()) {
			updateStateSelector();
		}
		bindSelectedStateContent();
	}

	protected void updateCardTitle() {
		TextView tvTitle = view.findViewById(R.id.card_title);
		tvTitle.setText(controller.getCardTitle());
	}

	private void updateStateSelector() {
		View selector = view.findViewById(R.id.card_selector);
		selector.setOnClickListener(v -> controller.onSelectorButtonClicked(selector));
		updateStateSelectorTitle();
	}

	private void updateStateSelectorTitle() {
		View selector = view.findViewById(R.id.card_selector);
		TextView tvTitle = selector.findViewById(R.id.title);
		tvTitle.setText(controller.getCardStateSelectorTitle());
	}

	private void bindSelectedStateContent() {
		ViewGroup contentContainer = view.findViewById(R.id.content);
		controller.onBindCardContent(activity, contentContainer, nightMode, usedOnMap);
	}

	@Override
	public View getSelectorView() {
		return view.findViewById(R.id.card_selector);
	}

	@Override
	public FragmentActivity getActivity() {
		return activity;
	}
}