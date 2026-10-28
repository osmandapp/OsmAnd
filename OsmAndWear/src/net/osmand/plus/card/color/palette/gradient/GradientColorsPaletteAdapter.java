package net.osmand.plus.card.color.palette.gradient;

import android.annotation.SuppressLint;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.R;
import net.osmand.plus.card.color.palette.gradient.GradientColorsPaletteAdapter.ColorViewHolder;
import net.osmand.plus.card.color.palette.main.IColorsPaletteController;
import net.osmand.plus.card.color.palette.main.data.PaletteColor;
import net.osmand.plus.card.color.palette.main.data.PaletteSortingMode;

import java.util.List;

class GradientColorsPaletteAdapter extends RecyclerView.Adapter<ColorViewHolder> {

	private final IColorsPaletteController controller;
	private final GradientUiHelper gradientUiHelper;
	private List<PaletteColor> colors;

	public GradientColorsPaletteAdapter(@NonNull FragmentActivity activity,
										@NonNull IColorsPaletteController controller,
										boolean nightMode) {
		this.controller = controller;
		this.colors = controller.getColors(PaletteSortingMode.LAST_USED_TIME);
		gradientUiHelper = new GradientUiHelper(activity, nightMode);
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
		View view = gradientUiHelper.createRectangleView(parent);
		return new ColorViewHolder(view);
	}

	@Override
	public void onBindViewHolder(@NonNull ColorViewHolder holder, int position) {
		PaletteColor paletteColor = colors.get(position);
		boolean isSelected = controller.isSelectedColor(paletteColor);
		gradientUiHelper.updateColorItemView(holder.itemView, paletteColor, isSelected);
		holder.itemView.setOnClickListener(v -> controller.onSelectColorFromPalette(paletteColor, false));
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
		return paletteColor.getId().hashCode();
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
