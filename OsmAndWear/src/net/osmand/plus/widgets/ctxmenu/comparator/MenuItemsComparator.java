package net.osmand.plus.widgets.ctxmenu.comparator;

import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem;

import java.util.Comparator;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.APP_PROFILES_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_CONFIGURE_PROFILE_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_SWITCH_PROFILE_ID;

public class MenuItemsComparator implements Comparator<ContextMenuItem> {

	@Override
	public int compare(ContextMenuItem i1, ContextMenuItem i2) {
		int result = compareSpecialItems(i1, i2);
		return result != 0 ? result : Integer.compare(i1.getOrder(), i2.getOrder());
	}

	private int compareSpecialItems(ContextMenuItem i1, ContextMenuItem i2) {
		if (DRAWER_CONFIGURE_PROFILE_ID.equals(i1.getId())
				&& DRAWER_SWITCH_PROFILE_ID.equals(i2.getId())) {
			return 1;
		} else if (DRAWER_SWITCH_PROFILE_ID.equals(i1.getId())
				&& DRAWER_CONFIGURE_PROFILE_ID.equals(i2.getId())) {
			return -1;
		} else if (DRAWER_SWITCH_PROFILE_ID.equals(i1.getId())
				|| DRAWER_CONFIGURE_PROFILE_ID.equals(i1.getId())) {
			return -1;
		} else if (DRAWER_SWITCH_PROFILE_ID.equals(i2.getId())
				|| DRAWER_CONFIGURE_PROFILE_ID.equals(i2.getId())) {
			return 1;
		} else if (APP_PROFILES_ID.equals(i1.getId())) {
			return -1;
		} else if (APP_PROFILES_ID.equals(i2.getId())) {
			return 1;
		}
		return 0;
	}

}
