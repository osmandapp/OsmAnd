package net.osmand.plus.widgets;

import android.content.Context;
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

public class HorizontalRadioGroup {

	private List<RadioItem> items = new ArrayList<>();
	private List<ViewGroup> buttons = new ArrayList<>();
	private List<View> dividers = new ArrayList<>();
	private RadioItem selectedItem;

	private LinearLayout container;
	private boolean nightMode;

	public HorizontalRadioGroup(LinearLayout container, boolean nightMode) {
		this.container = container;
		this.nightMode = nightMode;
	}

	public void setItems(RadioItem firstBtn, RadioItem secondBtn, RadioItem ... other) {
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
			RadioItem item = items.get(i);

			ViewGroup button = createBtn();
			TextView title = button.findViewById(R.id.title);
			title.setText(item.getTitle());
			button.setOnClickListener(getSelectClickListener(item));
			buttons.add(button);
			container.addView(button);

			boolean lastItem = i == items.size() - 1;
			if (!lastItem) {
				View divider = createDivider();
				dividers.add(divider);
				container.addView(divider);
			}
		}
		updateView();
	}

	private View.OnClickListener getSelectClickListener(final RadioItem item) {
		return new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				OnRadioItemClickListener l = item.getListener();
				if (l != null && l.onRadioItemClick(item, container)) {
					setSelectedItem(item);
				}
			}
		};
	}

	@NonNull
	private ViewGroup createBtn() {
		Context ctx = getThemedCtx();
		LayoutInflater inflater = LayoutInflater.from(ctx);
		return (ViewGroup) inflater.inflate(R.layout.custom_radio_btn_text_item, container, false);
	}

	@NonNull
	private View createDivider() {
		Context ctx = getThemedCtx();
		int dividerColor = nightMode ?
				R.color.stroked_buttons_and_links_outline_dark :
				R.color.stroked_buttons_and_links_outline_light;
		int width = AndroidUtils.dpToPx(ctx, 1.0f);
		View divider = new View(ctx);
		divider.setLayoutParams(new ViewGroup.LayoutParams(width, ViewGroup.LayoutParams.MATCH_PARENT));
		divider.setBackgroundColor(ContextCompat.getColor(ctx, dividerColor));
		return divider;
	}

	public void setSelectedItem(RadioItem selectedItem) {
		this.selectedItem = selectedItem;
		updateView();
	}

	private void updateView() {
		OsmandApplication app = getMyApplication();
		if (app == null) {
			return;
		}
		int activeColor = ContextCompat.getColor(app, nightMode
				? R.color.active_color_primary_dark
				: R.color.active_color_primary_light);
		int textColor = ContextCompat.getColor(app, nightMode
				? R.color.text_color_primary_dark
				: R.color.text_color_primary_light);
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

	private void hideDividers(int ... dividerIndexes) {
		for (int dividerIndex : dividerIndexes) {
			if (dividerIndex >= 0 && dividerIndex < dividers.size()) {
				dividers.get(dividerIndex).setVisibility(View.GONE);
			}
		}
	}

	private Context getThemedCtx() {
		Context ctx = container.getContext();
		return UiUtilities.getThemedContext(ctx, nightMode);
	}

	private OsmandApplication getMyApplication() {
		if (container != null) {
			return (OsmandApplication) container.getContext().getApplicationContext();
		}
		return null;
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
