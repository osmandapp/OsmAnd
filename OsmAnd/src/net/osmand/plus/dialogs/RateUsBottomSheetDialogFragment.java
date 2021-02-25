package net.osmand.plus.dialogs;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.View;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.helpers.RateUsHelper;
import net.osmand.plus.helpers.RateUsHelper.RateUsState;

import org.apache.commons.logging.Log;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

public class RateUsBottomSheetDialogFragment extends MenuBottomSheetDialogFragment {
	public static final String TAG = "RateUsBottomSheetDialogFragment";
	private static final Log LOG = PlatformUtil.getLog(SendAnalyticsBottomSheetDialogFragment.class);

	private RateUsHelper rateUsHelper;

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		Context context = getContext();
		if (context == null) {
			return;
		}

		rateUsHelper = new RateUsHelper();

		final View titleView = View.inflate(new ContextThemeWrapper(context, themeRes), R.layout.rate_us_title, null);
		final SimpleBottomSheetItem titleItem = (SimpleBottomSheetItem) new SimpleBottomSheetItem.Builder()
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
			try {
				Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
				startActivity(goToMarket);
			} catch (Exception e) {
				LOG.error(e.getMessage(), e);
			}
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
			if (fm.findFragmentByTag(RateUsBottomSheetDialogFragment.TAG) == null) {
				RateUsBottomSheetDialogFragment fragment = new RateUsBottomSheetDialogFragment();
				fragment.show(fm, RateUsBottomSheetDialogFragment.TAG);
			}
		} catch (RuntimeException e) {
			LOG.error("showInstance", e);
		}
	}

}
