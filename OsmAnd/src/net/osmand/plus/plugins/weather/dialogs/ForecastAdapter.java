package net.osmand.plus.plugins.weather.dialogs;

import static androidx.recyclerview.widget.RecyclerView.NO_POSITION;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.CallbackWithObject;
import net.osmand.plus.R;
import net.osmand.plus.plugins.weather.dialogs.ForecastAdapter.DateViewHolder;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.utils.UiUtilities;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

class ForecastAdapter extends RecyclerView.Adapter<DateViewHolder> {

	private final SimpleDateFormat DAY_FORMAT = new SimpleDateFormat("d.M", Locale.getDefault());
	private final SimpleDateFormat DAY_OF_WEEK_FORMAT = new SimpleDateFormat("E", Locale.getDefault());

	private static final int MAX_FORECAST_DAYS = 6;

	private final Context ctx;
	private final LayoutInflater inflater;

	private Calendar currentDate;
	private Calendar selectedDate;
	private final List<Date> dates = new ArrayList<>();

	private final boolean nightMode;
	private final CallbackWithObject<Date> callback;

	ForecastAdapter(@NonNull Context ctx, @Nullable CallbackWithObject<Date> callback, boolean nightMode) {
		this.ctx = ctx;
		this.callback = callback;
		this.nightMode = nightMode;
		inflater = UiUtilities.getInflater(ctx, nightMode);
	}

	protected void initDates(@NonNull Calendar currentDate, @NonNull Calendar selectedDate) {
		this.currentDate = currentDate;
		this.selectedDate = selectedDate;

		Calendar calendar = WeatherForecastFragment.getDefaultCalendar();
		dates.add(currentDate.getTime());
		for (int i = 0; i <= MAX_FORECAST_DAYS; i++) {
			calendar.add(Calendar.DAY_OF_MONTH, 1);
			dates.add(calendar.getTime());
		}
	}

	@NonNull
	@Override
	public DateViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		View view = inflater.inflate(R.layout.forecast_date_item, parent, false);
		AndroidUtils.setBackground(ctx, view, nightMode, R.drawable.ripple_solid_light_6dp, R.drawable.ripple_solid_dark_6dp);
		return new DateViewHolder(view);
	}

	@Override
	public void onBindViewHolder(@NonNull DateViewHolder holder, int position) {
		Date date = dates.get(position);

		boolean today = OsmAndFormatter.isSameDay(date, currentDate.getTime());
		boolean selected = OsmAndFormatter.isSameDay(date, selectedDate.getTime());

		holder.title.setText(today ? ctx.getString(R.string.today) : DAY_FORMAT.format(date));
		holder.description.setText(DAY_OF_WEEK_FORMAT.format(date));

		int descriptionColor = ColorUtilities.getSecondaryTextColor(ctx, nightMode);
		if (selected) {
			int activeColor = AndroidUtils.getColorFromAttr(ctx, R.attr.active_color_basic);
			descriptionColor = ColorUtilities.getColorWithAlpha(activeColor, 0.5f);
		}
		holder.description.setTextColor(descriptionColor);

		holder.itemView.setOnClickListener(view -> {
			int pos = holder.getAdapterPosition();
			if (callback != null) {
				callback.processResult(dates.get(pos));
			}
			notifyDataSetChanged();
		});
		updateBackground(holder, selected);
	}

	private void updateBackground(@NonNull DateViewHolder holder, boolean selected) {
		GradientDrawable rectContourDrawable = (GradientDrawable) AppCompatResources.getDrawable(ctx, R.drawable.bg_select_group_button_outline_small);
		if (rectContourDrawable != null) {
			if (selected) {
				int activeColor = AndroidUtils.getColorFromAttr(ctx, R.attr.active_color_basic);
				int strokeColor = ContextCompat.getColor(ctx, ColorUtilities.getActiveColorId(nightMode));

				rectContourDrawable.setStroke(AndroidUtils.dpToPx(ctx, 2), strokeColor);
				rectContourDrawable.setColor(ColorUtilities.getColorWithAlpha(activeColor, 0.1f));
			} else {
				int strokeColor = ContextCompat.getColor(ctx, nightMode ?
						R.color.stroked_buttons_and_links_outline_dark :
						R.color.stroked_buttons_and_links_outline_light);
				rectContourDrawable.setStroke(AndroidUtils.dpToPx(ctx, 1), strokeColor);
				rectContourDrawable.setColor(AndroidUtils.getColorFromAttr(ctx, R.attr.ctx_menu_card_btn));
			}
			holder.outlineRect.setImageDrawable(rectContourDrawable);
		}
	}

	private int getItemPosition(@NonNull Date date) {
		for (int i = 0; i < dates.size(); i++) {
			if (OsmAndFormatter.isSameDay(date, dates.get(i))) {
				return i;
			}
		}
		return NO_POSITION;
	}

	@Override
	public int getItemCount() {
		return dates.size();
	}

	protected static class DateViewHolder extends RecyclerView.ViewHolder {

		public final TextView title;
		public final TextView description;
		public final ImageView outlineRect;

		public DateViewHolder(@NonNull View itemView) {
			super(itemView);
			title = itemView.findViewById(R.id.title);
			description = itemView.findViewById(R.id.description);
			outlineRect = itemView.findViewById(R.id.outlineRect);
		}
	}
}