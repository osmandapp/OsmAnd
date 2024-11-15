package net.osmand.plus.card.color.palette.main;

import android.annotation.SuppressLint;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.R;
import net.osmand.plus.card.color.palette.main.ColorsPaletteAdapter.ColorViewHolder;
import net.osmand.plus.card.color.palette.main.data.PaletteColor;
import net.osmand.plus.card.color.palette.main.data.PaletteSortingMode;

import java.util.List;

class ColorsPaletteAdapter extends RecyclerView.Adapter<ColorViewHolder> {

	private final FragmentActivity activity;
	private final IColorsPaletteController controller;
	private final ColorsPaletteElements paletteElements;
	private final boolean nightMode;
	private List<PaletteColor> colors;

	public ColorsPaletteAdapter(@NonNull FragmentActivity activity,
	                            @NonNull IColorsPaletteController controller,
	                            boolean nightMode) {
		this.activity = activity;
		this.controller = controller;
		this.colors = controller.getColors(PaletteSortingMode.LAST_USED_TIME);
		this.nightMode = nightMode;
		paletteElements = new ColorsPaletteElements(activity, nightMode);
		setHasStableIds(true);
	}

	@SuppressLint("NotifyDataSetChanged")
	public void updateColorsList() {
		this.colors = controller.getColors(PaletteSortingMode.LAST_USED_TIME);
		notifyDataSetChanged();
	}

	@NonNull
	@Override
	public ColorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		View view = paletteElements.createCircleView(parent);
		return new ColorViewHolder(view);
	}

	@Override
	public void onBindViewHolder(@NonNull ColorViewHolder holder, int position) {
		PaletteColor paletteColor = colors.get(position);
		boolean isSelected = controller.isSelectedColor(paletteColor);
		paletteElements.updateColorItemView(holder.itemView, paletteColor.getColor(), isSelected);
		holder.itemView.setOnClickListener(v -> {
			controller.onSelectColorFromPalette(paletteColor, false);
		});
		holder.itemView.setOnLongClickListener(v -> {
			controller.onColorLongClick(activity, holder.background, paletteColor, nightMode);
			return false;
		});
	}

	public void askNotifyItemChanged(@Nullable PaletteColor paletteColor) {
		if (paletteColor != null) {
			int index = indexOf(paletteColor);
			if (index >= 0) {
				notifyItemChanged(index);
			}
		}
	}

	public int indexOf(@NonNull PaletteColor paletteColor) {
		return colors.indexOf(paletteColor);
	}

	@Override
	public int getItemCount() {
		return colors.size();
	}

	@Override
	public long getItemId(int position) {
		PaletteColor paletteColor = colors.get(position);
		return paletteColor.getId();
	}

	static class ColorViewHolder extends RecyclerView.ViewHolder {

		public final ImageView outline;
		public final ImageView background;

		public ColorViewHolder(@NonNull View itemView) {
			super(itemView);
			outline = itemView.findViewById(R.id.outline);
			background = itemView.findViewById(R.id.background);
		}
	}
}
