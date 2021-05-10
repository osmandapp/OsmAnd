package net.osmand.plus.widgets.multistatetoggle;

import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.widgets.multistatetoggle.TextToggleButton.TextRadioItem;

public class TextToggleButton extends MultiStateToggleButton<TextRadioItem> {

	public TextToggleButton(@NonNull OsmandApplication app,
	                        @NonNull LinearLayout container,
	                        boolean nightMode) {
		super(app, container, nightMode);
	}

	@Override
	protected int getRadioItemLayoutId() {
		return R.layout.custom_radio_btn_text_item;
	}

	@Override
	protected void initItemView(@NonNull ViewGroup view, @NonNull TextRadioItem item) {
		TextView title = view.findViewById(R.id.title);
		title.setText(item.getTitle());
	}

	@Override
	protected void updateItemView(@NonNull ViewGroup view,
	                              @NonNull TextRadioItem item,
	                              @ColorInt int color) {
		TextView tvTitle = (TextView) view.findViewById(R.id.title);
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
