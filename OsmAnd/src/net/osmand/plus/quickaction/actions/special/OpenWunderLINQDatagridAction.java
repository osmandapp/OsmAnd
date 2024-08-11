package net.osmand.plus.quickaction.actions.special;

import static net.osmand.plus.quickaction.QuickActionIds.OPEN_WUNDERLINQ_DATAGRID_ACTION;

import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.quickaction.QuickAction;
import net.osmand.plus.quickaction.QuickActionType;
import net.osmand.plus.utils.AndroidUtils;

public class OpenWunderLINQDatagridAction extends QuickAction {

	public static final QuickActionType TYPE = new QuickActionType(OPEN_WUNDERLINQ_DATAGRID_ACTION,
			"open.wunderlinq.datagrid", OpenWunderLINQDatagridAction.class)
			.nameRes(R.string.key_event_action_open_wunderlinq_datagrid)
			.iconRes(R.drawable.ic_action_settings)
			.nameActionRes(R.string.shared_string_open);

	private static final String APP_PATH = "wunderlinq://datagrid";

	public OpenWunderLINQDatagridAction() {
		super(TYPE);
	}

	public OpenWunderLINQDatagridAction(QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	public void execute(@NonNull MapActivity mapActivity) {
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setData(Uri.parse(APP_PATH));
		AndroidUtils.startActivityIfSafe(mapActivity, intent);
	}
}
