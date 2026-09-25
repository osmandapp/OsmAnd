package net.osmand.plus.card.base.headed;

import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.R;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;

public class HeadedContentCard extends BaseCard implements IHeadedContentCard {

	private final IHeadedCardController controller;

	public HeadedContentCard(@NonNull FragmentActivity activity,
	                         @NonNull IHeadedCardController controller) {
		this(activity, controller, true);
	}

	public HeadedContentCard(@NonNull FragmentActivity activity,
	                         @NonNull IHeadedCardController controller, boolean usedOnMap) {
		super(activity, usedOnMap);
		this.controller = controller;
		controller.bindComponent(this);
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.card_headed_content;
	}

	@Override
	protected void updateContent() {
		updateCardTitle();
		updateCardSummary();
		updateCardContent();
	}

	@Override
	public void updateCardTitle() {
		setText(R.id.card_title, controller.getCardTitle());
	}

	@Override
	public void updateCardSummary() {
		setText(R.id.card_summary, controller.getCardSummary());
	}

	@Override
	public void updateCardContent() {
		ViewGroup contentContainer = view.findViewById(R.id.content_container);
		contentContainer.removeAllViews();
		contentContainer.addView(controller.getCardContentView(activity, nightMode));
	}
}
