package net.osmand.plus.views.controls;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.views.controls.WidgetsPagerAdapter.PageViewHolder;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.MapWidgetRegistry;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.views.mapwidgets.widgets.MapWidget;
import net.osmand.plus.views.mapwidgets.widgets.TextInfoWidget;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

public class WidgetsPagerAdapter extends RecyclerView.Adapter<PageViewHolder> {

	private final OsmandSettings settings;
	private final WidgetsPanel widgetsPanel;
	private final MapWidgetRegistry widgetRegistry;

	private VisiblePages visiblePages;
	private ViewHolderBindListener bindListener;

	public WidgetsPagerAdapter(@NonNull OsmandApplication app, @NonNull WidgetsPanel widgetsPanel) {
		this.widgetsPanel = widgetsPanel;
		settings = app.getSettings();
		widgetRegistry = app.getOsmandMap().getMapLayers().getMapWidgetRegistry();
		visiblePages = collectVisiblePages();
	}

	@NonNull
	public VisiblePages getVisiblePages() {
		return visiblePages;
	}

	public void setViewHolderBindListener(@Nullable ViewHolderBindListener bindListener) {
		this.bindListener = bindListener;
	}

	public void updateIfNeeded() {
		VisiblePages newVisiblePages = collectVisiblePages();
		PagesDiffUtilCallback diffUtilCallback = new PagesDiffUtilCallback(visiblePages, newVisiblePages);
		DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffUtilCallback);

		visiblePages = newVisiblePages;
		diffResult.dispatchUpdatesTo(this);
	}

	@NonNull
	@Override
	public PageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		LayoutInflater inflater = LayoutInflater.from(parent.getContext());
		return new PageViewHolder(inflater.inflate(R.layout.widgets_panel_page, parent, false));
	}

	@Override
	public void onBindViewHolder(@NonNull PageViewHolder holder, int position) {
		holder.container.removeAllViews();

		List<View> visibleWidgetsViews = visiblePages.getWidgetsViews(position);
		for (int i = 0; i < visibleWidgetsViews.size(); i++) {
			View widgetView = visibleWidgetsViews.get(i);
			if (widgetView.getParent() instanceof ViewGroup) {
				((ViewGroup) widgetView.getParent()).removeView(widgetView);
			}
			holder.container.addView(visibleWidgetsViews.get(i));

			boolean last = i + 1 == visibleWidgetsViews.size();
			AndroidUiHelper.updateVisibility(widgetView.findViewById(R.id.bottom_divider), !last);
		}

		if (bindListener != null) {
			bindListener.onViewHolderBound(holder, position);
		}
	}

	@Override
	public int getItemCount() {
		return Math.max(1, visiblePages.getCount());
	}

	@NonNull
	public VisiblePages collectVisiblePages() {
		Map<Integer, List<View>> visibleViews = new TreeMap<>();
		ApplicationMode appMode = settings.getApplicationMode();
		Set<MapWidgetInfo> widgetInfos = widgetRegistry.getWidgetsForPanel(widgetsPanel);

		collectVisibleViews(visibleViews, widgetInfos, appMode);

		return new VisiblePages(visibleViews);
	}

	public void collectVisibleViews(@NonNull Map<Integer, List<View>> visibleViews,
	                                @NonNull Set<MapWidgetInfo> widgets,
	                                @NonNull ApplicationMode appMode) {
		Map<Integer, List<MapWidget>> textInfoWidgets = new TreeMap<>();
		for (MapWidgetInfo widgetInfo : widgets) {
			if (widgetInfo.isEnabledForAppMode(appMode)) {
				MapWidget widget = widgetInfo.widget;
				addWidgetViewToPage(textInfoWidgets, widgetInfo.pageIndex, widget);
			}
		}

		for (Map.Entry<Integer, List<MapWidget>> entry : textInfoWidgets.entrySet()) {
			List<View> widgetsViews = new ArrayList<>();
			for (MapWidget widget : entry.getValue()) {
				if (widget.isViewVisible()) {
					widgetsViews.add(widget.getView());
				}
				if (widget instanceof TextInfoWidget infoWidget) {
					infoWidget.updateBannerVisibility(false);
				}
			}
			if (Algorithms.isEmpty(widgetsViews)) {
				MapWidget widget = entry.getValue().get(0);
				widgetsViews.add(widget.getView());
				if (widget instanceof TextInfoWidget infoWidget) {
					infoWidget.updateBannerVisibility(textInfoWidgets.size() > 1);
				}
			}
			visibleViews.put(entry.getKey(), widgetsViews);
		}
	}

	private void addWidgetViewToPage(@NonNull Map<Integer, List<MapWidget>> textInfoWidgets,
	                                 int pageIndex, @NonNull MapWidget widget) {
		List<MapWidget> widgetsViews = textInfoWidgets.computeIfAbsent(pageIndex, k -> new ArrayList<>());
		widgetsViews.add(widget);
	}

	private static class PagesDiffUtilCallback extends DiffUtil.Callback {

		private final VisiblePages oldPages;
		private final VisiblePages newPages;

		public PagesDiffUtilCallback(@NonNull VisiblePages oldPages, @NonNull VisiblePages newPages) {
			this.oldPages = oldPages;
			this.newPages = newPages;
		}

		@Override
		public int getOldListSize() {
			return oldPages.getCount();
		}

		@Override
		public int getNewListSize() {
			return newPages.getCount();
		}

		@Override
		public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
			int oldIndex = oldPages.getPageIndex(oldItemPosition);
			int newIndex = newPages.getPageIndex(newItemPosition);
			return oldIndex == newIndex;
		}

		@Override
		public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
			List<View> oldViews = oldPages.getWidgetsViews(oldItemPosition);
			List<View> newViews = newPages.getWidgetsViews(newItemPosition);
			return oldViews.equals(newViews);
		}
	}

	public static class VisiblePages {

		private final Map<Integer, List<View>> visibleViews = new TreeMap<>();

		public VisiblePages(@NonNull Map<Integer, List<View>> visibleViews) {
			this.visibleViews.putAll(visibleViews);
		}

		public VisiblePages(@NonNull List<View> views) {
			this.visibleViews.put(0, views);
		}

		public int getPageIndex(int entryIndex) {
			Page page = getPage(entryIndex);
			return page != null ? page.getIndex() : -1;
		}

		@NonNull
		public List<View> getWidgetsViews(int entryIndex) {
			Page page = getPage(entryIndex);
			return page != null ? page.getWidgetsViews() : Collections.emptyList();
		}

		@Nullable
		public Page getPage(int entryIndex) {
			List<Entry<Integer, List<View>>> entries = new ArrayList<>(visibleViews.entrySet());
			return entryIndex < 0 || entryIndex >= entries.size() ? null : new Page(entries.get(entryIndex));
		}

		public int getCount() {
			return visibleViews.size();
		}
	}

	private static class Page {

		private final Entry<Integer, List<View>> entry;

		public Page(@NonNull Entry<Integer, List<View>> entry) {
			this.entry = entry;
		}

		public int getIndex() {
			return entry.getKey();
		}

		@NonNull
		public List<View> getWidgetsViews() {
			return entry.getValue();
		}
	}

	public static class PageViewHolder extends RecyclerView.ViewHolder {

		public final ViewGroup container;

		public PageViewHolder(@NonNull View view) {
			super(view);
			container = view.findViewById(R.id.container);
		}
	}

	public interface ViewHolderBindListener {

		void onViewHolderBound(@NonNull PageViewHolder viewHolder, int index);
	}
}
