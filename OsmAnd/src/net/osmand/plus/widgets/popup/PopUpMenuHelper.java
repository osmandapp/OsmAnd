package net.osmand.plus.widgets.popup;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;

import androidx.annotation.ColorInt;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.ListPopupWindow;

import net.osmand.plus.R;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;

import java.util.ArrayList;
import java.util.List;

public class PopUpMenuHelper {

	private final View anchorView;
	private final List<PopUpMenuItem> items;
	private final PopUpMenuWidthType widthType;
	@ColorInt
	private final int backgroundColor;
	private final AdapterView.OnItemClickListener listener;
	private final boolean nightMode;
	private final int layoutId;

	private PopUpMenuHelper(@NonNull View anchorView,
	                        @NonNull List<PopUpMenuItem> items,
	                        PopUpMenuWidthType widthType,
	                        @ColorInt int backgroundColor,
	                        @LayoutRes int layoutId,
	                        AdapterView.OnItemClickListener listener,
	                        boolean nightMode) {
		this.anchorView = anchorView;
		this.items = items;
		this.widthType = widthType;
		this.layoutId = layoutId;
		this.backgroundColor = backgroundColor;
		this.listener = listener;
		this.nightMode = nightMode;
	}

	private void show() {
		ListPopupWindow listPopupWindow = createPopUpWindow();
		listPopupWindow.show();
	}

	private ListPopupWindow createPopUpWindow() {
		Context ctx = UiUtilities.getThemedContext(anchorView.getContext(), nightMode);
		int contentPadding = getDimensionPixelSize(ctx, R.dimen.content_padding);
		int contentPaddingHalf = getDimensionPixelSize(ctx, R.dimen.content_padding_half);
		int defaultListTextSize = getDimensionPixelSize(ctx, R.dimen.default_list_text_size);
		int standardIconSize = getDimensionPixelSize(ctx, R.dimen.standard_icon_size);
		boolean hasIcon = false;

		List<String> titles = new ArrayList<>();
		for (PopUpMenuItem item : items) {
			titles.add(String.valueOf(item.getTitle()));
			hasIcon = hasIcon || item.getIcon() != null;
		}
		float itemWidth = AndroidUtils.getTextMaxWidth(defaultListTextSize, titles) + contentPadding * 2;
		float iconPartWidth = hasIcon ? standardIconSize + contentPaddingHalf : 0;
		float compoundBtnWidth = contentPadding * 3;
		int minWidth = widthType == PopUpMenuWidthType.AS_ANCHOR_VIEW ? anchorView.getWidth() : 0;
		float additional;
		if (widthType == PopUpMenuWidthType.STANDARD) {
			additional = iconPartWidth + compoundBtnWidth;
		} else {
			additional = iconPartWidth;
		}
		int totalWidth = (int) (Math.max(itemWidth, minWidth) + additional);

		PopUpMenuArrayAdapter adapter = new PopUpMenuArrayAdapter(ctx, layoutId, items, nightMode);
		ListPopupWindow listPopupWindow = new ListPopupWindow(ctx);
		listPopupWindow.setAnchorView(anchorView);
		listPopupWindow.setContentWidth(totalWidth);
		listPopupWindow.setDropDownGravity(Gravity.START | Gravity.TOP);
		listPopupWindow.setVerticalOffset(-anchorView.getHeight() + contentPaddingHalf);
		listPopupWindow.setModal(true);
		listPopupWindow.setAdapter(adapter);
		if (backgroundColor != 0) {
			listPopupWindow.setBackgroundDrawable(new ColorDrawable(backgroundColor));
		}
		listPopupWindow.setOnItemClickListener((parent, view, position, id) -> {
			if (listener != null) {
				listener.onItemClick(parent, view, position, id);
			}
			listPopupWindow.dismiss();
		});
		return listPopupWindow;
	}

	private int getDimensionPixelSize(Context ctx, int dimensionResId) {
		return ctx.getResources().getDimensionPixelSize(dimensionResId);
	}

	public enum PopUpMenuWidthType {
		STANDARD,
		AS_ANCHOR_VIEW
	}

	public static class Builder {

		private final View anchorView;
		private final List<PopUpMenuItem> items;
		@ColorInt
		private int backgroundColor;
		private AdapterView.OnItemClickListener listener;
		private PopUpMenuWidthType widthType = PopUpMenuWidthType.AS_ANCHOR_VIEW;
		private final boolean nightMode;
		@LayoutRes
		private final int layoutId;

		public Builder(View anchorView, List<PopUpMenuItem> items, boolean nightMode) {
			this(anchorView, items, nightMode, R.layout.popup_menu_item);
		}

		public Builder(View anchorView, List<PopUpMenuItem> items, boolean nightMode, int layoutId) {
			this.anchorView = anchorView;
			this.items = items;
			this.layoutId = layoutId;
			this.nightMode = nightMode;
		}

		public Builder setListener(AdapterView.OnItemClickListener listener) {
			this.listener = listener;
			return this;
		}

		public Builder setWidthType(@NonNull PopUpMenuWidthType widthType) {
			this.widthType = widthType;
			return this;
		}

		public Builder setBackgroundColor(@ColorInt int backgroundColor) {
			this.backgroundColor = backgroundColor;
			return this;
		}

		public void show() {
			if (listener == null) {
				listener = (parent, view, position, id) -> {
					if (position < items.size()) {
						View.OnClickListener listener = items.get(position).getOnClickListener();
						if (listener != null) {
							listener.onClick(view);
						}
					}
				};
			}
			new PopUpMenuHelper(anchorView, items, widthType, backgroundColor, layoutId, listener, nightMode).show();
		}
	}
}