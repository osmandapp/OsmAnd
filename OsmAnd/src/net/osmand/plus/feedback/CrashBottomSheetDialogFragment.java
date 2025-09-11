package net.osmand.plus.feedback;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.FRAGMENT_CRASH_ID;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import net.osmand.aidlapi.OsmAndCustomizationConstants;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.AndroidUtils;

public class CrashBottomSheetDialogFragment extends MenuBottomSheetDialogFragment {

	private static final String TAG = OsmAndCustomizationConstants.FRAGMENT_CRASH_ID;

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		View titleView = inflate(R.layout.crash_title);
		items.add(new SimpleBottomSheetItem.Builder().setCustomView(titleView).create());
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.shared_string_send;
	}

	@Override
	protected void onRightBottomButtonClick() {
		app.getFeedbackHelper().sendCrashLog();
		dismiss();
	}

	public static boolean shouldShow(@Nullable OsmandSettings settings, @NonNull MapActivity activity) {
		OsmandApplication app = activity.getApp();
		if (app.getAppCustomization().isFeatureEnabled(FRAGMENT_CRASH_ID)) {
			return app.getAppInitializer().checkPreviousRunsForExceptions(activity, settings != null);
		}
		return false;
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			CrashBottomSheetDialogFragment fragment = new CrashBottomSheetDialogFragment();
			fragment.show(fragmentManager, TAG);
		}
	}
}
