package net.osmand.plus.base.dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.dialog.data.DisplayData;
import net.osmand.plus.base.dialog.data.DisplayItem;
import net.osmand.plus.base.dialog.interfaces.controller.IDialogController;
import net.osmand.plus.base.dialog.interfaces.controller.IDialogItemClicked;
import net.osmand.plus.base.dialog.interfaces.controller.IDialogItemSelected;
import net.osmand.plus.base.dialog.interfaces.controller.IDisplayDataProvider;
import net.osmand.plus.base.dialog.interfaces.dialog.IAskDismissDialog;
import net.osmand.plus.base.dialog.interfaces.dialog.IAskRefreshDialogCompletely;
import net.osmand.plus.base.dialog.interfaces.dialog.IDialog;
import net.osmand.plus.myplaces.tracks.filters.BaseTrackFilter;
import net.osmand.plus.myplaces.tracks.filters.SmartFolderHelper;
import net.osmand.plus.utils.UiUtilities;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import studio.carbonylgroup.textfieldboxes.ExtendedEditText;

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

	public void showSaveSmartFolderDialog(@NonNull Activity activity, boolean nightMode, @Nullable List<BaseTrackFilter> filters) {
		OsmandApplication app = (OsmandApplication) activity.getApplication();
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		LayoutInflater themedInflater = UiUtilities.getInflater(activity, nightMode);
		View customLayout = themedInflater.inflate(R.layout.dialog_save_smart_folder, null);
		builder.setView(customLayout);
		AlertDialog dialog = builder.create();
		dialog.show();
		customLayout.findViewById(R.id.cancel_button).setOnClickListener(v -> {
			dialog.dismiss();
		});
		customLayout.findViewById(R.id.save_button).setOnClickListener(v -> {
			ExtendedEditText input = customLayout.findViewById(R.id.name_input);
			String newSmartFolderName = input.getText().toString().trim();
			SmartFolderHelper smartFolderHelper = app.getSmartFolderHelper();
			if (smartFolderHelper.isSmartFolderPresent(newSmartFolderName)) {
				Toast.makeText(app, R.string.smart_folder_name_present, Toast.LENGTH_SHORT).show();
			} else {
				smartFolderHelper.saveNewSmartFolder(newSmartFolderName, filters);
				dialog.dismiss();
			}
		});
	}
}
