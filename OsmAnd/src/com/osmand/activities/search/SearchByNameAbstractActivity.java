package com.osmand.activities.search;

import java.util.List;

import android.app.ListActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.osmand.R;

public abstract class SearchByNameAbstractActivity<T> extends ListActivity {

	private EditText searchText;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.search_by_name);
		NamesAdapter namesAdapter = new NamesAdapter(getObjects(""));
		setListAdapter(namesAdapter);
		searchText = (EditText) findViewById(R.id.SearchText);
		searchText.addTextChangedListener(new TextWatcher(){

			@Override
			public void afterTextChanged(Editable s) {
				setText(s.toString());
			}
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}
			
		});
		findViewById(R.id.ResetButton).setOnClickListener(new View.OnClickListener(){

			@Override
			public void onClick(View v) {
				searchText.setText("");
			}
			
		});
	}
	
	public Editable getFilter(){
		return searchText.getText();
	}
	public void setText(String filter){
		((NamesAdapter)getListAdapter()).clear();
		for(T o : getObjects(filter)){
			((NamesAdapter)getListAdapter()).add(o);
		}
	}

	public abstract List<T> getObjects(String filter);

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
			View row;
			if (convertView != null) {
				row = convertView;
			} else {
				LayoutInflater inflater = getLayoutInflater();
				row = inflater.inflate(R.layout.searchbyname_list, parent, false);
			}
			TextView label = (TextView) row.findViewById(R.id.NameLabel);
			updateTextView(getItem(position), label);
			return row;
		}
	}
}
