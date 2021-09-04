package net.osmand.plus.chooseplan;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import net.osmand.AndroidUtils;
import net.osmand.plus.ColorUtilities;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.UiUtilities.DialogButtonType;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;

public class NoPurchasesCard extends BaseCard {

	public NoPurchasesCard(@NonNull FragmentActivity activity,
	                       boolean usedOnMap) {
		super(activity, usedOnMap);
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.no_purchases_card;
	}

	@Override
	protected void updateContent() {
		TextView infoDescription = view.findViewById(R.id.info_description);
		String restorePurchases = app.getString(R.string.restore_purchases);
		String infoPurchases = String.format(app.getString(R.string.empty_purchases_description), restorePurchases);
		infoDescription.setText(infoPurchases);

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
