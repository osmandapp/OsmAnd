package net.osmand.plus.card.color.palette.gradient.editor.section

import android.view.View
import android.view.ViewGroup
import com.github.mikephil.charting.charts.GradientChart
import com.github.mikephil.charting.data.LineData
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.card.color.palette.gradient.GradientFormatter
import net.osmand.plus.card.color.palette.gradient.editor.data.EditorUiState
import net.osmand.plus.card.color.palette.gradient.editor.data.GradientStepData
import net.osmand.plus.charts.ChartUtils
import net.osmand.plus.palette.view.PaletteElements
import net.osmand.plus.utils.AndroidUtils
import net.osmand.plus.utils.ColorUtilities
import net.osmand.plus.widgets.chips.ChipItem
import net.osmand.plus.widgets.chips.HorizontalChipsView
import net.osmand.shared.palette.data.toColorPalette
import net.osmand.shared.palette.domain.GradientPoint

class ChartSection(
	rootView: View,
	app: OsmandApplication,
	nightMode: Boolean,
): UiSection(app, nightMode) {
	private val chart: GradientChart = rootView.findViewById(R.id.chart)
	private val chipsView: HorizontalChipsView = rootView.findViewById(R.id.gradient_step_chips)
	private val activeColor = ColorUtilities.getActiveColor(app, nightMode)

	lateinit var onStepClicked: (GradientStepData) -> Unit
	lateinit var onAddClicked: () -> Unit

	init {
		val labelColor = ColorUtilities.getPrimaryTextColor(app, nightMode)
		val axisColor = AndroidUtils.getColorFromAttr(app, R.attr.chart_x_grid_line_axis_color)

		ChartUtils.setupGradientChart(app, chart, 16f, 16f, false, axisColor, labelColor)
		chart.minOffset = 0f

		chipsView.setOnSelectChipListener { chip ->
			onStepClicked(chip.tag as GradientStepData)
			false
		}

		val paletteElements = PaletteElements(rootView.context, nightMode)
		val buttonContainer = rootView.findViewById<ViewGroup>(R.id.add_button_container)
		buttonContainer.addView(paletteElements.createAddButtonView(buttonContainer))
		buttonContainer.setOnClickListener { onAddClicked() }
	}

	override fun update(oldUiState: EditorUiState?, newUiState: EditorUiState) {
		val oldState = oldUiState?.gradientState
		val newState = newUiState.gradientState

		val needUpdate = oldState != newState || chart.data == null || chart.data.dataSetCount == 0

		if (needUpdate) {
			// 1. Update Chart
			val fileType = newState.gradientFileType
			val formatter = GradientFormatter.getAxisFormatter(fileType, realDataLimits = null)

			val steps = newState.stepData
			val points = mutableListOf<GradientPoint>()
			steps.forEach { step ->
				if (!step.point.value.isNaN()) {
					points.add(step.point)
				}
			}
			val colorPalette = points.toColorPalette()
			chart.data = ChartUtils.buildGradientChart<LineData>(
				app,
				chart,
				colorPalette,
				formatter,
				nightMode
			)

			val selectedStep = newState.selectedItem
			val selectedIndex = steps.indexOf(selectedStep)
			if (selectedIndex != -1 && !steps[selectedIndex].point.value.isNaN()) {
				chart.highlightValue(selectedIndex.toFloat(), 0, false)
			} else {
				chart.highlightValue(null)
			}

			chart.notifyDataSetChanged()
			chart.invalidate()

			// 2. Update chips
			val chipItems = mutableListOf<ChipItem>()
			steps.forEach { step ->
				val chipItem = ChipItem(step.id)
				chipItem.title = step.label
				chipItem.contentDescription = step.label
				chipItem.tag = step
				chipItem.titleColor = activeColor
				chipItems.add(chipItem)
			}
			chipsView.setItems(chipItems)
			chipsView.notifyDataSetChanged()

			if (selectedStep != null) {
				val selected = chipsView.getChipById(selectedStep.id)
				selected?.let {
					chipsView.setSelected(selected)
					chipsView.smoothScrollTo(selected)
				}
			} else {
				chipsView.setSelected(null)
			}
		}
	}
}