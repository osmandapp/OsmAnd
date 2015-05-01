package net.osmand.plus.views.mapwidgets;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import net.osmand.plus.ApplicationMode;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuAdapter.OnContextMenuClick;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.OsmandSettings.OsmandPreference;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.dialogs.ConfigureMapMenu;
import net.osmand.plus.views.MapInfoLayer;
import net.osmand.plus.views.OsmandMapLayer.DrawSettings;
import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;

public class MapWidgetRegistry {
	
	private Set<MapWidgetRegInfo> left = new TreeSet<MapWidgetRegistry.MapWidgetRegInfo>();
	private Set<MapWidgetRegInfo> right = new TreeSet<MapWidgetRegistry.MapWidgetRegInfo>();
	private Map<ApplicationMode, Set<String>> visibleElementsFromSettings = new LinkedHashMap<ApplicationMode, Set<String>>();
	private final OsmandSettings settings;
			
	
	public MapWidgetRegistry(OsmandSettings settings) {
		this.settings = settings;
		
		for(ApplicationMode ms : ApplicationMode.values(settings) ) {
			String mpf = settings.MAP_INFO_CONTROLS.getModeValue(ms);
			if(mpf.equals("")) {
				visibleElementsFromSettings.put(ms, null);
			} else {
				LinkedHashSet<String> set = new LinkedHashSet<String>();
				visibleElementsFromSettings.put(ms, set);
                Collections.addAll(set, mpf.split(";"));
			}
		}
	}
	
	public void populateStackControl(LinearLayout stack, 
			ApplicationMode mode, boolean left, boolean expanded) {
		Set<MapWidgetRegInfo> s = left ? this.left : this.right;
		for (MapWidgetRegInfo r : s) {
			if (r.visible(mode) || r.widget.isExplicitlyVisible()) {
				stack.addView(r.widget.getView());
			}
		}
		if (expanded) {
			for (MapWidgetRegInfo r : s) {
				if (r.visibleCollapsed(mode) && 
						!r.widget.isExplicitlyVisible()) {
					stack.addView(r.widget.getView());
				}
			}
		}
	}
	
	public boolean hasCollapsibles(ApplicationMode mode) {
		for (MapWidgetRegInfo r : left) {
			if (r.visibleCollapsed(mode)) {
				return true;
			}
		}
		for (MapWidgetRegInfo r : right) {
			if (r.visibleCollapsed(mode)) {
				return true;
			}
		}
		return false;
	}
	
	
	public void updateInfo(ApplicationMode mode, DrawSettings drawSettings, boolean expanded) {
		update(mode, drawSettings, expanded, left);
		update(mode, drawSettings, expanded, right);
	}

	private void update(ApplicationMode mode, DrawSettings drawSettings, boolean expanded, Set<MapWidgetRegInfo> l) {
		for (MapWidgetRegInfo r : l) {
			if (r.visible(mode) || (r.visibleCollapsed(mode) && expanded)) {
				r.widget.updateInfo(drawSettings);
			}
		}
	}
	public MapWidgetRegInfo registerSideWidgetInternal(TextInfoWidget widget, int drawableMenu, 
			int messageId, String key, boolean left, int priorityOrder) {
		MapWidgetRegInfo ii = new MapWidgetRegInfo(key, widget, drawableMenu,  
				messageId, priorityOrder, left);
		for (ApplicationMode ms : ApplicationMode.values(settings)) {
			boolean collapse = ms.isWidgetCollapsible(key);
			boolean def = ms.isWidgetVisible(key);
			Set<String> set = visibleElementsFromSettings.get(ms);
			if (set != null) {
				if (set.contains(key)) {
					def = true;
					collapse = false;
				} else if (set.contains("-" + key)) {
					def = false;
					collapse = false;
				} else if (set.contains("+" + key)) {
					def = false;
					collapse = true;
				}
			}
			if (def) {
				ii.visibleModes.add(ms);
			} else if (collapse) {
				ii.visibleCollapsible.add(ms);
			}
		}
		if (widget != null) {
			widget.setContentTitle(messageId);
		}
		if(left) {
			this.left.add(ii);
		} else {
			this.right.add(ii);
		}
		return ii;
	}
	
	private void restoreModes(Set<String> set, Set<MapWidgetRegInfo> mi, ApplicationMode mode) {
		for (MapWidgetRegInfo m : mi) {
			if (m.visibleModes.contains(mode)) {
				set.add(m.key);
			} else if (m.visibleCollapsible != null && m.visibleCollapsible.contains(mode)) {
				set.add("+" + m.key);
			} else {
				set.add("-" + m.key);
			}
		}
	}
	
