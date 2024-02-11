package net.osmand.plus.card.color.palette;

import android.annotation.SuppressLint;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

class ColorsPaletteAdapter extends RecyclerView.Adapter<ColorViewHolder> {

	private final FragmentActivity activity;
	private final IColorsPaletteUIController controller;
	private final ColorsPaletteElements paletteElements;
	private List<Integer> colors;
	private boolean nightMode;

	public ColorsPaletteAdapter(@NonNull FragmentActivity activity,
	                            @NonNull IColorsPaletteUIController controller,
	                            boolean nightMode) {
		this.activity = activity;
		this.controller = controller;
		this.colors = controller.getAllColors();
		this.nightMode = nightMode;
		paletteElements = new ColorsPaletteElements(activity, nightMode);
		setHasStableIds(true);
	}

	@SuppressLint("NotifyDataSetChanged")
	public void updateColorsList() {
		this.colors = controller.getAllColors();
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
		int color = colors.get(position);
		boolean isSelected = controller.getSelectedColor() == color;
		paletteElements.updateColorItemView(holder.itemView, color, isSelected);
		holder.background.setOnClickListener(v -> {
			controller.onSelectColorFromPalette(color);
		});
		holder.background.setOnLongClickListener(v -> {
			controller.onColorItemLongClicked(activity, holder.background, color, nightMode);
			return false;
		});
	}

	@Override
	public int getItemCount() {
		return colors.size();
	}

	@Override
	public long getItemId(int position) {
		return colors.get(position);
	}

}
