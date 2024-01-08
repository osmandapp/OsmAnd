package net.osmand.plus.search.dialogs;

import android.content.res.ColorStateList;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.widget.CompoundButtonCompat;

import net.osmand.osm.PoiType;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.render.RenderingIcons;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SubCategoriesAdapter extends ArrayAdapter<PoiType> {

	private final OsmandApplication app;
	private final UiUtilities uiUtilities;
	private final SubCategoryClickListener listener;
	private final boolean nightMode;
	private final boolean showCategory;
	private final int activeColorRes;
	private final int secondaryColorRes;
	private final int activeIconColorRes;
	private List<PoiType> selectedItems;
	private final List<PoiType> items;

	public SubCategoriesAdapter(@NonNull OsmandApplication app,
								@NonNull List<PoiType> items,
								boolean showCategory,
								@Nullable SubCategoryClickListener listener) {
		super(app, R.layout.profile_data_list_item_child, items);
		this.app = app;
		this.showCategory = showCategory;
		this.listener = listener;
		this.items = new ArrayList<>(items);
		selectedItems = new ArrayList<>();
		uiUtilities = app.getUIUtilities();
		nightMode = !app.getSettings().isLightContent();
		activeIconColorRes = nightMode
				? R.color.icon_color_osmand_dark
				: R.color.icon_color_osmand_light;
		activeColorRes = nightMode
				? R.color.icon_color_active_dark
				: R.color.icon_color_active_light;
		secondaryColorRes = nightMode
				? R.color.icon_color_secondary_dark
				: R.color.icon_color_secondary_light;
	}

	@NonNull
	@Override
	public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
		if (convertView == null) {
			convertView = UiUtilities.getInflater(app, nightMode)
					.inflate(R.layout.profile_data_list_item_child, parent, false);
		}
		PoiType poiType = getItem(position);
		boolean selected = selectedItems.contains(poiType);
		int tintColorRes = selected ? activeColorRes : secondaryColorRes;
		int tintIconColorRes = selected ? activeIconColorRes : secondaryColorRes;
		if (poiType != null) {
			TextView title = convertView.findViewById(R.id.title_tv);
			title.setMaxLines(Integer.MAX_VALUE);
			title.setEllipsize(null);
			title.setText(poiType.getTranslation());
			CheckBox checkBox = convertView.findViewById(R.id.check_box);
			checkBox.setChecked(selected);
			checkBox.setClickable(false);
			CompoundButtonCompat.setButtonTintList(checkBox, ColorStateList.valueOf(ContextCompat.getColor(app, tintColorRes)));
			if (showCategory) {
				TextView subTitle = convertView.findViewById(R.id.sub_title_tv);
				subTitle.setVisibility(View.VISIBLE);
				subTitle.setText(poiType.getCategory().getTranslation());
			} else {
				convertView.findViewById(R.id.sub_title_tv).setVisibility(View.GONE);
			}
			convertView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					if (selected) {
						selectedItems.remove(poiType);
					} else {
						selectedItems.add(poiType);
					}
					if (listener != null) {
						listener.onCategoryClick(selectedItems.containsAll(items));
					}
					notifyDataSetChanged();
				}
			});
			ImageView icon = convertView.findViewById(R.id.icon);
			int iconRes = RenderingIcons.getBigIconResourceId(poiType.getIconKeyName());
			if (iconRes == 0) {
				iconRes = RenderingIcons.getBigIconResourceId(poiType.getOsmTag() + "_" + poiType.getOsmValue());
				if (iconRes == 0) {
					iconRes = R.drawable.mx_special_custom_category;
				}
			}
			icon.setImageDrawable(uiUtilities.getIcon(iconRes, tintIconColorRes));
		}
		return convertView;
	}

	@Override
	public void addAll(@NonNull Collection<? extends PoiType> collection) {
		super.addAll(collection);
		items.addAll(collection);
	}

	@Override
	public void clear() {
		super.clear();
		items.clear();
	}

	public void selectAll(boolean selectAll) {
		selectedItems.clear();
		if (selectAll) {
			selectedItems.addAll(items);
		}
		notifyDataSetChanged();
	}

	public void setSelectedItems(List<PoiType> selectedItems) {
		this.selectedItems = selectedItems;
	}

	public List<PoiType> getSelectedItems() {
		return selectedItems;
	}

	interface SubCategoryClickListener {
		void onCategoryClick(boolean allSelected);
	}
}
