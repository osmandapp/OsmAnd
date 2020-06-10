package net.osmand.plus.mapcontextmenu.other;

import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.AndroidUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;

import java.util.List;

import static net.osmand.util.Algorithms.capitalizeFirstLetter;


public class HorizontalSelectionAdapter extends RecyclerView.Adapter<HorizontalSelectionAdapter.ItemViewHolder> {

	private List<String> items;
	private OsmandApplication app;
	private boolean nightMode;
	private HorizontalSelectionAdapterListener listener;

	private String selectedItem = "";

	public HorizontalSelectionAdapter(OsmandApplication app, boolean nightMode) {
		this.app = app;
		this.nightMode = nightMode;
	}

	public void setItems(List<String> items) {
		this.items = items;
	}

	@NonNull
	@Override
	public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		View view;
		view = UiUtilities.getInflater(parent.getContext(), nightMode).inflate(R.layout.point_editor_icon_category_item, parent, false);
		return new ItemViewHolder(view);
	}

	@Override
	public void onBindViewHolder(@NonNull ItemViewHolder holder, final int position) {
		final String item = items.get(holder.getAdapterPosition());
		TextView textView = holder.buttonText;
		int activeColorResId = nightMode ? R.color.active_color_primary_dark : R.color.active_color_primary_light;
		if (item.equals(selectedItem)) {
			AndroidUtils.setBackground(holder.button, app.getUIUtilities().getPaintedIcon(R.drawable.bg_select_icon_group_button,
					ContextCompat.getColor(app, activeColorResId)));
			textView.setTextColor(ContextCompat.getColor(app, R.color.color_white));
		} else {
			textView.setTextColor(ContextCompat.getColor(app,
					nightMode ? R.color.active_color_primary_dark : R.color.preference_category_title));
			GradientDrawable buttonBackground = (GradientDrawable) AppCompatResources.getDrawable(app,
					R.drawable.bg_select_icon_group_button).mutate();
			buttonBackground.setStroke(AndroidUtils.dpToPx(app, 1), ContextCompat.getColor(app,
					nightMode ? R.color.stroked_buttons_and_links_outline_dark
							: R.color.stroked_buttons_and_links_outline_light));
			buttonBackground.setColor(ContextCompat.getColor(app, R.color.color_transparent));
			AndroidUtils.setBackground(holder.button, buttonBackground);
		}
		textView.setText(capitalizeFirstLetter(item));
		holder.button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				selectedItem = item;
				if (listener != null) {
					listener.onItemSelected(item);
				}
			}
		});
	}

	@Override
	public int getItemCount() {
		return items.size();
	}

	public int getItemPosition(String name) {
		return items.indexOf(name);
	}

	public void setSelectedItem(String selectedItem) {
		this.selectedItem = selectedItem;
		notifyDataSetChanged();
	}

	public void setListener(HorizontalSelectionAdapterListener listener) {
		this.listener = listener;
	}

	public interface HorizontalSelectionAdapterListener {

		void onItemSelected(String item);
	}

	static class ItemViewHolder extends RecyclerView.ViewHolder {
		final TextView buttonText;
		final LinearLayout button;

		ItemViewHolder(@NonNull View itemView) {
			super(itemView);
			buttonText = itemView.findViewById(R.id.button_text);
			button = itemView.findViewById(R.id.button_container);
		}
	}
}
