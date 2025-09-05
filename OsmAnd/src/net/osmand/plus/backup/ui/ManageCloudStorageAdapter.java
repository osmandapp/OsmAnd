package net.osmand.plus.backup.ui;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import net.osmand.OnCompleteCallback;
import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.ExportCategory;
import net.osmand.plus.settings.backend.backup.exporttype.ExportType;
import net.osmand.plus.settings.fragments.SettingsCategoryItems;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.FontCache;
import net.osmand.plus.utils.UiUtilities;

public class ManageCloudStorageAdapter extends BackupTypesAdapter {

	private final int defaultIconColor;
	private final int secondaryIconColor;

	public ManageCloudStorageAdapter(@NonNull Context context,
	                                 @NonNull BaseBackupTypesController controller) {
		super(context, controller);
		defaultIconColor = ColorUtilities.getDefaultIconColor(context, nightMode);
		secondaryIconColor = ColorUtilities.getSecondaryIconColor(context, nightMode);
	}

	@Override
	public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
		View view = convertView;
		if (view == null) {
			view = themedInflater.inflate(R.layout.backup_type_item_with_action_button, parent, false);
		}
		ExportCategory category = controller.getCategory(groupPosition);
		long size = calculateExportCategorySize(category);

		String name = getString(category.getTitleId());
		TextView tvTitle = view.findViewById(R.id.title);
		tvTitle.setText(UiUtilities.createCustomFontSpannable(FontCache.getMediumFont(), name, name));

		TextView tvSummary = view.findViewById(R.id.description);
		tvSummary.setText(formatSize(size));

		setupDeleteButton(view, size > 0, () -> {
			controller.onCategorySelected(category, true);
			notifyDataSetChanged();
		});

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
		long size = calculateExportTypeSize(exportType);

		TextView tvTitle = view.findViewById(R.id.title);
		tvTitle.setText(exportType.getTitleId());

		TextView tvSummary = view.findViewById(R.id.description);
		tvSummary.setText(formatSize(size));

		boolean enabled = size > 0;
		setupChildIcon(view, exportType.getIconId(), enabled);

		OnCompleteCallback callback = () -> {
			controller.onTypeSelected(exportType, true);
			notifyDataSetChanged();
		};
		setupDeleteButton(view, enabled, callback);
		view.setOnClickListener(enabled ? v -> callback.onComplete() : null);

		AndroidUiHelper.updateVisibility(view.findViewById(R.id.pro_icon), false);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.divider), false);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.card_top_divider), false);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.card_bottom_divider), isLastChild);
		setupSelectableBackground(view);

		return view;
	}

	private void setupDeleteButton(@NonNull View view, boolean enabled,
	                               @NonNull OnCompleteCallback callback) {
		View container = view.findViewById(R.id.action_button_container);
		container.setOnClickListener(enabled ? v -> callback.onComplete() : null);
		container.setContentDescription(getString(R.string.shared_string_delete));

		ImageView ivIcon = view.findViewById(R.id.action_button);
		int color = enabled ? defaultIconColor : secondaryIconColor;
		ivIcon.setImageDrawable(iconsCache.getPaintedIcon(R.drawable.ic_action_delete_dark, color));
	}

	private long calculateExportCategorySize(@NonNull ExportCategory category) {
		return controller.getCategoryItems(category).calculateSize();
	}

	private long calculateExportTypeSize(@NonNull ExportType exportType) {
		long size = 0;
		for (Object object : controller.getItemsForType(exportType)) {
			size += getItemSize(object);
		}
		return size;
	}

	private String formatSize(long size) {
		return size == 0
				? getString(R.string.shared_string_none)
				: AndroidUtils.formatSize(app, size);
	}
}
