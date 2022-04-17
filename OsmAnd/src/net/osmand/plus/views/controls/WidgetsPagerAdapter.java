package net.osmand.plus.views.controls;

import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.views.controls.WidgetsPagerAdapter.PageViewHolder;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.MapWidgetRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

public class WidgetsPagerAdapter extends RecyclerView.Adapter<PageViewHolder> {

	private final OsmandApplication app;

	private VisiblePages visiblePages;
	private ViewHolderBindListener bindListener;

	public WidgetsPagerAdapter(@NonNull OsmandApplication app) {
		this.app = app;
		this.visiblePages = collectVisiblePages();
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
		LinearLayout linearLayout = new LinearLayout(parent.getContext());
		linearLayout.setOrientation(LinearLayout.VERTICAL);
		linearLayout.setLayoutParams(new RecyclerView.LayoutParams(MATCH_PARENT, MATCH_PARENT));
		return new PageViewHolder(linearLayout);
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
		return visiblePages.getCount();
	}

	@NonNull
	private VisiblePages collectVisiblePages() {
		MapWidgetRegistry widgetRegistry = app.getOsmandMap().getMapLayers().getMapWidgetRegistry();
		ApplicationMode appMode = app.getSettings().getApplicationMode();
		return new VisiblePages(widgetRegistry.getRightWidgets(), appMode);
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

	private static class VisiblePages {

		private final Map<Integer, List<View>> map = new TreeMap<>();

		public VisiblePages(@NonNull Set<MapWidgetInfo> widgets, @NonNull ApplicationMode appMode) {
			for (MapWidgetInfo widgetInfo : widgets) {
				if (widgetInfo.isEnabledForAppMode(appMode) && widgetInfo.widget.isViewVisible()) {
					addWidgetViewToPage(widgetInfo.pageIndex, widgetInfo.widget.getView());
				}
			}
		}

		private void addWidgetViewToPage(int pageIndex, @NonNull View widgetView) {
			List<View> widgetsViews = map.get(pageIndex);
			if (widgetsViews == null) {
				widgetsViews = new ArrayList<>();
				map.put(pageIndex, widgetsViews);
			}
			widgetsViews.add(widgetView);
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
			List<Entry<Integer, List<View>>> entries = new ArrayList<>(map.entrySet());
			return entryIndex < 0 || entryIndex >= entries.size() ? null : new Page(entries.get(entryIndex));
		}

		public int getCount() {
			return map.size();
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

		public PageViewHolder(@NonNull LinearLayout itemView) {
			super(itemView);
			container = itemView;
		}
	}

	public interface ViewHolderBindListener {

		void onViewHolderBound(@NonNull PageViewHolder viewHolder, int index);
	}
}