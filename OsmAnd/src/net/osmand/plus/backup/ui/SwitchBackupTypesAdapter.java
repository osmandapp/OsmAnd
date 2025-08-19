package net.osmand.plus.backup.ui;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.chooseplan.button.PurchasingUtils;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.ExportCategory;
import net.osmand.plus.settings.backend.backup.exporttype.ExportType;
import net.osmand.plus.settings.fragments.SettingsCategoryItems;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.FontCache;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.utils.UiUtilities.CompoundButtonType;
import net.osmand.util.Algorithms;

import java.util.List;

public class SwitchBackupTypesAdapter extends BackupTypesAdapter {

	private final SwitchBackupTypesController controller;

	public SwitchBackupTypesAdapter(@NonNull Context context, @NonNull SwitchBackupTypesController controller) {
		super(context, controller);
		this.controller = controller;
		notifyDataSetChanged();
	}

	@Override
	public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
		View view = convertView;
		if (view == null) {
			view = themedInflater.inflate(R.layout.backup_type_item, parent, false);
		}
		ExportCategory category = controller.getCategory(groupPosition);
		SettingsCategoryItems items = controller.getCategoryItems(category);

		String name = getString(category.getTitleId());
		TextView titleTv = view.findViewById(R.id.title);
		titleTv.setText(UiUtilities.createCustomFontSpannable(FontCache.getMediumFont(), name, name));

		TextView description = view.findViewById(R.id.description);
		description.setText(getGroupSummary(category));

		List<ExportType> selectedTypes = controller.getSelectedTypes(items.getTypes());
		CompoundButton compoundButton = view.findViewById(R.id.switch_widget);
		boolean available = controller.isBackupAvailable();
		compoundButton.setChecked(available && selectedTypes.size() == items.getTypes().size());
		compoundButton.setEnabled(available);
		UiUtilities.setupCompoundButton(compoundButton, nightMode, CompoundButtonType.GLOBAL);
		View switchContainer = view.findViewById(R.id.switch_container);
		if (available) {
			switchContainer.setOnClickListener(v -> {
				compoundButton.performClick();
				controller.onCategorySelected(category, compoundButton.isChecked());
				notifyDataSetChanged();
			});
		} else {
			switchContainer.setOnClickListener(null);
		}
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
			view = themedInflater.inflate(R.layout.backup_type_item, parent, false);
		}
		ExportCategory category = controller.getCategory(groupPosition);
		SettingsCategoryItems categoryItems = controller.getCategoryItems(category);

		ExportType exportType = categoryItems.getTypes().get(childPosition);
		List<?> items = categoryItems.getItemsForType(exportType);
		List<?> selectedItems = controller.getSelectedItemsOfType(exportType);

		boolean selected = selectedItems != null;
		TextView title = view.findViewById(R.id.title);
		title.setText(exportType.getTitleId());

		TextView description = view.findViewById(R.id.description);
		description.setText(getChildSummary(app, items, selectedItems));

		CompoundButton compoundButton = view.findViewById(R.id.switch_widget);
		compoundButton.setChecked(selected);
		UiUtilities.setupCompoundButton(compoundButton, nightMode, CompoundButtonType.GLOBAL);

		ImageView proIcon = view.findViewById(R.id.pro_icon);
		boolean showProIcon = !controller.isExportTypeAvailable(exportType);
		setupChildIcon(view, exportType.getIconId(), selected && !showProIcon);
		proIcon.setImageResource(PurchasingUtils.getProFeatureIconId(nightMode));
		view.setOnClickListener(v -> {
			compoundButton.performClick();
			controller.onTypeSelected(exportType, compoundButton.isChecked());
			notifyDataSetChanged();
		});
		AndroidUiHelper.updateVisibility(proIcon, showProIcon);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.switch_container), !showProIcon);
		setupSelectableBackground(view);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.divider), false);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.card_top_divider), false);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.card_bottom_divider), isLastChild);

		return view;
	}

	@NonNull
	private String getGroupSummary(@NonNull ExportCategory category) {
		SettingsCategoryItems items = controller.getCategoryItems(category);
		List<ExportType> selectedTypes = controller.getSelectedTypes(items.getTypes());
		long itemsSize = items.calculateSize(selectedTypes);

		String description;
		if (selectedTypes.isEmpty()) {
			description = getString(R.string.shared_string_none);
		} else if (selectedTypes.size() == items.getTypes().size()) {
			description = getString(R.string.shared_string_all);
		} else {
			description = getString(R.string.ltr_or_rtl_combine_via_slash, String.valueOf(selectedTypes.size()), String.valueOf(items.getTypes().size()));
		}
		String formattedSize = AndroidUtils.formatSize(app, itemsSize);
		return itemsSize == 0 ? description : getString(R.string.ltr_or_rtl_combine_via_comma, description, formattedSize);
	}

	@NonNull
	public String getChildSummary(@NonNull OsmandApplication app,
	                              @NonNull List<?> items, @Nullable List<?> selectedItems) {
		if (!Algorithms.isEmpty(selectedItems)) {
			long itemsSize = 0;
			int selectedTypesCount = 0;

			for (Object object : items) {
				if (selectedItems.contains(object)) {
					selectedTypesCount++;
					itemsSize += getItemSize(object);
				}
			}

			String description;
			if (selectedTypesCount == items.size()) {
				description = getString(R.string.shared_string_all);
				if (!items.isEmpty()) {
					description = getString(R.string.ltr_or_rtl_combine_via_comma, description, String.valueOf(items.size()));
				}
			} else {
				description = getString(R.string.ltr_or_rtl_combine_via_slash, String.valueOf(selectedTypesCount), String.valueOf(items.size()));
			}
			String formattedSize = AndroidUtils.formatSize(app, itemsSize);
			if (Algorithms.isEmpty(formattedSize)) {
				return description;
			} else {
				return getString(R.string.ltr_or_rtl_combine_via_comma, description, formattedSize);
			}
		} else {
			return getString(R.string.shared_string_none);
		}
	}
}