package net.osmand.plus.card.color.palette.gradient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.github.mikephil.charting.charts.GradientChart;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;

import net.osmand.plus.R;
import net.osmand.plus.card.color.palette.gradient.v2.GradientPaletteController;
import net.osmand.plus.card.color.palette.main.ColorsPaletteCard;
import net.osmand.plus.palette.contract.IPaletteView;
import net.osmand.plus.charts.ChartUtils;
import net.osmand.plus.palette.view.binder.GradientViewBinder;
import net.osmand.plus.palette.view.binder.PaletteItemViewBinder;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.shared.ColorPalette;
import net.osmand.shared.palette.domain.PaletteCategory;
import net.osmand.shared.palette.domain.PaletteItem;

public class GradientColorsPaletteCard extends ColorsPaletteCard implements IPaletteView {

	private final GradientPaletteController controller;

	public GradientColorsPaletteCard(@NonNull FragmentActivity activity,
	                                 @NonNull GradientPaletteController controller) {
		super(activity, controller);
		this.controller = controller;
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.card_gradient_colors_palette;
	}

	@Override
	protected void updateContent() {
		super.updateContent();
		updateChart();
	}

	@Override
	public void updatePaletteItems(@Nullable PaletteItem targetItem) {
		super.updatePaletteItems(targetItem);
		updateChart();
	}

	@Override
	public void updatePaletteSelection(@Nullable PaletteItem oldItem, @NonNull PaletteItem newItem) {
		super.updatePaletteSelection(oldItem, newItem);
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

	@NonNull
	@Override
	public PaletteItemViewBinder createViewBinder() {
		return new GradientViewBinder(activity, nightMode);
	}
}
