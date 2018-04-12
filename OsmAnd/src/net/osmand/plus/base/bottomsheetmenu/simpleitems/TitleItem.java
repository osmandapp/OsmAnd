package net.osmand.plus.base.bottomsheetmenu.simpleitems;

import android.view.ViewGroup;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;

public class TitleItem extends SimpleBottomSheetItem {

	public TitleItem(String title) {
		this.title = title;
		this.layoutId = R.layout.bottom_sheet_item_title;
	}

	@Override
	public void inflate(OsmandApplication app, ViewGroup container, boolean nightMode) {
		titleColorId = nightMode ? R.color.ctx_menu_info_text_dark : INVALID_ID;
		super.inflate(app, container, nightMode);
	}
}
