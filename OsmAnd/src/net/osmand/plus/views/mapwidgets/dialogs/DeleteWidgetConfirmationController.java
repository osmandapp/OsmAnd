package net.osmand.plus.views.mapwidgets.dialogs;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import net.osmand.OnCompleteCallback;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.dialog.BaseDialogController;
import net.osmand.plus.base.dialog.DialogManager;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.MapWidgetRegistry;

public class DeleteWidgetConfirmationController extends BaseDialogController {

	private static final String PROCESS_ID = "confirm_widget_removing";

	private final ApplicationMode appMode;
	private final MapWidgetInfo widgetInfo;
	private final MapWidgetRegistry widgetRegistry;

	private OnCompleteCallback onWidgetDeletedCallback;

	public DeleteWidgetConfirmationController(@NonNull OsmandApplication app,
											  @NonNull ApplicationMode appMode,
	                                          @NonNull MapWidgetInfo widgetInfo) {
		super(app);
		this.appMode = appMode;
		this.widgetRegistry = app.getOsmandMap().getMapLayers().getMapWidgetRegistry();
		this.widgetInfo = widgetInfo;
	}

	@NonNull
	@Override
	public String getProcessId() {
		return PROCESS_ID;
	}

	public void setListener(@Nullable OnCompleteCallback onWidgetDeletedCallback) {
		this.onWidgetDeletedCallback = onWidgetDeletedCallback;
	}

	public void onDeleteActionConfirmed(@NonNull MapActivity activity) {
		widgetRegistry.removeWidget(activity, appMode, widgetInfo);

		if (onWidgetDeletedCallback != null) {
			onWidgetDeletedCallback.onComplete();
		}
	}

	public static void askUpdateListener(@NonNull OsmandApplication app,
	                                     @Nullable OnCompleteCallback onWidgetDeletedCallback) {
		DeleteWidgetConfirmationController controller = getExistedInstance(app);
		if (controller != null) {
			controller.setListener(onWidgetDeletedCallback);
		}
	}

	@Nullable
	public static DeleteWidgetConfirmationController getExistedInstance(@NonNull OsmandApplication app) {
		return (DeleteWidgetConfirmationController) app.getDialogManager().findController(PROCESS_ID);
	}

	public static void showDialog(@NonNull FragmentActivity activity, @NonNull ApplicationMode appMode,
	                              @NonNull MapWidgetInfo widgetInfo, boolean usedOnMap,
	                              @Nullable OnCompleteCallback onWidgetDeletedCallback) {
		OsmandApplication app = (OsmandApplication) activity.getApplicationContext();
		DialogManager dialogManager = app.getDialogManager();

		DeleteWidgetConfirmationController controller = new DeleteWidgetConfirmationController(app, appMode, widgetInfo);
		controller.setListener(onWidgetDeletedCallback);

		dialogManager.register(PROCESS_ID, controller);
		DeleteWidgetConfirmationDialog.showInstance(activity, appMode, usedOnMap);
	}
}
