package net.osmand.plus.card.color.palette.gradient;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.GradientChart;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;

import net.osmand.plus.R;
import net.osmand.plus.card.color.palette.gradient.v2.GradientColorsPaletteAdapter;
import net.osmand.plus.card.color.palette.gradient.v2.GradientColorsPaletteController;
import net.osmand.plus.card.color.palette.main.v2.IColorsPalette;
import net.osmand.plus.charts.ChartUtils;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.tools.HorizontalSpaceItemDecoration;
import net.osmand.shared.ColorPalette;
import net.osmand.shared.palette.domain.PaletteCategory;
import net.osmand.shared.palette.domain.PaletteItem;

public class GradientColorsPaletteCard extends BaseCard implements IColorsPalette {

	public static final float MAX_ALTITUDE_ADDITION = 50f;

	private final GradientColorsPaletteController controller;
	private final GradientColorsPaletteAdapter paletteAdapter;
	private RecyclerView rvColors;


	public GradientColorsPaletteCard(@NonNull FragmentActivity activity,
	                                 @NonNull GradientColorsPaletteController controller) {
		this(activity, controller, true);
	}

	public GradientColorsPaletteCard(@NonNull FragmentActivity activity,
	                                 @NonNull GradientColorsPaletteController controller,
	                                 boolean usedOnMap) {
		super(activity, usedOnMap);
		this.controller = controller;

		controller.bindPalette(this);
		paletteAdapter = new GradientColorsPaletteAdapter(activity, controller, nightMode);
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.card_gradient_colors_palette;
	}

	@Override
	protected void updateContent() {
		setupColorsPalette();
		updateCard();
	}

	private void updateCard() {
		setupAllColorsButton();
		askScrollToTargetColorPosition(controller.getSelectedPaletteItem(), false);
		updateChart();
	}

	private void updateChart() {
		PaletteItem item = controller.getSelectedPaletteItem();
		if (!(item instanceof PaletteItem.Gradient gradientItem)) {
			return;
		}

		ColorPalette colorPalette = gradientItem.getColorPalette();
		GradientChart chart = view.findViewById(R.id.chart);

		int labelsColor = ColorUtilities.getPrimaryTextColor(app, nightMode);
		int xAxisGridColor = AndroidUtils.getColorFromAttr(app, R.attr.chart_x_grid_line_axis_color);

		ChartUtils.setupGradientChart(app, chart, 9, 24, false, xAxisGridColor, labelsColor);

		// TODO: create and use GradientPaletteCategory
		PaletteCategory paletteCategory = gradientItem.getPaletteCategory();
		IAxisValueFormatter formatter = GradientUiHelper.getGradientTypeFormatter(app, paletteCategory, controller.getAnalysis());

		chart.setData(ChartUtils.buildGradientChart(app, chart, colorPalette, formatter, nightMode));
		chart.notifyDataSetChanged();
		chart.invalidate();
	}

	private void setupColorsPalette() {
		rvColors = view.findViewById(R.id.colors_list);
		rvColors.setLayoutManager(new LinearLayoutManager(activity, RecyclerView.HORIZONTAL, false));
		rvColors.addItemDecoration(new HorizontalSpaceItemDecoration(getDimen(R.dimen.content_padding_small_half)));
		rvColors.setClipToPadding(false);
		rvColors.setAdapter(paletteAdapter);
	}

	private void setupAllColorsButton() {
		View buttonAllColors = view.findViewById(R.id.button_all_colors);
		buttonAllColors.setOnClickListener(v -> controller.onAllColorsButtonClicked(activity));
		updateAllColorsButton();
	}

	private void updateAllColorsButton() {
		View buttonAllColors = view.findViewById(R.id.button_all_colors);
		int controlsAccentColor = controller.getControlsAccentColor(nightMode);
		UiUtilities.setupListItemBackground(activity, buttonAllColors, controlsAccentColor);
	}

	private void askScrollToTargetColorPosition(@Nullable PaletteItem targetItem,
	                                            boolean smoothScroll) {
		if (targetItem == null) {
			return;
		}
		int targetPosition = paletteAdapter.indexOf(targetItem);
		LinearLayoutManager lm = (LinearLayoutManager) rvColors.getLayoutManager();
		int firstVisiblePosition = lm != null ? lm.findFirstCompletelyVisibleItemPosition() : 0;
		int lastVisiblePosition = lm != null ? lm.findLastCompletelyVisibleItemPosition() : paletteAdapter.getItemCount();
		if (targetPosition < firstVisiblePosition || targetPosition > lastVisiblePosition) {
			if (smoothScroll) {
				rvColors.smoothScrollToPosition(targetPosition);
			} else {
				rvColors.scrollToPosition(targetPosition);
			}
		}
	}

	@Override
	public void updatePaletteItems(@Nullable PaletteItem targetItem) {
		updateCard();
		paletteAdapter.updateItemsList();
	}

	@Override
	public void updatePaletteSelection(@Nullable PaletteItem oldItem, @NonNull PaletteItem newItem) {
		paletteAdapter.askNotifyItemChanged(oldItem);
		paletteAdapter.askNotifyItemChanged(newItem);
		askScrollToTargetColorPosition(newItem, true);
		if (controller.isAccentColorCanBeChanged()) {
			updateAllColorsButton();
		}
		updateChart();
	}
}
