package net.osmand.plus.views;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import net.osmand.Algoritms;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.OsmandSettings.CommonPreference;
import net.osmand.plus.OsmandSettings.OsmandPreference;
import net.osmand.plus.activities.ApplicationMode;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.LinearLayout.LayoutParams;

public class MapInfoControls {
	
	public static final int LEFT_CONTROL = -1;
	public static final int RIGHT_CONTROL = 1;
	public static final int MAIN_CONTROL = 0;
	
	private Set<MapInfoControlRegInfo> appearanceWidgets = new LinkedHashSet<MapInfoControls.MapInfoControlRegInfo>();
	private Set<MapInfoControlRegInfo> left = new TreeSet<MapInfoControls.MapInfoControlRegInfo>();
	private Set<MapInfoControlRegInfo> right = new TreeSet<MapInfoControls.MapInfoControlRegInfo>();
	private Set<MapInfoControlRegInfo> top = new TreeSet<MapInfoControls.MapInfoControlRegInfo>(new Comparator<MapInfoControlRegInfo>() {
		@Override
		public int compare(MapInfoControlRegInfo object1, MapInfoControlRegInfo object2) {
			if (object1.position != object2.position) {
				if(object1.position == LEFT_CONTROL) {
					return -1;
				} else if(object1.position == RIGHT_CONTROL) {
					return 1;
				} else {
					return object2.position == LEFT_CONTROL ? 1 : -1;
				}
			}
			int cmp = object1.priorityOrder - object2.priorityOrder;
			if(object1.position == RIGHT_CONTROL) {
				cmp = -cmp;
			}
			return cmp;
		}
	});
	private Map<ApplicationMode, Set<String>> visibleElements = new LinkedHashMap<ApplicationMode, Set<String>>();
	private final OsmandSettings settings;
			
	
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
	
	public MapInfoControlRegInfo registerAppearanceWidget(int drawable, int messageId, String key, 
			OsmandPreference<?> pref) {
		MapInfoControlRegInfo ii = new MapInfoControlRegInfo();
		ii.defaultModes = EnumSet.noneOf(ApplicationMode.class);
		ii.defaultCollapsible = null;
		ii.key = key;
		ii.preference = pref;
		ii.visibleModes = EnumSet.noneOf(ApplicationMode.class); 
		ii.visibleCollapsible = null;
		ii.drawable = drawable;
		ii.messageId = messageId;
		this.appearanceWidgets.add(ii);
		return ii;
	}
	
	public void removeApperanceWidgets(String category) {
		Iterator<MapInfoControlRegInfo> it = appearanceWidgets.iterator();
		while(it.hasNext()) {
			if(Algoritms.objectEquals(it.next().category, category)) {
				it.remove();
			}
		}
	}
	
	public MapInfoControlRegInfo registerAppearanceWidget(int drawable, String message, String key, 
			CommonPreference<?> pref, String subcategory) {
		MapInfoControlRegInfo ii = new MapInfoControlRegInfo();
		ii.defaultModes = EnumSet.noneOf(ApplicationMode.class);
		ii.defaultCollapsible = null;
		ii.key = key;
		ii.category = subcategory;
		ii.preference = pref;
		ii.visibleModes = EnumSet.noneOf(ApplicationMode.class); 
		ii.visibleCollapsible = null;
		ii.drawable = drawable;
		ii.messageId = message.hashCode();
		ii.message = message;
		this.appearanceWidgets.add(ii);
		return ii;
	}
	
