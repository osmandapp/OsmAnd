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
import androidx.core.content.ContextCompat;

import net.osmand.AndroidUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.widgets.multistatetoggle.RadioItem.OnRadioItemClickListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collection;
import java.util.List;

public abstract class MultiStateToggleButton<_Radio extends RadioItem> {

	protected final OsmandApplication app;
	protected final UiUtilities uiUtilities;

	private final LinearLayout container;
	private final List<ViewGroup> buttons = new ArrayList<>();
	private final List<View> dividers = new ArrayList<>();

	protected final List<_Radio> items = new ArrayList<>();
	protected final boolean nightMode;
	protected boolean isEnabled;
	protected RadioItem selectedItem;

	public MultiStateToggleButton(@NonNull OsmandApplication app,
	                              @NonNull LinearLayout container,
	                              boolean nightMode) {
		this.app = app;
		this.uiUtilities = app.getUIUtilities();
		this.container = container;
		this.nightMode = nightMode;
	}

	public void setItems(Collection<_Radio> radioItems) {
		if (radioItems == null || radioItems.size() < 2) return;
		items.clear();
		items.addAll(radioItems);
		initView();
	}

	@SafeVarargs
	public final void setItems(@NonNull _Radio firstBtn,
	                           @NonNull _Radio secondBtn,
	                           @Nullable _Radio... other) {
		items.clear();
		items.add(firstBtn);
		items.add(secondBtn);
		if (other != null && other.length > 0) {
			items.addAll(Arrays.asList(other));
		}
		initView();
	}

	public final void setSelectedItem(@Nullable RadioItem selectedItem) {
		this.selectedItem = selectedItem;
		updateView();
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

	private void createBtn(@NonNull final _Radio item) {
		LayoutInflater inflater = UiUtilities.getInflater(app, nightMode);
		ViewGroup button = (ViewGroup) inflater.inflate(
				getRadioItemLayoutId(), container, false);
		button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				OnRadioItemClickListener l = item.getListener();
				if (l != null && l.onRadioItemClick(item, container)) {
					setSelectedItem(item);
				}
			}
		});
		initItemView(button, item);
		buttons.add(button);
		container.addView(button);
	}

	private void createDivider() {
		int dividerColor = nightMode ?
				R.color.stroked_buttons_and_links_outline_dark :
				R.color.stroked_buttons_and_links_outline_light;
		int width = AndroidUtils.dpToPx(app, 1.0f);
		View divider = new View(app);
		divider.setLayoutParams(new ViewGroup.LayoutParams(width, ViewGroup.LayoutParams.MATCH_PARENT));
		divider.setBackgroundColor(ContextCompat.getColor(app, dividerColor));
		dividers.add(divider);
		container.addView(divider);
	}

	private void updateView() {
		updateView(true);
	}

	public void updateView(boolean isEnabled) {
		int activeColor = ContextCompat.getColor(app, nightMode
				? isEnabled ? R.color.active_color_primary_dark : R.color.icon_color_default_dark
				: isEnabled ? R.color.active_color_primary_light : R.color.icon_color_default_light);
		int textColor = ContextCompat.getColor(app, nightMode
				? isEnabled ? R.color.text_color_primary_dark : R.color.text_color_secondary_dark
				: isEnabled ? R.color.text_color_primary_light : R.color.text_color_secondary_light);
		int radius = AndroidUtils.dpToPx(app, 4);
		float[] leftBtnRadii = new float[]{radius, radius, 0, 0, 0, 0, radius, radius};
		float[] rightBtnRadii = new float[]{0, 0, radius, radius, radius, radius, 0, 0};
		float[] centerBtnRadii = new float[]{0, 0, 0, 0, 0, 0, 0, 0};
		boolean isLayoutRtl = AndroidUtils.isLayoutRtl(app);

		GradientDrawable background = new GradientDrawable();
		background.setColor(UiUtilities.getColorWithAlpha(activeColor, 0.1f));
		background.setStroke(AndroidUtils.dpToPx(app, 1), UiUtilities.getColorWithAlpha(activeColor, 0.5f));

		showAllDividers();
		for (int i = 0; i < items.size(); i++) {
			_Radio item = items.get(i);
			ViewGroup container = buttons.get(i);
			container.setEnabled(isEnabled);
			if (selectedItem == item) {
				if (i == 0) {
					background.setCornerRadii(isLayoutRtl ? rightBtnRadii : leftBtnRadii);
					hideDividers(0);
				} else if (isLastItem(i)) {
					background.setCornerRadii(isLayoutRtl ? leftBtnRadii : rightBtnRadii);
					hideDividers(dividers.size() - 1);
				} else {
					background.setCornerRadii(centerBtnRadii);
					hideDividers(i - 1, i);
				}
				container.setBackgroundDrawable(background);
				updateItemView(container, item, textColor);
			} else {
				container.setBackgroundColor(Color.TRANSPARENT);
				updateItemView(container, item, activeColor);
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
