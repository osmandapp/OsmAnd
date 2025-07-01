package net.osmand.plus.feedback;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.FRAGMENT_RENDER_INIT_ERROR_ID;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.LongDescriptionItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.widgets.dialogbutton.DialogButtonType;

public class RenderInitErrorBottomSheet extends MenuBottomSheetDialogFragment {

	private static final String TAG = FRAGMENT_RENDER_INIT_ERROR_ID;

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		items.add(new TitleItem(getString(R.string.rendering_engine_failed)));
		items.add(new LongDescriptionItem(getString(R.string.rendering_engine_failed_descr)));
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.share_crash_log;
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_close;
	}

	@Override
	protected DialogButtonType getRightBottomButtonType() {
		return DialogButtonType.SECONDARY;
	}

	@Override
	protected boolean useVerticalButtons() {
		return true;
	}

	@Override
	protected void onRightBottomButtonClick() {
		app.getFeedbackHelper().sendCrashLog();
		dismiss();
	}

	public static boolean shouldShow(@NonNull OsmandApplication app) {
		OsmandSettings settings = app.getSettings();
		if (app.getAppCustomization().isFeatureEnabled(FRAGMENT_RENDER_INIT_ERROR_ID)) {
			return settings.USE_OPENGL_RENDER.get() && settings.OPENGL_RENDER_FAILED.get() > 3;
		}
		return true;
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			RenderInitErrorBottomSheet fragment = new RenderInitErrorBottomSheet();
			fragment.show(fragmentManager, TAG);
		}
	}
}

