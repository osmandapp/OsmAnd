package net.osmand.plus.settings.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.StyleSpan;
import android.text.style.URLSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.google.android.material.appbar.AppBarLayout;

import net.osmand.AndroidUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.OsmandInAppPurchaseActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.inapp.InAppPurchaseHelper;
import net.osmand.plus.liveupdates.OsmLiveActivity;

public class PurchasesFragment extends BaseOsmAndFragment {

	public static final String TAG = PurchasesFragment.class.getName();
	private InAppPurchaseHelper purchaseHelper;
	private View mainView;
	private Context context;
	private OsmandApplication app;
	private String url;
	private static final String PLAY_STORE_SUBSCRIPTION_URL = "https://play.google.com/store/account/subscriptions";
	private static final String PLAY_STORE_SUBSCRIPTION_DEEPLINK_URL = "https://play.google.com/store/account/subscriptions?sku=%s&package=%s";


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		app = getMyApplication();
		context = requireContext();
		purchaseHelper = getInAppPurchaseHelper();
		final MapActivity mapActivity = (MapActivity) getActivity();
		boolean nightMode = !getMyApplication().getSettings().isLightContent();
		LayoutInflater themedInflater = UiUtilities.getInflater(context, nightMode);
		mainView = themedInflater.inflate(R.layout.purchases_layout, container, false);

		createToolbar(mainView, nightMode);
		LinearLayout purchasesRestore = mainView.findViewById(R.id.restore_purchases);
		purchasesRestore.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (purchaseHelper != null && !purchaseHelper.hasInventory()) {
					purchaseHelper.requestInventory();
				}
			}
		});
		LinearLayout reportContainer = mainView.findViewById(R.id.report_container);
		reportContainer.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(mapActivity, OsmLiveActivity.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
				if (mapActivity != null) {
					mapActivity.startActivity(intent);
				}
			}
		});
		LinearLayout liveUpdatesContainer = mainView.findViewById(R.id.live_updates_container);
		liveUpdatesContainer.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
//				Fragment parent = getParentFragment();
//				if (parent != null) {
//					((LiveUpdatesFragment) parent).updateSubscriptionHeader();
//				}
			}
		});
		setFormatLink();
		return mainView;
	}

	@Nullable
	public InAppPurchaseHelper getInAppPurchaseHelper() {
		Activity activity = getActivity();
		if (activity instanceof OsmandInAppPurchaseActivity) {
			return ((OsmandInAppPurchaseActivity) activity).getPurchaseHelper();
		} else {
			return null;
		}
	}

	private void createToolbar(View mainView, boolean nightMode) {
		AppBarLayout appbar = mainView.findViewById(R.id.appbar);
		View toolbar = UiUtilities.getInflater(getContext(), nightMode).inflate(R.layout.profile_preference_toolbar_with_icon, appbar, false);

		View iconToolbarContainer = toolbar.findViewById(R.id.icon_toolbar);
		ImageView icon = iconToolbarContainer.findViewById(R.id.profile_icon);
		icon.setImageResource(R.drawable.ic_action_help_online);
		icon.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Uri uri = Uri.parse("https://docs.osmand.net/en/main@latest/osmand/purchases");
				Intent intent = new Intent(Intent.ACTION_VIEW, uri);
				if (AndroidUtils.isIntentSafe(context, intent)) {
					startActivity(intent);
				}
			}
		});
		ImageButton backButton = toolbar.findViewById(R.id.close_button);
		backButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				FragmentActivity fragmentActivity = getActivity();
				if (fragmentActivity != null) {
					fragmentActivity.onBackPressed();
				}
			}
		});
		getSkuAppId();
		LinearLayout manageSubscriptionContainer = mainView.findViewById(R.id.manage_subscription_container);
		manageSubscriptionContainer.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setData(Uri.parse(url));
				if (AndroidUtils.isIntentSafe(context, intent)) {
					startActivity(intent);
				}
			}
		});
		TextView toolbarTitle = toolbar.findViewById(R.id.toolbar_title);
		toolbarTitle.setText(getString(R.string.purchases));
		appbar.addView(toolbar);
	}

	public void setFormatLink() {
		TextView newDeviceAccountLink = mainView.findViewById(R.id.new_device_account_title);
		TextView contactSupportLink = mainView.findViewById(R.id.contact_support_title);
		TextView supportDescription = mainView.findViewById(R.id.support_link_title);

		SpannableString spannableStringSupport = new SpannableString(getString(R.string.contact_support));
		String urlSupport = "mailto:support@osmand.net";
		spannableStringSupport.setSpan(new URLSpan(urlSupport), 0, spannableStringSupport.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

		SpannableString spannableStringNewDeviceAccount = new SpannableString(getString(R.string.new_device_account));
		String urlNewDeviceAccount = "https://docs.osmand.net/en/main@latest/osmand/purchases#new-device--new-account";
		spannableStringNewDeviceAccount.setSpan(new URLSpan(urlNewDeviceAccount), 0, spannableStringNewDeviceAccount.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

		String emailString = getString(R.string.contact_support_mail);
		String supportDescriptionString = getString(R.string.contact_support_description, emailString);
		SpannableString spannableStringMail = new SpannableString(supportDescriptionString);
		int startIndex = supportDescriptionString.indexOf(emailString);
		int endIndex = startIndex + emailString.length();
		StyleSpan boldSpan = new StyleSpan(Typeface.BOLD);
		spannableStringMail.setSpan(boldSpan, startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

		contactSupportLink.setText(spannableStringSupport);
		newDeviceAccountLink.setText(spannableStringNewDeviceAccount);
		supportDescription.setText(spannableStringMail);

		AndroidUtils.removeLinkUnderline(contactSupportLink);
		AndroidUtils.removeLinkUnderline(newDeviceAccountLink);

		contactSupportLink.setMovementMethod(LinkMovementMethod.getInstance());
		newDeviceAccountLink.setMovementMethod(LinkMovementMethod.getInstance());
	}

	private void getSkuAppId() {
		InAppPurchaseHelper purchaseHelper = app.getInAppPurchaseHelper();
		if (purchaseHelper != null) {
			String sku = purchaseHelper.getFullVersion().getSku();
			url = String.format(PLAY_STORE_SUBSCRIPTION_DEEPLINK_URL,
					sku, context.getPackageName());
		} else {
			url = PLAY_STORE_SUBSCRIPTION_URL;
		}
	}
}
