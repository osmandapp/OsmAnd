package net.osmand.plus.chooseplan;

import android.net.Uri;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.R;
import net.osmand.plus.inapp.InAppPurchaseHelper;
import net.osmand.plus.wikipedia.WikipediaDialogFragment;

public class TroubleshootingCard extends BasePurchasingCard {

	private static final String REDEEM_PROMO_CODE_URL = "https://support.google.com/googleplay/answer/3422659";

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
		redeemPromoCode.setOnClickListener(v ->
				WikipediaDialogFragment.showFullArticle(activity, Uri.parse(REDEEM_PROMO_CODE_URL), nightMode));
	}

}
