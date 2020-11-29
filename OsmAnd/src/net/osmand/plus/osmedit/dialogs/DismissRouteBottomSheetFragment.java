package net.osmand.plus.osmedit.dialogs;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerSpaceItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.ShortDescriptionItem;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.router.GeneralRouter;

import java.util.List;

public class DismissRouteBottomSheetFragment extends MenuBottomSheetDialogFragment {

	private OsmandApplication app;
	private OsmandSettings settings;

	public static final int REQUEST_CODE = 1001;

	public static final String TAG = DismissRouteBottomSheetFragment.class.getSimpleName();
	private DialogInterface.OnDismissListener dismissListener;

	public DismissRouteBottomSheetFragment() {
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		app = getMyApplication();
		if (app != null) {
			settings = app.getSettings();
		}

		items.add(new ShortDescriptionItem.Builder()
				.setDescription(getString(R.string.stop_routing_confirm))
				.setTitle(getString(R.string.cancel_route))
				.setLayoutId(R.layout.bottom_sheet_item_list_title_with_descr)
				.create());

		items.add(new DividerSpaceItem(getContext(),
				getResources().getDimensionPixelSize(R.dimen.content_padding_small)));

	}

	@Override
	protected boolean useVerticalButtons() {
		return false;
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_cancel;
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.shared_string_dismiss;
	}

	@Override
	protected UiUtilities.DialogButtonType getRightBottomButtonType() {
		return (UiUtilities.DialogButtonType.PRIMARY);
	}

	@Override
	public int getSecondDividerHeight() {
		return getResources().getDimensionPixelSize(R.dimen.bottom_sheet_icon_margin);
	}

	@Override
	protected void onRightBottomButtonClick() {
		stopNavigationWithoutConfirm();
		dismiss();
	}

	public void stopNavigationWithoutConfirm() {
		app.stopNavigation();
		getMapActivity().updateApplicationModeSettings();
		getMapActivity().getDashboard().clearDeletedPoints();
		List<ApplicationMode> modes = ApplicationMode.values(app);
		for (ApplicationMode mode : modes) {
			if (settings.FORCE_PRIVATE_ACCESS_ROUTING_ASKED.getModeValue(mode)) {
				settings.FORCE_PRIVATE_ACCESS_ROUTING_ASKED.setModeValue(mode, false);
				settings.getCustomRoutingBooleanProperty(GeneralRouter.ALLOW_PRIVATE, false).setModeValue(mode, false);
			}
		}
	}

	@Override
	public void onDismiss(@NonNull DialogInterface dialog) {
		super.onDismiss(dialog);
		if (dismissListener != null) {
			dismissListener.onDismiss(dialog);
		}
	}

	@Nullable
	public MapActivity getMapActivity() {
		Activity activity = getActivity();
		if (activity instanceof MapActivity) {
			return (MapActivity) activity;
		}
		return null;

	}

	public void setOnDismissListener(DialogInterface.OnDismissListener dismissListener) {
		this.dismissListener = dismissListener;
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager, @Nullable Fragment targetFragment, DialogInterface.OnDismissListener dismissListener) {
		if (!fragmentManager.isStateSaved()) {
			DismissRouteBottomSheetFragment fragment = new DismissRouteBottomSheetFragment();
			fragment.dismissListener = dismissListener;
			fragment.setTargetFragment(targetFragment, REQUEST_CODE);
			fragment.show(fragmentManager, TAG);
		}
	}
}

