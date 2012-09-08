/**
 * 
 */
package net.osmand.plus.activities;

import java.text.Collator;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;

import net.osmand.OsmAndFormatter;
import net.osmand.access.AccessibleToast;
import net.osmand.data.AmenityType;
import net.osmand.osm.LatLon;
import net.osmand.osm.MapRenderingTypes;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.PoiFilter;
import net.osmand.plus.PoiFiltersHelper;
import net.osmand.plus.R;
import net.osmand.plus.SpecialPhrases;
import net.osmand.plus.activities.search.SearchActivity;
import net.osmand.plus.activities.search.SearchPOIActivity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * 
 */
public class EditPOIFilterActivity extends OsmandListActivity {
	public static final String AMENITY_FILTER = "net.osmand.amenity_filter"; //$NON-NLS-1$
	private Button filterLevel;
	private PoiFilter filter;
	private PoiFiltersHelper helper;
	private CustomTitleBar titleBar;
	public static final String SEARCH_LAT = SearchActivity.SEARCH_LAT; //$NON-NLS-1$
	public static final String SEARCH_LON = SearchActivity.SEARCH_LON; //$NON-NLS-1$
	

	@Override
	public void onCreate(final Bundle icicle) {
		super.onCreate(icicle);
		titleBar = new CustomTitleBar(this, R.string.searchpoi_activity, R.drawable.tab_search_poi_icon);
		setContentView(R.layout.editing_poi_filter);
		titleBar.afterSetContentView();
		

		filterLevel = (Button) findViewById(R.id.filter_currentButton);
		filterLevel.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Bundle extras = getIntent().getExtras();
				boolean searchNearBy = true;
				LatLon lastKnownMapLocation = ((OsmandApplication) getApplication()).getSettings().getLastKnownMapLocation();
				double latitude = lastKnownMapLocation != null ? lastKnownMapLocation.getLatitude() : 0;
				double longitude = lastKnownMapLocation != null ? lastKnownMapLocation.getLongitude() : 0;
				final Intent newIntent = new Intent(EditPOIFilterActivity.this, SearchPOIActivity.class);
				if(extras != null && extras.containsKey(SEARCH_LAT) && extras.containsKey(SEARCH_LON)){
					latitude = extras.getDouble(SEARCH_LAT);
					longitude = extras.getDouble(SEARCH_LON);
					searchNearBy = false;
				}
				final double lat = latitude;
				final double lon = longitude;
				newIntent.putExtra(SearchPOIActivity.AMENITY_FILTER, filter.getFilterId());
				if (searchNearBy) {
					AlertDialog.Builder b = new AlertDialog.Builder(EditPOIFilterActivity.this);
					b.setItems(new String[] { getString(R.string.search_nearby), getString(R.string.search_near_map) },
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									if (which == 1) {
										newIntent.putExtra(SearchPOIActivity.SEARCH_LAT, lat);
										newIntent.putExtra(SearchPOIActivity.SEARCH_LON, lon);
									}
									startActivity(newIntent);
								}
							});
					b.show();
				} else {
					newIntent.putExtra(SearchPOIActivity.SEARCH_LAT, lat);
					newIntent.putExtra(SearchPOIActivity.SEARCH_LON, lon);
					startActivity(newIntent);
				}
			}
		});
		
		((ImageButton) findViewById(R.id.SaveButton)).setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				savePoiFilter();
			}
		});

		Bundle bundle = this.getIntent().getExtras();
		String filterId = bundle.getString(AMENITY_FILTER);
		
		helper = ((OsmandApplication)getApplication()).getPoiFilters();
		filter = helper.getFilterById(filterId);
		titleBar.getTitleView().setText(getString(R.string.filterpoi_activity) + " - " + filter.getName());

		setListAdapter(new AmenityAdapter(AmenityType.getCategories()));
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.edit_filter_menu, menu);
		return true;
	}
	public void savePoiFilter() {
		Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.edit_filter_save_as_menu_item);
		final EditText editText = new EditText(this);
		LinearLayout ll = new LinearLayout(this);
		ll.setPadding(5, 3, 5, 0);
		ll.addView(editText, new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
		builder.setView(ll);
		builder.setNegativeButton(R.string.default_buttons_cancel, null);
		builder.setPositiveButton(R.string.default_buttons_yes, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				PoiFilter nFilter = new PoiFilter(editText.getText().toString(), null, filter.getAcceptedTypes(), (OsmandApplication) getApplication());
				if (helper.createPoiFilter(nFilter)) {
					AccessibleToast.makeText(
							EditPOIFilterActivity.this,
							MessageFormat.format(EditPOIFilterActivity.this.getText(R.string.edit_filter_create_message).toString(),
									editText.getText().toString()), Toast.LENGTH_SHORT).show();
				}
				EditPOIFilterActivity.this.finish();
			}
		});
		builder.create().show();
		
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.edit_filter_delete) {
			Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(R.string.edit_filter_delete_dialog_title);
			builder.setNegativeButton(R.string.default_buttons_no, null);
			builder.setPositiveButton(R.string.default_buttons_yes, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					if (helper.removePoiFilter(filter)) {
						AccessibleToast.makeText(
								EditPOIFilterActivity.this,
								MessageFormat.format(EditPOIFilterActivity.this.getText(R.string.edit_filter_delete_message).toString(),
										filter.getName()), Toast.LENGTH_SHORT).show();
						EditPOIFilterActivity.this.finish();
					}

				}
			});
			builder.create().show();
			return true;
		} else if (item.getItemId() == R.id.edit_filter_save_as) {
			savePoiFilter();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	private void showDialog(final AmenityType amenity) {
		Builder builder = new AlertDialog.Builder(this);
		ScrollView scroll = new ScrollView(this);
		ListView listView = new ListView(this);
		
		final LinkedHashSet<String> subCategories = new LinkedHashSet<String>(AmenityType.getSubCategories(amenity, MapRenderingTypes.getDefault()));
		Set<String> acceptedCategories = filter.getAcceptedSubtypes(amenity);
		if (acceptedCategories != null) {
			for (String s : acceptedCategories) {
				if (!subCategories.contains(s)) {
					subCategories.add(s);
				}
			}
		}

		final String[] array = subCategories.toArray(new String[0]);
		final Collator cl = Collator.getInstance();
		cl.setStrength(Collator.SECONDARY);
		Arrays.sort(array, 0, array.length, new Comparator<String>() {

			@Override
			public int compare(String object1, String object2) {
				String v1 = SpecialPhrases.getSpecialPhrase(object1).replace('_', ' ');
				String v2 = SpecialPhrases.getSpecialPhrase(object2).replace('_', ' ');
				return cl.compare(v1, v2);
			}
		});
		final String[] visibleNames = new String[array.length];
		final boolean[] selected = new boolean[array.length];
		
		for (int i = 0; i < array.length; i++) {
			visibleNames[i] = SpecialPhrases.getSpecialPhrase(array[i]).replace('_', ' ');			
			if (acceptedCategories == null) {
				selected[i] = true;
			} else {
				selected[i] = acceptedCategories.contains(array[i]);
			}
		}

		scroll.addView(listView);
		builder.setView(scroll);
		builder.setNeutralButton(EditPOIFilterActivity.this.getText(R.string.close), new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				LinkedHashSet<String> accepted = new LinkedHashSet<String>();
				for (int i = 0; i < selected.length; i++) {
					if(selected[i]){
						accepted.add(array[i]);
					}
				}
				if (subCategories.size() == accepted.size()) {
					filter.selectSubTypesToAccept(amenity, null);
				} else if(accepted.size() == 0){
					filter.setTypeToAccept(amenity, false);
				} else {
					filter.selectSubTypesToAccept(amenity, accepted);
				}
				helper.editPoiFilter(filter);
				((AmenityAdapter) EditPOIFilterActivity.this.getListAdapter()).notifyDataSetInvalidated();
			}
		});
	
		builder.setPositiveButton(EditPOIFilterActivity.this.getText(R.string.default_buttons_selectall), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				filter.selectSubTypesToAccept(amenity, null);
				helper.editPoiFilter(filter);
				((AmenityAdapter) EditPOIFilterActivity.this.getListAdapter()).notifyDataSetInvalidated();
			}
		});

		builder.setMultiChoiceItems(visibleNames, selected, new DialogInterface.OnMultiChoiceClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int item, boolean isChecked) {
				selected[item] = isChecked;
			}
		});
		builder.show();

	}
	
	
	@Override
	public AmenityAdapter getListAdapter() {
		return (AmenityAdapter) super.getListAdapter();
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		showDialog(getListAdapter().getItem(position));
	}

	class AmenityAdapter extends ArrayAdapter<AmenityType> {
		AmenityAdapter(AmenityType[] amenityTypes) {
			super(EditPOIFilterActivity.this, R.layout.editing_poi_filter_list, amenityTypes);
		}

		@Override
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
			text.setText(OsmAndFormatter.toPublicString(model, EditPOIFilterActivity.this));
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
						helper.editPoiFilter(filter);
					}
				}
			});
		}

	}

}
