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
	
	public int getImageId(int pos) {
		return iconList.get(pos);
	}
	
	public void registerItem(int stringResId, int icon, OnContextMenuClick listener, int pos) {
		registerSelectedItem(stringResId, -1, icon, listener, pos);
	}
	
	public void registerItem(int stringResId, int icon, OnContextMenuClick listener) {
		registerSelectedItem(stringResId, -1, icon, listener, -1);
	}
	
	public void registerItem(int stringResId, OnContextMenuClick listener) {
		registerSelectedItem(stringResId, -1, 0, listener, -1);
	}
	
	
	public void registerSelectedItem(int stringResId, int selected, int icon, OnContextMenuClick listener, int pos) {
		if(pos >= items.size() || pos < 0) {
			pos = items.size();
		}
		items.insert(pos, stringResId);
		itemNames.add(pos, ctx.getString(stringResId));
		selectedList.insert(pos, selected);
		iconList.insert(pos, icon);
		listeners.add(pos, listener);
	}
	
	public void registerSelectedItem(int stringResId, int selected, int icon) {
		registerSelectedItem(stringResId, selected, icon, null, -1);
	}

	public void registerItem(int stringResId, int icon) {
		registerSelectedItem(stringResId, -1, icon);
	}

	public void registerItem(int stringResId) {
		registerSelectedItem(stringResId, -1, 0);
	}

	public String[] getItemNames() {
		return itemNames.toArray(new String[itemNames.size()]);
	}

	public ListAdapter createListAdapter(final Activity activity, final int layoutId) {
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
				if (getImageId(position) != 0) {
					tv.setCompoundDrawablesWithIntrinsicBounds(getImageId(position), 0, 0, 0);
				} else {
					tv.setCompoundDrawablesWithIntrinsicBounds(R.drawable.list_activities_transparent, 0, 0, 0);
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
