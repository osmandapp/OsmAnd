package net.osmand.plus.chooseplan;

import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.R;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
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
		UiUtilities.setupDialogButton(nightMode, btnLearnMore, DialogButtonType.PRIMARY, R.string.shared_string_learn_more);
		btnLearnMore.setOnClickListener(v -> {
			if (activity != null) {
				ChoosePlanFragment.showDefaultInstance(activity);
			}
		});

		int bgResId = R.drawable.promo_banner_bg;
		int bgColor = ColorUtilities.getColorWithAlpha(getActiveColor(), 0.15f);
		View background = view.findViewById(R.id.banner_background);
		Drawable bgDrawable = app.getUIUtilities().getPaintedIcon(bgResId, bgColor);
		AndroidUtils.setBackground(background, bgDrawable);
	}

}
