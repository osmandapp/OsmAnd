package net.osmand.plus.mapcontextmenu.editors;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
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

import java.util.List;

import static net.osmand.util.Algorithms.capitalizeFirstLetter;


public class IconCategoriesAdapter extends RecyclerView.Adapter<IconCategoriesAdapter.NameViewHolder> {

	private List<String> items;
	private OsmandApplication app;
	private boolean nightMode;
	private IconCategoriesAdapterListener listenerCategory;

	public IconCategoriesAdapter(OsmandApplication app) {
		this.app = app;
		nightMode = app.getDaynightHelper().isNightModeForMapControls();
	}

	public void setItems(List<String> items) {
		this.items = items;
	}

	@NonNull
	@Override
	public NameViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		View view;
		view = LayoutInflater.from(parent.getContext()).inflate(R.layout.point_editor_icon_category_item, parent, false);
		return new NameViewHolder(view);
	}

	@Override
	public void onBindViewHolder(@NonNull NameViewHolder holder, final int position) {
		final String category = items.get(holder.getAdapterPosition());
		TextView textView = holder.buttonText;
		int activeColorResId = nightMode ? R.color.active_color_primary_dark : R.color.active_color_primary_light;
		if (category.equals(listenerCategory.getSelectedItem())) {
			AndroidUtils.setBackground(holder.button, app.getUIUtilities().getPaintedIcon(R.drawable.bg_select_icon_group_button,
					ContextCompat.getColor(app, activeColorResId)));
			textView.setTextColor(ContextCompat.getColor(app, R.color.color_white));
		} else {
			textView.setTextColor(ContextCompat.getColor(app, R.color.preference_category_title));
			GradientDrawable buttonBackground = (GradientDrawable) AppCompatResources.getDrawable(app,
					R.drawable.bg_select_icon_group_button).mutate();
			buttonBackground.setStroke(AndroidUtils.dpToPx(app, 1), ContextCompat.getColor(app,
					nightMode ? R.color.stroked_buttons_and_links_outline_dark
							: R.color.stroked_buttons_and_links_outline_light));
			buttonBackground.setColor(ContextCompat.getColor(app, R.color.color_transparent));
			AndroidUtils.setBackground(holder.button, buttonBackground);
		}
		textView.setText(capitalizeFirstLetter(category));
		holder.button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (listenerCategory != null) {
					listenerCategory.onItemClick(category);
				}
			}
		});
	}

	@Override
	public int getItemCount() {
		return items.size();
	}

	int getItemPosition(String name) {
		return items.indexOf(name);
	}

	public void setListenerCategory(IconCategoriesAdapterListener listenerCategory) {
		this.listenerCategory = listenerCategory;
	}

	public interface IconCategoriesAdapterListener {
		void onItemClick(String item);

		String getSelectedItem();
	}

	static class NameViewHolder extends RecyclerView.ViewHolder {
		final TextView buttonText;
		final LinearLayout button;

		NameViewHolder(@NonNull View itemView) {
			super(itemView);
			buttonText = itemView.findViewById(R.id.button_text);
			button = itemView.findViewById(R.id.button_container);
		}
	}
}
