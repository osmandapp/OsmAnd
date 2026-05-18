package net.osmand.plus.widgets.multistatetoggle;

import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.widgets.multistatetoggle.TextToggleButton.TextRadioItem;

import java.util.Collection;

public class TextToggleButton extends MultiStateToggleButton<TextRadioItem> {

	private final boolean forceHeight;
	private int maxItemHeight;
	private int resizedItemsCount;

	public TextToggleButton(@NonNull OsmandApplication app,
	                        @NonNull LinearLayout container,
	                        boolean nightMode) {
		this(app, container, nightMode, false);
	}

	public TextToggleButton(@NonNull OsmandApplication app,
	                        @NonNull LinearLayout container,
	                        boolean nightMode,
	                        boolean forceHeight) {
		super(app, container, nightMode);
		this.forceHeight = forceHeight;
	}

	@Override
	protected int getRadioItemLayoutId() {
		return R.layout.custom_radio_btn_text_item;
	}

	@Override
	public void setItems(Collection<TextRadioItem> radioItems) {
		maxItemHeight = 0;
		resizedItemsCount = 0;
		super.setItems(radioItems);
	}

	@Override
	protected void initItemView(@NonNull ViewGroup view, @NonNull TextRadioItem item) {
		if (forceHeight && view.getLayoutParams() != null) {
			updateHeight(view, LayoutParams.WRAP_CONTENT);
			view.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {

				@Override
				public void onGlobalLayout() {
					view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
					if (view.getHeight() > maxItemHeight) {
						maxItemHeight = view.getHeight();
					}
					resizedItemsCount++;
					if (resizedItemsCount == buttons.size()) {
						for (View button : buttons) {
							updateHeight(button, maxItemHeight);
						}
					}
				}
			});
		}
		TextView title = view.findViewById(R.id.title);
		title.setText(item.getTitle());
	}

	private void updateHeight(@NonNull View view, int height) {
		LayoutParams params = view.getLayoutParams();
		params.height = height;
		view.setLayoutParams(params);
	}

	@Override
	protected void updateItemView(@NonNull ViewGroup view, @NonNull TextRadioItem item,
								  boolean selected, @ColorInt int color) {
		TextView tvTitle = view.findViewById(R.id.title);
		tvTitle.setTextColor(color);
	}

	public static class TextRadioItem extends RadioItem {

		private final String title;

		public TextRadioItem(String title) {
			this.title = title;
		}

		public String getTitle() {
			return title;
		}
	}
}
