package com.osmand.activities;

import java.util.Calendar;

import android.content.Context;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import android.widget.TimePicker.OnTimeChangedListener;

import com.osmand.R;
import com.osmand.osm.OpeningHoursParser;

public class OpeningHoursView {
	
	private final Context ctx;
	private int selectedDay = -1;
	private int[][] time;
	private TimePicker timePickerStart;
	private TimePicker timePickerEnd;
	
	private boolean firstTime = true;
	private boolean notifyingTime = true;

	public OpeningHoursView(Context ctx){
		this.ctx = ctx;
	}
	
	public View createOpeningHoursEditView(int[][] t){
		this.time = t;
		LayoutInflater inflater = (LayoutInflater)ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view = inflater.inflate(R.layout.open_hours_edit, null);
		timePickerStart = (TimePicker)view.findViewById(R.id.TimePickerStart);
		timePickerEnd = (TimePicker)view.findViewById(R.id.TimePickerEnd);
		final TextView timeText =(TextView)view.findViewById(R.id.TimeText);
		
		
		OnTimeChangedListener onTimeChangedListener = new TimePicker.OnTimeChangedListener(){
			@Override
			public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
				if(selectedDay == -1 || !notifyingTime){
					return;
				}
				if(view == timePickerStart ){
					time[selectedDay][0] = hourOfDay * 60 + minute;
				} else {
					time[selectedDay][1] = hourOfDay * 60 + minute;
				}
				
				timeText.setText(OpeningHoursParser.toStringOpenedHours(time));
				
			}
		};
		
		Calendar inst = Calendar.getInstance();
		int first = inst.getFirstDayOfWeek();
		int[] ids = new int[]{R.id.Day1, R.id.Day2, R.id.Day3, R.id.Day4, R.id.Day5, R.id.Day6, R.id.Day7};
		for (int i = 0; i < 7; i++) {
			int d = (first + i - 1) % 7 + 1; 
			final CheckBox day = (CheckBox) view.findViewById(ids[i]);
			inst.set(Calendar.DAY_OF_WEEK, d);
			day.setText(DateFormat.format("E", inst)); //$NON-NLS-1$
			final int pos = (d + 5) % 7;
			if(time[pos][0] >= 0 && time[pos][1] >= 0){
				day.setChecked(true);
			} else {
				day.setChecked(false);
			}
			day.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
				
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					// try to unselect not current day 
					if(selectedDay != pos && !isChecked){
						selectedDay = pos;
						if(firstTime){
							Toast.makeText(ctx, "Press once to select day, twice to unselect it", Toast.LENGTH_LONG).show();
							firstTime = false;
						}
						// select it again
						day.setChecked(true);
					} else {
						// uncheck
						if(!isChecked){
							time[pos][0] = -1;
							time[pos][1] = -1;
							selectedDay = -1;
						} else {
							// check again
							if (selectedDay > -1 && pos != selectedDay) {
								time[pos][0] = time[selectedDay][0];
								time[pos][1] = time[selectedDay][1];
							}
							if (time[pos][0] < 0) {
								time[pos][0] = 8 * 60;
							}
							if (time[pos][1] < 0) {
								time[pos][1] = 20 * 60;
							}
							selectedDay = pos;
						}
					}
					timeText.setText(OpeningHoursParser.toStringOpenedHours(time));
					updateTimePickers();
					
				}
				
			});
		}
		
		// init 
		
		timePickerEnd.setIs24HourView(true);
		timePickerStart.setIs24HourView(true);
		timePickerStart.setCurrentHour(8);
		timePickerStart.setCurrentMinute(0);
		timePickerEnd.setCurrentHour(20);
		timePickerEnd.setCurrentMinute(0);
		timeText.setText(OpeningHoursParser.toStringOpenedHours(time));
		
		
		timePickerEnd.setOnTimeChangedListener(onTimeChangedListener);
		timePickerStart.setOnTimeChangedListener(onTimeChangedListener);
		
		return view;
	}
		
	public void updateTimePickers(){
		if(selectedDay > -1){
			notifyingTime = false;
			timePickerStart.setCurrentHour(time[selectedDay][0] / 60);
			timePickerStart.setCurrentMinute(time[selectedDay][0] % 60);
			timePickerEnd.setCurrentHour(time[selectedDay][1] / 60);
			timePickerEnd.setCurrentMinute(time[selectedDay][1] % 60);
			notifyingTime = true;
		}
	}
	
	public int[][] getTime() {
		return time;
	}

}
