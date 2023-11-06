package net.osmand.plus.views.controls;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.LinearLayout;
import android.widget.TableLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListUpdateCallback;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.views.layers.MapInfoLayer.TextState;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.MapWidgetRegistry;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.views.mapwidgets.widgets.LanesWidget;
import net.osmand.plus.views.mapwidgets.widgets.MapMarkersBarWidget;
import net.osmand.plus.views.mapwidgets.widgets.MapWidget;
import net.osmand.plus.views.mapwidgets.widgets.SimpleWidget;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class VerticalWidgetPanel extends LinearLayout {
	protected final OsmandSettings settings;
	private final MapWidgetRegistry widgetRegistry;

	private Map<Integer, Row> rows = new HashMap<>();
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
		OsmandApplication app = (OsmandApplication) context.getApplicationContext();
		settings = app.getSettings();
		nightMode = app.getDaynightHelper().isNightMode();
		widgetRegistry = app.getOsmandMap().getMapLayers().getMapWidgetRegistry();
		definePanelSide(context, attrs);
		init();
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
			rows.put(i, row);
		}
		updateRows();
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

		PagesDiffUtilCallback diffUtilCallback = new PagesDiffUtilCallback(rows, newRows);
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
					row.setupRow();
					addView(row.view, position + i);
				}
				rows = newRows;
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
				rows = newRows;
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
						row.setupRow();
						addView(row.view, position + i);
					}
				}
				rows = newRows;
			}
		});
	}

	public void updateRow(MapWidget widget) {
		for (Row row : rows.values()) {
			for (int i = 0; i < row.enabledMapWidgets.size(); i++) {
				if (widget.equals(row.enabledMapWidgets.get(i).widget)) {
					row.updateRow();
				}
			}
		}
	}

	public void updateRows() {
		for (Row row : rows.values()) {
			row.updateRow();
		}
	}

	public void updateColors(@NonNull TextState textState) {
		nightMode = textState.night;
		invalidate();
	}

	private void updateValueAlign(List<MapWidgetInfo> widgetsInRow, int visibleViewsInRowCount) {
		for (MapWidgetInfo widgetInfo : widgetsInRow) {
			if (widgetInfo.widget instanceof SimpleWidget) {
				((SimpleWidget) widgetInfo.widget).updateValueAlign(visibleViewsInRowCount <= 1);
			}
		}
	}

	@NonNull
	private List<Set<MapWidgetInfo>> getWidgetsToShow(ApplicationMode mode, List<MapWidget> widgetsToShow) {
		Set<MapWidgetInfo> allPanelWidget = widgetRegistry.getWidgetsForPanel(getWidgetsPanel());

		Map<Integer, Set<MapWidgetInfo>> rowWidgetMap = new TreeMap<>();
		for (MapWidgetInfo widgetInfo : allPanelWidget) {
			if (widgetInfo.isEnabledForAppMode(mode)) {
				addWidgetViewToPage(rowWidgetMap, widgetInfo.pageIndex, widgetInfo);
				if (widgetInfo.widget.isViewVisible()) {
					widgetsToShow.add(widgetInfo.widget);
				}
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
		if (widgetIndex != -1 && widgetIndex + 1 == widgetsToShow.size()) {
			followingWidgets = widgetsToShow.subList(widgetIndex + 1, widgetsToShow.size());
		}
		return followingWidgets;
	}

	@NonNull
	private WidgetsPanel getWidgetsPanel() {
		return topPanel ? WidgetsPanel.TOP : WidgetsPanel.BOTTOM;
	}

	private void addVerticalDivider(@NonNull ViewGroup container) {
		inflate(getContext(), R.layout.vertical_divider, container);
	}

	private class Row {
		protected ViewGroup view;
		protected View bottomDivider;
		protected LinearLayout rowContainer;

		protected List<MapWidgetInfo> enabledMapWidgets = new ArrayList<>();
		protected List<MapWidget> flatOrderedWidgets;

		public Row(List<MapWidgetInfo> rowWidgets, List<MapWidget> flatOrderedWidgets) {
			this.view = (ViewGroup) inflate(getContext(), R.layout.vertical_widget_row, null);
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

		public void updateRow() {
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
			updateValueAlign(enabledMapWidgets, visibleViewsInRowCount);
			AndroidUiHelper.updateVisibility(bottomDivider, visibleViewsInRowCount > 0 && showBottomDivider);
		}

		private void showHideVerticalDivider(int widgetIndex, boolean show) {
			int dividerIndexInContainer = (widgetIndex * 2) + 1;
			if (widgetIndex >= 0 && dividerIndexInContainer < rowContainer.getChildCount()) {
				AndroidUiHelper.updateVisibility(rowContainer.getChildAt(dividerIndexInContainer), show);
			}
		}

		public void setupRow() {
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
			updateRow();
		}

		private void setupWidgetSize(@NonNull MapWidgetInfo firstWidgetInfo, @NonNull MapWidgetInfo widgetInfo) {
			if (firstWidgetInfo.widget instanceof SimpleWidget && widgetInfo.widget instanceof SimpleWidget) {
				SimpleWidget firstSimpleWidget = (SimpleWidget) firstWidgetInfo.widget;
				SimpleWidget simpleWidget = (SimpleWidget) widgetInfo.widget;
				if (firstSimpleWidget.getWidgetSizePref().get() != simpleWidget.getWidgetSizePref().get()) {
					simpleWidget.getWidgetSizePref().set(firstSimpleWidget.getWidgetSizePref().get());
					simpleWidget.recreateView();
				}
			}
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
			return oldMapWidgets.equals(newMapWidgets);
		}
	}
}