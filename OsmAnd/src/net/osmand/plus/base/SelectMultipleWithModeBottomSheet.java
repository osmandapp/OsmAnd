package net.osmand.plus.base;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.widgets.MultiStateToggleButton.RadioItem;

import java.util.List;

public class SelectMultipleWithModeBottomSheet extends SelectMultipleItemsBottomSheet {

	public static final String TAG = SelectMultipleWithModeBottomSheet.class.getSimpleName();

	private List<RadioItem> modes;

	@Override
	protected void initHeaderUi() {
		super.initHeaderUi();
		radioGroup.setItems(modes);

		AndroidUiHelper.setVisibility(View.VISIBLE, secondaryDescription, toggleContainer,
				checkBox, checkBoxTitle, titleDescription, selectedSize, selectAllButton);
	}

	private void setModes(@NonNull List<RadioItem> modes) {
		this.modes = modes;
	}

	public void setSelectedMode(@NonNull RadioItem mode) {
		radioGroup.setSelectedItem(mode);
	}

	public static SelectMultipleWithModeBottomSheet showInstance(@NonNull AppCompatActivity activity,
	                                                             @NonNull List<SelectableItem> items,
	                                                             @Nullable List<SelectableItem> selected,
	                                                             @NonNull List<RadioItem> modes,
	                                                             boolean usedOnMap) {
		SelectMultipleWithModeBottomSheet fragment = new SelectMultipleWithModeBottomSheet();
		fragment.setUsedOnMap(usedOnMap);
		fragment.setItems(items);
		fragment.setSelectedItems(selected);
		fragment.setModes(modes);
		FragmentManager fm = activity.getSupportFragmentManager();
		fragment.show(fm, TAG);
		return fragment;
	}

}
