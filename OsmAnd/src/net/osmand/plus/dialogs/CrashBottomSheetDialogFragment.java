package net.osmand.plus.dialogs;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.FRAGMENT_CRASH_ID;

import android.content.Context;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import net.osmand.PlatformUtil;
import net.osmand.aidlapi.OsmAndCustomizationConstants;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.settings.backend.OsmandSettings;

import org.apache.commons.logging.Log;

public class CrashBottomSheetDialogFragment extends MenuBottomSheetDialogFragment {

	private static final String TAG = OsmAndCustomizationConstants.FRAGMENT_CRASH_ID;
	private static final Log LOG = PlatformUtil.getLog(CrashBottomSheetDialogFragment.class);

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		Context context = getContext();
		if (context == null) {
			return;
		}

		View titleView = View.inflate(new ContextThemeWrapper(context, themeRes), R.layout.crash_title, null);
		SimpleBottomSheetItem titleItem = (SimpleBottomSheetItem) new SimpleBottomSheetItem.Builder()
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

	public static boolean shouldShow(@Nullable OsmandSettings settings, @NonNull MapActivity activity) {
		OsmandApplication app = activity.getMyApplication();
		if (app.getAppCustomization().isFeatureEnabled(FRAGMENT_CRASH_ID)) {
			return app.getAppInitializer().checkPreviousRunsForExceptions(activity, settings != null);
		}
		return false;
	}

	public static void showInstance(@NonNull FragmentManager fm) {
		try {
			if (fm.findFragmentByTag(TAG) == null) {
				CrashBottomSheetDialogFragment fragment = new CrashBottomSheetDialogFragment();
				fragment.show(fm, TAG);
			}
		} catch (RuntimeException e) {
			LOG.error("showInstance", e);
		}
	}
}
