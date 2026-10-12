package net.osmand.plus.mapcontextmenu;

import android.view.View;

import androidx.annotation.NonNull;

import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.mapcontextmenu.MenuBuilder.CollapseExpandListener;

public class CollapsableView {

	private final View contentView;
	private final MenuBuilder menuBuilder;

	private OsmandPreference<Boolean> collapsedPref;
	private CollapseExpandListener collapseExpandListener;
	private boolean collapsed;

	public CollapsableView(@NonNull View contentView, MenuBuilder menuBuilder,
	                       @NonNull OsmandPreference<Boolean> collapsedPref) {
		this.contentView = contentView;
		this.menuBuilder = menuBuilder;
		this.collapsedPref = collapsedPref;
	}

	public CollapsableView(@NonNull View contentView, MenuBuilder menuBuilder, boolean collapsed) {
		this.contentView = contentView;
		this.collapsed = collapsed;
		this.menuBuilder = menuBuilder;
	}

	public View getContentView() {
		return contentView;
	}

	public boolean isCollapsed() {
		if (collapsedPref != null) {
			return collapsedPref.get();
		} else {
			return collapsed;
		}
	}

	public void setCollapsed(boolean collapsed) {
		if (collapsedPref != null) {
			collapsedPref.set(collapsed);
		} else {
			this.collapsed = collapsed;
		}
		if (collapseExpandListener != null) {
			collapseExpandListener.onCollapseExpand(collapsed);
		}
		if (menuBuilder != null) {
			menuBuilder.notifyCollapseExpand(collapsed);
		}
	}

	public CollapseExpandListener getCollapseExpandListener() {
		return collapseExpandListener;
	}

	public void setCollapseExpandListener(CollapseExpandListener collapseExpandListener) {
		this.collapseExpandListener = collapseExpandListener;
	}
}