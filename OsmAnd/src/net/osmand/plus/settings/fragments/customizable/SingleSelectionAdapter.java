package net.osmand.plus.settings.fragments.customizable;

import static net.osmand.plus.base.dialog.data.DialogExtra.SELECTED_INDEX;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.dialog.BaseDialogController;
import net.osmand.plus.base.dialog.data.DisplayData;
import net.osmand.plus.base.dialog.data.DisplayItem;
import net.osmand.plus.base.dialog.interfaces.controller.IDialogItemSelected;
import net.osmand.plus.base.dialog.interfaces.controller.IDisplayDataProvider;
import net.osmand.plus.settings.fragments.customizable.SingleSelectionAdapter.ItemViewHolder;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;

public class SingleSelectionAdapter extends RecyclerView.Adapter<ItemViewHolder> {

	private final OsmandApplication app;
	private final BaseDialogController controller;
	private final LayoutInflater themedInflater;
	private final boolean nightMode;
	private DisplayData displayData;

	public SingleSelectionAdapter(@NonNull OsmandApplication app, @NonNull Context context,
	                              @Nullable BaseDialogController controller) {
		this.app = app;
		this.controller = controller;
		this.nightMode = controller != null && controller.isNightMode();
		this.themedInflater = UiUtilities.getInflater(context, nightMode);
		updateDisplayData(false);
	}

	public void updateDisplayData() {
		updateDisplayData(true);
	}

	@SuppressLint("NotifyDataSetChanged")
	private void updateDisplayData(boolean refresh) {
		if (controller instanceof IDisplayDataProvider displayDataProvider) {
			this.displayData = displayDataProvider.getDisplayData(controller.getProcessId());
			if (refresh) {
				notifyDataSetChanged();
			}
		}
	}

	@NonNull
	@Override
	public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		int layoutId = R.layout.bottom_sheet_item_with_radio_btn;
		View itemView = themedInflater.inflate(layoutId, parent, false);
		return new ItemViewHolder(itemView);
	}

	@Override
	public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
		DisplayItem displayItem = displayData.getDisplayItems().get(position);
		holder.title.setText(displayItem.getTitle());
		holder.icon.setImageDrawable(displayItem.getNormalIcon());

		int controlsColor = displayData.getControlsColor(app, displayItem, nightMode);
		UiUtilities.setupCompoundButton(nightMode, controlsColor, holder.compoundButton);
		Integer selectedIndex = (Integer) displayData.getExtra(SELECTED_INDEX);
		boolean isChecked = selectedIndex != null && selectedIndex == position;
		holder.compoundButton.setChecked(isChecked);

		holder.itemView.setOnClickListener(v -> {
			if (controller instanceof IDialogItemSelected l) {
				l.onDialogItemSelected(controller.getProcessId(), displayItem);
			}
		});
		setupSelectableBackground(holder.itemView, displayItem);
	}

	public void setupSelectableBackground(@NonNull View view, @NonNull DisplayItem displayItem) {
		Integer color = displayData.getBackgroundColor(displayItem);
		if (color != null) {
			AndroidUtils.setBackground(view, UiUtilities.getColoredSelectableDrawable(app, color));
		}
	}

	@Override
	public int getItemCount() {
		return displayData.getDisplayItems().size();
	}

	public static class ItemViewHolder extends RecyclerView.ViewHolder {

		public final TextView title;
		public final ImageView icon;
		public final CompoundButton compoundButton;

		public ItemViewHolder(@NonNull View itemView) {
			super(itemView);
			icon = itemView.findViewById(R.id.icon);
			title = itemView.findViewById(R.id.title);
			compoundButton = itemView.findViewById(R.id.compound_button);
		}
	}
}
