package net.osmand.plus.plugins.openplacereviews;

import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.PlatformUtil;
import net.osmand.plus.R;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.plugins.openplacereviews.OprAuthHelper.OprAuthorizationListener;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;

import org.apache.commons.logging.Log;

public class OprStartFragment extends BaseOsmAndFragment implements OprAuthorizationListener {
	private static final String TAG = OprStartFragment.class.getSimpleName();
	private static final Log LOG = PlatformUtil.getLog(OprStartFragment.class);
	private static final String openPlaceReviewsUrl = "OpenPlaceReviews.org";

	@Override
	protected boolean isUsedOnMap() {
		return true;
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		updateNightMode();
		View view = UiUtilities.getInflater(requireMyActivity(), nightMode).inflate(R.layout.fragment_opr_login, container, false);
		AndroidUtils.addStatusBarPadding21v(requireMyActivity(), view);

		Toolbar toolbar = view.findViewById(R.id.toolbar);
		int icBackResId = AndroidUtils.getNavigationIconResId(view.getContext());
		toolbar.setNavigationIcon(getContentIcon(icBackResId));
		toolbar.setNavigationContentDescription(R.string.access_shared_string_navigate_up);
		toolbar.setNavigationOnClickListener(v -> dismiss());

		View createAccount = view.findViewById(R.id.register_opr_create_account);
		createAccount.setOnClickListener(v -> handleCreateAccount());
		View haveAccount = view.findViewById(R.id.register_opr_have_account);
		haveAccount.setOnClickListener(v -> handleHaveAccount());
		setURLSpan(view);
		return view;
	}

	private void handleHaveAccount() {
		String url = OPRConstants.getLoginUrl(app);
		CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
		CustomTabsIntent customTabsIntent = builder.build();
		customTabsIntent.launchUrl(requireContext(), Uri.parse(url));
	}

	private void handleCreateAccount() {
		String url = OPRConstants.getRegisterUrl(app);
		CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
		CustomTabsIntent customTabsIntent = builder.build();
		customTabsIntent.launchUrl(requireContext(), Uri.parse(url));
	}

	private void setURLSpan(View v) {
		String desc = requireContext().getString(R.string.register_on_openplacereviews_desc);
		SpannableString ss = new SpannableString(desc);
		ss.setSpan(new URLSpanNoUnderline(getActivity().getString(R.string.opr_base_url)), desc.indexOf(openPlaceReviewsUrl),
				desc.indexOf(openPlaceReviewsUrl) + openPlaceReviewsUrl.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
		v.<TextView>findViewById(R.id.start_opr_description).setText(ss);
		v.<TextView>findViewById(R.id.start_opr_description).setMovementMethod(LinkMovementMethod.getInstance());
	}

	@Override
	public int getStatusBarColorId() {
		AndroidUiHelper.setStatusBarContentColor(getView(), nightMode);
		return ColorUtilities.getListBgColorId(nightMode);
	}

	private class URLSpanNoUnderline extends URLSpan {
		public URLSpanNoUnderline(String url) {
			super(url);
		}

		@Override
		public void updateDrawState(@NonNull TextPaint ds) {
			super.updateDrawState(ds);
			ds.setUnderlineText(false);
			ds.setTypeface(Typeface.DEFAULT_BOLD);
		}
	}

	@Override
	public void authorizationCompleted() {
		dismiss();
	}

	protected void dismiss() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			activity.getSupportFragmentManager().popBackStack();
		}
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			OprStartFragment fragment = new OprStartFragment();
			fragmentManager.beginTransaction()
					.add(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(null)
					.commitAllowingStateLoss();
		}
	}
}
