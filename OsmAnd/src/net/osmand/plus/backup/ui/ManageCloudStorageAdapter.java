package net.osmand.plus.backup.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.ExportCategory;
import net.osmand.plus.settings.backend.backup.exporttype.ExportType;
import net.osmand.plus.settings.fragments.ExportSettingsAdapter;
import net.osmand.plus.settings.fragments.SettingsCategoryItems;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.FontCache;
import net.osmand.plus.utils.UiUtilities;

import java.util.List;

public class ManageCloudStorageAdapter extends BackupTypesAdapter {

	private final int primaryIconColor;
	private final int secondaryIconColor;

	public ManageCloudStorageAdapter(@NonNull Context context,
	                                 @NonNull BaseBackupTypesController controller) {
		super(context, controller);
		primaryIconColor = ColorUtilities.getPrimaryIconColor(context, nightMode);
		secondaryIconColor = ColorUtilities.getSecondaryIconColor(context, nightMode);
	}

	@Override
	public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
		View view = convertView;
		if (view == null) {
			view = themedInflater.inflate(R.layout.backup_type_item_with_action_button, parent, false);
		}
		ExportCategory category = controller.getCategory(groupPosition);

		String name = getString(category.getTitleId());
		TextView tvTitle = view.findViewById(R.id.title);
		tvTitle.setText(UiUtilities.createCustomFontSpannable(FontCache.getMediumFont(), name, name));

		TextView tvSummary = view.findViewById(R.id.description);
		tvSummary.setText(getGroupSummary(category));

		View actionButtonContainer = view.findViewById(R.id.action_button_container);
		actionButtonContainer.setOnClickListener(v -> {
			controller.onCategorySelected(category, true);
			notifyDataSetChanged(); // TODO we may not need this
		});

		ImageButton actionButton = view.findViewById(R.id.action_button);
		actionButton.setImageDrawable(iconsCache.getPaintedIcon(R.drawable.ic_action_delete_dark, primaryIconColor));
		// TODO: setup action button

		setupSelectableBackground(view);
		adjustIndicator(app, groupPosition, isExpanded, view, !nightMode);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.divider), isExpanded);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.card_top_divider), true);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.card_bottom_divider), !isExpanded);

		return view;
	}

	@Override
	public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
		View view = convertView;
		if (view == null) {
			view = themedInflater.inflate(R.layout.backup_type_item_with_action_button, parent, false);
		}
		ExportCategory category = controller.getCategory(groupPosition);
		SettingsCategoryItems categoryItems = controller.getCategoryItems(category);

		ExportType exportType = categoryItems.getTypes().get(childPosition);
		List<?> items = categoryItems.getItemsForType(exportType);

		TextView tvTitle = view.findViewById(R.id.title);
		tvTitle.setText(exportType.getTitleId());

		TextView tvSummary = view.findViewById(R.id.description);
		tvSummary.setText(getChildSummary(app, items));

		View actionButtonContainer = view.findViewById(R.id.action_button_container);
		actionButtonContainer.setOnClickListener(v -> {
			controller.onTypeSelected(exportType, true);
			notifyDataSetChanged(); // TODO we may not need this
		});

		ImageButton actionButton = view.findViewById(R.id.action_button);
		actionButton.setImageDrawable(iconsCache.getPaintedIcon(R.drawable.ic_action_delete_dark, primaryIconColor));
		// TODO: setup action button

		AndroidUiHelper.updateVisibility(view.findViewById(R.id.pro_icon), false);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.divider), false);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.card_top_divider), false);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.card_bottom_divider), isLastChild);
		setupSelectableBackground(view);

		return view;
	}

	private void setupSelectableBackground(@NonNull View view) {
		View selectableView = view.findViewById(R.id.selectable_list_item);
		if (selectableView.getBackground() == null) {
			Drawable drawable = UiUtilities.getColoredSelectableDrawable(app, activeColor, 0.3f);
			AndroidUtils.setBackground(selectableView, drawable);
		}
	}

	private void setupChildIcon(@NonNull View view, @DrawableRes int iconRes, boolean selected) {
		int colorRes;
		if (selected) {
			colorRes = nightMode ? R.color.icon_color_active_dark : R.color.icon_color_osmand_light;
		} else {
			colorRes = nightMode ? R.color.icon_color_secondary_dark : R.color.icon_color_secondary_light;
		}
		ImageView icon = view.findViewById(R.id.explicit_indicator);
		icon.setImageDrawable(iconsCache.getIcon(iconRes, colorRes));
	}

	@NonNull
	private String getGroupSummary(@NonNull ExportCategory category) {
		long itemsSize = 0;
		SettingsCategoryItems items = controller.getCategoryItems(category);
		for (ExportType exportType : items.getTypes()) {
			itemsSize += ExportSettingsAdapter.calculateItemsSize(items.getItemsForType(exportType));
		}
		return itemsSize == 0
				? getString(R.string.shared_string_none)
				: AndroidUtils.formatSize(app, itemsSize);
	}

	@NonNull
	public String getChildSummary(@NonNull OsmandApplication app, @NonNull List<?> items) {
		long itemsSize = 0;
		for (Object object : items) {
			itemsSize += getItemSize(object);
		}
		return itemsSize == 0
				? getString(R.string.shared_string_none)
				: AndroidUtils.formatSize(app, itemsSize);
	}
}
