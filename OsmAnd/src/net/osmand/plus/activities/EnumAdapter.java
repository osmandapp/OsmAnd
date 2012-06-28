package net.osmand.plus.activities;

import net.osmand.plus.activities.EnumAdapter.IEnumWithResource;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class EnumAdapter<T extends IEnumWithResource> 
	extends ArrayAdapter<T> 
{

	public EnumAdapter(Context context, int textViewResourceId, T[] enums)
	{
		super(context, textViewResourceId, enums);
	}
	
	@Override
	public View getDropDownView(int position, View convertView, ViewGroup parent) {
		TextView textView = (TextView) super.getDropDownView(position, convertView, parent);
		T item = getItem(position);
		textView.setText(item.stringResource());
		return textView;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		TextView textView = (TextView) super.getView(position, convertView, parent);
		T item = getItem(position);
		textView.setText(item.stringResource());
		return textView;
	}
	
	public static interface IEnumWithResource {
		int stringResource();
	}
}
