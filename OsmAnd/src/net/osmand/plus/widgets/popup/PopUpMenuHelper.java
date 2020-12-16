package net.osmand.plus.widgets.popup;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.ListPopupWindow;

import net.osmand.AndroidUtils;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;

import java.util.ArrayList;
import java.util.List;

public class PopUpMenuHelper {

	private View anchorView;
	private List<PopUpMenuItem> items;
	private PopUpMenuWidthType widthType;
	private AdapterView.OnItemClickListener listener;
	private boolean nightMode;

	private PopUpMenuHelper(@NonNull View anchorView,
	                        @NonNull List<PopUpMenuItem> items,
	                        PopUpMenuWidthType widthType,
	                        AdapterView.OnItemClickListener listener,
	                        boolean nightMode) {
		this.anchorView = anchorView;
		this.items = items;
		this.widthType = widthType;
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
		int totalWidth =(int) (Math.max(itemWidth, minWidth) + additional);

		PopUpMenuArrayAdapter adapter =
				new PopUpMenuArrayAdapter(ctx, R.layout.popup_menu_item, items, nightMode);
		final ListPopupWindow listPopupWindow = new ListPopupWindow(ctx);
		listPopupWindow.setAnchorView(anchorView);
		listPopupWindow.setContentWidth((int) (totalWidth));
		listPopupWindow.setDropDownGravity(Gravity.START | Gravity.TOP);
		listPopupWindow.setVerticalOffset(-anchorView.getHeight() + contentPaddingHalf);
		listPopupWindow.setModal(true);
		listPopupWindow.setAdapter(adapter);
		listPopupWindow.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				if (listener != null) {
					listener.onItemClick(parent, view, position, id);
				}
				listPopupWindow.dismiss();
			}
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
		private View anchorView;
		private List<PopUpMenuItem> items;
		private AdapterView.OnItemClickListener listener;
		private PopUpMenuWidthType widthType = PopUpMenuWidthType.AS_ANCHOR_VIEW;
		private boolean nightMode;

		public Builder(View anchorView, List<PopUpMenuItem> items, boolean nightMode) {
			this.anchorView = anchorView;
			this.items = items;
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

		public void show() {
			if (listener == null) {
				listener = new AdapterView.OnItemClickListener() {
					@Override
					public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
						if (position < items.size()) {
							View.OnClickListener listener = items.get(position).getOnClickListener();
							if (listener != null) {
								listener.onClick(view);
							}
						}
					}
				};
			}
			new PopUpMenuHelper(anchorView, items, widthType, listener, nightMode).show();
		}
	}
}
