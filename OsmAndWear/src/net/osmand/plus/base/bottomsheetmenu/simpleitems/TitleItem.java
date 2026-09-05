package net.osmand.plus.base.bottomsheetmenu.simpleitems;

import android.content.Context;
import android.view.ViewGroup;

import androidx.annotation.ColorRes;

import net.osmand.plus.R;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;

public class TitleItem extends SimpleBottomSheetItem {

	public TitleItem(CharSequence title) {
		this.title = title;
		this.layoutId = R.layout.bottom_sheet_item_title;
	}

	public TitleItem(CharSequence title, @ColorRes int titleColorId) {
		this(title);
		this.titleColorId = titleColorId;
	}

	@Override
	public void inflate(Context context, ViewGroup container, boolean nightMode) {
		if (titleColorId == INVALID_ID) {
			titleColorId = nightMode ? R.color.text_color_primary_dark : INVALID_ID;
		}
		super.inflate(context, container, nightMode);
	}
}
