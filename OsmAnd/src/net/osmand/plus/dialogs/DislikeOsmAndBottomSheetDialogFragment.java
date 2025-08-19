package net.osmand.plus.dialogs;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.utils.AndroidUtils;
import net.osmand.PlatformUtil;
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
		rateUsHelper = new RateUsHelper();
		View titleView = inflate(R.layout.dislike_title);
		items.add(new SimpleBottomSheetItem.Builder().setCustomView(titleView).create());
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_cancel;
	}

	@Override
	protected void onDismissButtonClickAction() {
		rateUsHelper.updateState(RateUsState.DISLIKED_WITHOUT_MESSAGE);
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
		rateUsHelper.storeRateResult(getActivity());
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG, true)) {
			DislikeOsmAndBottomSheetDialogFragment fragment = new DislikeOsmAndBottomSheetDialogFragment();
			fragment.show(fragmentManager, TAG);
		}
	}
}
