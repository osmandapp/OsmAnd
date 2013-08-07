package net.osmand.plus;

import gnu.trove.list.array.TIntArrayList;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListAdapter;
import android.widget.TextView;

public class ContextMenuAdapter {
	
	public interface OnContextMenuClick {
		
		public void onContextMenuClick(int itemId, int pos, boolean isChecked, DialogInterface dialog);
	}
	
	private final Context ctx;
	final TIntArrayList items = new TIntArrayList();
	final ArrayList<String> itemNames = new ArrayList<String>();
	final ArrayList<OnContextMenuClick> listeners = new ArrayList<ContextMenuAdapter.OnContextMenuClick>();
	final TIntArrayList selectedList = new TIntArrayList();
	final TIntArrayList iconList = new TIntArrayList();
	final TIntArrayList iconListLight = new TIntArrayList();

	public ContextMenuAdapter(Context ctx) {
		this.ctx = ctx;
	}
	
	public int length(){
		return items.size();
	}
	
	public int getItemId(int pos){
		return items.get(pos);
	}
	
	public OnContextMenuClick getClickAdapter(int i) {
		return listeners.get(i);
	}
	
	public String getItemName(int pos){
		return itemNames.get(pos);
	}
	
	public int getSelection(int pos) {
		return selectedList.get(pos);
	}
	
	public void setSelection(int pos, int s) {
		selectedList.set(pos, s);
	}
	
	public int getImageId(int pos, boolean light) {
		if(!light || iconListLight.get(pos) == 0) {
			return iconList.get(pos);
		}
		return iconListLight.get(pos);
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
		int pos = -1;
		private OnContextMenuClick listener;

		private Item() {
		}

		public Item icon(int icon) {
			this.icon = icon;
			return this;
		}

		public Item icons(int icon, int lightIcon) {
			this.icon = icon;
			this.lightIcon = lightIcon;
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

		public Item listen(OnContextMenuClick l) {
			this.listener = l;
			return this;

		}

		public void reg() {
			if (pos >= items.size() || pos < 0) {
				pos = items.size();
			}
			items.insert(pos, id);
			itemNames.add(pos, name);
			selectedList.insert(pos, selected);
			iconList.insert(pos, icon);
			iconListLight.insert(pos, lightIcon);
			listeners.add(pos, listener);

		}

	}
	
	public String[] getItemNames() {
		return itemNames.toArray(new String[itemNames.size()]);
	}

	

	public ListAdapter createListAdapter(final Activity activity, final int layoutId, final boolean holoLight) {
		final int padding = (int) (12 * activity.getResources().getDisplayMetrics().density + 0.5f);
		ListAdapter listadapter = new ArrayAdapter<String>(activity, layoutId, R.id.title,
				getItemNames()) {
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				// User super class to create the View
				View v = convertView;
				if (v == null) {
					v = activity.getLayoutInflater().inflate(layoutId, null);
				}
				TextView tv = (TextView) v.findViewById(R.id.title);
				tv.setText(getItemName(position));

				// Put the image on the TextView
				if (getImageId(position, holoLight) != 0) {
					tv.setCompoundDrawablesWithIntrinsicBounds(getImageId(position, holoLight), 0, 0, 0);
				} else {
					tv.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_action_transparent, 0, 0, 0);
				}
				tv.setCompoundDrawablePadding(padding);

				final CheckBox ch = ((CheckBox) v.findViewById(R.id.check_item));
				ch.setVisibility(View.GONE);
				return v;
			}
		};
		return listadapter;
	}

}
