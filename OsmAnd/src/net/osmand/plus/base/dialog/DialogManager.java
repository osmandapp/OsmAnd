package net.osmand.plus.base.dialog;

import android.content.DialogInterface;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.dialog.data.DisplayData;
import net.osmand.plus.base.dialog.data.DisplayItem;
import net.osmand.plus.base.dialog.interfaces.controller.IDialogController;
import net.osmand.plus.base.dialog.interfaces.controller.IDialogItemClicked;
import net.osmand.plus.base.dialog.interfaces.controller.IDialogItemSelected;
import net.osmand.plus.base.dialog.interfaces.controller.IDisplayDataProvider;
import net.osmand.plus.base.dialog.interfaces.controller.IOnDialogDismissed;
import net.osmand.plus.base.dialog.interfaces.dialog.IAskDismissDialog;
import net.osmand.plus.base.dialog.interfaces.dialog.IAskRefreshDialogCompletely;
import net.osmand.plus.base.dialog.interfaces.dialog.IDialog;
import net.osmand.shared.gpx.SmartFolderHelper;
import net.osmand.plus.widgets.alert.AlertDialogData;
import net.osmand.plus.widgets.alert.AlertDialogExtra;
import net.osmand.plus.widgets.alert.CustomAlert;
import net.osmand.shared.gpx.filters.BaseTrackFilter;
import net.osmand.util.Algorithms;

import java.util.HashMap;
import java.util.List;
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

	public void showSaveSmartFolderDialog(@NonNull FragmentActivity activity,
	                                      boolean nightMode,
	                                      @Nullable List<BaseTrackFilter> filters) {
		OsmandApplication app = (OsmandApplication) activity.getApplication();
		int titleResId = filters == null ? R.string.add_smart_folder : R.string.save_as_smart_folder;
		AlertDialogData dialogData = new AlertDialogData(activity, nightMode)
				.setTitle(titleResId)
				.setNegativeButton(R.string.shared_string_cancel, null);
		dialogData.setPositiveButton(R.string.shared_string_save, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				Object extra = dialogData.getExtra(AlertDialogExtra.EDIT_TEXT);
				if (extra instanceof EditText) {
					String newSmartFolderName = ((EditText) extra).getText().toString();
					if (Algorithms.isBlank(newSmartFolderName)) {
						app.showToastMessage(R.string.empty_name);
					} else {
						SmartFolderHelper smartFolderHelper = app.getSmartFolderHelper();
						if (smartFolderHelper.isSmartFolderPresent(newSmartFolderName)) {
							Toast.makeText(app, R.string.smart_folder_name_present, Toast.LENGTH_SHORT).show();
						} else {
							smartFolderHelper.saveNewSmartFolder(newSmartFolderName, filters);
							dialog.dismiss();
						}
					}
				}
			}
		});
		String caption = activity.getString(R.string.enter_new_name);
		CustomAlert.showInput(dialogData, activity, null, caption);
	}
}
