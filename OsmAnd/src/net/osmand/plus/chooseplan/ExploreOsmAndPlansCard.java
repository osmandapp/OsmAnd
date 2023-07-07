package net.osmand.plus.chooseplan;

import android.view.View;

import net.osmand.plus.R;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.dialogbutton.DialogButtonType;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

public class ExploreOsmAndPlansCard extends BaseCard {

	private final DialogFragment target;

	public ExploreOsmAndPlansCard(@NonNull FragmentActivity activity, @NonNull DialogFragment target) {
		super(activity, false);
		this.target = target;
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
			FragmentManager fragmentManager = activity.getSupportFragmentManager();
			Fragment choosePlanFragment = fragmentManager.findFragmentByTag(ChoosePlanFragment.TAG);
			boolean returnToChoosePlanFragment = choosePlanFragment != null;
			if (returnToChoosePlanFragment) {
				target.dismiss();
			} else {
				ChoosePlanFragment.showDefaultInstance(activity);
			}
		});
	}
}