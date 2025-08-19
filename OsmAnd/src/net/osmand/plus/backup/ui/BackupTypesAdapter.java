package net.osmand.plus.backup.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.backup.RemoteFile;
import net.osmand.plus.base.OsmandBaseExpandableListAdapter;
import net.osmand.plus.mapmarkers.MapMarkersGroup;
import net.osmand.plus.settings.backend.ExportCategory;
import net.osmand.plus.settings.backend.backup.exporttype.ExportType;
import net.osmand.plus.settings.backend.backup.items.FileSettingsItem;
import net.osmand.plus.settings.fragments.SettingsCategoryItems;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.util.CollectionUtils;

import java.io.File;

public abstract class BackupTypesAdapter extends OsmandBaseExpandableListAdapter {

	protected final OsmandApplication app;
	protected final UiUtilities iconsCache;
	protected final BaseBackupTypesController controller;

	protected final LayoutInflater themedInflater;
	protected final int activeColor;
	protected final boolean nightMode;

	public BackupTypesAdapter(@NonNull Context context,
	                          @NonNull BaseBackupTypesController controller) {
		this.app = (OsmandApplication) context.getApplicationContext();
		this.controller = controller;
		this.nightMode = controller.isNightMode();
		iconsCache = app.getUIUtilities();
		themedInflater = UiUtilities.getInflater(context, nightMode);
		activeColor = ColorUtilities.getActiveIconColor(app, nightMode);
	}

	protected long getItemSize(@NonNull Object object) {
		if (object instanceof FileSettingsItem fileSettingsItem) {
			return fileSettingsItem.getSize();
		} else if (object instanceof File file) {
			return file.length();
		} else if (object instanceof RemoteFile remoteFile) {
			return  remoteFile.getZipSize();
		} else if (object instanceof MapMarkersGroup markersGroup) {
			if (CollectionUtils.equalsToAny(markersGroup.getId(), ExportType.ACTIVE_MARKERS.name(), ExportType.HISTORY_MARKERS.name())) {
				return  ((MapMarkersGroup) object).getMarkers().size();
			}
		}
		return 0;
	}

	protected void setupChildIcon(@NonNull View view, @DrawableRes int iconRes, boolean selected) {
		int colorRes;
		if (selected) {
			colorRes = nightMode ? R.color.icon_color_active_dark : R.color.icon_color_osmand_light;
		} else {
			colorRes = nightMode ? R.color.icon_color_secondary_dark : R.color.icon_color_secondary_light;
		}
		ImageView icon = view.findViewById(R.id.explicit_indicator);
		icon.setImageDrawable(iconsCache.getIcon(iconRes, colorRes));
	}

	protected void setupSelectableBackground(@NonNull View view) {
		View selectableView = view.findViewById(R.id.selectable_list_item);
		if (selectableView.getBackground() == null) {
			Drawable drawable = UiUtilities.getColoredSelectableDrawable(app, activeColor, 0.3f);
			AndroidUtils.setBackground(selectableView, drawable);
		}
	}

	@Override
	public int getGroupCount() {
		return controller.getCategories().size();
	}

	@Override
	public int getChildrenCount(int i) {
		ExportCategory category = controller.getCategory(i);
		return controller.getCategoryItems(category).getTypes().size();
	}

	@Override
	public Object getGroup(int i) {
		ExportCategory category = controller.getCategory(i);
		return controller.getCategoryItems(category);
	}

	@Override
	public Object getChild(int groupPosition, int childPosition) {
		ExportCategory category = controller.getCategory(groupPosition);
		SettingsCategoryItems categoryItems = controller.getCategoryItems(category);
		ExportType exportType = categoryItems.getTypes().get(groupPosition);
		return categoryItems.getItemsForType(exportType).get(childPosition);
	}

	@Override
	public long getGroupId(int i) {
		return i;
	}

	@Override
	public long getChildId(int groupPosition, int childPosition) {
		return groupPosition * 10000L + childPosition;
	}

	@Override
	public boolean hasStableIds() {
		return true;
	}

	@Override
	public boolean isChildSelectable(int i, int i1) {
		return true;
	}

	public String getString(@StringRes int resId, Object ... formatArgs) {
		return app.getString(resId, formatArgs);
	}

	public interface OnItemSelectedListener {

		void onTypeSelected(@NonNull ExportType exportType, boolean selected);

		void onCategorySelected(ExportCategory exportCategory, boolean selected);
	}
}
