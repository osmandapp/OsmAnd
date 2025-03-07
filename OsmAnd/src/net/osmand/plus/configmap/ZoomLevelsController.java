package net.osmand.plus.configmap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.containers.Limits;
import net.osmand.plus.base.dialog.BaseDialogController;
import net.osmand.plus.base.dialog.DialogManager;

public abstract class ZoomLevelsController extends BaseDialogController {

	private static final String PROCESS_ID = "select_zoom_levels";

	protected Limits selectedLimits;
	protected final Limits initialLimits;
	protected final Limits supportedLimits;

	public ZoomLevelsController(@NonNull OsmandApplication app,
	                            @NonNull Limits initialLimits) {
		this(app, initialLimits, initialLimits);
	}

	public ZoomLevelsController(@NonNull OsmandApplication app,
	                            @NonNull Limits initialLimits,
	                            @NonNull Limits supportedLimits) {
		super(app);
		this.initialLimits = initialLimits;
		this.selectedLimits = new Limits(initialLimits.min(), initialLimits.max());
		this.supportedLimits = supportedLimits;
	}

	@NonNull
	@Override
	public String getProcessId() {
		return PROCESS_ID;
	}

	public abstract void onBackPressed(@NonNull MapActivity mapActivity);

	@NonNull
	public abstract String getDialogTitle();

	@NonNull
	public abstract String getDialogSummary();

	public abstract void onApplyChanges();

	public abstract void onResetToDefault();

	public boolean hasChanges() {
		return !selectedLimits.equals(initialLimits);
	}

	@NonNull
	public Limits getSupportedLimits() {
		return supportedLimits;
	}

	@NonNull
	public Limits getSelectedLimits() {
		return selectedLimits;
	}

	public void setSelectedLimits(float min, float max) {
		float clampedMin = Math.max(supportedLimits.min(), Math.min(supportedLimits.max(), min));
		float clampedMax = Math.max(supportedLimits.min(), Math.min(supportedLimits.max(), max));
		selectedLimits = new Limits(clampedMin, clampedMax);
	}

	@Nullable
	public static ZoomLevelsController getExistedInstance(@NonNull OsmandApplication app) {
		DialogManager dialogManager = app.getDialogManager();
		return (ZoomLevelsController) dialogManager.findController(PROCESS_ID);
	}
}
