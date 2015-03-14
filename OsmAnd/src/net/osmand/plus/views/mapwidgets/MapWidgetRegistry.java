package net.osmand.plus.views.mapwidgets;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import net.osmand.plus.ApplicationMode;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.OsmandSettings.CommonPreference;
import net.osmand.plus.OsmandSettings.OsmandPreference;
import net.osmand.plus.views.OsmandMapTileView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

public class MapWidgetRegistry {
	
	public static final int LEFT_CONTROL = -1;
	public static final int RIGHT_CONTROL = 1;
	public static final int MAIN_CONTROL = 0;
	
	private Set<MapWidgetRegInfo> appearanceWidgets = new LinkedHashSet<MapWidgetRegistry.MapWidgetRegInfo>();
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
	
	public MapWidgetRegInfo registerAppearanceWidget(int drawableDark,int drawableLight, int messageId, String key,
			OsmandPreference<?> pref) {
		MapWidgetRegInfo ii = new MapWidgetRegInfo();
		ii.key = key;
		ii.preference = pref;
		ii.visibleModes = new LinkedHashSet<ApplicationMode>(); 
		ii.visibleCollapsible = null;
		ii.drawableDark = drawableDark;
		ii.drawableLight = drawableLight;
		ii.messageId = messageId;
		this.appearanceWidgets.add(ii);
		return ii;
	}
	
	
	
	public void registerSideWidget(TextInfoWidget widget, int drawableDark,int drawableLight, 
			int messageId, String key, boolean left, int priorityOrder) {
		MapWidgetRegInfo ii = new MapWidgetRegInfo();
		ii.key = key;
		ii.visibleModes = new LinkedHashSet<ApplicationMode>(); 
		ii.visibleCollapsible = new LinkedHashSet<ApplicationMode>();
		for(ApplicationMode ms : ApplicationMode.values(settings) ) {
			boolean collapse = ms.isWidgetCollapsible(key);
			boolean def = ms.isWidgetVisible(key);
			Set<String> set = visibleElementsFromSettings.get(ms);
			if(set != null) {
				if (set.contains(key)) {
					def = true;
					collapse = false;
				} else if (set.contains("-" + key)) {
					def = false;
					collapse = false;
				} else if(set.contains("+"+key)){
					def = false;
					collapse = true;	
				}
			}
			if(def){
				ii.visibleModes.add(ms);
			} else if(collapse) {
				ii.visibleCollapsible.add(ms);
			}
		}
		if (widget != null)
			widget.setContentTitle(widget.getContext().getString(messageId));
		ii.drawableDark = drawableDark;
		ii.drawableLight = drawableLight;
		ii.messageId = messageId;
		ii.widget = widget;
		ii.priorityOrder = priorityOrder;
		if(left) {
			this.left.add(ii);
		} else {
			this.right.add(ii);
		}
	}
	
	private void restoreModes(Set<String> set, Set<MapWidgetRegInfo> mi, ApplicationMode mode) {
		for (MapWidgetRegInfo m : mi) {
			if (m.preference == null) {
				if (m.visibleModes.contains(mode)) {
					set.add(m.key);
				} else if (m.visibleCollapsible != null && m.visibleCollapsible.contains(mode)) {
					set.add("+" + m.key);
				} else {
					set.add("-" + m.key);
				}
			}
		}
	}
	
