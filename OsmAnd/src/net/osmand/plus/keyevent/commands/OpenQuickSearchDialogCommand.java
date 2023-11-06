package net.osmand.plus.keyevent.commands;

import android.content.Context;
import android.view.KeyEvent;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.search.ShowQuickSearchMode;

public class OpenQuickSearchDialogCommand extends KeyEventCommand {

	public static final String ID = "open_quick_search_dialog";

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		requireMapActivity().getFragmentsHelper().showQuickSearch(ShowQuickSearchMode.NEW_IF_EXPIRED, false);
		return true;
	}

	@NonNull
	@Override
	public String toHumanString(@NonNull Context context) {
		return context.getString(R.string.key_event_action_open_search_view);
	}
}
