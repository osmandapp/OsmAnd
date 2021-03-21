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
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.OsmandInAppPurchaseActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.chooseplan.ChoosePlanDialogFragment;
import net.osmand.plus.inapp.InAppPurchaseHelper;

public class PurchasesFragmentEmpty extends BaseOsmAndFragment {
	public static final String TAG = PurchasesFragmentEmpty.class.getName();
	private InAppPurchaseHelper purchaseHelper;
	private View mainView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Context context = requireContext();
		purchaseHelper = getInAppPurchaseHelper();
		boolean nightMode = !getMyApplication().getSettings().isLightContent();
		LayoutInflater themedInflater = UiUtilities.getInflater(context, nightMode);
		mainView = themedInflater.inflate(R.layout.empty_purchases_layout, container, false);
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
		LinearLayout osmandLive = mainView.findViewById(R.id.osmand_live);
		osmandLive.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				ChoosePlanDialogFragment.showDialogInstance(getMyApplication(), getMyActivity().getSupportFragmentManager(), ChoosePlanDialogFragment.ChoosePlanDialogType.OSM_LIVE);
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
				startActivity(intent);
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

}
