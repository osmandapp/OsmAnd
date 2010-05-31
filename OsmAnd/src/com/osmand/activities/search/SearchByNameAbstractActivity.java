package com.osmand.activities.search;

import java.util.List;

import android.app.ListActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.osmand.R;

public abstract class SearchByNameAbstractActivity<T> extends ListActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.search_by_name);
		NamesAdapter namesAdapter = new NamesAdapter(getObjects());
		setListAdapter(namesAdapter);
	}

	public abstract List<T> getObjects();

	public abstract void updateTextView(T obj, TextView txt);
	
	public abstract void itemSelected(T obj);
	
	@SuppressWarnings("unchecked")
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		T repo = (T) getListAdapter().getItem(position);
		itemSelected(repo);
		
	}

	class NamesAdapter extends ArrayAdapter<T> {
		NamesAdapter(List<T> list) {
			super(SearchByNameAbstractActivity.this, R.layout.searchbyname_list, list);
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			LayoutInflater inflater = getLayoutInflater();
			View row = inflater.inflate(R.layout.searchbyname_list, parent, false);
			TextView label = (TextView) row.findViewById(R.id.NameLabel);
			updateTextView(getItem(position), label);
			return row;
		}
	}
}
