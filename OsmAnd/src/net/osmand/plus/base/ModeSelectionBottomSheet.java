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

import net.osmand.AndroidUtils;
import net.osmand.plus.R;
import net.osmand.plus.widgets.multistatetoggle.RadioItem;

import java.util.Collections;
import java.util.List;

public class ModeSelectionBottomSheet extends SelectionBottomSheet {

	public static final String TAG = ModeSelectionBottomSheet.class.getSimpleName();

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		showElements(primaryDescription, toggleContainer);
		hideElements(checkBox, checkBoxTitle, titleDescription,
				secondaryDescription, selectedSize, selectAllButton);
	}

	@Override
	protected void updateItemView(SelectableItem item, View view) {
		ImageView ivIcon = view.findViewById(R.id.icon);
		TextView tvTitle = view.findViewById(R.id.title);
		TextView tvDescr = view.findViewById(R.id.description);

		Drawable icon = uiUtilities.getIcon(item.getIconId(), activeColorRes);
		ivIcon.setImageDrawable(icon);
		tvTitle.setText(item.getTitle());
		tvDescr.setText(item.getDescription());
		tvDescr.setTextColor(ContextCompat.getColor(app, AndroidUtils.getSecondaryTextColorId(nightMode)));
	}

	@Override
	protected int getItemLayoutId() {
		return R.layout.bottom_sheet_item_with_descr_56dp;
	}

	public void setItem(SelectableItem item) {
		setItems(Collections.singletonList(item));
	}

	@NonNull
	@Override
	public List<SelectableItem> getSelectedItems() {
		return allItems;
	}

	@Override
	protected boolean shouldShowDivider() {
		return false;
	}

	public static ModeSelectionBottomSheet showInstance(@NonNull AppCompatActivity activity,
	                                                    @NonNull SelectableItem previewItem,
	                                                    @NonNull List<RadioItem> radioItems,
	                                                    boolean usedOnMap) {
		ModeSelectionBottomSheet fragment = new ModeSelectionBottomSheet();
		fragment.setUsedOnMap(usedOnMap);
		fragment.setModes(radioItems);
		fragment.setItems(Collections.singletonList(previewItem));
		FragmentManager fm = activity.getSupportFragmentManager();
		fragment.show(fm, TAG);
		return fragment;
	}

}
