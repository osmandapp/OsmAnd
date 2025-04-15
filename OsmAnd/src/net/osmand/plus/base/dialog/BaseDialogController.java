package net.osmand.plus.base.dialog;

import android.content.Context;

import androidx.annotation.DimenRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.base.dialog.interfaces.controller.IDialogController;
import net.osmand.plus.base.dialog.interfaces.dialog.IDialog;
import net.osmand.plus.base.dialog.interfaces.dialog.IDialogNightModeInfoProvider;
import net.osmand.plus.base.dialog.interfaces.dialog.IContextDialog;

public abstract class BaseDialogController implements IDialogController {

	protected final OsmandApplication app;
	protected final DialogManager dialogManager;

	public BaseDialogController(@NonNull OsmandApplication app) {
		this.app = app;
		this.dialogManager = app.getDialogManager();
	}

	public void registerDialog(@NonNull IDialog dialog) {
		dialogManager.register(getProcessId(), dialog);
	}

	public boolean finishProcessIfNeeded(@Nullable FragmentActivity activity) {
		if (activity != null && !activity.isChangingConfigurations()) {
			dialogManager.unregister(getProcessId());
			return true;
		}
		return false;
	}

	@NonNull
	public abstract String getProcessId();

	@Nullable
	public FragmentActivity getActivity() {
		IDialog dialog = getDialog();
		if (dialog instanceof IContextDialog contextDialog) {
			return contextDialog.getActivity();
		}
		return null;
	}

	@Nullable
	public Context getContext() {
		IDialog dialog = getDialog();
		if (dialog instanceof IContextDialog contextDialog) {
			return contextDialog.getContext();
		}
		return null;
	}

	public boolean isNightMode() {
		IDialog dialog = getDialog();
		if (dialog instanceof IDialogNightModeInfoProvider nightModeInfoProvider) {
			return nightModeInfoProvider.isNightMode();
		}
		return false;
	}

	@Nullable
	public IDialog getDialog() {
		return dialogManager.findDialog(getProcessId());
	}

	public int getDimension(@DimenRes int id) {
		return app.getResources().getDimensionPixelSize(id);
	}

	@NonNull
	public String getString(@StringRes int stringId, Object... formatArgs) {
		return app.getString(stringId, formatArgs);
	}
}
