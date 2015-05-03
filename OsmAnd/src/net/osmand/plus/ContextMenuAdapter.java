package net.osmand.plus;

import gnu.trove.list.array.TIntArrayList;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.osmand.plus.activities.actions.AppModeDialog;
import net.osmand.plus.dialogs.ConfigureMapMenu;
import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class ContextMenuAdapter {

	public interface OnContextMenuClick {
		//boolean return type needed to desribe if drawer needed to be close or not
		public boolean onContextMenuClick(ArrayAdapter<?> adapter, int itemId, int pos, boolean isChecked);
	}
	
	public static abstract class OnRowItemClick implements OnContextMenuClick {
		
		public OnRowItemClick() {
		}
		//boolean return type needed to desribe if drawer needed to be close or not
		public boolean onRowItemClick(ArrayAdapter<?> adapter, View view, int itemId, int pos) {
			CompoundButton btn = (CompoundButton) view.findViewById(R.id.check_item);
			if (btn != null && btn.getVisibility() == View.VISIBLE) {
				btn.setChecked(!btn.isChecked());
				return false;
			} else {
				return onContextMenuClick(adapter, itemId, pos, false);
			}
		}
	}

	public class BooleanResult {
		private boolean result = false;

		public void setResult(boolean value) { result = value; }
		public boolean getResult() { return result; }
	}
	
	private final Context ctx;
	private View anchor;
	private int defaultLayoutId = Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB ?
			R.layout.list_menu_item : R.layout.list_menu_item_native;
	final TIntArrayList items = new TIntArrayList();
	final TIntArrayList isCategory = new TIntArrayList();
	final ArrayList<String> itemNames = new ArrayList<String>();
	final ArrayList<OnContextMenuClick> checkListeners = new ArrayList<ContextMenuAdapter.OnContextMenuClick>();
	final TIntArrayList selectedList = new TIntArrayList();
	final TIntArrayList loadingList = new TIntArrayList();
	final TIntArrayList layoutIds = new TIntArrayList();
	final TIntArrayList iconList = new TIntArrayList();
	final TIntArrayList iconListLight = new TIntArrayList();
	final ArrayList<String> itemDescription = new ArrayList<String>();
	private List<ApplicationMode> visibleModes = new ArrayList<ApplicationMode>();
	private ConfigureMapMenu.OnClickListener changeAppModeListener = null;
	//neded to detect whether user opened all modes or not
	private BooleanResult allModes = new BooleanResult();

	public ContextMenuAdapter(Context ctx, boolean allModes) {
		this.ctx = ctx;
		this.allModes.setResult(allModes);
	}

	public ContextMenuAdapter(Context ctx) {
		this.ctx = ctx;
	}
	
	public void setAnchor(View anchor) {
		this.anchor = anchor;
	}
	
	public View getAnchor() {
		return anchor;
	}
	
	public int length(){
		return items.size();
	}
	
	public int getElementId(int pos){
		return items.get(pos);
	}
	
	public OnContextMenuClick getClickAdapter(int i) {
		return checkListeners.get(i);
	}

	public String getItemName(int pos){
		return itemNames.get(pos);
	}

	public String getItemDescr(int pos){
		return itemDescription.get(pos);
	}
	
	public void setItemName(int pos, String str) {
		itemNames.set(pos, str);
	}

	public void setItemDescription(int pos, String str) {
		itemDescription.set(pos, str);
	}
	
	public int getSelection(int pos) {
		return selectedList.get(pos);
	}

	public int getLoading(int pos) {
		return loadingList.get(pos);
	}
	
	public void setSelection(int pos, int s) {
		selectedList.set(pos, s);
	}
	
	
	public Drawable getImage(OsmandApplication ctx, int pos, boolean light) {
		int lst = iconList.get(pos);
		if(lst != 0) {
			return ctx.getResources().getDrawable(lst);
		}
		int lstLight = iconListLight.get(pos);
		if(lstLight != 0) {
			return ctx.getIconsCache().getIcon(lstLight, light);
		}
		return null;
	}
	
	public int getBackgroundColor(Context ctx, boolean holoLight) {
		if (holoLight) {
			return ctx.getResources().getColor(R.color.bg_color_light);
		} else {
			return ctx.getResources().getColor(R.color.bg_color_dark);
		}
	}
	
	
	public boolean isCategory(int pos) {
		return isCategory.get(pos) > 0;
	}
	
	public Item item(String name){
		Item i = new Item();
		i.id = (name.hashCode() << 4) | items.size();
		i.name = name;
		return i;
	}
	
	public Item item(int resId){
		Item i = new Item();
		i.id = resId;
		i.name = ctx.getString(resId);
		return i;
	}
	
	public class Item {
		int icon = 0;
		int lightIcon = 0;
		int id;
		String name;
		int selected = -1;
		int layout = -1;
		int loading = -1;
		boolean cat;
		int pos = -1;
		String description = "";
		private OnContextMenuClick checkBoxListener;

		private Item() {
		}

		public Item icon(int icon) {
			this.icon = icon;
			return this;
		}
		
		public Item iconColor(int icon) {
			this.lightIcon = icon;
			return this;
		}

		public Item position(int pos) {
			this.pos = pos;
			return this;
		}

		public Item selected(int selected) {
			this.selected = selected;
			return this;
		}

		public Item loading(int loading) {
			this.loading = loading;
			return this;
		}
		
		public Item layout(int l) {
			this.layout = l;
			return this;
		}

		public Item description(String descr){
			this.description = descr;
			return this;
		}

		public Item listen(OnContextMenuClick l) {
			this.checkBoxListener = l;
			return this;
		}

		public void reg() {
			if (pos >= items.size() || pos < 0) {
				pos = items.size();
			}
			items.insert(pos, id);
			itemNames.add(pos, name);
			itemDescription.add(pos, description);
			selectedList.insert(pos, selected);
			loadingList.insert(pos, loading);
			layoutIds.insert(pos, layout);
			iconList.insert(pos, icon);
			iconListLight.insert(pos, lightIcon);
			checkListeners.add(pos, checkBoxListener);
			isCategory.insert(pos, cat ? 1 : 0);
		}

		public Item setCategory(boolean b) {
			cat = b;
			return this;
		}

		

	}
	
	public String[] getItemNames() {
		return itemNames.toArray(new String[itemNames.size()]);
	}
	
	public void removeItem(int pos) {
		items.removeAt(pos);
		itemNames.remove(pos);
		selectedList.removeAt(pos);
		iconList.removeAt(pos);
		iconListLight.removeAt(pos);
		checkListeners.remove(pos);
		isCategory.removeAt(pos);
		layoutIds.removeAt(pos);
		loadingList.removeAt(pos);
	}

	public int getLayoutId(int position) {
		int l = layoutIds.get(position);
		if(l != -1) {
			return l;
		}
		return defaultLayoutId; 
	}
	
	
	public void setDefaultLayoutId(int defaultLayoutId) {
		this.defaultLayoutId = defaultLayoutId;
	}
	
	

	public void setChangeAppModeListener(ConfigureMapMenu.OnClickListener changeAppModeListener) {
		this.changeAppModeListener = changeAppModeListener;
	}
	
	
	public ArrayAdapter<?> createListAdapter(final Activity activity, final boolean holoLight) {
		final int layoutId = defaultLayoutId;
		final OsmandApplication app = ((OsmandApplication) activity.getApplication());
		ArrayAdapter<String> listAdapter = new ArrayAdapter<String>(activity, layoutId, R.id.title,
				getItemNames()) {
			@Override
			public View getView(final int position, View convertView, ViewGroup parent) {
				// User super class to create the View
				View v = convertView;
				Integer lid = getLayoutId(position);
				if (lid == R.layout.mode_toggles){
					final Set<ApplicationMode> selected = new LinkedHashSet<ApplicationMode>();
					return AppModeDialog.prepareAppModeDrawerView(activity, visibleModes, selected, allModes, new View.OnClickListener() {
						@Override
						public void onClick(View view) {
							if (selected.size() > 0) {
								app.getSettings().APPLICATION_MODE.set(selected.iterator().next());
								notifyDataSetChanged();
							}
							if (changeAppModeListener != null) {
								changeAppModeListener.onClick(allModes.getResult());
							}
						}
					});
				}
				if (v == null || (v.getTag() != lid)) {
					v = activity.getLayoutInflater().inflate(lid, null);
					v.setTag(lid);
				}
				TextView tv = (TextView) v.findViewById(R.id.title);
				tv.setText(isCategory(position) ? getItemName(position).toUpperCase() : getItemName(position));

				Drawable imageId = getImage(app, position, holoLight);
				if (imageId != null) {
					((ImageView) v.findViewById(R.id.icon)).setImageDrawable(imageId);
					v.findViewById(R.id.icon).setVisibility(View.VISIBLE);
				} else if (v.findViewById(R.id.icon) != null){
					v.findViewById(R.id.icon).setVisibility(View.GONE);
				}
				
				if(isCategory(position)) {
					tv.setTypeface(Typeface.DEFAULT_BOLD);
				} else {
					tv.setTypeface(null);
				}

				if (v.findViewById(R.id.check_item) != null) {
					final CompoundButton ch = (CompoundButton) v.findViewById(R.id.check_item);
					if(selectedList.get(position) != -1) {
						ch.setOnCheckedChangeListener(null);
						ch.setVisibility(View.VISIBLE);
						ch.setChecked(selectedList.get(position) > 0);
						final ArrayAdapter<String> la = this;
						final OnCheckedChangeListener listener = new OnCheckedChangeListener() {

							@Override
							public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
								OnContextMenuClick ca = getClickAdapter(position);
								selectedList.set(position, isChecked ? 1 : 0);
								if (ca != null) {
									ca.onContextMenuClick(la, getElementId(position), position, isChecked);
								}
							}
						};
						ch.setOnCheckedChangeListener(listener);
						ch.setVisibility(View.VISIBLE);
					} else if (ch != null) {
						ch.setVisibility(View.GONE);
					}
				}

				if (v.findViewById(R.id.ProgressBar) != null){
					ProgressBar bar = (ProgressBar) v.findViewById(R.id.ProgressBar);
					if(loadingList.get(position) == 1){
						bar.setVisibility(View.VISIBLE);
					} else {
						bar.setVisibility(View.INVISIBLE);
					}
				}

				String itemDescr = getItemDescr(position);
				if (v.findViewById(R.id.descr) != null){
					((TextView)v.findViewById(R.id.descr)).setText(itemDescr);
				}
				return v;
			}
		};
		return listAdapter;
	}

}
