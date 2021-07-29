package net.osmand.plus.chooseplan;

import android.graphics.Typeface;
import android.net.Uri;
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
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.wikipedia.WikipediaDialogFragment;

public class TroubleshootingCard extends BaseCard {

	public static final int REDEEM_PROMO_CODE_BUTTON_INDEX = 0;
	public static final int PURCHASES_RESTORE_BUTTON_INDEX = 1;

	private static final String OSMAND_NEW_DEVICE_URL = "https://docs.osmand.net/en/main@latest/osmand/purchases#new-device--new-account";
	private static final String OSMAND_EMAIL = "support@osmand.net";

	public TroubleshootingCard(@NonNull FragmentActivity activity, boolean usedOnMap) {
		super(activity, usedOnMap);
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
			CardListener listener = getListener();
			if (listener != null) {
				listener.onCardButtonPressed(TroubleshootingCard.this, REDEEM_PROMO_CODE_BUTTON_INDEX);
			}
			AuthorizeFragment.showInstance(activity.getSupportFragmentManager(), true);
		});
		boolean showPromoCodeBtn = !Version.isGooglePlayEnabled();
		AndroidUiHelper.updateVisibility(redeemPromoCode, showPromoCodeBtn);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.redeem_promo_code_divider), showPromoCodeBtn);
	}

	protected void setupRestorePurchasesBtn() {
		View purchasesRestore = view.findViewById(R.id.restore_purchases);
		purchasesRestore.setOnClickListener(v -> {
			CardListener listener = getListener();
			if (listener != null) {
				listener.onCardButtonPressed(TroubleshootingCard.this, PURCHASES_RESTORE_BUTTON_INDEX);
			}
		});
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
