package net.osmand.plus.base;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.widgets.multistatetoggle.RadioItem;

import java.util.Collections;
import java.util.List;

public class ModeSelectionBottomSheet<T> extends SelectionBottomSheet<T> {

	public static final String TAG = ModeSelectionBottomSheet.class.getSimpleName();

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		showElements(primaryDescription, toggleContainer);
		hideElements(checkBox, checkBoxTitle, titleDescription,
				secondaryDescription, selectedSize, selectAllButton);
	}

	@Override
	protected void updateItemView(SelectableItem<T> item, View view) {
		ImageView ivIcon = view.findViewById(R.id.icon);
		TextView tvTitle = view.findViewById(R.id.title);
		TextView tvDescr = view.findViewById(R.id.description);

		Drawable icon = getIcon(item.getIconId(), activeColorRes);
		ivIcon.setImageDrawable(icon);
		tvTitle.setText(item.getTitle());
		tvDescr.setText(item.getDescription());
		tvDescr.setTextColor(ContextCompat.getColor(app, ColorUtilities.getSecondaryTextColorId(nightMode)));
	}

	@Override
	protected int getItemLayoutId() {
		return R.layout.bottom_sheet_item_with_descr_56dp;
	}

	public void setItem(SelectableItem<T> item) {
		setItems(Collections.singletonList(item));
	}

	@NonNull
	@Override
	public List<SelectableItem<T>> getSelectedItems() {
		return allItems;
	}

	@Override
	protected boolean shouldShowDivider() {
		return false;
	}

	public static <T> ModeSelectionBottomSheet<T> showInstance(@NonNull AppCompatActivity activity,
	                                                           @NonNull SelectableItem<T> previewItem,
	                                                           @NonNull List<RadioItem> radioItems,
	                                                           boolean usedOnMap) {
		ModeSelectionBottomSheet<T> fragment = new ModeSelectionBottomSheet<>();
		fragment.setUsedOnMap(usedOnMap);
		fragment.setModes(radioItems);
		fragment.setItems(Collections.singletonList(previewItem));
		FragmentManager fm = activity.getSupportFragmentManager();
		fragment.show(fm, TAG);
		return fragment;
	}
}