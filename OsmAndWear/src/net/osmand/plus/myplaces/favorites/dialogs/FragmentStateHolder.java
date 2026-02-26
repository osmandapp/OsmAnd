package net.osmand.plus.myplaces.favorites.dialogs;

import android.os.Bundle;

public interface FragmentStateHolder {
	String ITEM_POSITION = "item_position";
	String GROUP_POSITION = "group_position";
	String GROUP_NAME_TO_SHOW = "group_name_to_show";

	Bundle storeState();

	void restoreState(Bundle bundle);
}
