package net.osmand.plus.views.controls;

import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.AVAILABLE_MODE;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.ENABLED_MODE;
import static net.osmand.plus.views.mapwidgets.MapWidgetRegistry.MATCHING_PANELS_MODE;

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

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.MapWidgetRegistry;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.views.mapwidgets.widgets.MapWidget;
import net.osmand.plus.views.mapwidgets.widgets.SimpleWidget;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class VerticalWidgetPanel extends LinearLayout {
	private static final int enabledWidgetsFilter = AVAILABLE_MODE | ENABLED_MODE | MATCHING_PANELS_MODE;

	private final OsmandApplication app;
	private MapActivity mapActivity;
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

	public void update() {
		removeAllViews();
		ApplicationMode mode = app.getSettings().getApplicationMode();
		List<MapWidget> widgetsToShow = getWidgetsToShow(mode);
		List<Set<MapWidgetInfo>> pagedWidgets = widgetRegistry.getPagedWidgetsForPanel(mapActivity, mode, getWidgetsPanel(), enabledWidgetsFilter);

		Iterator<Set<MapWidgetInfo>> rowIterator = pagedWidgets.listIterator();
		while (rowIterator.hasNext()) {
			boolean anyRowWidgetVisible = false;
			LinearLayout widgetRow = new LinearLayout(getContext());
			widgetRow.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
			widgetRow.setOrientation(LinearLayout.HORIZONTAL);

			List<MapWidgetInfo> widgetsInRow = new ArrayList<>(rowIterator.next());
			Iterator<MapWidgetInfo> widgetsIterator = widgetsInRow.listIterator();
			MapWidgetInfo firstMapWidgetInfoInRow = null;
			while (widgetsIterator.hasNext()) {
				MapWidgetInfo widgetInfo = widgetsIterator.next();
				MapWidget widget = widgetInfo.widget;
				if (firstMapWidgetInfoInRow == null) {
					firstMapWidgetInfoInRow = widgetInfo;
				} else {
					setupWidgetSize(firstMapWidgetInfoInRow, widgetInfo);
				}

				if (widgetInfo.isEnabledForAppMode(mode) && widget.isViewVisible()) {
					attachViewToRow(widget, widgetRow, getFollowingWidgets(widget, widgetsToShow));
					if (widgetsIterator.hasNext()) {
						addDivider(widgetRow, true);
					}
					if (widget instanceof SimpleWidget) {
						((SimpleWidget) widget).updateValueAlign(widgetsInRow.size() == 1);
					}
					anyRowWidgetVisible = true;
				} else {
					widgetInfo.widget.detachView(getWidgetsPanel());
				}
			}
			addView(widgetRow);
			if (rowIterator.hasNext() && anyRowWidgetVisible) {
				addDivider(this, false);
			}
		}
		requestLayout();
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

	private List<MapWidget> getWidgetsToShow(@NonNull ApplicationMode mode) {
		Set<MapWidgetInfo> widgets = widgetRegistry.getWidgetsForPanel(getWidgetsPanel());

		List<MapWidget> widgetsToShow = new ArrayList<>();
		for (MapWidgetInfo widgetInfo : widgets) {
			if (widgetInfo.isEnabledForAppMode(mode) && widgetInfo.widget.isViewVisible()) {
				widgetsToShow.add(widgetInfo.widget);
			} else {
				widgetInfo.widget.detachView(getWidgetsPanel());
			}
		}
		return widgetsToShow;
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
				.inflate(verticalDivider ? R.layout.vertical_widget_divider : R.layout.horizontal_widget_divider, viewGroup);
	}

	public void setMapActivity(@NonNull MapActivity mapActivity) {
		this.mapActivity = mapActivity;
	}
}
