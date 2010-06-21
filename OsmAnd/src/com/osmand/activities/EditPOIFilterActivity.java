/**
 * 
 */
package com.osmand.activities;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.osmand.PoiFilter;
import com.osmand.PoiFiltersHelper;
import com.osmand.R;
import com.osmand.activities.search.SearchPOIActivity;
import com.osmand.data.AmenityType;

/**
 * @author Frolov
 * 
 */
public class EditPOIFilterActivity extends ListActivity {

	private Button filterLevel;
	private PoiFilter filter;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.editing_poi_filter);

		filterLevel = (Button) findViewById(R.id.filter_currentButton);
		filterLevel.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO

			}
		});

		Bundle bundle = this.getIntent().getExtras();
		String filterId = bundle.getString(SearchPOIActivity.AMENITY_FILTER);
		filter = PoiFiltersHelper.getFilterById(this, filterId);

		setListAdapter(new AmenityAdapter(AmenityType.getCategories()));
	}

	private void showDialog(AmenityType amenity) {
		Builder builder = new AlertDialog.Builder(this);
		ScrollView scroll = new ScrollView(this);
		ListView listView = new ListView(this);
		scroll.addView(listView);
		builder.setView(scroll);
		builder.setNegativeButton("Close", null);
		builder.setNeutralButton("Select all", null);
		builder.setMultiChoiceItems(AmenityType.getSubCategories(amenity).toArray(new String[0]), null, null);
		builder.show();

	}
	

	class AmenityAdapter extends ArrayAdapter<AmenityType> {
		AmenityAdapter(AmenityType[] amenityTypes) {
			super(EditPOIFilterActivity.this, R.layout.editing_poi_filter_list, amenityTypes);
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			LayoutInflater inflater = getLayoutInflater();
			View row = convertView;
			if (row == null) {
				row = inflater.inflate(R.layout.editing_poi_filter_list, parent, false);
			}
			AmenityType model = getItem(position);

			CheckBox check = (CheckBox) row.findViewById(R.id.filter_poi_check);
			check.setChecked(filter.isTypeAccepted(model));

			TextView text = (TextView) row.findViewById(R.id.filter_poi_label);
			text.setText(AmenityType.toPublicString(model));
			addRowListener(model, text, check);
			return (row);
		}

		private void addRowListener(final AmenityType model,final TextView text, final CheckBox check) {
			text.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					showDialog(model);
				}
			});

			check.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					if(check.isChecked()) {
						showDialog(model);
					} else {
						filter.setTypeToAccept(model,false);
					}
				}
			});
		}

	}

}
