package net.osmand.plus.views.controls;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.LinearLayout;
import android.widget.TableLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.views.layers.MapInfoLayer;
import net.osmand.plus.views.layers.base.OsmandMapLayer;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.MapWidgetRegistry;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.views.mapwidgets.widgets.MapWidget;
import net.osmand.plus.views.mapwidgets.widgets.SimpleWidget;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class VerticalWidgetPanel extends LinearLayout {
	private final OsmandApplication app;
	protected boolean nightMode;

	private final MapWidgetRegistry widgetRegistry;
	private boolean topPanel;

	public VerticalWidgetPanel(Context context) {
		this(context, null);
	}

	public VerticalWidgetPanel(Context context, @Nullable AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public VerticalWidgetPanel(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		this(context, attrs, defStyleAttr, 0);
	}

	public VerticalWidgetPanel(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		app = getMyApplication();
		widgetRegistry = app.getOsmandMap().getMapLayers().getMapWidgetRegistry();
		definePanelSide(context, attrs);
		nightMode = getMyApplication().getDaynightHelper().isNightMode();
	}

	private void definePanelSide(@NonNull Context context, @Nullable AttributeSet attrs) {
		TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.VerticalWidgetPanel);
		topPanel = typedArray.getBoolean(R.styleable.VerticalWidgetPanel_topPanel, true);
		typedArray.recycle();
	}

	@NonNull
	protected OsmandApplication getMyApplication() {
		return ((OsmandApplication) getContext().getApplicationContext());
	}

	public void update(@Nullable OsmandMapLayer.DrawSettings drawSettings) {
		if (drawSettings != null) {
			nightMode = drawSettings.isNightMode();
		}
		removeAllViews();
		ApplicationMode mode = app.getSettings().getApplicationMode();
		List<MapWidget> flatOrderedWidgets = new ArrayList<>();
		List<Set<MapWidgetInfo>> pagedWidgets = getWidgetsToShow(mode, flatOrderedWidgets);

		Iterator<Set<MapWidgetInfo>> rowIterator = pagedWidgets.listIterator();
		while (rowIterator.hasNext()) {
			int visibleViewsInRowCount = 0;
			MapWidgetInfo firstMapWidgetInfoInRow = null;
			List<MapWidgetInfo> widgetsInRow = new ArrayList<>(rowIterator.next());
			View row = inflate(getContext(), R.layout.vertical_widget_row, null);
			LinearLayout rowContainer = row.findViewById(R.id.widgets_container);

			for (int i = 0; i < widgetsInRow.size(); i++) {
				MapWidgetInfo widgetInfo = widgetsInRow.get(i);
				MapWidget widget = widgetInfo.widget;

				if (firstMapWidgetInfoInRow == null) {
					firstMapWidgetInfoInRow = widgetInfo;
				} else {
					setupWidgetSize(firstMapWidgetInfoInRow, widgetInfo);
				}
				if (widgetInfo.isEnabledForAppMode(mode)) {
					attachViewToRow(widget, rowContainer, getFollowingWidgets(widget, flatOrderedWidgets));
					int nextElementIndex = i + 1;
					if (nextElementIndex < widgetsInRow.size() && widgetsInRow.get(nextElementIndex).widget.isViewVisible()) {
						addDivider(rowContainer, true);
					}
					if (widget.isViewVisible()) {
						visibleViewsInRowCount++;
					}
				} else {
					widgetInfo.widget.detachView(getWidgetsPanel());
				}
			}

			updateValueAlign(widgetsInRow, visibleViewsInRowCount);
			addView(row);
			if (rowIterator.hasNext() && visibleViewsInRowCount > 0) {
				addDivider((ViewGroup) row, false);
			}
		}
		requestLayout();
	}

	public void updateColors(@NonNull MapInfoLayer.TextState textState) {
		this.nightMode = textState.night;
		invalidate();
	}

	private void updateValueAlign(List<MapWidgetInfo> widgetsInRow, int visibleViewsInRowCount) {
		for (MapWidgetInfo widgetInfo : widgetsInRow) {
			if (widgetInfo.widget instanceof SimpleWidget) {
				((SimpleWidget) widgetInfo.widget).updateValueAlign(visibleViewsInRowCount == 1);
			}
		}
	}

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

	private List<MapWidget> getFollowingWidgets(@NonNull MapWidget widget, @NonNull List<MapWidget> widgetsToShow) {
		List<MapWidget> followingWidgets = new ArrayList<>();
		int widgetIndex = widgetsToShow.indexOf(widget);
		if (widgetIndex != -1 && widgetIndex + 1 == widgetsToShow.size()) {
			followingWidgets = widgetsToShow.subList(widgetIndex + 1, widgetsToShow.size());
		}
		return followingWidgets;
	}

	private WidgetsPanel getWidgetsPanel() {
		return topPanel ? WidgetsPanel.TOP : WidgetsPanel.BOTTOM;
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

	private void addDivider(@NonNull ViewGroup viewGroup, boolean verticalDivider) {
		int themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;
		LayoutInflater.from(new ContextThemeWrapper(getContext(), themeRes))
				.inflate(verticalDivider ? R.layout.vertical_divider : R.layout.simple_divider_item, viewGroup);
	}
}
