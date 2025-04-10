package net.osmand.plus.configmap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.math.MathUtils;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.containers.Limits;
import net.osmand.plus.base.dialog.BaseDialogController;
import net.osmand.plus.base.dialog.DialogManager;

public abstract class ZoomLevelsController extends BaseDialogController {

	private static final String PROCESS_ID = "select_zoom_levels";

	protected final Limits<Integer> supportedLimits;
	protected final Limits<Integer> initialLimits;
	protected Limits<Integer> selectedLimits;

	public ZoomLevelsController(@NonNull OsmandApplication app,
	                            @NonNull Limits<Integer> initialLimits,
	                            @NonNull Limits<Integer> supportedLimits) {
		super(app);
		this.initialLimits = initialLimits;
		this.supportedLimits = supportedLimits;
		int min = MathUtils.clamp(initialLimits.min(), supportedLimits.min(), supportedLimits.max());
		int max = MathUtils.clamp(initialLimits.max(), supportedLimits.min(), supportedLimits.max());
		this.selectedLimits = new Limits<>(min, max);
	}

	@NonNull
	@Override
	public String getProcessId() {
		return PROCESS_ID;
	}

	public abstract void onCloseScreen(@NonNull MapActivity activity);

	@NonNull
	public String getDialogTitle() {
		return getString(R.string.shared_string_zoom_levels);
	}

	public abstract void onApplyChanges();

	public abstract void onResetToDefault();

	public boolean hasChanges() {
		return !selectedLimits.equals(initialLimits);
	}

	@NonNull
	public Limits<Integer> getSupportedLimits() {
		return supportedLimits;
	}

	@NonNull
	public Limits<Integer> getSelectedLimits() {
		return selectedLimits;
	}

	public void setSelectedLimits(float min, float max) {
		int clampedMin = (int) Math.max(supportedLimits.min(), Math.min(supportedLimits.max(), min));
		int clampedMax = (int) Math.max(supportedLimits.min(), Math.min(supportedLimits.max(), max));
		selectedLimits = new Limits<>(clampedMin, clampedMax);
		setSavedZoomLimits(selectedLimits);
	}

	protected abstract void setSavedZoomLimits(@NonNull Limits<Integer> limits);

	@NonNull
	protected abstract Limits<Integer> getSavedZoomLimits();

	@Nullable
	public static ZoomLevelsController getExistedInstance(@NonNull OsmandApplication app) {
		DialogManager dialogManager = app.getDialogManager();
		return (ZoomLevelsController) dialogManager.findController(PROCESS_ID);
	}
}
