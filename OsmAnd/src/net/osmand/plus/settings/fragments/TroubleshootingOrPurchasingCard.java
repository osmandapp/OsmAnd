package net.osmand.plus.settings.fragments;

import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.StyleSpan;
import android.text.style.URLSpan;
import android.view.View;
import android.widget.TextView;

import net.osmand.AndroidUtils;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.chooseplan.ChoosePlanDialogFragment;
import net.osmand.plus.inapp.InAppPurchaseHelper;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.wikipedia.WikipediaDialogFragment;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

public class TroubleshootingOrPurchasingCard extends BaseCard {

	private static final String OSMAND_NEW_DEVICE_URL = "https://docs.osmand.net/en/main@latest/osmand/purchases#new-device--new-account";
	private static final String OSMAND_EMAIL = "support@osmand.net";
	private static final String EMAIL_DEEPLINK_URI = "mailto:support@osmand.net";

	protected InAppPurchaseHelper purchaseHelper;

	private final boolean isPaidVersion;

	@Override
	public int getCardLayoutId() {
		return isPaidVersion ? R.layout.troubleshooting_card : R.layout.no_purchases_card;
	}

	public TroubleshootingOrPurchasingCard(@NonNull MapActivity mapActivity, @NonNull InAppPurchaseHelper purchaseHelper, boolean isPaidVersion) {
		super(mapActivity, false);
		this.purchaseHelper = purchaseHelper;
		this.isPaidVersion = isPaidVersion;
	}

	@Override
	protected void updateContent() {
		setupRestorePurchasesBtn();
		setupNewDeviceOrAccountBtn();
		setupSupportDescription();
		setupContactUsLink();

		if (!isPaidVersion) {
			TextView infoDescription = view.findViewById(R.id.info_description);
			String restorePurchases = app.getString(R.string.restore_purchases);
			String infoPurchases = String.format(app.getString(R.string.empty_purchases_description), restorePurchases);
			infoDescription.setText(infoPurchases);

			View osmandLive = view.findViewById(R.id.osmand_live);
			osmandLive.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					ChoosePlanDialogFragment.showDialogInstance(getMyApplication(),
							mapActivity.getSupportFragmentManager(),
							ChoosePlanDialogFragment.ChoosePlanDialogType.OSM_LIVE);
				}
			});

			CardView getItButtonContainer = view.findViewById(R.id.card_view);
			int colorRes = nightMode ? R.color.switch_button_active_dark : R.color.switch_button_active_light;
			getItButtonContainer.setCardBackgroundColor(ContextCompat.getColor(mapActivity, colorRes));

			View getItButton = view.findViewById(R.id.card_container);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				AndroidUtils.setBackground(mapActivity, getItButton, nightMode, R.drawable.ripple_light, R.drawable.ripple_dark);
			} else {
				AndroidUtils.setBackground(mapActivity, getItButton, nightMode, R.drawable.btn_unstroked_light, R.drawable.btn_unstroked_dark);
			}
		}
	}

	protected void setupRestorePurchasesBtn() {
		View purchasesRestore = view.findViewById(R.id.restore_purchases);
		purchasesRestore.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (purchaseHelper != null) {
					purchaseHelper.requestInventory();
				}
			}
		});
	}

	protected void setupNewDeviceOrAccountBtn() {
		View newDeviceAccountContainer = view.findViewById(R.id.new_device_account_container);
		newDeviceAccountContainer.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				WikipediaDialogFragment.showFullArticle(mapActivity, Uri.parse(OSMAND_NEW_DEVICE_URL), nightMode);
			}
		});
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

	protected void setupContactUsLink() {
		TextView contactSupportLink = view.findViewById(R.id.contact_support_title);
		SpannableString spannableStringSupport = new SpannableString(app.getString(R.string.contact_support));
		spannableStringSupport.setSpan(new URLSpan(EMAIL_DEEPLINK_URI), 0, spannableStringSupport.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		contactSupportLink.setText(spannableStringSupport);
		AndroidUtils.removeLinkUnderline(contactSupportLink);
		contactSupportLink.setMovementMethod(LinkMovementMethod.getInstance());
	}
}