package net.osmand.plus.base;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.widgets.multistatetoggle.RadioItem;

import java.util.List;

public class MultipleSelectionWithModeBottomSheet extends MultipleSelectionBottomSheet {

	public static final String TAG = MultipleSelectionWithModeBottomSheet.class.getSimpleName();

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		showElements(secondaryDescription, toggleContainer, checkBox,
				checkBoxTitle, titleDescription, selectedSize, selectAllButton);
	}

	public static MultipleSelectionWithModeBottomSheet showInstance(@NonNull AppCompatActivity activity,
	                                                                @NonNull List<SelectableItem> items,
	                                                                @Nullable List<SelectableItem> selected,
	                                                                @NonNull List<RadioItem> modes,
	                                                                boolean usedOnMap) {
		MultipleSelectionWithModeBottomSheet fragment = new MultipleSelectionWithModeBottomSheet();
		fragment.setUsedOnMap(usedOnMap);
		fragment.setItems(items);
		fragment.setSelectedItems(selected);
		fragment.setModes(modes);
		FragmentManager fm = activity.getSupportFragmentManager();
		fragment.show(fm, TAG);
		return fragment;
	}

}
