package net.osmand.plus.widgets.popup;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.ListPopupWindow;

import net.osmand.plus.R;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;

import java.util.ArrayList;
import java.util.List;

public class PopUpMenu {

	private final PopUpMenuDisplayData displayData;

	private PopUpMenu(@NonNull PopUpMenuDisplayData displayData) {
		this.displayData = displayData;
	}

	private ListPopupWindow createListPopupWindow() {
		View anchorView = displayData.anchorView;
		boolean nightMode = displayData.nightMode;
		PopUpMenuWidthMode widthMode = displayData.widthMode;
		Context ctx = displayData.anchorView.getContext();
		ctx = UiUtilities.getThemedContext(ctx, nightMode);
		List<PopUpMenuItem> menuItems = displayData.menuItems;

		int contentPadding = getDimension(ctx, R.dimen.content_padding);
		int contentPaddingHalf = getDimension(ctx, R.dimen.content_padding_half);
		int defaultListTextSize = getDimension(ctx, R.dimen.default_list_text_size);
		int standardIconSize = getDimension(ctx, R.dimen.standard_icon_size);
		boolean hasIcon = false;

		List<String> titles = new ArrayList<>();
		for (PopUpMenuItem item : menuItems) {
			titles.add(String.valueOf(item.getTitle()));
			hasIcon = hasIcon || item.getIcon() != null;
		}

		int minWidth = 0;
		if (widthMode == PopUpMenuWidthMode.AS_ANCHOR_VIEW) {
			minWidth = anchorView.getWidth();
		}

		float itemWidth = AndroidUtils.getTextMaxWidth(defaultListTextSize, titles) + contentPadding * 2;
		float iconPartWidth = hasIcon ? standardIconSize + contentPaddingHalf : 0;
		float compoundBtnWidth = contentPadding * 3;

		float additional = iconPartWidth;
		if (widthMode == PopUpMenuWidthMode.STANDARD) {
			additional += compoundBtnWidth;
		}
		int totalWidth = (int) (Math.max(itemWidth, minWidth) + additional);

		PopUpMenuArrayAdapter adapter = new PopUpMenuArrayAdapter(ctx, displayData.layoutId, menuItems, nightMode);
		ListPopupWindow listPopupWindow = new ListPopupWindow(ctx);
		listPopupWindow.setAnchorView(anchorView);
		listPopupWindow.setContentWidth(totalWidth);
		listPopupWindow.setModal(true);
		listPopupWindow.setAdapter(adapter);
		if (shouldShowAsDropDown(ctx)) {
			listPopupWindow.setDropDownGravity(Gravity.START | Gravity.TOP);
			listPopupWindow.setVerticalOffset(-anchorView.getHeight() + contentPaddingHalf);
		} else {
			listPopupWindow.setDropDownGravity(Gravity.START | Gravity.BOTTOM);
			listPopupWindow.setVerticalOffset(anchorView.getHeight() - contentPaddingHalf);
		}
		if (displayData.bgColor != 0) {
			listPopupWindow.setBackgroundDrawable(new ColorDrawable(displayData.bgColor));
		}
		listPopupWindow.setOnItemClickListener((parent, view, position, id) -> {
			if (position < menuItems.size()) {
				View.OnClickListener listener = menuItems.get(position).getOnClickListener();
				if (listener != null) {
					listener.onClick(view);
				}
			}
			listPopupWindow.dismiss();
		});
		return listPopupWindow;
	}

	private boolean shouldShowAsDropDown(@NonNull Context ctx) {
		int screenHeight = ctx.getResources().getDisplayMetrics().heightPixels;
		int anchorViewTopY = AndroidUtils.getViewOnScreenY(displayData.anchorView);
		int leftScreenSpace = screenHeight - anchorViewTopY;
		int approxPopupHeight = calculateApproxPopupWindowHeight(ctx);
		return leftScreenSpace > approxPopupHeight;
	}

	private int calculateApproxPopupWindowHeight(@NonNull Context ctx) {
		LayoutInflater inflater = LayoutInflater.from(ctx);
		View view = inflater.inflate(displayData.layoutId, null, false);
		int widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
		int heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
		view.measure(widthMeasureSpec, heightMeasureSpec);
		return view.getMeasuredHeight() * displayData.menuItems.size();
	}

	private int getDimension(@NonNull Context ctx, int resId) {
		Resources resources = ctx.getResources();
		return resources.getDimensionPixelSize(resId);
	}

	public static void show(@NonNull PopUpMenuDisplayData displayData) {
		PopUpMenu popUpMenu = new PopUpMenu(displayData);
		popUpMenu.createListPopupWindow().show();
	}

}