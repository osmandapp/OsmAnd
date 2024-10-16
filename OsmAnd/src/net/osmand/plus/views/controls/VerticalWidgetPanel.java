package net.osmand.plus.views.controls;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.ViewParent;
import android.widget.LinearLayout;
import android.widget.TableLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListUpdateCallback;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.layers.MapInfoLayer.TextState;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.MapWidgetRegistry;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.views.mapwidgets.widgetinterfaces.ISupportMultiRow;
import net.osmand.plus.views.mapwidgets.widgetinterfaces.ISupportWidgetResizing;
import net.osmand.plus.views.mapwidgets.widgets.LanesWidget;
import net.osmand.plus.views.mapwidgets.widgets.MapMarkersBarWidget;
import net.osmand.plus.views.mapwidgets.widgets.MapWidget;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class VerticalWidgetPanel extends LinearLayout implements WidgetsContainer {

	private final OsmandApplication app;
	private final OsmandSettings settings;
	private final MapWidgetRegistry widgetRegistry;

	private Map<Integer, Row> visibleRows = new HashMap<>();
	private boolean topPanel;
	private boolean nightMode;

	public VerticalWidgetPanel(@NonNull Context context) {
		this(context, null);
	}

	public VerticalWidgetPanel(@NonNull Context context, @Nullable AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public VerticalWidgetPanel(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		this(context, attrs, defStyleAttr, 0);
	}

	public VerticalWidgetPanel(@NonNull Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		app = (OsmandApplication) context.getApplicationContext();
		settings = app.getSettings();
		nightMode = app.getDaynightHelper().isNightMode();
		widgetRegistry = app.getOsmandMap().getMapLayers().getMapWidgetRegistry();
		definePanelSide(context, attrs);
		init();
		applyShadow();
	}

	private void definePanelSide(@NonNull Context context, @Nullable AttributeSet attrs) {
		TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.VerticalWidgetPanel);
		topPanel = typedArray.getBoolean(R.styleable.VerticalWidgetPanel_topPanel, true);
		typedArray.recycle();
	}

	private void init() {
		removeAllViews();

		ApplicationMode appMode = settings.getApplicationMode();
		List<MapWidget> flatOrderedWidgets = new ArrayList<>();
		List<Set<MapWidgetInfo>> pagedWidgets = getWidgetsToShow(appMode, flatOrderedWidgets);

		for (int i = 0; i < pagedWidgets.size(); i++) {
			List<MapWidgetInfo> rowWidgets = new ArrayList<>(pagedWidgets.get(i));
			Row row = new Row(rowWidgets, flatOrderedWidgets);
			addView(row.view);
			visibleRows.put(i, row);
		}
		updateRows();
	}

	private boolean isAnyRowVisible() {
		for (Row row : visibleRows.values()) {
			if (row.isAnyWidgetVisible()) {
				return true;
			}
		}
		return false;
	}

	private void applyShadow() {
		setClipToPadding(false);
		setOutlineProvider(ViewOutlineProvider.BOUNDS);
		ViewCompat.setElevation(this, isAnyRowVisible() ? 5f : 0);
	}

	private void updateVisibility() {
		AndroidUiHelper.updateVisibility(this, isAnyRowVisible());
	}

	public void update(@Nullable DrawSettings drawSettings) {
		nightMode = drawSettings != null ? drawSettings.isNightMode() : nightMode;
		Map<Integer, Row> newRows = new HashMap<>();

		ApplicationMode appMode = settings.getApplicationMode();
		List<MapWidget> flatOrderedWidgets = new ArrayList<>();
		List<Set<MapWidgetInfo>> pagedWidgets = getWidgetsToShow(appMode, flatOrderedWidgets);

		for (int i = 0; i < pagedWidgets.size(); i++) {
			List<MapWidgetInfo> rowWidgets = new ArrayList<>(pagedWidgets.get(i));
			Row row = new Row(rowWidgets, flatOrderedWidgets);
			newRows.put(i, row);
		}

		PagesDiffUtilCallback diffUtilCallback = new PagesDiffUtilCallback(visibleRows, newRows);
		DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffUtilCallback);
		diffResult.dispatchUpdatesTo(new ListUpdateCallback() {
			@Override
			public void onInserted(int position, int count) {
				for (int i = 0; i < count; i++) {
					Row row = newRows.get(position + i);
					if (row == null) {
						break;
					}
					View widgetView = row.view;
					ViewParent viewParent = widgetView.getParent();
					if (viewParent instanceof ViewGroup) {
						((ViewGroup) viewParent).removeView(widgetView);
					}
					row.setupRow(i == count - 1);
					addView(row.view, position + i);
				}
				visibleRows = newRows;
				applyShadow();
				updateVisibility();
			}

			@Override
			public void onRemoved(int position, int count) {
				List<View> viewsToDelete = new ArrayList<>();
				for (int i = 0; i < count; i++) {
					viewsToDelete.add(getChildAt(position + i));
				}
				for (View view : viewsToDelete) {
					removeView(view);
				}
				visibleRows = newRows;
				applyShadow();
				updateVisibility();
			}

			@Override
			public void onMoved(int fromPosition, int toPosition) {
			}

			@Override
			public void onChanged(int position, int count, @Nullable Object payload) {
				for (int i = 0; i < count; i++) {
					removeViewAt(position + i);
					Row row = newRows.get(position + i);
					if (row != null) {
						row.setupRow(i == count - 1);
						addView(row.view, position + i);
					}
				}
				visibleRows = newRows;
				applyShadow();
				updateVisibility();
			}
		});
		updateVisibility();
	}

	public void updateRow(@NonNull MapWidget widget) {
		Iterator<Row> rowIterator = visibleRows.values().iterator();
		while (rowIterator.hasNext()) {
			Row row = rowIterator.next();
			for (MapWidgetInfo widgetInfo : row.enabledMapWidgets) {
				if (Algorithms.objectEquals(widget, widgetInfo.widget)) {
					row.updateRow(!rowIterator.hasNext());
					break;
				}
			}
		}
		updateVisibility();
	}

	private void updateDividerColors(boolean nightMode) {
		for (Row row : visibleRows.values()) {
			row.updateDividerColor(nightMode);
		}
	}

	public void updateRows() {
		Iterator<Row> rowIterator = visibleRows.values().iterator();
		while (rowIterator.hasNext()) {
			Row row = rowIterator.next();
			row.updateRow(!rowIterator.hasNext());
		}
	}

	public void updateColors(@NonNull TextState textState) {
		boolean oldNightMode = nightMode;
		nightMode = textState.night;
		invalidate();
		updateRows();
		if (oldNightMode != nightMode) {
			updateDividerColors(nightMode);
		}
	}

	private void updateValueAlign(List<MapWidgetInfo> widgetsInRow, int visibleViewsInRowCount) {
		for (MapWidgetInfo widgetInfo : widgetsInRow) {
			if(widgetInfo.widget instanceof ISupportMultiRow supportMultiRow){
				supportMultiRow.updateValueAlign(visibleViewsInRowCount <= 1);
			}
		}
	}

	private void updateFullRowState(List<MapWidgetInfo> widgetsInRow, int visibleViewsInRowCount) {
		for (MapWidgetInfo widgetInfo : widgetsInRow) {
			if(widgetInfo.widget instanceof ISupportMultiRow supportMultiRow){
				supportMultiRow.updateFullRowState(visibleViewsInRowCount <= 1);
			}
		}
	}

	@NonNull
	protected List<Set<MapWidgetInfo>> getWidgetsToShow(ApplicationMode mode, List<MapWidget> widgetsToShow) {
		Set<MapWidgetInfo> allPanelWidget = widgetRegistry.getWidgetsForPanel(getWidgetsPanel());

		Map<Integer, Set<MapWidgetInfo>> rowWidgetMap = new TreeMap<>();
		for (MapWidgetInfo widgetInfo : allPanelWidget) {
			if (widgetInfo.isEnabledForAppMode(mode)) {
				addWidgetViewToPage(rowWidgetMap, widgetInfo.pageIndex, widgetInfo);
				widgetsToShow.add(widgetInfo.widget);
			} else {
				widgetInfo.widget.detachView(getWidgetsPanel());
			}
		}
		return new ArrayList<>(rowWidgetMap.values());
	}

	private void addWidgetViewToPage(@NonNull Map<Integer, Set<MapWidgetInfo>> mapInfoWidgets,
									 int pageIndex, @NonNull MapWidgetInfo mapWidgetInfo) {
		Set<MapWidgetInfo> widgetsViews = mapInfoWidgets.get(pageIndex);
		if (widgetsViews == null) {
			widgetsViews = new TreeSet<>();
			mapInfoWidgets.put(pageIndex, widgetsViews);
		}
		widgetsViews.add(mapWidgetInfo);
	}

	private void attachViewToRow(@NonNull MapWidget widget, @NonNull ViewGroup container, @NonNull List<MapWidget> followingWidgets) {
		View widgetView = widget.getView();
		ViewParent viewParent = widgetView.getParent();
		if (viewParent instanceof ViewGroup) {
			((ViewGroup) viewParent).removeView(widgetView);
		}

		widgetView.setLayoutParams(new TableLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, 1f));
		widget.attachView(container, getWidgetsPanel(), followingWidgets);
	}

	@NonNull
	private List<MapWidget> getFollowingWidgets(@NonNull MapWidget widget, @NonNull List<MapWidget> widgetsToShow) {
		List<MapWidget> followingWidgets = new ArrayList<>();
		int widgetIndex = widgetsToShow.indexOf(widget);
		if (widgetIndex != -1 && widgetIndex + 1 < widgetsToShow.size()) {
			followingWidgets = widgetsToShow.subList(widgetIndex + 1, widgetsToShow.size());
		}
		return followingWidgets;
	}

	@NonNull
	private WidgetsPanel getWidgetsPanel() {
		return topPanel ? WidgetsPanel.TOP : WidgetsPanel.BOTTOM;
	}

	private void addVerticalDivider(@NonNull ViewGroup container) {
		inflate(UiUtilities.getThemedContext(getContext(), nightMode), R.layout.vertical_divider, container);
	}

	private class Row {

		private final View view;
		private final View bottomDivider;
		private final LinearLayout rowContainer;

		private final List<MapWidgetInfo> enabledMapWidgets = new ArrayList<>();
		private final List<MapWidget> flatOrderedWidgets;

		Row(@NonNull List<MapWidgetInfo> rowWidgets, @NonNull List<MapWidget> flatOrderedWidgets) {
			this.view = inflate(UiUtilities.getThemedContext(getContext(), nightMode), R.layout.vertical_widget_row, null);
			this.bottomDivider = view.findViewById(R.id.bottom_divider);
			this.rowContainer = view.findViewById(R.id.widgets_container);
			this.flatOrderedWidgets = flatOrderedWidgets;

			ApplicationMode appMode = settings.getApplicationMode();
			for (int j = 0; j < rowWidgets.size(); j++) {
				MapWidgetInfo widgetInfo = rowWidgets.get(j);
				if (widgetInfo.isEnabledForAppMode(appMode)) {
					enabledMapWidgets.add(widgetInfo);
				} else {
					widgetInfo.widget.detachView(getWidgetsPanel());
				}
			}
		}

		public void updateRow(boolean lastRow) {
			int visibleViewsInRowCount = 0;
			boolean showBottomDivider = true;

			for (int i = 0; i < enabledMapWidgets.size(); i++) {
				MapWidget widget = enabledMapWidgets.get(i).widget;
				if (widget.isViewVisible()) {
					visibleViewsInRowCount++;
					int nextWidgetIndex = i + 1;
					showHideVerticalDivider(i, nextWidgetIndex < enabledMapWidgets.size() && enabledMapWidgets.get(nextWidgetIndex).widget.isViewVisible());
				} else {
					showHideVerticalDivider(i, false);
				}
				if (widget instanceof MapMarkersBarWidget || widget instanceof LanesWidget) {
					showBottomDivider = false;
				}
			}
			updateFullRowState(enabledMapWidgets, visibleViewsInRowCount);
			updateValueAlign(enabledMapWidgets, visibleViewsInRowCount);
			AndroidUiHelper.updateVisibility(bottomDivider, (visibleViewsInRowCount > 0 && showBottomDivider) && !lastRow);
		}

		public void updateDividerColor(boolean nightMode) {
			for (int i = 1; i <= rowContainer.getChildCount(); i++) {
				if (i % 2 == 0) {
					View divider = rowContainer.getChildAt(i - 1).findViewById(R.id.vertical_divider);
					if (divider != null) {
						divider.setBackgroundColor(ContextCompat.getColor(app, nightMode ? R.color.divider_color_dark : R.color.divider_color_light));
					}
				}
			}
			bottomDivider.setBackgroundColor(ContextCompat.getColor(app, nightMode ? R.color.divider_color_dark : R.color.divider_color_light));
		}

		private void showHideVerticalDivider(int widgetIndex, boolean show) {
			int dividerIndexInContainer = (widgetIndex * 2) + 1;
			if (widgetIndex >= 0 && dividerIndexInContainer < rowContainer.getChildCount()) {
				AndroidUiHelper.updateVisibility(rowContainer.getChildAt(dividerIndexInContainer), show);
			}
		}

		public void setupRow(boolean lastRow) {
			MapWidgetInfo firstMapWidgetInfoInRow = null;
			for (int j = 0; j < enabledMapWidgets.size(); j++) {
				MapWidgetInfo widgetInfo = enabledMapWidgets.get(j);
				MapWidget widget = widgetInfo.widget;

				if (firstMapWidgetInfoInRow == null) {
					firstMapWidgetInfoInRow = widgetInfo;
				} else {
					setupWidgetSize(firstMapWidgetInfoInRow, widgetInfo);
				}
				attachViewToRow(widget, rowContainer, getFollowingWidgets(widget, flatOrderedWidgets));
				int nextElementIndex = j + 1;
				if (nextElementIndex < enabledMapWidgets.size()) {
					addVerticalDivider(rowContainer);
				}
			}
			updateRow(lastRow);
		}

		private void setupWidgetSize(@NonNull MapWidgetInfo firstWidgetInfo, @NonNull MapWidgetInfo widgetInfo) {
			if (firstWidgetInfo.widget instanceof ISupportWidgetResizing firstResizableWidget && widgetInfo.widget instanceof ISupportWidgetResizing secondResizableWidget) {
				if (firstResizableWidget.getWidgetSizePref().get() != secondResizableWidget.getWidgetSizePref().get()) {
					secondResizableWidget.getWidgetSizePref().set(firstResizableWidget.getWidgetSizePref().get());
					secondResizableWidget.recreateView();
				}
			}
		}

		protected boolean isAnyWidgetVisible() {
			for (MapWidgetInfo widgetInfo : enabledMapWidgets) {
				if (widgetInfo.widget.isViewVisible()) {
					return true;
				}
			}
			return false;
		}
	}

	private static class PagesDiffUtilCallback extends DiffUtil.Callback {

		private final Map<Integer, Row> oldRows;
		private final Map<Integer, Row> newRows;

		public PagesDiffUtilCallback(@NonNull Map<Integer, Row> oldRows, @NonNull Map<Integer, Row> newRows) {
			this.oldRows = oldRows;
			this.newRows = newRows;
		}

		@Override
		public int getOldListSize() {
			return oldRows.size();
		}

		@Override
		public int getNewListSize() {
			return newRows.size();
		}

		@Override
		public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
			return oldItemPosition == newItemPosition;
		}

		@Override
		public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
			Row oldRow = oldRows.get(oldItemPosition);
			Row newRow = newRows.get(newItemPosition);
			List<MapWidgetInfo> oldMapWidgets = oldRow != null ? oldRow.enabledMapWidgets : Collections.emptyList();
			List<MapWidgetInfo> newMapWidgets = newRow != null ? newRow.enabledMapWidgets : Collections.emptyList();
			return Algorithms.objectEquals(oldMapWidgets, newMapWidgets);
		}
	}
}