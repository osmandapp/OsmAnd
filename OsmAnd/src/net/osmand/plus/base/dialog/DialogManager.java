package net.osmand.plus.base.dialog;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.base.dialog.uidata.DialogDisplayData;
import net.osmand.plus.base.dialog.uidata.DialogDisplayItem;
import net.osmand.plus.base.dialog.interfaces.IDialogController;
import net.osmand.plus.base.dialog.interfaces.IDialogDisplayDataProvider;
import net.osmand.plus.base.dialog.interfaces.IDialogItemSelected;

import java.util.HashMap;
import java.util.Map;

public class DialogManager {

	private final Map<String, IDialogController> controllers = new HashMap<>();

	public void register(@NonNull String processId, @NonNull IDialogController controller) {
		controllers.put(processId, controller);
	}

	public void unregister(@NonNull String processId) {
		controllers.remove(processId);
	}

	@Nullable
	public IDialogController findController(@NonNull String processId) {
		return controllers.get(processId);
	}

	@Nullable
	public DialogDisplayData getDialogDisplayData(@NonNull String processId) {
		IDialogController controller = controllers.get(processId);
		if (controller instanceof IDialogDisplayDataProvider) {
			return ((IDialogDisplayDataProvider) controller).getDialogDisplayData(processId);
		}
		return null;
	}

	public void onDialogItemSelected(@NonNull String processId,
	                                 @NonNull DialogDisplayItem selectedItem) {
		IDialogController controller = controllers.get(processId);
		if (controller instanceof IDialogItemSelected) {
			((IDialogItemSelected) controller).onDialogItemSelected(processId, selectedItem);
		}
	}

}
