package net.osmand.plus.card.color.palette.gradient;

import static net.osmand.plus.card.color.palette.gradient.AllGradientsPaletteAdapter.*;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatRadioButton;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.ColorPalette.ColorValue;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.card.color.palette.main.IColorsPaletteController;
import net.osmand.plus.card.color.palette.main.data.PaletteColor;
import net.osmand.plus.card.color.palette.main.data.PaletteSortingMode;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;

import java.util.List;

public class AllGradientsPaletteAdapter extends RecyclerView.Adapter<GradientViewHolder> {

	private final OsmandApplication app;
	private final IColorsPaletteController controller;
	private final List<PaletteColor> colors;
	private final LayoutInflater themedInflater;
	private final boolean nightMode;

	public AllGradientsPaletteAdapter(@NonNull OsmandApplication app, @NonNull Context context, @NonNull IColorsPaletteController controller,
									  boolean nightMode) {
		this.app = app;
		this.nightMode = nightMode;
		this.controller = controller;
		this.colors = controller.getColors(PaletteSortingMode.LAST_USED_TIME);
		this.themedInflater = UiUtilities.getInflater(context, nightMode);
		setHasStableIds(true);
	}

	@NonNull
	@Override
	public GradientViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		View itemView = themedInflater.inflate(R.layout.gradient_palette_item, parent, false);
		return new GradientViewHolder(itemView);
	}

	@Override
	public void onBindViewHolder(@NonNull GradientViewHolder holder, int position) {
		PaletteColor paletteColor = colors.get(position);
		boolean isSelected = controller.isSelectedColor(paletteColor);
		holder.onBindViewHolder(app, paletteColor, isSelected, nightMode);
		holder.itemView.setOnClickListener(v -> controller.onSelectColorFromPalette(paletteColor, false));
	}

	public int indexOf(@NonNull PaletteColor paletteColor) {
		return colors.indexOf(paletteColor);
	}

	public void askNotifyItemChanged(@Nullable PaletteColor paletteColor) {
		if (paletteColor != null) {
			int index = indexOf(paletteColor);
			if (index >= 0) {
				notifyItemChanged(index);
			}
		}
	}

	@Override
	public int getItemCount() {
		return colors.size();
	}

	@Override
	public long getItemId(int position) {
		PaletteColor paletteColor = colors.get(position);
		return paletteColor.isDefault() ? paletteColor.getId().hashCode() : paletteColor.getCreationTime();
	}

	static class GradientViewHolder extends RecyclerView.ViewHolder {

		public final AppCompatRadioButton radioButton;
		public final ImageView icon;
		public final TextView title;
		public final TextView description;
		public final ImageView endButtonIcon;
		public final View endButton;
		public final View bottomDivider;
		public final View verticalDivider;

		public GradientViewHolder(@NonNull View itemView) {
			super(itemView);
			radioButton = itemView.findViewById(R.id.compound_button);
			icon = itemView.findViewById(R.id.icon);
			title = itemView.findViewById(R.id.title);
			description = itemView.findViewById(R.id.description);
			endButtonIcon = itemView.findViewById(R.id.end_button_icon);
			bottomDivider = itemView.findViewById(R.id.divider_bottom);
			verticalDivider = itemView.findViewById(R.id.vertical_end_button_divider);
			endButton = itemView.findViewById(R.id.end_button);
		}

		public void onBindViewHolder(@NonNull OsmandApplication app, @NonNull PaletteColor paletteColor, boolean isSelected, boolean nightMode) {
			UiUtilities.setupCompoundButton(nightMode , ColorUtilities.getActiveColor(app, nightMode), radioButton);
			radioButton.setChecked(isSelected);
			if (paletteColor instanceof PaletteGradientColor) {
				PaletteGradientColor gradientColor = (PaletteGradientColor) paletteColor;
				List<ColorValue> colorsList = gradientColor.getColorPalette().getColors();
				int[] colors = new int[colorsList.size()];
				for (int i = 0; i < colorsList.size(); i++) {
					ColorValue value = colorsList.get(i);
					colors[i] = Color.argb(value.a, value.r, value.g, value.b);
				}
				GradientDrawable gradientDrawable = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, colors);
				gradientDrawable.setGradientType(GradientDrawable.LINEAR_GRADIENT);
				gradientDrawable.setShape(GradientDrawable.RECTANGLE);
				gradientDrawable.setCornerRadius(AndroidUtils.dpToPx(app, 2));
				icon.setImageDrawable(gradientDrawable);

				title.setText(gradientColor.getPaletteName());
				StringBuilder descriptionBuilder = new StringBuilder();
				List<ColorValue> colorValues = gradientColor.getColorPalette().getColors();
				for (int i = 0; i < colorValues.size(); i++) {
					if (i != 0) {
						descriptionBuilder.append(" • ");
					}
					descriptionBuilder.append(colorValues.get(i).val);
				}
				description.setText(descriptionBuilder);
				bottomDivider.setVisibility(View.VISIBLE);
				verticalDivider.setVisibility(View.GONE);
				endButton.setVisibility(View.GONE);
			}
		}
	}
}