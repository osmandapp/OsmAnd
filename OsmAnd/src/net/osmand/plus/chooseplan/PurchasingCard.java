package net.osmand.plus.chooseplan;

import android.os.Build;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import net.osmand.AndroidUtils;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.inapp.InAppPurchaseHelper;

public class PurchasingCard extends BasePurchasingCard {

	public PurchasingCard(@NonNull FragmentActivity activity, @NonNull InAppPurchaseHelper purchaseHelper) {
		super(activity, purchaseHelper);
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.no_purchases_card;
	}

	@Override
	protected void updateContent() {
		super.updateContent();

		TextView infoDescription = view.findViewById(R.id.info_description);
		String restorePurchases = app.getString(R.string.restore_purchases);
		String infoPurchases = String.format(app.getString(R.string.empty_purchases_description), restorePurchases);
		infoDescription.setText(infoPurchases);

		View osmandLive = view.findViewById(R.id.osmand_live);
		osmandLive.setOnClickListener(
				v -> ChoosePlanFragment.showInstance(activity, OsmAndFeature.HOURLY_MAP_UPDATES));
		CardView getItButtonContainer = view.findViewById(R.id.card_view);
		int colorRes = nightMode ? R.color.switch_button_active_dark : R.color.switch_button_active_light;
		getItButtonContainer.setCardBackgroundColor(ContextCompat.getColor(activity, colorRes));

		View getItButton = view.findViewById(R.id.card_container);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			AndroidUtils.setBackground(activity, getItButton, nightMode, R.drawable.ripple_light, R.drawable.ripple_dark);
		} else {
			AndroidUtils.setBackground(activity, getItButton, nightMode, R.drawable.btn_unstroked_light, R.drawable.btn_unstroked_dark);
		}

		ImageView getItArrow = view.findViewById(R.id.additional_button_icon);
		UiUtilities.rotateImageByLayoutDirection(getItArrow);
	}
}
