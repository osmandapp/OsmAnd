package net.osmand.plus.quickaction.controller;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.quickaction.MapButtonsHelper;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.views.mapwidgets.configure.buttons.QuickActionButtonState;

import java.util.List;

public class AddMapQuickActionController extends AddQuickActionController {

	private final QuickActionButtonState buttonState;

	public AddMapQuickActionController(@NonNull OsmandApplication app,
	                                   @NonNull QuickActionButtonState buttonState) {
		super(app);
		this.buttonState = buttonState;
	}

	@Override
	@NonNull
	public QuickAction produceQuickAction(boolean isNew, int type, long actionId) {
		return MapButtonsHelper.produceAction(isNew ? mapButtonsHelper.newActionByType(type) : buttonState.getQuickAction(actionId));
	}

	@Override
	public boolean isNameUnique(@NonNull QuickAction action) {
		List<QuickAction> actions = buttonState.getQuickActions();
		return mapButtonsHelper.isActionNameUnique(actions, action);
	}

	@Override
	@NonNull
	public QuickAction generateUniqueActionName(@NonNull QuickAction action) {
		List<QuickAction> actions = buttonState.getQuickActions();
		return mapButtonsHelper.generateUniqueActionName(actions, action);
	}

	@Override
	public void askSaveAction(boolean isNew, @NonNull QuickAction action) {
		if (isNew) {
			mapButtonsHelper.addQuickAction(buttonState, action);
		} else {
			mapButtonsHelper.updateQuickAction(buttonState, action);
		}
	}

	@Override
	public void askRemoveAction(@NonNull QuickAction action) {
		mapButtonsHelper.deleteQuickAction(buttonState, action);
	}

	@Nullable
	@Override
	public QuickActionButtonState getButtonState() {
		return buttonState;
	}
}
