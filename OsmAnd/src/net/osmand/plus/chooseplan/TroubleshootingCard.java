package net.osmand.plus.chooseplan;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.R;
import net.osmand.plus.backup.ui.AuthorizeFragment;
import net.osmand.plus.inapp.InAppPurchaseHelper;

public class TroubleshootingCard extends BasePurchasingCard {

	private final boolean showPromoCodeBtn;

	public TroubleshootingCard(@NonNull FragmentActivity activity,
							   @NonNull InAppPurchaseHelper purchaseHelper,
							   boolean showPromoCodeBtn) {
		super(activity, purchaseHelper);
		this.showPromoCodeBtn = showPromoCodeBtn;
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.troubleshooting_card;
	}

	@Override
	protected void updateContent() {
		super.updateContent();
		setupRedeemPromoCodeBtn();
	}

	protected void setupRedeemPromoCodeBtn() {
		View redeemPromoCode = view.findViewById(R.id.redeem_promo_code);
		redeemPromoCode.setVisibility(showPromoCodeBtn ? View.VISIBLE : View.GONE);
		redeemPromoCode.setOnClickListener(v -> {
			CardListener listener = getListener();
			if (listener != null) {
				listener.onCardPressed(this);
			}
			AuthorizeFragment.showInstance(activity.getSupportFragmentManager(), true);
		});
	}
}
