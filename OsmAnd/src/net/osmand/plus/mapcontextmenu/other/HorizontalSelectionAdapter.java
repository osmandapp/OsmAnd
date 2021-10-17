package net.osmand.plus.mapcontextmenu.other;

import static net.osmand.util.Algorithms.capitalizeFirstLetter;

import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.AndroidUtils;
import net.osmand.plus.ColorUtilities;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.helpers.AndroidUiHelper;

import java.util.ArrayList;
import java.util.List;


public class HorizontalSelectionAdapter extends RecyclerView.Adapter<HorizontalSelectionAdapter.ItemViewHolder> {

	public static int INVALID_ID = -1;

	private List<HorizontalSelectionItem> items;
	private OsmandApplication app;
	private boolean nightMode;
	private HorizontalSelectionAdapterListener listener;
	private HorizontalSelectionItem selectedItem = null;

	public HorizontalSelectionAdapter(OsmandApplication app, boolean nightMode) {
		this.app = app;
		this.nightMode = nightMode;
	}

	public void setTitledItems(List<String> titles) {
		List<HorizontalSelectionItem> items = new ArrayList<>();
		for (String title : titles) {
			items.add(new HorizontalSelectionItem(title));
		}
		setItems(items);
	}

	public void setItems(List<HorizontalSelectionItem> items) {
		this.items = items;
	}

	@NonNull
	@Override
	public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		View view;
		view = UiUtilities.getInflater(parent.getContext(), nightMode)
				.inflate(R.layout.point_editor_icon_category_item, parent, false);
		return new ItemViewHolder(view);
	}

	@Override
	public void onBindViewHolder(@NonNull ItemViewHolder holder, final int position) {
		final HorizontalSelectionItem item = items.get(holder.getAdapterPosition());
		TextView textView = holder.buttonText;
		ImageView imageView = holder.buttonIcon;

		int itemColor;
		if (item.equals(selectedItem) && item.isEnabled()) {
			int activeColor = ColorUtilities.getActiveColor(app, nightMode);
			AndroidUtils.setBackground(holder.button, UiUtilities.createTintedDrawable(app,
							R.drawable.bg_select_icon_group_button, activeColor));
			itemColor = ContextCompat.getColor(app, R.color.color_white);
		} else {
			if (!item.isEnabled()) {
				int inactiveColorId = nightMode ?
						R.color.icon_color_default_dark : R.color.icon_color_secondary_light;
				itemColor = ContextCompat.getColor(app, inactiveColorId);
			} else {
				int defaultTitleColorId = nightMode ? R.color.active_color_primary_dark : R.color.preference_category_title;
				itemColor = ContextCompat.getColor(app,
						item.getTitleColorId() != INVALID_ID ? item.getTitleColorId() : defaultTitleColorId);
			}
			GradientDrawable buttonBackground = (GradientDrawable) AppCompatResources.getDrawable(app,
					R.drawable.bg_select_icon_group_button).mutate();
			buttonBackground.setStroke(AndroidUtils.dpToPx(app, 1), ContextCompat.getColor(app,
					nightMode ? R.color.stroked_buttons_and_links_outline_dark
							: R.color.stroked_buttons_and_links_outline_light));
			buttonBackground.setColor(ContextCompat.getColor(app, R.color.color_transparent));
			AndroidUtils.setBackground(holder.button, buttonBackground);
		}
		textView.setTextColor(itemColor);
		if (item.iconId != INVALID_ID) {
			imageView.setImageDrawable(app.getUIUtilities().getPaintedIcon(item.iconId, itemColor));
		}
		AndroidUiHelper.updateVisibility(textView, !item.isShowOnlyIcon());
		AndroidUiHelper.updateVisibility(imageView, item.iconId != INVALID_ID);
		AndroidUiHelper.updateVisibility(holder.space,
				item.iconId != INVALID_ID && !item.isShowOnlyIcon());
		textView.setText(capitalizeFirstLetter(item.title));
		textView.requestLayout();
		holder.button.setEnabled(item.isEnabled());
		holder.button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				selectedItem = item;
				if (listener != null) {
					listener.onItemSelected(item);
				}
			}
		});
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			View buttonContainer = holder.button.findViewById(R.id.button_container);
			AndroidUtils.setBackground(app, buttonContainer, nightMode, R.drawable.ripple_solid_light_18dp,
					R.drawable.ripple_solid_dark_18dp);
		}
	}

	@Override
	public int getItemCount() {
		return items.size();
	}

	public int getItemPositionByTitle(String title) {
		return getItemPosition(getItemByTitle(title));
	}

	public int getItemPosition(HorizontalSelectionItem item) {
		return items.indexOf(item);
	}

	public void setSelectedItemByTitle(String title) {
		setSelectedItem(getItemByTitle(title));
	}

	public void setSelectedItem(HorizontalSelectionItem selectedItem) {
		this.selectedItem = selectedItem;
		notifyDataSetChanged();
	}

	public HorizontalSelectionItem getItemByTitle(String title) {
		for (HorizontalSelectionItem item : items) {
			if (title.equals(item.getTitle())) {
				return item;
			}
		}
		return null;
	}

	public void setListener(HorizontalSelectionAdapterListener listener) {
		this.listener = listener;
	}

	public interface HorizontalSelectionAdapterListener {

		void onItemSelected(HorizontalSelectionItem item);
	}

	static class ItemViewHolder extends RecyclerView.ViewHolder {
		final TextView buttonText;
		final ImageView buttonIcon;
		final View space;
		final LinearLayout button;

		ItemViewHolder(@NonNull View itemView) {
			super(itemView);
			buttonText = itemView.findViewById(R.id.button_text);
			buttonIcon = itemView.findViewById(R.id.button_icon);
			space = itemView.findViewById(R.id.space);
			button = itemView.findViewById(R.id.button);
		}
	}

	public static class HorizontalSelectionItem {
		private String title;
		private boolean enabled = true;
		private int titleColorId = INVALID_ID;
		private Object object;
		private int iconId = INVALID_ID;
		private boolean showOnlyIcon;

		public HorizontalSelectionItem(String title) {
			this(title, null);
		}

		public HorizontalSelectionItem(String title, Object object) {
			this.title = title;
			this.object = object;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public void setTitleColorId(int titleColorId) {
			this.titleColorId = titleColorId;
		}

		public void setIconId(int iconId) {
			this.iconId = iconId;
		}

		public void setShowOnlyIcon(boolean showOnlyIcon) {
			this.showOnlyIcon = showOnlyIcon;
		}

		public String getTitle() {
			return title;
		}

		public boolean isEnabled() {
			return enabled;
		}

		public int getTitleColorId() {
			return titleColorId;
		}

		public int getIconId() {
			return iconId;
		}

		public Object getObject() {
			return object;
		}

		public boolean isShowOnlyIcon() {
			return iconId != INVALID_ID && showOnlyIcon;
		}
	}
}
