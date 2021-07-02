package net.osmand.plus.backup.ui;

import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import net.osmand.AndroidUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.UiUtilities.CompoundButtonType;
import net.osmand.plus.activities.OsmandBaseExpandableListAdapter;
import net.osmand.plus.backup.RemoteFile;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.helpers.FontCache;
import net.osmand.plus.mapmarkers.MapMarkersGroup;
import net.osmand.plus.settings.backend.ExportSettingsCategory;
import net.osmand.plus.settings.backend.ExportSettingsType;
import net.osmand.plus.settings.backend.backup.items.FileSettingsItem;
import net.osmand.plus.settings.fragments.ExportSettingsAdapter;
import net.osmand.plus.settings.fragments.SettingsCategoryItems;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class BackupTypesAdapter extends OsmandBaseExpandableListAdapter {

	private final OsmandApplication app;
	private final UiUtilities uiUtilities;

	private List<ExportSettingsCategory> itemsTypes;
	private Map<ExportSettingsType, List<?>> selectedItemsMap;
	private Map<ExportSettingsCategory, SettingsCategoryItems> itemsMap;

	private final OnItemSelectedListener listener;

	private final LayoutInflater themedInflater;

	private final int activeColor;
	private final boolean nightMode;

	public BackupTypesAdapter(OsmandApplication app, OnItemSelectedListener listener, boolean nightMode) {
		this.app = app;
		this.listener = listener;
		this.nightMode = nightMode;
		uiUtilities = app.getUIUtilities();
		themedInflater = UiUtilities.getInflater(app, nightMode);
		activeColor = ContextCompat.getColor(app, nightMode ? R.color.icon_color_active_dark : R.color.icon_color_active_light);
	}

	@Override
	public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
		View view = convertView;
		if (view == null) {
			view = themedInflater.inflate(R.layout.backup_type_item, parent, false);
		}
		ExportSettingsCategory category = itemsTypes.get(groupPosition);
		SettingsCategoryItems items = itemsMap.get(category);

		String name = app.getString(category.getTitleId());
		Typeface typeface = FontCache.getRobotoMedium(app);
		TextView titleTv = view.findViewById(R.id.title);
		titleTv.setText(UiUtilities.createCustomFontSpannable(typeface, name, name));

		TextView description = view.findViewById(R.id.description);
		description.setText(getCategoryDescr(category));

		int selectedTypes = 0;
		for (ExportSettingsType type : items.getTypes()) {
			if (selectedItemsMap.get(type) != null) {
				selectedTypes++;
			}
		}
		CompoundButton compoundButton = view.findViewById(R.id.switch_widget);
		compoundButton.setChecked(selectedTypes == items.getTypes().size());
		UiUtilities.setupCompoundButton(compoundButton, nightMode, CompoundButtonType.GLOBAL);

		view.findViewById(R.id.switch_container).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				compoundButton.performClick();
				if (listener != null) {
					listener.onCategorySelected(category, compoundButton.isChecked());
				}
				notifyDataSetChanged();
			}
		});
		setupSelectableBackground(view);
		adjustIndicator(app, groupPosition, isExpanded, view, !nightMode);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.divider), isExpanded);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.card_top_divider), true);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.card_bottom_divider), !isExpanded);

		return view;
	}

	@Override
	public View getChildView(int groupPosition, final int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
		View view = convertView;
		if (view == null) {
			view = themedInflater.inflate(R.layout.backup_type_item, parent, false);
		}
		ExportSettingsCategory category = itemsTypes.get(groupPosition);
		SettingsCategoryItems categoryItems = itemsMap.get(category);
		ExportSettingsType type = categoryItems.getTypes().get(childPosition);
		List<?> items = categoryItems.getItemsForType(type);
		List<?> selectedItems = selectedItemsMap.get(type);

		boolean selected = selectedItems != null;
		TextView title = view.findViewById(R.id.title);
		title.setText(type.getTitleId());

		TextView description = view.findViewById(R.id.description);
		description.setText(getSelectedTypeDescr(app, selectedItemsMap, type, items));

		CompoundButton compoundButton = view.findViewById(R.id.switch_widget);
		compoundButton.setChecked(selected);
		UiUtilities.setupCompoundButton(compoundButton, nightMode, CompoundButtonType.GLOBAL);

		view.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				compoundButton.performClick();
				if (listener != null) {
					listener.onTypeSelected(type, compoundButton.isChecked());
				}
				notifyDataSetChanged();
			}
		});
		setupSelectableBackground(view);
		setupChildIcon(view, type.getIconRes(), selected);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.divider), false);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.card_top_divider), false);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.card_bottom_divider), isLastChild);

		return view;
	}

	private void setupSelectableBackground(View view) {
		View selectableView = view.findViewById(R.id.selectable_list_item);
		if (selectableView.getBackground() == null) {
			Drawable drawable = UiUtilities.getColoredSelectableDrawable(app, activeColor, 0.3f);
			AndroidUtils.setBackground(selectableView, drawable);
		}
	}

	private void setupChildIcon(View view, int iconRes, boolean itemSelected) {
		int colorRes;
		if (itemSelected) {
			colorRes = nightMode ? R.color.icon_color_active_dark : R.color.icon_color_osmand_light;
		} else {
			colorRes = nightMode ? R.color.icon_color_secondary_dark : R.color.icon_color_secondary_light;
		}
		ImageView icon = view.findViewById(R.id.explicit_indicator);
		icon.setImageDrawable(uiUtilities.getIcon(iconRes, colorRes));
	}

	public void updateSettingsItems(Map<ExportSettingsCategory, SettingsCategoryItems> itemsMap,
									Map<ExportSettingsType, List<?>> selectedItemsMap) {
		this.itemsMap = itemsMap;
		this.itemsTypes = new ArrayList<>(itemsMap.keySet());
		this.selectedItemsMap = selectedItemsMap;
		Collections.sort(itemsTypes);
		notifyDataSetChanged();
	}

	private String getCategoryDescr(ExportSettingsCategory category) {
		long itemsSize = 0;
		int selectedTypes = 0;
		SettingsCategoryItems items = itemsMap.get(category);
		for (ExportSettingsType type : items.getTypes()) {
			if (selectedItemsMap.get(type) != null) {
				selectedTypes++;
				itemsSize += ExportSettingsAdapter.calculateItemsSize(items.getItemsForType(type));
			}
		}
		String description;
		if (selectedTypes == 0) {
			description = app.getString(R.string.shared_string_none);
		} else if (selectedTypes == items.getTypes().size()) {
			description = app.getString(R.string.shared_string_all);
		} else {
			description = app.getString(R.string.ltr_or_rtl_combine_via_slash, String.valueOf(selectedTypes), String.valueOf(items.getTypes().size()));
		}
		String formattedSize = AndroidUtils.formatSize(app, itemsSize);
		return itemsSize == 0 ? description : app.getString(R.string.ltr_or_rtl_combine_via_comma, description, formattedSize);
	}

	public static String getSelectedTypeDescr(OsmandApplication app, Map<ExportSettingsType, List<?>> selectedItemsMap,
											  ExportSettingsType type, List<?> items) {
		long itemsSize = 0;
		int selectedTypes = 0;

		List<?> selectedItems = selectedItemsMap.get(type);
		if (!Algorithms.isEmpty(selectedItems)) {
			for (int i = 0; i < items.size(); i++) {
				Object object = items.get(i);
				if (selectedItems.contains(object)) {
					selectedTypes++;
					if (object instanceof FileSettingsItem) {
						itemsSize += ((FileSettingsItem) object).getSize();
					} else if (object instanceof File) {
						itemsSize += ((File) object).length();
					} else if (object instanceof RemoteFile) {
						itemsSize += ((RemoteFile) object).getZipSize();
					} else if (object instanceof MapMarkersGroup) {
						MapMarkersGroup markersGroup = (MapMarkersGroup) object;
						if (Algorithms.stringsEqual(markersGroup.getId(), ExportSettingsType.ACTIVE_MARKERS.name())
								|| Algorithms.stringsEqual(markersGroup.getId(), ExportSettingsType.HISTORY_MARKERS.name())) {
							itemsSize += ((MapMarkersGroup) object).getMarkers().size();
						}
					}
				}
			}
			if (itemsSize > 0 && type == ExportSettingsType.ACTIVE_MARKERS) {
				String itemsDescr = app.getString(R.string.shared_string_items);
				return app.getString(R.string.ltr_or_rtl_combine_via_colon, itemsDescr, String.valueOf(itemsSize));
			}
			String description;
			if (selectedTypes == items.size()) {
				description = app.getString(R.string.shared_string_all);
				if (items.size() > 0) {
					description = app.getString(R.string.ltr_or_rtl_combine_via_comma, description, String.valueOf(items.size()));
				}
			} else {
				description = app.getString(R.string.ltr_or_rtl_combine_via_slash, String.valueOf(selectedTypes), String.valueOf(items.size()));
			}
			String formattedSize = AndroidUtils.formatSize(app, itemsSize);
			if (Algorithms.isEmpty(formattedSize)) {
				return description;
			} else {
				return app.getString(R.string.ltr_or_rtl_combine_via_comma, description, formattedSize);
			}
		} else {
			return app.getString(R.string.shared_string_none);
		}
	}

	@Override
	public int getGroupCount() {
		return itemsTypes.size();
	}

	@Override
	public int getChildrenCount(int i) {
		return itemsMap.get(itemsTypes.get(i)).getTypes().size();
	}

	@Override
	public Object getGroup(int i) {
		return itemsMap.get(itemsTypes.get(i));
	}

	@Override
	public Object getChild(int groupPosition, int childPosition) {
		SettingsCategoryItems categoryItems = itemsMap.get(itemsTypes.get(groupPosition));
		ExportSettingsType type = categoryItems.getTypes().get(groupPosition);
		return categoryItems.getItemsForType(type).get(childPosition);
	}

	@Override
	public long getGroupId(int i) {
		return i;
	}

	@Override
	public long getChildId(int groupPosition, int childPosition) {
		return groupPosition * 10000 + childPosition;
	}

	@Override
	public boolean hasStableIds() {
		return true;
	}

	@Override
	public boolean isChildSelectable(int i, int i1) {
		return true;
	}

	public interface OnItemSelectedListener {

		void onTypeSelected(ExportSettingsType type, boolean selected);

		void onCategorySelected(ExportSettingsCategory type, boolean selected);

	}
}