	public void changeVisibility(MapWidgetRegInfo m) {
		ApplicationMode mode = settings.APPLICATION_MODE.get();
		boolean visible = m.visible(mode);
		boolean collapse = m.visibleCollapsed(mode);
		defineDefaultSettingsElement(mode);
		// clear everything
		this.visibleElementsFromSettings.get(mode).remove(m.key);
		this.visibleElementsFromSettings.get(mode).remove("+" + m.key);
		this.visibleElementsFromSettings.get(mode).remove("-" + m.key);
		m.visibleModes.remove(mode);
		m.visibleCollapsible.remove(mode);
		if (visible || collapse) {
			if (!collapse) {
				m.visibleCollapsible.add(mode);
				this.visibleElementsFromSettings.get(mode).add("+" + m.key);
			} else {
				this.visibleElementsFromSettings.get(mode).add("-" + m.key);
			}
		} else {
			m.visibleModes.add(mode);
			this.visibleElementsFromSettings.get(mode).add("" + m.key);
		}
		saveVisibleElementsToSettings(mode);
		if (m.stateChangeListener != null) {
			m.stateChangeListener.run();
		}
	}

	private void defineDefaultSettingsElement(ApplicationMode mode) {
		if (this.visibleElementsFromSettings.get(mode) == null) {
			LinkedHashSet<String> set = new LinkedHashSet<String>();
			restoreModes(set, left, mode);
			restoreModes(set, right, mode);
			this.visibleElementsFromSettings.put(mode, set);
		}
	}

	private void saveVisibleElementsToSettings(ApplicationMode mode) {
		StringBuilder bs = new StringBuilder();
		for (String ks : this.visibleElementsFromSettings.get(mode)) {
			bs.append(ks).append(";");
		}
		settings.MAP_INFO_CONTROLS.set(bs.toString());
	}
	
	
	private void resetDefault(ApplicationMode mode, Set<MapWidgetRegInfo> set ){
		for (MapWidgetRegInfo ri : set) {
			ri.visibleCollapsible.remove(mode);
			ri.visibleModes.remove(mode);
			if (mode.isWidgetVisible(ri.key)) {
				if (mode.isWidgetCollapsible(ri.key)) {
					ri.visibleCollapsible.add(mode);
				} else {
					ri.visibleModes.add(mode);
				}
			}
		}
	}
	
	public void resetToDefault() {
		ApplicationMode appMode = settings.getApplicationMode();
		resetDefault(appMode, left);
		resetDefault(appMode, right);
		resetDefaultAppearance(appMode);
		this.visibleElementsFromSettings.put(appMode, null);
		settings.MAP_INFO_CONTROLS.set("");
	}
	
	private void resetDefaultAppearance(ApplicationMode appMode) {
//		settings.SHOW_RULER.resetToDefault();		
		settings.SHOW_DESTINATION_ARROW.resetToDefault();
		settings.TRANSPARENT_MAP_THEME.resetToDefault();
		settings.SHOW_STREET_NAME.resetToDefault();
		settings.CENTER_POSITION_ON_MAP.resetToDefault();
	}
	
	public void addControlsAppearance(final MapActivity map, ContextMenuAdapter cm, ApplicationMode mode) {
//		addControlId(mil, cm, R.string.map_widget_show_ruler, settings.SHOW_RULER);
		addControlId(map, cm, R.string.map_widget_show_destination_arrow, settings.SHOW_DESTINATION_ARROW);
		addControlId(map, cm, R.string.map_widget_transparent, settings.TRANSPARENT_MAP_THEME);
		addControlId(map, cm, R.string.always_center_position_on_map, settings.CENTER_POSITION_ON_MAP);
		if(mode != ApplicationMode.DEFAULT) {
			addControlId(map, cm, R.string.map_widget_top_text, settings.SHOW_STREET_NAME);
		}
		
	}

	private void addControlId(final MapActivity map, ContextMenuAdapter cm, int stringId, OsmandPreference<Boolean> pref) {
		cm.item(stringId).selected( pref.get() ? 1 : 0)
//			.icons(r.drawableDark, r.drawableLight)
			.listen(new ApearanceOnContextMenuClick(pref, map)).reg();
	}
	
	class ApearanceOnContextMenuClick implements OnContextMenuClick {
		
		private MapActivity map;
		private OsmandPreference<Boolean> pref;

		public ApearanceOnContextMenuClick(OsmandPreference<Boolean> pref, MapActivity map) {
			this.pref = pref;
			this.map = map;
		}
		
		@Override
		public boolean onContextMenuClick(ArrayAdapter<?> a, int itemId, int pos, boolean isChecked) {
			pref.set(!pref.get());
			map.updateApplicationModeSettings();
			a.notifyDataSetInvalidated();
			return false;
		}
	};	

	
	public static boolean distChanged(int oldDist, int dist){
		if(oldDist != 0 && oldDist - dist < 100 && Math.abs(((float) dist - oldDist)/oldDist) < 0.01){
			return false;
		}
		return true;
	}
	

