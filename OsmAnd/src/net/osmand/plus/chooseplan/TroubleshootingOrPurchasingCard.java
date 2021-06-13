package net.osmand.plus.chooseplan;

import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;
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
import net.osmand.plus.chooseplan.ChoosePlanDialogFragment;
import net.osmand.plus.inapp.InAppPurchaseHelper;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.wikipedia.WikipediaDialogFragment;

public class TroubleshootingOrPurchasingCard extends BaseCard {

	private static final String OSMAND_NEW_DEVICE_URL = "https://docs.osmand.net/en/main@latest/osmand/purchases#new-device--new-account";
	private static final String REDEEM_PROMO_CODE_URL = "https://support.google.com/googleplay/answer/3422659";
	private static final String OSMAND_EMAIL = "support@osmand.net";

	protected InAppPurchaseHelper purchaseHelper;

	private final boolean isPaidVersion;
	private final boolean showPromoCodeBtn;

	@Override
	public int getCardLayoutId() {
		return isPaidVersion ? R.layout.troubleshooting_card : R.layout.no_purchases_card;
	}

	public TroubleshootingOrPurchasingCard(@NonNull FragmentActivity activity,
	                                       @NonNull InAppPurchaseHelper purchaseHelper,
	                                       boolean isPaidVersion,
	                                       boolean showPromoCodeBtn) {
		super(activity, false);
		this.purchaseHelper = purchaseHelper;
		this.isPaidVersion = isPaidVersion;
		this.showPromoCodeBtn = showPromoCodeBtn;
	}

	@Override
	protected void updateContent() {
		setupRestorePurchasesBtn();
		setupRedeemPromoCodeBtn();
		setupNewDeviceOrAccountBtn();
		setupSupportDescription();
		setupContactUsLink();

		if (!isPaidVersion) {
			TextView infoDescription = view.findViewById(R.id.info_description);
			String restorePurchases = app.getString(R.string.restore_purchases);
			String infoPurchases = String.format(app.getString(R.string.empty_purchases_description), restorePurchases);
			infoDescription.setText(infoPurchases);

			View osmandLive = view.findViewById(R.id.osmand_live);
			osmandLive.setOnClickListener(v -> ChoosePlanDialogFragment.showDialogInstance(getMyApplication(),
					activity.getSupportFragmentManager(),
					ChoosePlanDialogFragment.ChoosePlanDialogType.SUBSCRIPTION));

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

	protected void setupRestorePurchasesBtn() {
		View purchasesRestore = view.findViewById(R.id.restore_purchases);
		purchasesRestore.setOnClickListener(v -> {
			if (purchaseHelper != null) {
				purchaseHelper.requestInventory();
			}
		});
	}

	protected void setupRedeemPromoCodeBtn() {
		View redeemPromoCode = view.findViewById(R.id.redeem_promo_code);
		redeemPromoCode.setVisibility(showPromoCodeBtn ? View.VISIBLE : View.GONE);
		redeemPromoCode.setOnClickListener(v ->
				WikipediaDialogFragment.showFullArticle(activity, Uri.parse(REDEEM_PROMO_CODE_URL), nightMode));
	}

	protected void setupNewDeviceOrAccountBtn() {
		View newDeviceAccountContainer = view.findViewById(R.id.new_device_account_container);
		newDeviceAccountContainer.setOnClickListener(v ->
				WikipediaDialogFragment.showFullArticle(activity, Uri.parse(OSMAND_NEW_DEVICE_URL), nightMode));
	}

	protected void setupSupportDescription() {
		TextView supportDescription = view.findViewById(R.id.support_link_title);
		String supportDescriptionString = app.getString(R.string.contact_support_description, OSMAND_EMAIL);
		SpannableString spannableStringMail = new SpannableString(supportDescriptionString);
		int startIndex = supportDescriptionString.indexOf(OSMAND_EMAIL);
		int endIndex = startIndex + OSMAND_EMAIL.length();
		StyleSpan boldSpan = new StyleSpan(Typeface.BOLD);
		spannableStringMail.setSpan(boldSpan, startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		supportDescription.setText(spannableStringMail);
	}

	private void setupContactUsLink() {
		View contactSupportLinkContainer = view.findViewById(R.id.contact_support_title_container);
		contactSupportLinkContainer.setOnClickListener(
				v -> app.sendSupportEmail(app.getString(R.string.purchases)));
	}
}