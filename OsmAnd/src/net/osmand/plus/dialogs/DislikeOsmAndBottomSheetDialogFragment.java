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

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.dialogs.RateUsBottomSheetDialogFragment.RateUsState;

import org.apache.commons.logging.Log;

public class DislikeOsmAndBottomSheetDialogFragment extends MenuBottomSheetDialogFragment {
	public static final String TAG = "DislikeOsmAndBottomSheetDialogFragment";
	private static final Log LOG = PlatformUtil.getLog(DislikeOsmAndBottomSheetDialogFragment.class);

	private RateUsState newRateUsState = RateUsState.IGNORED;

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		Context context = getContext();
		if (context == null) {
			return;
		}

		final View titleView = View.inflate(new ContextThemeWrapper(context, themeRes), R.layout.dislike_title, null);
		final SimpleBottomSheetItem titleItem = (SimpleBottomSheetItem) new SimpleBottomSheetItem.Builder()
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
			newRateUsState = RateUsState.DISLIKED_WITHOUT_MESSAGE;
		}
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.shared_string_send;
	}

	@Override
	protected void onRightBottomButtonClick() {
		OsmandApplication app = getMyApplication();
		if (app != null) {
			newRateUsState = RateUsState.DISLIKED_WITH_MESSAGE;
			String email = getString(R.string.support_email);
			Intent sendEmail = new Intent(Intent.ACTION_SENDTO);
			sendEmail.setData(Uri.parse("mailto:" + email));
			sendEmail.putExtra(Intent.EXTRA_EMAIL, email);
			startActivity(Intent.createChooser(sendEmail, getString(R.string.send_report)));
			dismiss();
		}
	}

	@Override
	public void onDismiss(@NonNull DialogInterface dialog) {
		super.onDismiss(dialog);
		FragmentActivity activity = getActivity();
		if (newRateUsState != null && activity != null && !activity.isChangingConfigurations()) {
			OsmandApplication app = (OsmandApplication) activity.getApplication();
			OsmandSettings settings = app.getSettings();
			RateUsState newState = RateUsState.getNewState(app, newRateUsState);
			settings.RATE_US_STATE.set(newState);
			settings.NUMBER_OF_APP_STARTS_ON_DISLIKE_MOMENT.set(app.getAppInitializer().getNumberOfStarts());
			settings.LAST_DISPLAY_TIME.set(System.currentTimeMillis());
		}
	}

	public static void showInstance(@NonNull FragmentManager fm) {
		try {
			if (fm.findFragmentByTag(DislikeOsmAndBottomSheetDialogFragment.TAG) == null) {
				DislikeOsmAndBottomSheetDialogFragment fragment = new DislikeOsmAndBottomSheetDialogFragment();
				fragment.show(fm, DislikeOsmAndBottomSheetDialogFragment.TAG);
			}
		} catch (RuntimeException e) {
			LOG.error("showInstance", e);
		}
	}
}
