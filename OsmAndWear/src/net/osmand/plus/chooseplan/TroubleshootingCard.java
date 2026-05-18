package net.osmand.plus.chooseplan;

import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.backup.ui.AuthorizeFragment;
import net.osmand.plus.backup.ui.LoginDialogType;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.inapp.InAppPurchaseHelper;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.utils.AndroidUtils;

public class TroubleshootingCard extends BaseCard {

	protected InAppPurchaseHelper purchaseHelper;

	public TroubleshootingCard(@NonNull FragmentActivity activity,
	                           @NonNull InAppPurchaseHelper purchaseHelper,
	                           boolean usedOnMap) {
		super(activity, usedOnMap);
		this.purchaseHelper = purchaseHelper;
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.troubleshooting_card;
	}

	@Override
	protected void updateContent() {
		setupRestorePurchasesBtn();
		setupNewDeviceOrAccountBtn();
		setupSupportDescription();
		setupContactUsLink();
		setupRedeemPromoCodeBtn();
	}

	protected void setupRedeemPromoCodeBtn() {
		View redeemPromoCode = view.findViewById(R.id.redeem_promo_code);
		redeemPromoCode.setOnClickListener(v -> {
			notifyCardPressed();
			AuthorizeFragment.showInstance(activity.getSupportFragmentManager(), LoginDialogType.SIGN_UP);
		});
		boolean showPromoCodeBtn = !Version.isGooglePlayEnabled();
		AndroidUiHelper.updateVisibility(redeemPromoCode, showPromoCodeBtn);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.redeem_promo_code_divider), showPromoCodeBtn);
	}

	protected void setupRestorePurchasesBtn() {
		View purchasesRestore = view.findViewById(R.id.restore_purchases);
		purchasesRestore.setOnClickListener(v -> {
			if (purchaseHelper != null) {
				purchaseHelper.requestInventory(true);
			}
		});
	}

	protected void setupNewDeviceOrAccountBtn() {
		View newDeviceAccountContainer = view.findViewById(R.id.new_device_account_container);
		newDeviceAccountContainer.setOnClickListener(v -> {
			AndroidUtils.openUrl(activity, R.string.docs_purchases_new_device, nightMode);
		});
	}

	protected void setupSupportDescription() {
		TextView supportDescription = view.findViewById(R.id.support_link_title);
		String email = app.getString(R.string.support_email);
		String supportDescriptionString = app.getString(R.string.contact_support_description, email);
		SpannableString spannableStringMail = new SpannableString(supportDescriptionString);
		int startIndex = supportDescriptionString.indexOf(email);
		int endIndex = startIndex + email.length();
		StyleSpan boldSpan = new StyleSpan(Typeface.BOLD);
		spannableStringMail.setSpan(boldSpan, startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		supportDescription.setText(spannableStringMail);
	}

	private void setupContactUsLink() {
		View contactSupportLinkContainer = view.findViewById(R.id.contact_support_title_container);
		contactSupportLinkContainer.setOnClickListener(v -> app.getFeedbackHelper().sendSupportEmail(app.getString(R.string.purchases)));
	}
}