	public void addControls(MapActivity map, ContextMenuAdapter cm, ApplicationMode mode) {
		cm.item(R.string.map_widget_right).setCategory(true).layout(R.layout.drawer_list_sub_header).reg();
		addControls(map, cm, right, mode);
		if(mode != ApplicationMode.DEFAULT) {
			cm.item(R.string.map_widget_left).setCategory(true).layout(R.layout.drawer_list_sub_header).reg();
			addControls(map, cm, left, mode);
		}
		cm.item(R.string.map_widget_appearance_rem).setCategory(true).layout(R.layout.drawer_list_sub_header).reg();
		addControlsAppearance(map, cm, mode);		
	}
	
	public String getText(Context ctx, final ApplicationMode mode, final MapWidgetRegInfo r) {
		return (r.visibleCollapsed(mode)? " + " : "  ") + ctx.getString(r.messageId);
	}
	
	public Set<MapWidgetRegInfo> getRight() {
		return right;
	}
	
	public Set<MapWidgetRegInfo> getLeft() {
		return left;
	}
	
	private void addControls(final MapActivity map, final ContextMenuAdapter adapter, Set<MapWidgetRegInfo> top, final ApplicationMode mode) {
		for(final MapWidgetRegInfo r : top){
			adapter.item(r.messageId).selected(r.visibleCollapsed(mode) || r.visible(mode) ? 1 : 0)
				.iconColor(r.drawableMenu).listen(new OnContextMenuClick() {
				
				@Override
				public boolean onContextMenuClick(ArrayAdapter<?> a, int itemId, int pos, boolean isChecked) {
					changeVisibility(r);
					MapInfoLayer mil = map.getMapLayers().getMapInfoLayer();
					if (mil != null) {
						mil.recreateControls();
					}
					adapter.setItemName(pos, getText(mil.getMapActivity(), mode, r));
					adapter.setSelection(pos, r.visibleCollapsed(mode) || r.visible(mode) ? 1 : 0);
					a.notifyDataSetInvalidated();
					return false;
				}
			}).reg();
			adapter.setItemName(adapter.length() - 1, getText(map, mode, r));
		}
	}



	
	
	public static class MapWidgetRegInfo implements Comparable<MapWidgetRegInfo>  {
		public final TextInfoWidget widget;
		public final int drawableMenu;
		public final int messageId;
		public final String key;
		public final boolean left;
		public final int priorityOrder;
		private final Set<ApplicationMode> visibleCollapsible = new LinkedHashSet<ApplicationMode>();
		private final Set<ApplicationMode> visibleModes = new LinkedHashSet<ApplicationMode>();
		private Runnable stateChangeListener = null;
		
		public MapWidgetRegInfo(String key, TextInfoWidget widget, int drawableMenu, 
				int messageId, int priorityOrder, boolean left) {
			this.key = key;
			this.widget = widget;
			this.drawableMenu = drawableMenu;
			this.messageId = messageId;
			this.priorityOrder = priorityOrder;
			this.left = left;
		}
		
		public boolean visibleCollapsed(ApplicationMode mode){
			return visibleCollapsible.contains(mode);
		}
		
		public boolean visible(ApplicationMode mode){
			return visibleModes.contains(mode);
		}
		
		public MapWidgetRegInfo required(ApplicationMode... modes){
            Collections.addAll(visibleModes, modes);
			return this;
		}
		
		
		public void setStateChangeListener(Runnable stateChangeListener) {
			this.stateChangeListener = stateChangeListener;
		}
		
		@Override
		public int hashCode() {
			return messageId;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			MapWidgetRegInfo other = (MapWidgetRegInfo) obj;
			if (messageId != other.messageId)
				return false;
			return true;
		}
		@Override
		public int compareTo(MapWidgetRegInfo another) {
			if (messageId == another.messageId) {
				return 0;
			}
			if(priorityOrder == another.priorityOrder) {
				return messageId - another.messageId;
			}
			return priorityOrder - another.priorityOrder;
		}
	}
	
	public ContextMenuAdapter getViewConfigureMenuAdapter(final MapActivity map) {
		ContextMenuAdapter cm = new ContextMenuAdapter(map);
		cm.setDefaultLayoutId(R.layout.drawer_list_item);
		cm.item(R.string.app_modes_choose).layout(R.layout.mode_toggles).reg();
		cm.setChangeAppModeListener(new ConfigureMapMenu.OnClickListener() {

			@Override
			public void onClick(boolean allModes) {
				map.getDashboard().updateListAdapter(getViewConfigureMenuAdapter(map));
			}
		});
		cm.item(R.string.map_widget_reset).iconColor(R.drawable.ic_action_reset_to_default_dark)
				.listen(new OnContextMenuClick() {

					@Override
					public boolean onContextMenuClick(ArrayAdapter<?> adapter, int itemId, int pos, boolean isChecked) {
						resetToDefault();
						MapInfoLayer mil = map.getMapLayers().getMapInfoLayer();
						if (mil != null) {
							mil.recreateControls();
						}
						adapter.notifyDataSetInvalidated();
						return false;
					}
				}).reg();
		final ApplicationMode mode = settings.getApplicationMode();
		addControls(map, cm, mode);
		return cm;
	}

}
