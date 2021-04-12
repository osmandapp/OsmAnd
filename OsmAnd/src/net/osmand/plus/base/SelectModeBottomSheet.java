package net.osmand.plus.base;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.AndroidUtils;
import net.osmand.plus.R;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.widgets.MultiStateToggleButton.RadioItem;

import java.util.Collections;
import java.util.List;

public class SelectModeBottomSheet extends SelectionBottomSheet {

	public static final String TAG = SelectModeBottomSheet.class.getSimpleName();

	private BottomSheetItemWithDescription previewUi;

	private List<RadioItem> modes;
	private SelectableItem previewItem;

	@Override
	protected void initHeaderUi() {
		radioGroup.setItems(modes);

		AndroidUiHelper.setVisibility(View.VISIBLE, secondaryDescription, toggleContainer);

		AndroidUiHelper.setVisibility(View.GONE, checkBox, checkBoxTitle,
				primaryDescription, selectedSize, selectAllButton);
	}

	@Override
	protected void createSelectionUi() {
		previewUi = (BottomSheetItemWithDescription) new BottomSheetItemWithDescription.Builder()
				.setDescription(previewItem.getDescription())
				.setDescriptionColorId(AndroidUtils.getSecondaryTextColorId(nightMode))
				.setTitle(previewItem.getTitle())
				.setIcon(uiUtilities.getIcon(previewItem.getIconId(), activeColorRes))
				.setTag(previewItem)
				.setLayoutId(R.layout.bottom_sheet_item_with_descr_56dp)
				.create();
		items.add(previewUi);
	}

	private void updatePreviewUi() {
		previewUi.setTitle(previewItem.getTitle());
		previewUi.setIcon(uiUtilities.getIcon(previewItem.getIconId(), activeColorRes));
		previewUi.setDescription(previewItem.getDescription());
		previewUi.setTag(previewItem);
	}

	private void setModes(@NonNull List<RadioItem> modes) {
		this.modes = modes;
	}

	public void setSelectedMode(@NonNull RadioItem mode) {
		radioGroup.setSelectedItem(mode);
	}

	public void setPreviewItem(@NonNull SelectableItem preview) {
		this.previewItem = preview;
		if (previewUi != null) {
			updatePreviewUi();
		}
	}

	@Override
	public void setDescription(@NonNull String description) {
		secondaryDescription.setText(description);
	}

	@NonNull
	@Override
	public List<SelectableItem> getSelection() {
		return Collections.singletonList(previewItem);
	}

	public static SelectModeBottomSheet showInstance(@NonNull AppCompatActivity activity,
	                                                 @NonNull SelectableItem previewItem,
	                                                 @NonNull List<RadioItem> radioItems,
	                                                 boolean usedOnMap) {
		SelectModeBottomSheet fragment = new SelectModeBottomSheet();
		fragment.setUsedOnMap(usedOnMap);
		fragment.setModes(radioItems);
		fragment.setPreviewItem(previewItem);
		FragmentManager fm = activity.getSupportFragmentManager();
		fragment.show(fm, TAG);
		return fragment;
	}

}
