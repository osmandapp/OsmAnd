package net.osmand.plus.myplaces;

import android.os.Bundle;

public interface FavoritesFragmentStateHolder {
	String ITEM_POSITION = "item_position";
	String GROUP_POSITION = "group_position";
	String GROUP_NAME_TO_SHOW = "group_name_to_show";

	Bundle storeState();

	void restoreState(Bundle bundle);
}
