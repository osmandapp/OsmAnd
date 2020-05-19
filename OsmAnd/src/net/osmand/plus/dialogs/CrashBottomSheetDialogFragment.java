package net.osmand.plus.dialogs;

import android.content.Context;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;

import org.apache.commons.logging.Log;

public class CrashBottomSheetDialogFragment extends MenuBottomSheetDialogFragment {
	public static final String TAG = "CrashBottomSheetDialogFragment";
	private static final Log LOG = PlatformUtil.getLog(CrashBottomSheetDialogFragment.class);

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		Context context = getContext();
		if (context == null) {
			return;
		}

		final View titleView = View.inflate(new ContextThemeWrapper(context, themeRes), R.layout.crash_title, null);
		final SimpleBottomSheetItem titleItem = (SimpleBottomSheetItem) new SimpleBottomSheetItem.Builder()
				.setCustomView(titleView)
				.create();
		items.add(titleItem);
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.shared_string_send;
	}

	@Override
	protected void onRightBottomButtonClick() {
		OsmandApplication app = getMyApplication();
		if (app != null) {
			app.sendCrashLog();
		}
		dismiss();
	}

	public static boolean shouldShow(OsmandSettings settings, MapActivity activity) {
		return activity.getMyApplication().getAppInitializer()
				.checkPreviousRunsForExceptions(activity, settings != null);
	}

	public static void showInstance(@NonNull FragmentManager fm) {
		try {
			if (fm.findFragmentByTag(CrashBottomSheetDialogFragment.TAG) == null) {
				CrashBottomSheetDialogFragment fragment = new CrashBottomSheetDialogFragment();
				fragment.show(fm, CrashBottomSheetDialogFragment.TAG);
			}
		} catch (RuntimeException e) {
			LOG.error("showInstance", e);
		}
	}
}
