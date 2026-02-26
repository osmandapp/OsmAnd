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
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.fragments.customizable.SingleSelectionAdapter.ItemViewHolder;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.util.Algorithms;

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
			displayData = displayDataProvider.getDisplayData(controller.getProcessId());
			if (refresh) {
				notifyDataSetChanged();
			}
		}
	}

	@NonNull
	@Override
	public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		return new ItemViewHolder(themedInflater.inflate(viewType, parent, false));
	}

	@Override
	public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
		DisplayItem displayItem = displayData.getItemAt(position);

		if (holder.tvTitle != null) {
			String title = displayItem.getTitle();
			if (!Algorithms.isEmpty(title)) {
				holder.tvTitle.setText(title);
			}
			AndroidUiHelper.updateVisibility(holder.tvTitle, !Algorithms.isEmpty(title));
		}

		if (holder.tvDesc != null) {
			String desc = displayItem.getDescription();
			if (!Algorithms.isEmpty(desc)) {
				holder.tvDesc.setText(desc);
			}
			AndroidUiHelper.updateVisibility(holder.tvDesc, !Algorithms.isEmpty(desc));
		}

		if (holder.ivIcon != null) {
			Drawable icon = displayItem.getNormalIcon();
			if (icon != null) {
				holder.ivIcon.setImageDrawable(icon);
			}
			AndroidUiHelper.updateVisibility(holder.ivIcon, icon != null);
		}

		if (holder.cbCompoundButton != null) {
			int controlsColor = displayData.getControlsColor(app, displayItem, nightMode);
			UiUtilities.setupCompoundButton(nightMode, controlsColor, holder.cbCompoundButton);
			Integer selectedIndex = (Integer) displayData.getExtra(SELECTED_INDEX);
			boolean isChecked = selectedIndex != null && selectedIndex == position;
			holder.cbCompoundButton.setChecked(isChecked);
		}

		if (holder.bottomDivider != null) {
			holder.bottomDivider.setVisibility(displayItem.shouldShowBottomDivider() ? View.VISIBLE : View.GONE);
		}

		if (displayItem.getTag() != null) {
			holder.itemView.setOnClickListener(v -> {
				if (controller instanceof IDialogItemSelected l) {
					l.onDialogItemSelected(controller.getProcessId(), displayItem);
				}
			});
			setupSelectableBackground(holder.itemView, displayItem);
		}
	}

	@Override
	public int getItemViewType(int position) {
		return displayData.getItemAt(position).getLayoutId();
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

		public final TextView tvTitle;
		public final TextView tvDesc;
		public final ImageView ivIcon;
		public final CompoundButton cbCompoundButton;
		public final View bottomDivider;

		public ItemViewHolder(@NonNull View itemView) {
			super(itemView);
			ivIcon = itemView.findViewById(R.id.icon);
			tvTitle = itemView.findViewById(R.id.title);
			tvDesc = itemView.findViewById(R.id.description);
			cbCompoundButton = itemView.findViewById(R.id.compound_button);
			bottomDivider = itemView.findViewById(R.id.divider_bottom);
		}
	}
}
