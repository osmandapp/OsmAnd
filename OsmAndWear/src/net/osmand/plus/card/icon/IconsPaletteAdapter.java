package net.osmand.plus.card.icon;

import android.annotation.SuppressLint;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.card.icon.IconsPaletteAdapter.IconViewHolder;

import java.util.List;

public class IconsPaletteAdapter<IconData> extends RecyclerView.Adapter<IconViewHolder> {

	private final FragmentActivity activity;
	private final IIconsPaletteController<IconData> controller;
	private final boolean nightMode;

	public IconsPaletteAdapter(@NonNull FragmentActivity activity,
	                           @NonNull IIconsPaletteController<IconData> controller,
	                           boolean nightMode) {
		this.activity = activity;
		this.controller = controller;
		this.nightMode = nightMode;
	}

	@SuppressLint("NotifyDataSetChanged")
	public void updateIconsList() {
		notifyDataSetChanged();
	}

	@NonNull
	@Override
	public IconViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		IconsPaletteElements<IconData> paletteElements = controller.getPaletteElements(activity, nightMode);
		return new IconViewHolder(paletteElements.createView(parent));
	}

	@Override
	public void onBindViewHolder(@NonNull IconViewHolder holder, int position) {
		IconData icon = controller.getIcons().get(position);
		boolean isSelected = controller.isSelectedIcon(icon);
		int controlsColor = controller.getIconsAccentColor(nightMode);
		IconsPaletteElements<IconData> paletteElements = controller.getPaletteElements(activity, nightMode);
		paletteElements.bindView(holder.itemView, icon, controlsColor, isSelected);
		holder.itemView.setOnClickListener(v -> controller.onSelectIconFromPalette(icon));
	}

	public void askNotifyItemChanged(@Nullable IconData icon) {
		int index = indexOf(icon);
		if (index >= 0) {
			notifyItemChanged(index);
		}
	}

	public int indexOf(@Nullable IconData icon) {
		return controller.getIcons().indexOf(icon);
	}

	@Override
	public int getItemCount() {
		return controller.getIcons().size();
	}

	static class IconViewHolder extends RecyclerView.ViewHolder {
		public IconViewHolder(@NonNull View itemView) {
			super(itemView);
		}
	}
}