	public void changeVisibility(MapWidgetRegInfo m) {
		boolean selecteable = m.selecteable();
		if (selecteable) {
			ApplicationMode mode = settings.APPLICATION_MODE.get();
			boolean visible = m.visible(mode);
			boolean collapseEnabled = m.collapseEnabled(mode);
			boolean collapse = m.visibleCollapsed(mode);
			if (this.visibleElementsFromSettings.get(mode) == null) {
				LinkedHashSet<String> set = new LinkedHashSet<String>();
				restoreModes(set, left, mode);
				restoreModes(set, right, mode);
				this.visibleElementsFromSettings.put(mode, set);
			}
			// clear everything
			this.visibleElementsFromSettings.get(mode).remove(m.key);
			this.visibleElementsFromSettings.get(mode).remove("+" + m.key);
			this.visibleElementsFromSettings.get(mode).remove("-" + m.key);
			m.visibleModes.remove(mode);
			if (m.visibleCollapsible != null) {
				m.visibleCollapsible.remove(mode);
			}
			if (visible || collapse) {
				if (collapseEnabled && !collapse) {
					m.visibleCollapsible.add(mode);
					this.visibleElementsFromSettings.get(mode).add("+" + m.key);
				} else {
					this.visibleElementsFromSettings.get(mode).add("-" + m.key);
				}
			} else {
				m.visibleModes.add(mode);
				this.visibleElementsFromSettings.get(mode).add("" + m.key);
			}
			StringBuilder bs = new StringBuilder();
			for (String ks : this.visibleElementsFromSettings.get(mode)) {
				bs.append(ks).append(";");
			}
			settings.MAP_INFO_CONTROLS.set(bs.toString());
		}
		if(m.stateChangeListener != null) {
			m.stateChangeListener.run();
		}
	}
	
	public Set<MapWidgetRegInfo> getLeft() {
		return left;
	}
	
	public Set<MapWidgetRegInfo> getRight() {
		return right;
	}
	
	public Set<MapWidgetRegInfo> getAppearanceWidgets() {
		return appearanceWidgets;
	}
	
	public void populateStackControl(StackWidgetView stack, OsmandMapTileView v, boolean left){
		ApplicationMode appMode = settings.getApplicationMode();
		Set<MapWidgetRegInfo> st = left ? this.left : this.right;
		for (MapWidgetRegInfo r : st) {
			if (r.visibleCollapsible != null && r.visibleCollapsible.contains(appMode)) {
				stack.addCollapsedView(r.widget);
			} else if (r.visibleModes.contains(appMode)) {
				stack.addStackView(r.widget);
			}
		}
	}
	
	private void resetDefault(ApplicationMode mode, Set<MapWidgetRegInfo> set ){
		for(MapWidgetRegInfo ri : set) {
			if(ri.preference != null) {
				ri.preference.resetToDefault();
			} else {
				if (ri.visibleCollapsible != null) {
					ri.visibleCollapsible.remove(mode);
				}
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
	}
	
	public void resetToDefault() {
		ApplicationMode appMode = settings.getApplicationMode();
		resetDefault(appMode, left);
		resetDefault(appMode, right);
		resetDefault(appMode, appearanceWidgets);
		this.visibleElementsFromSettings.put(appMode, null);
		settings.MAP_INFO_CONTROLS.set("");
	}
	
	public static boolean distChanged(int oldDist, int dist){
		if(oldDist != 0 && oldDist - dist < 100 && Math.abs(((float) dist - oldDist)/oldDist) < 0.01){
			return false;
		}
		return true;
	}
	
	
	public static class MapWidgetRegInfo implements Comparable<MapWidgetRegInfo>  {
		public TextInfoWidget widget;
		public int drawableDark;
		public int drawableLight;
		public int messageId;
		private String key;
		private int position;
		private Set<ApplicationMode> visibleModes;
		private Set<ApplicationMode> visibleCollapsible;
		private OsmandPreference<?> preference = null;
		private Runnable stateChangeListener = null;
		public int priorityOrder;
		
		public boolean visibleCollapsed(ApplicationMode mode){
			return preference == null && visibleCollapsible != null && visibleCollapsible.contains(mode);
		}
		
		public boolean collapseEnabled(ApplicationMode mode){
			return visibleCollapsible != null && preference == null;
		}
		
		public boolean selecteable(){
			return preference == null || (preference.get() instanceof Boolean);
		}
		
		public boolean visible(ApplicationMode mode){
			if(preference != null) {
				Object value = preference.getModeValue(mode);
				if(value instanceof Boolean) {
					return (Boolean) value;
				}
				return true;
			}
			return visibleModes.contains(mode);
		}
		
		public MapWidgetRegInfo required(ApplicationMode... modes){
            Collections.addAll(visibleModes, modes);
			return this;
		}
		
		public void setPreference(CommonPreference<Boolean> blPreference) {
			this.preference = blPreference;
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
}