	public MapInfoControlRegInfo registerTopWidget(View m, int drawable, int messageId, String key, int left,
			EnumSet<ApplicationMode> appDefaultModes, int priorityOrder) {
		MapInfoControlRegInfo ii = new MapInfoControlRegInfo();
		ii.defaultModes = appDefaultModes.clone();
		ii.defaultCollapsible = null;
		ii.key = key;
		ii.visibleModes = EnumSet.noneOf(ApplicationMode.class); 
		ii.visibleCollapsible = null;
		for(ApplicationMode ms : ApplicationMode.values() ) {
			boolean def = appDefaultModes.contains(ms);
			Set<String> set = visibleElements.get(ms);
			if (set != null) {
				if (set.contains(key)) {
					def = true;
				} else if (set.contains("-" + key)) {
					def = false;
				}
			}
			if(def){
				ii.visibleModes.add(ms);
			}
		}
		ii.drawable = drawable;
		ii.messageId = messageId;
		ii.m = m;
		ii.priorityOrder = priorityOrder;
		ii.position = left;
		this.top.add(ii);
		return ii;
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
		for (MapInfoControlRegInfo m : mi) {
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
	
	public void changeVisibility(MapInfoControlRegInfo m) {
		boolean selecteable = m.selecteable();
		if (selecteable) {
			ApplicationMode mode = settings.APPLICATION_MODE.get();
			boolean visible = m.visible(mode);
			boolean collapseEnabled = m.collapseEnabled(mode);
			boolean collapse = m.visibleCollapsed(mode);
			if (this.visibleElements.get(mode) == null) {
				LinkedHashSet<String> set = new LinkedHashSet<String>();
				restoreModes(set, left, mode);
				restoreModes(set, right, mode);
				restoreModes(set, top, mode);
				this.visibleElements.put(mode, set);
			}
			// clear everything
			this.visibleElements.get(mode).remove(m.key);
			this.visibleElements.get(mode).remove("+" + m.key);
			this.visibleElements.get(mode).remove("-" + m.key);
			m.visibleModes.remove(mode);
			if (m.visibleCollapsible != null) {
				m.visibleCollapsible.remove(mode);
			}
			if (visible || collapse) {
				if (collapseEnabled && !collapse) {
					m.visibleCollapsible.add(mode);
					this.visibleElements.get(mode).add("+" + m.key);
				} else {
					this.visibleElements.get(mode).add("-" + m.key);
				}
			} else {
				m.visibleModes.add(mode);
				this.visibleElements.get(mode).add("" + m.key);
			}
			StringBuilder bs = new StringBuilder();
			for (String ks : this.visibleElements.get(mode)) {
				bs.append(ks).append(";");
			}
			settings.MAP_INFO_CONTROLS.set(bs.toString());
		}
		if(m.stateChangeListener != null) {
			m.stateChangeListener.run();
		}
	}
	
	public Set<MapInfoControlRegInfo> getLeft() {
		return left;
	}
	
	public Set<MapInfoControlRegInfo> getRight() {
		return right;
	}
	
	public Set<MapInfoControlRegInfo> getTop() {
		return top;
	}
	
	public Set<MapInfoControlRegInfo> getAppearanceWidgets() {
		return appearanceWidgets;
	}
	
	public void populateStackControl(MapStackControl stack, OsmandMapTileView v, boolean left){
		ApplicationMode appMode = settings.getApplicationMode();
		Set<MapInfoControlRegInfo> st = left ? this.left : this.right;
		for (MapInfoControlRegInfo r : st) {
			if (r.visibleCollapsible != null && r.visibleCollapsible.contains(appMode)) {
				stack.addCollapsedView((MapInfoControl) r.m);
			} else if (r.visibleModes.contains(appMode)) {
				stack.addStackView((MapInfoControl) r.m);
			}
		}
	}
	
	public void populateStatusBar(ViewGroup statusBar){
		ApplicationMode appMode = settings.getApplicationMode();
		for (MapInfoControlRegInfo r : top) {
			boolean main = r.position == MAIN_CONTROL;
			if (r.visibleModes.contains(appMode)) {
				LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, main? 1 : 0);
				statusBar.addView((View) r.m, params);
			} else if (main) {
				LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 1);
				statusBar.addView(new TextView(((View) r.m).getContext()), params);
			}
		}
	}
	
	private void resetDefault(ApplicationMode mode, Set<MapInfoControlRegInfo> set ){
		for(MapInfoControlRegInfo ri : set) {
			if(ri.preference != null) {
				ri.preference.resetToDefault();
			} else {
				if (ri.visibleCollapsible != null) {
					ri.visibleCollapsible.remove(mode);
				}
				ri.visibleModes.remove(mode);
				if (ri.defaultCollapsible != null && ri.defaultCollapsible.contains(mode)) {
					ri.visibleCollapsible.add(mode);
				}
				if (ri.defaultModes.contains(mode)) {
					ri.visibleModes.add(mode);
				}
			}
		}
	}
	
	public void resetToDefault() {
		ApplicationMode appMode = settings.getApplicationMode();
		resetDefault(appMode, left);
		resetDefault(appMode, right);
		resetDefault(appMode, top);
		resetDefault(appMode, appearanceWidgets);
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
		public View m;
		public int drawable;
		public int messageId;
		public String message;
		private String key;
		private int position;
		private String category;
		private EnumSet<ApplicationMode> defaultModes;
		private EnumSet<ApplicationMode> defaultCollapsible;
		private EnumSet<ApplicationMode> visibleModes;
		private EnumSet<ApplicationMode> visibleCollapsible;
		private OsmandPreference<?> preference = null;
		private Runnable stateChangeListener = null;
		public int priorityOrder;
		
		public boolean visibleCollapsed(ApplicationMode mode){
			return preference == null && visibleCollapsible != null && visibleCollapsible.contains(mode);
		}
		
		public boolean collapseEnabled(ApplicationMode mode){
			return visibleCollapsible != null && preference == null;
		}
		
		
		public String getCategory() {
			return category;
		}
		public boolean selecteable(){
			return preference == null || (preference.get() instanceof Boolean);
		}
		
		public boolean visible(ApplicationMode mode){
			if(preference != null) {
				Object value = preference.getModeValue(mode);
				if(value instanceof Boolean) {
					return ((Boolean) value).booleanValue();
				}
				return true;
			}
			return visibleModes.contains(mode);
		}
		
		public MapInfoControlRegInfo required(ApplicationMode... modes){
			for(ApplicationMode ms : modes) {
				visibleModes.add(ms);
			}
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
