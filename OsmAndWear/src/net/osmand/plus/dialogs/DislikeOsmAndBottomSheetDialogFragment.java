package net.osmand.plus.dialogs;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.utils.AndroidUtils;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.feedback.RateUsHelper;
import net.osmand.plus.feedback.RateUsState;

import org.apache.commons.logging.Log;

public class DislikeOsmAndBottomSheetDialogFragment extends MenuBottomSheetDialogFragment {

	public static final String TAG = DislikeOsmAndBottomSheetDialogFragment.class.getSimpleName();
	private static final Log LOG = PlatformUtil.getLog(DislikeOsmAndBottomSheetDialogFragment.class);

	private RateUsHelper rateUsHelper;

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		Context context = getContext();
		if (context == null) {
			return;
		}

		rateUsHelper = new RateUsHelper();

		View titleView = View.inflate(new ContextThemeWrapper(context, themeRes), R.layout.dislike_title, null);
		SimpleBottomSheetItem titleItem = (SimpleBottomSheetItem) new SimpleBottomSheetItem.Builder()
				.setCustomView(titleView)
				.create();
		items.add(titleItem);
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_cancel;
	}

	@Override
	protected void onDismissButtonClickAction() {
		OsmandApplication app = getMyApplication();
		if (app != null) {
			rateUsHelper.updateState(RateUsState.DISLIKED_WITHOUT_MESSAGE);
		}
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.shared_string_send;
	}

	@Override
	protected void onRightBottomButtonClick() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			rateUsHelper.updateState(RateUsState.DISLIKED_WITH_MESSAGE);
			String email = getString(R.string.support_email);
			Intent sendEmail = new Intent(Intent.ACTION_SENDTO);
			sendEmail.setData(Uri.parse("mailto:" + email));
			sendEmail.putExtra(Intent.EXTRA_EMAIL, email);
			Intent chooserIntent = Intent.createChooser(sendEmail, getString(R.string.send_report));
			AndroidUtils.startActivityIfSafe(activity, sendEmail, chooserIntent);
			dismiss();
		}
	}

	@Override
	public void onDismiss(@NonNull DialogInterface dialog) {
		super.onDismiss(dialog);
		FragmentActivity activity = getActivity();
		rateUsHelper.storeRateResult(activity);
	}

	public static void showInstance(@NonNull FragmentManager fm) {
		try {
			if (fm.findFragmentByTag(TAG) == null) {
				DislikeOsmAndBottomSheetDialogFragment fragment = new DislikeOsmAndBottomSheetDialogFragment();
				fragment.show(fm, TAG);
			}
		} catch (RuntimeException e) {
			LOG.error("showInstance", e);
		}
	}
}
