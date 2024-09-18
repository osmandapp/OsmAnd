package net.osmand.plus.feedback;

import android.view.View;
import android.widget.TextView;

import net.osmand.plus.R;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithCompoundButton;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.LongDescriptionItem;
import net.osmand.plus.widgets.TextViewEx;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class SendAnalyticsSearchableInfoProvider {

	private final List<BaseBottomSheetItem> items;

	public SendAnalyticsSearchableInfoProvider(final List<BaseBottomSheetItem> items) {
		this.items = items;
	}

	public String getSearchableInfo() {
		return String.join(", ", getTitle(), getButtonTitles(), getLongDescriptions());
	}

	private String getTitle() {
		final View titleView = items.get(0).getView();
		final CharSequence titleTop = titleView.<TextViewEx>findViewById(R.id.titleTop).getText();
		final CharSequence titleMiddle = titleView.<TextView>findViewById(R.id.titleMiddle).getText();
		final CharSequence titleBottom = titleView.<TextViewEx>findViewById(R.id.titleBottom).getText();
		return String.join(", ", titleTop, titleMiddle, titleBottom);
	}

	private String getButtonTitles() {
		return this
				.getButtons()
				.map(baseBottomSheetItem -> baseBottomSheetItem.getView().<TextViewEx>findViewById(R.id.title).getText())
				.collect(Collectors.joining(", "));
	}

	private Stream<BottomSheetItemWithCompoundButton> getButtons() {
		return items
				.stream()
				.filter(BottomSheetItemWithCompoundButton.class::isInstance)
				.map(BottomSheetItemWithCompoundButton.class::cast);
	}

	private String getLongDescriptions() {
		return this
				._getLongDescriptions()
				.map(BottomSheetItemWithDescription::getDescription)
				.collect(Collectors.joining(", "));
	}

	private Stream<LongDescriptionItem> _getLongDescriptions() {
		return items
				.stream()
				.filter(LongDescriptionItem.class::isInstance)
				.map(LongDescriptionItem.class::cast);
	}
}
