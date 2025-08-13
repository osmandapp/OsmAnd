package net.osmand.plus.download.ui;

import static net.osmand.IProgress.EMPTY_PROGRESS;
import static net.osmand.plus.download.local.OperationType.RESTORE_OPERATION;

import android.view.ViewGroup.LayoutParams;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmAndTaskManager;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.download.local.LocalItem;
import net.osmand.plus.download.local.LocalOperationTask;
import net.osmand.plus.download.local.LocalOperationTask.OperationListener;
import net.osmand.plus.download.local.OperationType;
import net.osmand.plus.utils.AndroidUtils;

public class ActivateMapToolbarController extends SuggestMapToolbarController {

	private final LocalItem localItem;

	public ActivateMapToolbarController(@NonNull MapActivity mapActivity,
	                                    @NonNull LocalItem localItem, @NonNull String regionName) {
		super(mapActivity, regionName);
		this.localItem = localItem;
		initializeUI();
	}

	@Override
	protected int getPrimaryTextPattern() {
		return R.string.suggest_activate_map_msg;
	}

	@NonNull
	@Override
	protected String getSecondaryText() {
		return app.getString(R.string.deactivated_map);
	}

	@Override
	protected int getIconId() {
		return R.drawable.ic_action_box_open_arrow_colored;
	}

	@Override
	protected int getPreferredIconHeight() {
		return LayoutParams.MATCH_PARENT;
	}

	@Override
	protected int getPreferredIconWidth() {
		return AndroidUtils.dpToPx(app, 72);
	}

	@NonNull
	@Override
	protected String getApplyButtonTitle() {
		return app.getString(R.string.local_index_mi_restore);
	}

	@Override
	protected void onApply() {
		LocalOperationTask task = new LocalOperationTask(app, RESTORE_OPERATION, new OperationListener() {
			@Override
			public void onOperationFinished(@NonNull OperationType type, @NonNull String result) {
				app.getResourceManager().reloadIndexesAsync(EMPTY_PROGRESS, warnings -> app.getOsmandMap().refreshMap());
			}
		});
		OsmAndTaskManager.executeTask(task, localItem);
		dismiss();
	}
}
