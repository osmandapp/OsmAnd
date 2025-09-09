package net.osmand.plus.settings.fragments.customizable;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.R;
import net.osmand.plus.base.BaseFullScreenDialogFragment;
import net.osmand.plus.base.dialog.BaseDialogController;
import net.osmand.plus.base.dialog.DialogManager;
import net.osmand.plus.base.dialog.data.DisplayData;
import net.osmand.plus.base.dialog.data.DisplayItem;
import net.osmand.plus.base.dialog.interfaces.dialog.IAskDismissDialog;
import net.osmand.plus.base.dialog.interfaces.dialog.IAskRefreshDialogCompletely;
import net.osmand.plus.base.dialog.interfaces.dialog.IDialog;
import net.osmand.plus.base.dialog.interfaces.dialog.IDialogNightModeInfoProvider;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.utils.ColorUtilities;

public abstract class CustomizableDialogFragment extends BaseFullScreenDialogFragment
		implements IDialog, IAskDismissDialog, IAskRefreshDialogCompletely, IDialogNightModeInfoProvider {

	private static final String PROCESS_ID_ATTR = "process_id";

	protected DialogManager manager;
	protected DisplayData displayData;
	protected String processId;

	public void setProcessId(@NonNull String processId) {
		this.processId = processId;
	}

	@Override
	protected int getThemeId() {
		return nightMode ? R.style.OsmandDarkTheme_DarkActionbar : R.style.OsmandLightTheme_DarkActionbar;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
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
		askUpdateContent();
	}

	protected void askUpdateContent() {
		View view = getView();
		if (view != null) {
			updateContent(view);
		}
	}

	protected abstract void updateContent(@NonNull View view);

	@Nullable
	protected BaseDialogController getController() {
		return (BaseDialogController) manager.findController(processId);
	}
}
