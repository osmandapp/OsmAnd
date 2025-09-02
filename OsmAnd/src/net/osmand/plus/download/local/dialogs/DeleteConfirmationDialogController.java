package net.osmand.plus.download.local.dialogs;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.base.dialog.BaseDialogController;
import net.osmand.plus.base.dialog.DialogManager;
import net.osmand.plus.download.local.BaseLocalItem;

public class DeleteConfirmationDialogController extends BaseDialogController {

	private static final String PROCESS_ID = "delete_confirmation";

	private final BaseLocalItem localItem;
	private ConfirmDeletionListener listener;

	public DeleteConfirmationDialogController(@NonNull OsmandApplication app,
	                                          @NonNull BaseLocalItem localItem) {
		super(app);
		this.localItem = localItem;
	}

	public void setListener(@NonNull ConfirmDeletionListener listener) {
		this.listener = listener;
	}

	@NonNull
	@Override
	public String getProcessId() {
		return PROCESS_ID;
	}

	@NonNull
	public CharSequence getItemName() {
		return localItem.getName(app);
	}

	public void onDeleteConfirmed() {
		listener.onDeletionConfirmed(localItem);
	}

	public static void askUpdateListener(@NonNull OsmandApplication app,
	                                     @NonNull ConfirmDeletionListener listener) {
		DeleteConfirmationDialogController controller = getExistedInstance(app);
		if (controller != null) controller.setListener(listener);
	}

	@Nullable
	public static DeleteConfirmationDialogController getExistedInstance(@NonNull OsmandApplication app) {
		return (DeleteConfirmationDialogController) app.getDialogManager().findController(PROCESS_ID);
	}

	public static void showDialog(@NonNull OsmandApplication app,
	                              @NonNull FragmentManager manager,
	                              @NonNull BaseLocalItem localItem,
	                              @NonNull ConfirmDeletionListener listener) {
		DeleteConfirmationDialogController controller =
				new DeleteConfirmationDialogController(app, localItem);
		controller.setListener(listener);

		DialogManager dialogManager = app.getDialogManager();
		dialogManager.register(PROCESS_ID, controller);

		DeleteConfirmationBottomSheet.showInstance(manager);
	}

	public interface ConfirmDeletionListener {
		void onDeletionConfirmed(@NonNull BaseLocalItem localItem);
	}
}
