package net.osmand.plus.chooseplan;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.R;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.utils.UiUtilities.DialogButtonType;

import org.jetbrains.annotations.NotNull;

public class ExploreOsmAndPlansCard extends BaseCard {

	public ExploreOsmAndPlansCard(@NonNull @NotNull FragmentActivity activity) {
		super(activity, false);
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.card_explore_osmand_plans;
	}

	@Override
	protected void updateContent() {
		View btnLearnMore = view.findViewById(R.id.button_learn_more);
		UiUtilities.setupDialogButton(nightMode, btnLearnMore, DialogButtonType.SECONDARY_ACTIVE, R.string.shared_string_learn_more);
		btnLearnMore.setOnClickListener(v -> {
			if (activity != null) {
				ChoosePlanFragment.showDefaultInstance(activity);
			}
		});
	}

}
