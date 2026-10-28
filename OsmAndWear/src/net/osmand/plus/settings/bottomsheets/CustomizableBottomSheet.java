package net.osmand.plus.settings.bottomsheets;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerItem;
import net.osmand.plus.base.dialog.DialogManager;
import net.osmand.plus.base.dialog.interfaces.dialog.IAskDismissDialog;
import net.osmand.plus.base.dialog.interfaces.dialog.IAskRefreshDialogCompletely;
import net.osmand.plus.base.dialog.interfaces.dialog.IDialog;
import net.osmand.plus.base.dialog.data.DisplayData;
import net.osmand.plus.base.dialog.data.DisplayItem;
import net.osmand.plus.base.dialog.interfaces.dialog.IDialogNightModeInfoProvider;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;

public abstract class CustomizableBottomSheet extends MenuBottomSheetDialogFragment
		implements IDialog, IAskDismissDialog, IAskRefreshDialogCompletely, IDialogNightModeInfoProvider {

	private static final String PROCESS_ID_ATTR = "process_id";

	protected OsmandApplication app;
	protected DialogManager manager;
	protected DisplayData displayData;
	protected String processId;

	public void setProcessId(@NonNull String processId) {
		this.processId = processId;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.app = requiredMyApplication();
		this.manager = app.getDialogManager();
		if (savedInstanceState != null) {
			processId = savedInstanceState.getString(PROCESS_ID_ATTR);
		}
		if (processId != null) {
			manager.register(processId, this);
			refreshDisplayData();
		}
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(PROCESS_ID_ATTR, processId);
	}

	@Override
	public void onDismiss(@NonNull DialogInterface dialog) {
		super.onDismiss(dialog);
		FragmentActivity activity = getActivity();
		if (activity != null && !activity.isChangingConfigurations()) {
			manager.onDialogDismissed(processId, activity);
			// Automatically unregister controller when close the dialog
			// to avoid any possible memory leaks
			manager.unregister(processId);
		}
	}

	private void refreshDisplayData() {
		displayData = manager.getDisplayData(processId);
	}

	protected void onItemClicked(@NonNull DisplayItem item) {
		manager.onDialogItemClick(processId, item);
	}

	protected void onItemSelected(@NonNull DisplayItem item) {
		manager.onDialogItemSelected(processId, item);
	}

	@Override
	public void onAskDismissDialog(@NonNull String processId) {
		dismiss();
	}

	@Override
	public void onAskRefreshDialogCompletely(@NonNull String processId) {
		refreshDisplayData();
		updateMenuItems();
	}

	@NonNull
	public ColorStateList createCompoundButtonTintList(@NonNull DisplayItem displayItem) {
		int controlsColor = displayData.getControlsColor(app, displayItem, nightMode);
		int defaultColor = ColorUtilities.getDefaultIconColor(app, nightMode);
		return AndroidUtils.createCheckedColorIntStateList(defaultColor, controlsColor);
	}

	@Nullable
	public Drawable createSelectableBackground(@NonNull DisplayItem displayItem) {
		Integer color = displayData.getBackgroundColor(displayItem);
		if (color != null) {
			return UiUtilities.getColoredSelectableDrawable(app, color);
		}
		return null;
	}

	@Nullable
	public DividerItem createDividerIfNeeded(@NonNull Context ctx, @NonNull DisplayItem displayItem) {
		if (displayItem.shouldShowBottomDivider()) {
			DividerItem divider = new DividerItem(ctx);
			divider.setMargins(displayItem.getDividerStartPadding(), 0, 0, 0);
			return divider;
		}
		return null;
	}

	@Override
	public boolean isNightMode() {
		return super.isNightMode(app);
	}
}
