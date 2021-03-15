package net.osmand.plus.widgets;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import net.osmand.AndroidUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MultiStateToggleButton {

	private List<RadioItem> items = new ArrayList<>();
	private OsmandApplication app;
	private List<ViewGroup> buttons = new ArrayList<>();
	private List<View> dividers = new ArrayList<>();
	private RadioItem selectedItem;

	private LinearLayout container;
	private boolean nightMode;

	public MultiStateToggleButton(OsmandApplication app, LinearLayout container, boolean nightMode) {
		this.app = app;
		this.container = container;
		this.nightMode = nightMode;
	}

	public void setItems(RadioItem firstBtn, RadioItem secondBtn, RadioItem... other) {
		items.clear();
		items.add(firstBtn);
		items.add(secondBtn);
		if (other != null && other.length > 0) {
			items.addAll(Arrays.asList(other));
		}
		initView();
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

	private boolean isLastItem(int index) {
		return index == items.size() - 1;
	}

	private void createBtn(@NonNull final RadioItem item) {
		LayoutInflater inflater = UiUtilities.getInflater(app, nightMode);
		ViewGroup button = (ViewGroup) inflater.inflate(
				R.layout.custom_radio_btn_text_item, container, false);
		TextView title = button.findViewById(R.id.title);
		title.setText(item.getTitle());
		button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				OnRadioItemClickListener l = item.getListener();
				if (l != null && l.onRadioItemClick(item, container)) {
					setSelectedItem(item);
				}
			}
		});
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

	public void setSelectedItem(RadioItem selectedItem) {
		this.selectedItem = selectedItem;
		updateView();
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
			RadioItem item = items.get(i);
			ViewGroup container = buttons.get(i);
			container.setEnabled(isEnabled);
			TextView tvTitle = (TextView) container.findViewById(R.id.title);
			if (selectedItem == item) {
				if (i == 0) {
					background.setCornerRadii(isLayoutRtl ? rightBtnRadii : leftBtnRadii);
					hideDividers(0);
				} else if (i == items.size() - 1) {
					background.setCornerRadii(isLayoutRtl ? leftBtnRadii : rightBtnRadii);
					hideDividers(dividers.size() - 1);
				} else {
					background.setCornerRadii(centerBtnRadii);
					hideDividers(i - 1, i);
				}
				container.setBackgroundDrawable(background);
				tvTitle.setTextColor(textColor);
			} else {
				container.setBackgroundColor(Color.TRANSPARENT);
				tvTitle.setTextColor(activeColor);
			}
		}
	}

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

	public static class RadioItem {
		private String title;
		private OnRadioItemClickListener listener;

		public RadioItem(String title) {
			this.title = title;
		}

		public void setOnClickListener(OnRadioItemClickListener listener) {
			this.listener = listener;
		}

		public String getTitle() {
			return title;
		}

		public OnRadioItemClickListener getListener() {
			return listener;
		}
	}

	public interface OnRadioItemClickListener {
		boolean onRadioItemClick(RadioItem radioItem, View view);
	}
}
