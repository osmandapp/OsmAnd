package net.osmand.plus.base.bottomsheetmenu.simpleitems;

import net.osmand.plus.R;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;

public class DescriptionItem extends BottomSheetItemWithDescription {

	public DescriptionItem(CharSequence description) {
		this.description = description;
		this.layoutId = R.layout.bottom_sheet_item_description;
	}
}
