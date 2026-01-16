package net.osmand.plus.card.color.palette.gradient;

import static android.graphics.Typeface.BOLD;
import static net.osmand.IndexConstants.TXT_EXT;
import static net.osmand.plus.helpers.ColorPaletteHelper.GRADIENT_ID_SPLITTER;
import static net.osmand.plus.helpers.ColorPaletteHelper.ROUTE_PREFIX;
import static net.osmand.plus.utils.UiUtilities.createSpannableString;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatRadioButton;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.card.color.palette.gradient.AllGradientsPaletteAdapter.GradientViewHolder;
import net.osmand.plus.card.color.palette.main.data.PaletteColor;
import net.osmand.plus.card.color.palette.main.data.PaletteSortingMode;
import net.osmand.plus.helpers.ColorPaletteHelper;
import net.osmand.plus.plugins.srtm.TerrainMode;
import net.osmand.plus.plugins.srtm.TerrainMode.TerrainType;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.alert.AlertDialogData;
import net.osmand.plus.widgets.alert.CustomAlert;
import net.osmand.plus.widgets.popup.PopUpMenu;
import net.osmand.plus.widgets.popup.PopUpMenuDisplayData;
import net.osmand.plus.widgets.popup.PopUpMenuItem;
import net.osmand.shared.ColorPalette.ColorValue;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

public class AllGradientsPaletteAdapter extends RecyclerView.Adapter<GradientViewHolder> {

	private final OsmandApplication app;
	private final GradientColorsPaletteController controller;
	private final ColorPaletteHelper colorPaletteHelper;
	private List<PaletteColor> colors;
	private final LayoutInflater themedInflater;
	private final boolean nightMode;

	public AllGradientsPaletteAdapter(@NonNull OsmandApplication app, @NonNull Context context,
	                                  @NonNull GradientColorsPaletteController controller, boolean nightMode) {
		this.app = app;
		this.nightMode = nightMode;
		this.controller = controller;
		this.colorPaletteHelper = app.getColorPaletteHelper();
		this.colors = controller.getColors(PaletteSortingMode.LAST_USED_TIME);
		this.themedInflater = UiUtilities.getInflater(context, nightMode);
		setHasStableIds(true);
	}

