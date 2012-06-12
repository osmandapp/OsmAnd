package net.osmand.plus;

import gnu.trove.list.array.TIntArrayList;

import java.util.ArrayList;

import android.content.Context;
import android.content.DialogInterface;

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

}
