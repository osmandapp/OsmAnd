package net.osmand.plus.keyevent.commands;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.KeyEvent;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.utils.AndroidUtils;

public class OpenWunderLINQDatagridCommand extends KeyEventCommand {

	public static final String ID = "open_wunderlinq_datagrid";

	private static final String APP_PATH = "wunderlinq://datagrid";

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setData(Uri.parse(APP_PATH));
		AndroidUtils.startActivityIfSafe(requireMapActivity(), intent);
		return true;
	}

	@NonNull
	@Override
	public String toHumanString(@NonNull Context context) {
		return context.getString(R.string.key_event_action_open_wunderlinq_datagrid);
	}
}
