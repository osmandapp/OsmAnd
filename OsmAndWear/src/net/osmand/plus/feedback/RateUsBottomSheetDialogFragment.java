package net.osmand.plus.feedback;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.dialogs.DislikeOsmAndBottomSheetDialogFragment;
import net.osmand.plus.utils.AndroidUtils;

import org.apache.commons.logging.Log;

public class RateUsBottomSheetDialogFragment extends MenuBottomSheetDialogFragment {

	public static final String TAG = RateUsBottomSheetDialogFragment.class.getSimpleName();
	private static final Log LOG = PlatformUtil.getLog(SendAnalyticsBottomSheetDialogFragment.class);

	private RateUsHelper rateUsHelper;

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		Context context = getContext();
		if (context == null) {
			return;
		}

		rateUsHelper = new RateUsHelper();

		View titleView = View.inflate(new ContextThemeWrapper(context, themeRes), R.layout.rate_us_title, null);
		SimpleBottomSheetItem titleItem = (SimpleBottomSheetItem) new SimpleBottomSheetItem.Builder()
				.setCustomView(titleView)
				.create();
		items.add(titleItem);
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_no;
	}

	@Override
	protected void onDismissButtonClickAction() {
		FragmentManager fm = getFragmentManager();
		if (fm != null) {
			rateUsHelper.updateState(null);
			DislikeOsmAndBottomSheetDialogFragment.showInstance(fm);
		}
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.button_rate;
	}

	@Override
	protected void onRightBottomButtonClick() {
		OsmandApplication app = getMyApplication();
		if (app != null) {
			rateUsHelper.updateState(RateUsState.LIKED);
			Uri uri = Uri.parse(Version.getUrlWithUtmRef(app, app.getPackageName()));
			Intent intent = new Intent(Intent.ACTION_VIEW, uri);
			AndroidUtils.startActivityIfSafe(app, intent);
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
				RateUsBottomSheetDialogFragment fragment = new RateUsBottomSheetDialogFragment();
				fragment.show(fm, TAG);
			}
		} catch (RuntimeException e) {
			LOG.error("showInstance", e);
		}
	}

}
