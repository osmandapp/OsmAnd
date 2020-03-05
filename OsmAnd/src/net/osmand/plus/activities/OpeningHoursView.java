package net.osmand.plus.activities;

import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.graphics.Typeface;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.TimePicker.OnTimeChangedListener;

import androidx.appcompat.app.AlertDialog;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.util.OpeningHoursParser.BasicOpeningHourRule;
import net.osmand.util.OpeningHoursParser.OpeningHoursRule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

public class OpeningHoursView {
	
	private final Context ctx;
	private int selectedRule = 0;
	private TimeAdapter time;
	private TimePicker timePickerStart;
	private TimePicker timePickerEnd;
	
	private boolean notifyingTime = true;
	private ListView list;
	private OsmandApplication app;

	public OpeningHoursView(Context ctx){
		this.ctx = ctx;
		app = (OsmandApplication) ctx.getApplicationContext();
	}
	
	public View createOpeningHoursEditView(List<BasicOpeningHourRule> t){
		this.time = new TimeAdapter(t);
		// editing object
		time.add(null);
		LayoutInflater inflater = (LayoutInflater)ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view = inflater.inflate(R.layout.open_hours_edit, null);
		timePickerStart = (TimePicker)view.findViewById(R.id.TimePickerStart);
		timePickerEnd = (TimePicker)view.findViewById(R.id.TimePickerEnd);
		list = (ListView)view.findViewById(R.id.ListView);
		list.setAdapter(time);
		OnTimeChangedListener onTimeChangedListener = new TimePicker.OnTimeChangedListener(){
			@Override
			public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
				if(selectedRule == -1 || !notifyingTime || time.getItem(selectedRule) == null){
					return;
				}
				if(view == timePickerStart ){
					time.getItem(selectedRule).setStartTime(hourOfDay * 60 + minute);
				} else {
					time.getItem(selectedRule).setEndTime(hourOfDay * 60 + minute);
				}
				time.notifyDataSetChanged();
			}
		};
		
		timePickerEnd.setIs24HourView(true);
		timePickerStart.setIs24HourView(true);
		timePickerStart.setCurrentHour(9);
		timePickerStart.setCurrentMinute(0);
		timePickerEnd.setCurrentHour(20);
		timePickerEnd.setCurrentMinute(0);

		timePickerEnd.setOnTimeChangedListener(onTimeChangedListener);
		timePickerStart.setOnTimeChangedListener(onTimeChangedListener);
		
		updateTimePickers();
		
		return view;
	}
	
	private class TimeAdapter extends ArrayAdapter<BasicOpeningHourRule> {
		
		public TimeAdapter(List<BasicOpeningHourRule> l ){
			super(ctx, R.layout.open_hours_list_item, l);
		}
		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			View row = convertView;
			final BasicOpeningHourRule item = getItem(position);
			if(item == null){
				TextView text = new TextView(getContext());
				text.setText(ctx.getString(R.string.add_new_rule));
				text.setTextSize(21);
				text.setTypeface(null, Typeface.ITALIC);
				text.setOnClickListener(new View.OnClickListener(){

					@Override
					public void onClick(View v) {
						BasicOpeningHourRule r = new BasicOpeningHourRule();
						r.setStartTime(timePickerStart.getCurrentHour()*60 + timePickerStart.getCurrentMinute());
						r.setEndTime(timePickerEnd.getCurrentHour()*60 + timePickerEnd.getCurrentMinute());
						
						boolean[] days = r.getDays();
						if(position == 0){
							// first time full all
							Arrays.fill(days, true);
						}
						showDaysDialog(r, position);
					}
					
				});
				return text;
			}
			if(row == null || row instanceof TextView){
				LayoutInflater inflater = (LayoutInflater)ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				row = inflater.inflate(R.layout.open_hours_list_item, parent, false);
			}
			TextView label = (TextView)row.findViewById(R.id.label);
			ImageView icon = (ImageView)row.findViewById(R.id.remove);
			icon.setBackgroundDrawable(app.getUIUtilities().getThemedIcon(R.drawable.ic_action_remove_dark));
			if(selectedRule == position){
				label.setTypeface(null, Typeface.BOLD);
				label.setTextSize(22);
			} else {
				label.setTypeface(null);
				label.setTextSize(20);
			}

			label.setText(item.toRuleString());
			icon.setOnClickListener(new View.OnClickListener(){
				@Override
				public void onClick(View v) {
					time.remove(item);
					selectedRule = time.getPosition(null);
					updateTimePickers();
				}
				
			});
			View.OnClickListener clickListener = new View.OnClickListener(){
				@Override
				public void onClick(View v) {
					if(selectedRule == position){
						showDaysDialog(item, -1);
					} else {
						selectedRule = position;
						updateTimePickers();
						time.notifyDataSetChanged();
					}
				}
				
			};
			label.setOnClickListener(clickListener);
			return row;
		}
	}
	
	public void showDaysDialog(final BasicOpeningHourRule item, final int positionToAdd) {
		AlertDialog.Builder b = new AlertDialog.Builder(ctx);

		boolean add = positionToAdd > -1;
		Calendar inst = Calendar.getInstance();
		final int first = inst.getFirstDayOfWeek();
		final boolean[] dayToShow = new boolean[7];
		String[] daysToShow = new String[7];
		for (int i = 0; i < 7; i++) {
			int d = (first + i - 1) % 7 + 1;
			inst.set(Calendar.DAY_OF_WEEK, d);
			daysToShow[i] = DateFormat.format("EEEE", inst).toString(); //$NON-NLS-1$
			final int pos = (d + 5) % 7;
			dayToShow[i] = item.getDays()[pos];
		}
		b.setMultiChoiceItems(daysToShow, dayToShow, new OnMultiChoiceClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which, boolean isChecked) {
				dayToShow[which] = isChecked;

			}

		});
		b.setPositiveButton(add ? ctx.getString(R.string.shared_string_add) : ctx.getString(R.string.shared_string_apply),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						boolean[] days = item.getDays();
						for (int i = 0; i < 7; i++) {
							days[(first + 5 + i) % 7] = dayToShow[i];
						}
						if (positionToAdd != -1) {
							time.insert(item, positionToAdd);
							selectedRule = positionToAdd;
						} else {
							time.notifyDataSetChanged();
						}
						updateTimePickers();

					}

				});

		b.setNegativeButton(ctx.getString(R.string.shared_string_cancel), null);

		b.show();

	}
		
	public void updateTimePickers() {
		if (selectedRule > -1) {
			BasicOpeningHourRule item = time.getItem(selectedRule);
			if (item != null) {
				notifyingTime = false;
				timePickerStart.setCurrentHour(item.getStartTime() / 60);
				timePickerStart.setCurrentMinute(item.getStartTime() % 60);
				timePickerEnd.setCurrentHour(item.getEndTime() / 60);
				timePickerEnd.setCurrentMinute(item.getEndTime() % 60);
				notifyingTime = true;
			}
		}
	}
	
	public List<OpeningHoursRule> getTime() {
		List<OpeningHoursRule> rules = new ArrayList<OpeningHoursRule>();
		for (int i = 0; i < time.getCount(); i++) {
			BasicOpeningHourRule r = time.getItem(i);
			if (r != null) {
				rules.add(r);
			}
		}
		return rules;
	}

}
