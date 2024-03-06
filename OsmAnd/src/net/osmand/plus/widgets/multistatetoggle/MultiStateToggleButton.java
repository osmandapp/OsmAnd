package net.osmand.plus.widgets.multistatetoggle;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.multistatetoggle.RadioItem.OnRadioItemClickListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public abstract class MultiStateToggleButton<_Radio extends RadioItem> {

	protected final OsmandApplication app;
	protected final UiUtilities uiUtilities;

	protected final LinearLayout container;
	protected final List<ViewGroup> buttons = new ArrayList<>();
	protected final List<View> dividers = new ArrayList<>();

	protected final List<_Radio> items = new ArrayList<>();
	protected final boolean nightMode;
	protected _Radio selectedItem;

	public MultiStateToggleButton(@NonNull OsmandApplication app,
	                              @NonNull LinearLayout container,
	                              boolean nightMode) {
		this.app = app;
		this.uiUtilities = app.getUIUtilities();
		this.container = container;
		this.nightMode = nightMode;
	}

	@SafeVarargs
	public final void setItems(@NonNull _Radio... radioItems) {
		setItems(Arrays.asList(radioItems));
	}

	public void setItems(Collection<_Radio> radioItems) {
		if (radioItems == null || radioItems.size() < 2) return;
		items.clear();
		items.addAll(radioItems);
		initView();
	}

	public final void setSelectedItem(int itemIndex) {
		_Radio selectedItem = itemIndex < 0 || itemIndex >= items.size() ? null : items.get(itemIndex);
		setSelectedItem(selectedItem);
	}

	public final void setSelectedItem(@Nullable _Radio selectedItem) {
		this.selectedItem = selectedItem;
		updateView();
	}

	public int getSelectedItemIndex() {
		return items.indexOf(selectedItem);
	}

	private void initView() {
		buttons.clear();
		dividers.clear();
		container.removeAllViews();
		for (int i = 0; i < items.size(); i++) {
			createBtn(items.get(i));
			if (!isLastItem(i)) {
				createDivider();
			}
		}
		updateView();
	}

	private void createBtn(@NonNull _Radio item) {
		LayoutInflater inflater = UiUtilities.getInflater(app, nightMode);
		ViewGroup button = (ViewGroup) inflater.inflate(
				getRadioItemLayoutId(), container, false);
		button.setOnClickListener(v -> {
			OnRadioItemClickListener l = item.getListener();
			if (l != null && l.onRadioItemClick(item, container)) {
				setSelectedItem(item);
			}
		});
		initItemView(button, item);
		buttons.add(button);
		container.addView(button);
	}

	private void createDivider() {
		int dividerColor = ColorUtilities.getStrokedButtonsOutlineColor(app, nightMode);
		int width = AndroidUtils.dpToPx(app, 1.0f);
		View divider = new View(app);
		divider.setLayoutParams(new ViewGroup.LayoutParams(width, ViewGroup.LayoutParams.MATCH_PARENT));
		divider.setBackgroundColor(dividerColor);
		dividers.add(divider);
		container.addView(divider);
	}

	public void setItemsEnabled(boolean enable) {
		for (_Radio item: items) {
			item.setEnabled(enable);
		}
		updateView();
	}

	private void updateView() {
		int activeColor = ColorUtilities.getActiveColor(app, nightMode);
		int defaultColor = ColorUtilities.getDefaultIconColor(app, nightMode);
		int textColorPrimary = ColorUtilities.getPrimaryTextColor(app, nightMode);
		int textColorSecondary = ColorUtilities.getSecondaryTextColor(app, nightMode);

		int radius = AndroidUtils.dpToPx(app, 4);
		float[] leftBtnRadii = {radius, radius, 0, 0, 0, 0, radius, radius};
		float[] rightBtnRadii = {0, 0, radius, radius, radius, radius, 0, 0};
		float[] internalBtnRadii = {0, 0, 0, 0, 0, 0, 0, 0};
		boolean isLayoutRtl = AndroidUtils.isLayoutRtl(app);

		showAllDividers();

		for (int i = 0; i < items.size(); i++) {
			_Radio item = items.get(i);
			ViewGroup button = buttons.get(i);
			boolean enabled = item.isEnabled();
			button.setEnabled(enabled);
			int textColor = enabled ? textColorPrimary : textColorSecondary;
			int selectedBgColor = enabled ? activeColor : defaultColor;
			GradientDrawable background = new GradientDrawable();
			background.setColor(ColorUtilities.getColorWithAlpha(selectedBgColor, 0.1f));
			background.setStroke(AndroidUtils.dpToPx(app, 1.5f), ColorUtilities.getColorWithAlpha(selectedBgColor, 0.5f));

			if (selectedItem == item) {
				if (i == 0) {
					background.setCornerRadii(isLayoutRtl ? rightBtnRadii : leftBtnRadii);
					hideDividers(0);
				} else if (isLastItem(i)) {
					background.setCornerRadii(isLayoutRtl ? leftBtnRadii : rightBtnRadii);
					hideDividers(dividers.size() - 1);
				} else {
					background.setCornerRadii(internalBtnRadii);
					hideDividers(i - 1, i);
				}
				button.setBackground(background);
				updateItemView(button, item, textColor);
			} else {
				button.setBackgroundColor(Color.TRANSPARENT);
				updateItemView(button, item, selectedBgColor);
			}
		}
	}

	protected abstract int getRadioItemLayoutId();

	protected abstract void initItemView(@NonNull ViewGroup view, @NonNull _Radio item);

	protected abstract void updateItemView(@NonNull ViewGroup view, @NonNull _Radio item,
	                                       @ColorInt int color);

	private void showAllDividers() {
		for (View divider : dividers) {
			divider.setVisibility(View.VISIBLE);
		}
	}

	private void hideDividers(int... dividerIndexes) {
		for (int dividerIndex : dividerIndexes) {
			if (dividerIndex >= 0 && dividerIndex < dividers.size()) {
				dividers.get(dividerIndex).setVisibility(View.GONE);
			}
		}
	}

	private boolean isLastItem(int index) {
		return index == items.size() - 1;
	}
}