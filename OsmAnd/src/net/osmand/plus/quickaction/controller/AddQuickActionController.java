package net.osmand.plus.quickaction.controller;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.base.dialog.DialogManager;
import net.osmand.plus.base.dialog.interfaces.controller.IDialogController;
import net.osmand.plus.quickaction.MapButtonsHelper;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;
import net.osmand.plus.views.mapwidgets.configure.buttons.QuickActionButtonState;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class AddQuickActionController implements IDialogController {

	public static final String PROCESS_ID = "add_quick_action";

	protected final OsmandApplication app;
	protected final DialogManager dialogManager;
	protected final MapButtonsHelper mapButtonsHelper;
	private final Set<String> boundDialogs = new HashSet<>();

	public AddQuickActionController(@NonNull OsmandApplication app) {
		this.app = app;
		this.dialogManager = app.getDialogManager();
		this.mapButtonsHelper = app.getMapButtonsHelper();
	}

	public void registerDialog(@NonNull String dialogTag) {
		boundDialogs.add(dialogTag);
	}

	public void unregisterDialog(@NonNull String dialogTag) {
		boundDialogs.remove(dialogTag);
		if (Algorithms.isEmpty(boundDialogs)) {
			dialogManager.unregister(PROCESS_ID);
		}
	}

	@NonNull
	public abstract QuickAction produceQuickAction(boolean isNew, int type, long actionId);

	@NonNull
	public Map<QuickActionType, List<QuickActionType>> getAdapterItems() {
		return mapButtonsHelper.produceTypeActionsListWithHeaders(getButtonState());
	}

	public abstract boolean isNameUnique(@NonNull QuickAction action);

	public abstract QuickAction generateUniqueActionName(@NonNull QuickAction action);

	@Nullable
	public QuickActionType getCategoryActionTypeFromId(int categoryTypeId) {
		return mapButtonsHelper.getCategoryActionTypeFromId(categoryTypeId);
	}

	@NonNull
	public List<QuickActionType> getCategoryTypes(@Nullable QuickActionType categoryAction) {
		List<QuickActionType> actionTypes = new ArrayList<>();
		if (categoryAction != null) {
			mapButtonsHelper.filterQuickActions(getButtonState(), categoryAction, actionTypes);
		}
		return actionTypes;
	}
	public abstract void askSaveAction(boolean isNew, @NonNull QuickAction action);

	public abstract void askRemoveAction(@NonNull QuickAction action);

	@Nullable
	protected abstract QuickActionButtonState getButtonState();

	public static void showAddQuickActionDialog() {

	}

	@Nullable
	public static AddQuickActionController getExistedInstance(@NonNull OsmandApplication app) {
		DialogManager dialogManager = app.getDialogManager();
		return (AddQuickActionController) dialogManager.findController(PROCESS_ID);
	}
}