	@NonNull
	@Override
	public GradientViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		View itemView = themedInflater.inflate(R.layout.gradient_palette_item, parent, false);
		return new GradientViewHolder(app, controller, itemView);
	}

	@Override
	public void onBindViewHolder(@NonNull GradientViewHolder holder, int position) {
		PaletteColor paletteColor = colors.get(position);
		boolean isSelected = controller.isSelectedColor(paletteColor);
		holder.onBindViewHolder(paletteColor, isSelected, nightMode);
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
		if (paletteColor instanceof PaletteGradientColor) {
			return ((PaletteGradientColor) paletteColor).getStringId().hashCode();
		}
		return paletteColor.getId();
	}

	@SuppressLint("NotifyDataSetChanged")
	public void update() {
		this.colors = controller.getColors(PaletteSortingMode.LAST_USED_TIME);
		notifyDataSetChanged();
	}

	public void duplicatePalette(@NonNull Object gradientType, @NonNull String fileName) {
		colorPaletteHelper.duplicateGradient(fileName, duplicated -> {
			if (duplicated) {
				reloadGradientColors(gradientType);
			}
		});
	}

	public void showDeleteDialog(@NonNull Context ctx, @NonNull String displayName,
	                             @NonNull Object gradientType, @NonNull String fileName) {
		int warningColor = ColorUtilities.getColor(app, R.color.deletion_color_warning);
		int textColor = ColorUtilities.getSecondaryTextColor(ctx, nightMode);

		AlertDialogData dialogData = new AlertDialogData(ctx, nightMode)
				.setTitle(ctx.getString(R.string.delete_palette))
				.setNegativeButton(R.string.shared_string_cancel, null)
				.setPositiveButton(R.string.shared_string_delete, ((dialog, which) -> colorPaletteHelper.deleteGradient(fileName, deleted -> {
					if (deleted) {
						reloadGradientColors(gradientType);
					}
				})))
				.setPositiveButtonTextColor(warningColor);

		String description = ctx.getString(R.string.delete_colors_palette_dialog_summary, displayName);
		SpannableString spannable = createSpannableString(description, BOLD, displayName);
		UiUtilities.setSpan(spannable, new ForegroundColorSpan(textColor), description, description);
		CustomAlert.showSimpleMessage(dialogData, spannable);
	}

	private void reloadGradientColors(@NonNull Object gradientType) {
		if (gradientType instanceof TerrainType) {
			TerrainMode.reloadTerrainMods(app);
		}
		controller.reloadGradientColors();
	}

	public class GradientViewHolder extends RecyclerView.ViewHolder {
		private final OsmandApplication app;
		private final GradientColorsPaletteController controller;
		public final AppCompatRadioButton radioButton;
		public final ImageView icon;
		public final TextView title;
		public final TextView description;
		public final ImageButton menuButton;
		public final View bottomDivider;
		public final View verticalDivider;

		public GradientViewHolder(@NonNull OsmandApplication app, @NonNull GradientColorsPaletteController controller, @NonNull View itemView) {
			super(itemView);
			this.app = app;
			this.controller = controller;
			radioButton = itemView.findViewById(R.id.compound_button);
			icon = itemView.findViewById(R.id.icon);
			title = itemView.findViewById(R.id.title);
			description = itemView.findViewById(R.id.description);
			bottomDivider = itemView.findViewById(R.id.divider_bottom);
			verticalDivider = itemView.findViewById(R.id.vertical_end_button_divider);
			menuButton = itemView.findViewById(R.id.menu_button);
		}

		public void onBindViewHolder(@NonNull PaletteColor paletteColor, boolean isSelected, boolean nightMode) {
			UiUtilities.setupCompoundButton(nightMode, ColorUtilities.getActiveColor(app, nightMode), radioButton);
			radioButton.setChecked(isSelected);
			if (paletteColor instanceof PaletteGradientColor gradientColor) {
				List<ColorValue> colorsList = gradientColor.getColorPalette().getColors();
				int[] colors = new int[colorsList.size()];
				for (int i = 0; i < colorsList.size(); i++) {
					ColorValue value = colorsList.get(i);
					colors[i] = Color.argb(value.getA(), value.getR(), value.getG(), value.getB());
				}
				GradientDrawable gradientDrawable = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, colors);
				gradientDrawable.setGradientType(GradientDrawable.LINEAR_GRADIENT);
				gradientDrawable.setShape(GradientDrawable.RECTANGLE);
				gradientDrawable.setCornerRadius(AndroidUtils.dpToPx(app, 2));
				icon.setImageDrawable(gradientDrawable);

				String titleString = gradientColor.getDisplayName();
				title.setText(titleString);

				StringBuilder descriptionBuilder = new StringBuilder();
				List<ColorValue> colorValues = gradientColor.getColorPalette().getColors();
				for (int i = 0; i < colorValues.size(); i++) {
					if (i != 0) {
						descriptionBuilder.append(" • ");
					}
					String formattedValue = String.valueOf(colorValues.get(i).getValue());
					if (controller.getGradientType() instanceof TerrainType) {
						formattedValue = GradientUiHelper.formatTerrainTypeValues((float) colorValues.get(i).getValue());
					}
					descriptionBuilder.append(formattedValue);
				}
				description.setText(descriptionBuilder);
				bottomDivider.setVisibility(View.VISIBLE);
				verticalDivider.setVisibility(View.GONE);
				setupMenuButton(gradientColor, nightMode, isSelected);
			}
		}

		private void setupMenuButton(@NonNull PaletteGradientColor gradientColor, boolean nightMode, boolean isSelected) {
			Object gradientType = controller.getGradientType();
			boolean isDefaultColor;
			String displayName;
			String fileName;

			if (gradientType instanceof TerrainType) {
				TerrainMode terrainMode = TerrainMode.getMode((TerrainType) gradientType, gradientColor.getPaletteName());
				if (terrainMode == null) {
					menuButton.setVisibility(View.GONE);
					return;
				}
				displayName = terrainMode.getKeyName();
				fileName = terrainMode.getMainFile();
				isDefaultColor = terrainMode.isDefaultMode();
			} else {
				displayName = gradientColor.getDisplayName();
				fileName = ROUTE_PREFIX + gradientColor.getTypeName() + GRADIENT_ID_SPLITTER + displayName + TXT_EXT;
				isDefaultColor = gradientColor.getPaletteName().equals(PaletteGradientColor.DEFAULT_NAME);
			}

			menuButton.setVisibility(View.VISIBLE);
			menuButton.setOnClickListener(view -> showItemOptionsMenu(gradientType, displayName, fileName, isDefaultColor, view, nightMode, isSelected));
		}

		public void showItemOptionsMenu(@NonNull Object gradientType, @NonNull String displayName,
		                                @NonNull String fileName, boolean isDefaultColor,
		                                @NonNull View view, boolean nightMode, boolean isSelected) {
			List<PopUpMenuItem> items = new ArrayList<>();
			items.add(new PopUpMenuItem.Builder(app)
					.setTitleId(R.string.shared_string_duplicate)
					.setIcon(getContentIcon(R.drawable.ic_action_copy))
					.setOnClickListener(v -> duplicatePalette(gradientType, fileName))
					.create());

			if (!isDefaultColor && !isSelected) {
				items.add(new PopUpMenuItem.Builder(app)
						.setTitleId(R.string.shared_string_remove)
						.setIcon(getContentIcon(R.drawable.ic_action_delete_outlined))
						.setOnClickListener(item -> showDeleteDialog(view.getContext(), displayName, gradientType, fileName))
						.create());
			}

			PopUpMenuDisplayData displayData = new PopUpMenuDisplayData();
			displayData.anchorView = view;
			displayData.menuItems = items;
			displayData.nightMode = nightMode;
			PopUpMenu.show(displayData);
		}

		@Nullable
		private Drawable getContentIcon(@DrawableRes int id) {
			return app.getUIUtilities().getThemedIcon(id);
		}
	}
}