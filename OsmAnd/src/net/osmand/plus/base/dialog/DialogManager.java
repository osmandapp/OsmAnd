package net.osmand.plus.base.dialog;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.base.dialog.data.DisplayData;
import net.osmand.plus.base.dialog.data.DisplayItem;
import net.osmand.plus.base.dialog.interfaces.controller.IDialogController;
import net.osmand.plus.base.dialog.interfaces.controller.IDialogItemClicked;
import net.osmand.plus.base.dialog.interfaces.controller.IDialogItemSelected;
import net.osmand.plus.base.dialog.interfaces.controller.IDisplayDataProvider;
import net.osmand.plus.base.dialog.interfaces.controller.IOnDialogDismissed;
import net.osmand.plus.base.dialog.interfaces.controller.IDialogProgressChanged;
import net.osmand.plus.base.dialog.interfaces.dialog.IAskDismissDialog;
import net.osmand.plus.base.dialog.interfaces.dialog.IAskRefreshDialogCompletely;
import net.osmand.plus.base.dialog.interfaces.dialog.IDialog;

import java.util.HashMap;
import java.util.Map;

public class DialogManager {

	private final Map<String, IDialog> dialogs = new HashMap<>();
	private final Map<String, IDialogController> controllers = new HashMap<>();

	public void register(@NonNull String processId, @NonNull IDialog dialog) {
		dialogs.put(processId, dialog);
	}

	public void register(@NonNull String processId, @NonNull IDialogController controller) {
		controllers.put(processId, controller);
	}

	public void unregister(@NonNull String processId) {
		dialogs.remove(processId);
		controllers.remove(processId);
	}

	@Nullable
	public IDialogController findController(@NonNull String processId) {
		return controllers.get(processId);
	}

	@Nullable
	public IDialog findDialog(@NonNull String processId) {
		return dialogs.get(processId);
	}

	@Nullable
	public DisplayData getDisplayData(@NonNull String processId) {
		IDialogController controller = controllers.get(processId);
		if (controller instanceof IDisplayDataProvider) {
			return ((IDisplayDataProvider) controller).getDisplayData(processId);
		}
		return null;
	}

	public void onDialogDismissed(@NonNull String processId, @NonNull FragmentActivity activity) {
		IDialogController controller = controllers.get(processId);
		if (controller instanceof IOnDialogDismissed) {
			((IOnDialogDismissed) controller).onDialogDismissed(activity);
		}
	}

	public void onDialogItemSelected(@NonNull String processId, @NonNull DisplayItem item) {
		IDialogController controller = controllers.get(processId);
		if (controller instanceof IDialogItemSelected) {
			((IDialogItemSelected) controller).onDialogItemSelected(processId, item);
		}
	}

	public void onDialogItemClick(@NonNull String processId, @NonNull DisplayItem item) {
		IDialogController controller = controllers.get(processId);
		if (controller instanceof IDialogItemClicked) {
			((IDialogItemClicked) controller).onDialogItemClicked(processId, item);
		}
	}

	public void askDismissDialog(@NonNull String processId) {
		IDialog dialog = dialogs.get(processId);
		if (dialog instanceof IAskDismissDialog) {
			((IAskDismissDialog) dialog).onAskDismissDialog(processId);
		}
	}

	public void askRefreshDialogCompletely(@NonNull String processId) {
		IDialog dialog = dialogs.get(processId);
		if (dialog instanceof IAskRefreshDialogCompletely) {
			((IAskRefreshDialogCompletely) dialog).onAskRefreshDialogCompletely(processId);
		}
	}

	public void notifyOnProgress(@NonNull String progressTag, int progress) {
		for (IDialogController controller : controllers.values()) {
			if (controller instanceof IDialogProgressChanged callback) {
				callback.onDialogProgressChanged(progressTag, progress);
			}
		}
	}
}
