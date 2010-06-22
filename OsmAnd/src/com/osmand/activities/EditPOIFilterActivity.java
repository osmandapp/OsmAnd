/**
 * 
 */
package com.osmand.activities;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.osmand.OsmandSettings;
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
				OsmandSettings.setPoiFilterForMap(EditPOIFilterActivity.this, filter.getFilterId());
				OsmandSettings.setShowPoiOverMap(EditPOIFilterActivity.this, true);
				Intent newIntent = new Intent(EditPOIFilterActivity.this, MapActivity.class);
				startActivity(newIntent);
			}
		});

		Bundle bundle = this.getIntent().getExtras();
		String filterId = bundle.getString(SearchPOIActivity.AMENITY_FILTER);
		filter = PoiFiltersHelper.getFilterById(this, filterId);

		setListAdapter(new AmenityAdapter(AmenityType.getCategories()));
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.edit_filter_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.edit_filter_delete) {
			EditPOIFilterActivity.this.finish();
			Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(R.string.edit_filter_delete_dialog_title);
			builder.setNegativeButton(R.string.default_buttons_no, null);
			builder.setPositiveButton(R.string.default_buttons_yes, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					if (PoiFiltersHelper.removePoiFilter(EditPOIFilterActivity.this, filter)) {
						Toast.makeText(
								EditPOIFilterActivity.this,
								MessageFormat.format(EditPOIFilterActivity.this.getText(R.string.edit_filter_delete_message).toString(),
										filter.getName()), Toast.LENGTH_SHORT).show();
					}

				}
			});
			builder.create().show();
			return true;
		} else if (item.getItemId() == R.id.edit_filter_save_as) {
			Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.edit_filter_save_as_menu_item);
			final EditText editText = new EditText(this);
			builder.setView(editText);
			builder.setNegativeButton(R.string.default_buttons_cancel, null);
			builder.setPositiveButton(R.string.default_buttons_yes, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					PoiFilter nFilter = new PoiFilter(editText.getText().toString(), null, filter.getAcceptedTypes());
					if (PoiFiltersHelper.createPoiFilter(EditPOIFilterActivity.this, nFilter)) {
						Toast.makeText(
								EditPOIFilterActivity.this,
								MessageFormat.format(EditPOIFilterActivity.this.getText(R.string.edit_filter_create_message).toString(),
										editText.getText().toString()), Toast.LENGTH_SHORT).show();
					}
					EditPOIFilterActivity.this.finish();
				}
			});
			builder.create().show();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void showDialog(final AmenityType amenity) {
		Builder builder = new AlertDialog.Builder(this);
		ScrollView scroll = new ScrollView(this);
		ListView listView = new ListView(this);
		final List<String> accepted = new ArrayList<String>();
		final LinkedHashSet<String> subCategories = new LinkedHashSet<String>(AmenityType.getSubCategories(amenity));
		List<String> subtypes = filter.getAcceptedSubtypes(amenity);
		boolean allSubTypesAccepted = subtypes == null;
		LinkedHashSet<String> acceptedCategories = subtypes == null ? null : new LinkedHashSet<String>(subtypes);
		if (subtypes != null) {
			for (String s : acceptedCategories) {
				if (!subCategories.contains(s)) {
					subCategories.add(s);
				}
			}
		}

		final String[] array = subCategories.toArray(new String[0]);
		boolean[] selected = new boolean[array.length];
		for (int i = 0; i < selected.length; i++) {
			if (allSubTypesAccepted) {
				selected[i] = true;
				accepted.add(array[i]);
			} else {
				selected[i] = acceptedCategories.contains(array[i]);
				if (selected[i]) {
					accepted.add(array[i]);
				}
			}
		}

		scroll.addView(listView);
		builder.setView(scroll);
		builder.setNegativeButton(EditPOIFilterActivity.this.getText(R.string.default_buttons_cancel), new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				if (subCategories.size() == accepted.size()) {
					filter.selectSubTypesToAccept(amenity, null);
				} else {
					filter.selectSubTypesToAccept(amenity, accepted);
				}
				PoiFiltersHelper.editPoiFilter(EditPOIFilterActivity.this, filter);
				((AmenityAdapter) EditPOIFilterActivity.this.getListAdapter()).notifyDataSetInvalidated();
			}
		});
	
		builder.setNeutralButton(EditPOIFilterActivity.this.getText(R.string.default_buttons_selectall), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				filter.selectSubTypesToAccept(amenity, null);
				PoiFiltersHelper.editPoiFilter(EditPOIFilterActivity.this, filter);
				((AmenityAdapter) EditPOIFilterActivity.this.getListAdapter()).notifyDataSetInvalidated();
			}
		});

		builder.setMultiChoiceItems(array, selected, new DialogInterface.OnMultiChoiceClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int item, boolean isChecked) {
				if (isChecked && !accepted.contains(array[item])) {
					accepted.add(array[item]);
				} else if (!isChecked && accepted.contains(array[item])) {
					accepted.remove(array[item]);
				}

			}
		});
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

		private void addRowListener(final AmenityType model, final TextView text, final CheckBox check) {
			text.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					showDialog(model);
				}
			});

			check.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					if (check.isChecked()) {
						filter.setTypeToAccept(model, true);
						showDialog(model);
					} else {
						filter.setTypeToAccept(model, false);
						PoiFiltersHelper.editPoiFilter(EditPOIFilterActivity.this, filter);
					}
				}
			});
		}

	}

}
