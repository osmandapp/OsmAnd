package net.osmand.plus.keyevent.commands;

import android.view.KeyEvent;

import net.osmand.plus.activities.MapActivity.ShowQuickSearchMode;

public class OpenQuickSearchDialogCommand extends KeyEventCommand {

	public static final String ID = "open_quick_search_dialog";

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		requireMapActivity().showQuickSearch(ShowQuickSearchMode.NEW_IF_EXPIRED, false);
		return true;
	}
}
