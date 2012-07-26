package net.osmand.plus.views;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import net.osmand.plus.OsmandSettings;
import net.osmand.plus.activities.ApplicationMode;
import android.graphics.Paint;

public class MapInfoControls {
	
	private Set<MapInfoControlRegInfo> left = new TreeSet<MapInfoControls.MapInfoControlRegInfo>();
	private Set<MapInfoControlRegInfo> right = new TreeSet<MapInfoControls.MapInfoControlRegInfo>();
	private Map<ApplicationMode, Set<String>> visibleElements = new LinkedHashMap<ApplicationMode, Set<String>>();
	private final OsmandSettings settings;
			
	
	public interface MapInfoControlFactoryMethod {
		
		/**
		 * @param mapView
		 * @param paints array of paints (4) 0 - normal, 1 - subtext, 2 - small, 3 - small subtext
		 */
		public MapInfoControl createControl(OsmandMapTileView mapView, Paint[] paints);
	}
	
	public MapInfoControls(OsmandSettings settings) {
		this.settings = settings;
		
		for(ApplicationMode ms : ApplicationMode.values() ) {
			String mpf = settings.MAP_INFO_CONTROLS.getModeValue(ms);
			if(mpf.equals("")) {
				visibleElements.put(ms, null);
			} else {
				LinkedHashSet<String> set = new LinkedHashSet<String>();
				visibleElements.put(ms, set);
				for(String s : mpf.split(";")){
					set.add(s);
				}
			}
		}
		
	}
	
	
	
	public void registerSideWidget(MapInfoControl m, int drawable, int messageId, String key, boolean left,
			EnumSet<ApplicationMode> appDefaultModes, EnumSet<ApplicationMode> defaultCollapsible, int priorityOrder) {
		MapInfoControlRegInfo ii = new MapInfoControlRegInfo();
		ii.defaultModes = appDefaultModes.clone();
		ii.defaultCollapsible = defaultCollapsible.clone();
		ii.key = key;
		ii.visibleModes = EnumSet.noneOf(ApplicationMode.class); 
		ii.visibleCollapsible = EnumSet.noneOf(ApplicationMode.class);
		for(ApplicationMode ms : ApplicationMode.values() ) {
			boolean collapse = defaultCollapsible.contains(ms);;
			boolean def = appDefaultModes.contains(ms);
			Set<String> set = visibleElements.get(ms);
			if(set != null) {
				def = set.contains(key);
				collapse = set.contains("+"+key);
			}
			if(def){
				ii.visibleModes.add(ms);
			} else if(collapse) {
				ii.visibleCollapsible.add(ms);
			}
		}
		ii.drawable = drawable;
		ii.messageId = messageId;
		ii.m = m;
		ii.priorityOrder = priorityOrder;
		if(left) {
			this.left.add(ii);
		} else {
			this.right.add(ii);
		}
	}
	
	private void restoreModes(Set<String> set, Set<MapInfoControlRegInfo> mi, ApplicationMode mode) {
		for(MapInfoControlRegInfo m : mi){
			if(m.visibleModes.contains(mode)) {
				set.add(m.key) ;
			} else if(m.visibleCollapsible.contains(mode)) {
				set.add("+"+m.key) ;
			}  
		}
	}
	
	public void changeVisibility(MapInfoControlRegInfo m, boolean visible, boolean collapse) {
		ApplicationMode mode = settings.APPLICATION_MODE.get();
		if(this.visibleElements.get(mode) == null){
			LinkedHashSet<String> set = new LinkedHashSet<String>();
			restoreModes(set, left, mode);
			restoreModes(set, right, mode);
			this.visibleElements.put(mode, set);
		}
		this.visibleElements.get(mode).remove(m.key);
		this.visibleElements.get(mode).remove("+" + m.key);
		m.visibleModes.remove(mode);
		m.visibleCollapsible.remove(mode);
		if(visible) {
			if(collapse) {
				m.visibleCollapsible.add(mode);
				this.visibleElements.get(mode).add("+" + m.key);
			} else {
				m.visibleModes.add(mode);
				this.visibleElements.get(mode).add(m.key);
				
			}
		}
		StringBuilder bs = new StringBuilder();
		for(String ks : this.visibleElements.get(mode)){
			bs.append(ks).append(";");
		}
		settings.MAP_INFO_CONTROLS.set(bs.toString());
	}
	
	public Set<MapInfoControlRegInfo> getLeft() {
		return left;
	}
	
	public Set<MapInfoControlRegInfo> getRight() {
		return right;
	}
	
	public void registerTopBarButton(MapInfoControlFactoryMethod m, int drawable, int messageId, boolean left,
			EnumSet<ApplicationMode> appModes, int priorityOrder) {
		
	}
	
	public void populateStackControl(MapStackControl stack, OsmandMapTileView v, boolean left){
		ApplicationMode appMode = settings.getApplicationMode();
		Set<MapInfoControlRegInfo> st = left ? this.left : this.right;
		for (MapInfoControlRegInfo r : st) {
			if (r.visibleCollapsible.contains(appMode)) {
				stack.addCollapsedView(r.m);
			} else if (r.visibleModes.contains(appMode)) {
				stack.addStackView(r.m);
			}
		}
	}
	
	private void resetDefault(ApplicationMode mode, Set<MapInfoControlRegInfo> set ){
		for(MapInfoControlRegInfo ri : set) {
			ri.visibleCollapsible.remove(mode);
			ri.visibleModes.remove(mode);
			if(ri.defaultCollapsible.contains(mode)) {
				ri.visibleCollapsible.add(mode);
			}
			if(ri.defaultModes.contains(mode)) {
				ri.visibleModes.add(mode);
			}
		}
	}
	
	public void resetToDefault() {
		ApplicationMode appMode = settings.getApplicationMode();
		resetDefault(appMode, left);
		resetDefault(appMode, right);
		this.visibleElements.put(appMode, null);
		settings.MAP_INFO_CONTROLS.set("");
	}
	
	public static boolean distChanged(int oldDist, int dist){
		if(oldDist != 0 && oldDist - dist < 100 && Math.abs(((float) dist - oldDist)/oldDist) < 0.01){
			return false;
		}
		return true;
	}

	
	
	public static class MapInfoControlRegInfo implements Comparable<MapInfoControlRegInfo>  {
		public MapInfoControl m;
		public int drawable;
		public int messageId;
		private String key;
		private EnumSet<ApplicationMode> defaultModes;
		private EnumSet<ApplicationMode> defaultCollapsible;
		private EnumSet<ApplicationMode> visibleModes;
		private EnumSet<ApplicationMode> visibleCollapsible;
		public int priorityOrder;
		
		public boolean visibleCollapsed(ApplicationMode mode){
			return visibleCollapsible.contains(mode);
		}
		
		public boolean visible(ApplicationMode mode){
			return visibleModes.contains(mode);
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
			MapInfoControlRegInfo other = (MapInfoControlRegInfo) obj;
			if (messageId != other.messageId)
				return false;
			return true;
		}
		@Override
		public int compareTo(MapInfoControlRegInfo another) {
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
