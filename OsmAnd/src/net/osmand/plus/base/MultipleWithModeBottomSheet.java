package net.osmand.plus.base;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.widgets.MultiStateToggleButton.RadioItem;

import java.util.List;

public class MultipleWithModeBottomSheet extends MultipleSelectionBottomSheet {

	public static final String TAG = MultipleWithModeBottomSheet.class.getSimpleName();

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		showElements(secondaryDescription, toggleContainer, checkBox,
				checkBoxTitle, titleDescription, selectedSize, selectAllButton);
	}

	public static MultipleWithModeBottomSheet showInstance(@NonNull AppCompatActivity activity,
	                                                       @NonNull List<SelectableItem> items,
	                                                       @Nullable List<SelectableItem> selected,
	                                                       @NonNull List<RadioItem> modes,
	                                                       boolean usedOnMap) {
		MultipleWithModeBottomSheet fragment = new MultipleWithModeBottomSheet();
		fragment.setUsedOnMap(usedOnMap);
		fragment.setItems(items);
		fragment.setSelectedItems(selected);
		fragment.setModes(modes);
		FragmentManager fm = activity.getSupportFragmentManager();
		fragment.show(fm, TAG);
		return fragment;
	}

}
