package net.osmand.plus.dialogs;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.support.v7.view.ContextThemeWrapper;
import android.view.View;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;

import org.apache.commons.logging.Log;

public class DislikeOsmAndBottomSheetDialogFragment extends MenuBottomSheetDialogFragment {
	public static final String TAG = "DislikeOsmAndBottomSheetDialogFragment";
	private static final Log LOG = PlatformUtil.getLog(DislikeOsmAndBottomSheetDialogFragment.class);

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
			app.getSettings().RATE_US_STATE.set(RateUsBottomSheetDialogFragment.RateUsState.DISLIKED_WITHOUT_MESSAGE);
			app.getSettings().NUMBER_OF_APP_STARTS_ON_DISLIKE_MOMENT.set(app.getAppInitializer().getNumberOfStarts());
			app.getSettings().LAST_DISPLAY_TIME.set(System.currentTimeMillis());
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
			OsmandSettings settings = app.getSettings();
			String email = getString(R.string.support_email);
			settings.RATE_US_STATE.set(RateUsBottomSheetDialogFragment.RateUsState.DISLIKED_WITH_MESSAGE);
			settings.NUMBER_OF_APP_STARTS_ON_DISLIKE_MOMENT.set(app.getAppInitializer().getNumberOfStarts());
			settings.LAST_DISPLAY_TIME.set(System.currentTimeMillis());
			Intent sendEmail = new Intent(Intent.ACTION_SENDTO);
			sendEmail.setData(Uri.parse("mailto:" + email));
			sendEmail.putExtra(Intent.EXTRA_EMAIL, email);
			startActivity(Intent.createChooser(sendEmail, getString(R.string.send_report)));
			dismiss();
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
