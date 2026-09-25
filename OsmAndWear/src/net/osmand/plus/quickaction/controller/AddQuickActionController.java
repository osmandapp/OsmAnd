package net.osmand.plus.quickaction.controller;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.base.dialog.DialogManager;
import net.osmand.plus.base.dialog.interfaces.controller.IDialogController;
import net.osmand.plus.base.dialog.interfaces.dialog.IAskDismissDialog;
import net.osmand.plus.base.dialog.interfaces.dialog.IDialog;
import net.osmand.plus.quickaction.AddQuickActionFragment;
import net.osmand.plus.quickaction.CreateEditActionDialog;
import net.osmand.plus.quickaction.MapButtonsHelper;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;
import net.osmand.plus.views.mapwidgets.configure.buttons.QuickActionButtonState;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class AddQuickActionController implements IDialogController {

	public static final String PROCESS_ID = "add_quick_action";

	protected final OsmandApplication app;
	protected final DialogManager dialogManager;
	protected final MapButtonsHelper mapButtonsHelper;
	private final Map<String, IDialog> boundDialogs = new LinkedHashMap<>();

	public AddQuickActionController(@NonNull OsmandApplication app) {
		this.app = app;
		this.dialogManager = app.getDialogManager();
		this.mapButtonsHelper = app.getMapButtonsHelper();
	}

	public void registerDialog(@NonNull String dialogTag, @NonNull IDialog dialog) {
		boundDialogs.put(dialogTag, dialog);
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

	public void onAskSaveAction(boolean isNew, @NonNull QuickAction action) {
		askSaveAction(isNew, action);
		finishProcess();
	}

	public abstract void askSaveAction(boolean isNew, @NonNull QuickAction action);

	private void finishProcess() {
		for (IDialog dialog : getBoundDialogs()) {
			if (dialog instanceof IAskDismissDialog) {
				((IAskDismissDialog) dialog).onAskDismissDialog(PROCESS_ID);
			}
		}
	}

	@NonNull
	private List<IDialog> getBoundDialogs() {
		return new ArrayList<>(boundDialogs.values());
	}

	public abstract void askRemoveAction(@NonNull QuickAction action);

	@Nullable
	protected QuickActionButtonState getButtonState() {
		return null;
	}

	public static void showAddQuickActionDialog(@NonNull OsmandApplication app,
												@NonNull FragmentManager fragmentManager,
	                                            @NonNull QuickActionButtonState buttonState) {
		registerControllerIfNotExists(app, buttonState);
		AddQuickActionFragment.showInstance(fragmentManager);
	}

	public static void showAddQuickActionDialog(@NonNull OsmandApplication app,
	                                            @NonNull FragmentManager fragmentManager,
	                                            @NonNull AddQuickActionController controller) {
		DialogManager dialogManager = app.getDialogManager();
		dialogManager.register(PROCESS_ID, controller);
		AddQuickActionFragment.showInstance(fragmentManager);
	}

	public static void showCreateEditActionDialog(@NonNull OsmandApplication app,
	                                              @NonNull FragmentManager fragmentManager,
	                                              @NonNull QuickActionButtonState buttonState,
	                                              @NonNull QuickAction action) {
		registerControllerIfNotExists(app, buttonState);
		CreateEditActionDialog.showInstance(fragmentManager, action);
	}

	public static void showCreateEditActionDialog(@NonNull OsmandApplication app,
	                                              @NonNull FragmentManager fragmentManager,
	                                              @NonNull AddQuickActionController controller,
	                                              @NonNull QuickAction action) {
		DialogManager dialogManager = app.getDialogManager();
		dialogManager.register(PROCESS_ID, controller);
		CreateEditActionDialog.showInstance(fragmentManager, action);
	}

	private static void registerControllerIfNotExists(@NonNull OsmandApplication app,
	                                                  @NonNull QuickActionButtonState buttonState) {
		DialogManager dialogManager = app.getDialogManager();
		if (dialogManager.findController(PROCESS_ID) == null) {
			dialogManager.register(PROCESS_ID, new AddMapQuickActionController(app, buttonState));
		}
	}

	@Nullable
	public static AddQuickActionController getExistedInstance(@NonNull OsmandApplication app) {
		DialogManager dialogManager = app.getDialogManager();
		return (AddQuickActionController) dialogManager.findController(PROCESS_ID);
	}
}
