package net.osmand.plus.base.bottomsheetmenu.simpleitems;

import net.osmand.plus.R;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;

public class LongDescriptionStandardHeightItem extends BottomSheetItemWithDescription {

	public LongDescriptionStandardHeightItem(CharSequence description) {
		this.description = description;
		this.layoutId =R.layout.bottom_sheet_item_description_long_without_min_height;
	}
}
