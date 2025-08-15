package net.osmand.plus.plugins.osmedit.fragments.holders;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.R;
import net.osmand.plus.plugins.osmedit.fragments.BasicEditPoiFragment.OpenHoursItem;
import net.osmand.plus.plugins.osmedit.fragments.BasicEditPoiFragment.OpeningHoursAdapter;
import net.osmand.plus.plugins.osmedit.dialogs.OpeningHoursDaysDialogFragment;
import net.osmand.plus.plugins.osmedit.dialogs.OpeningHoursHoursDialogFragment;
import net.osmand.plus.plugins.osmedit.fragments.EditPoiContentAdapter.EditPoiAdapterListener;
import net.osmand.plus.plugins.osmedit.fragments.EditPoiContentAdapter.EditPoiListener;
import net.osmand.util.Algorithms;
import net.osmand.util.OpeningHoursParser;

import gnu.trove.list.array.TIntArrayList;

public class OpenTimeListHolder extends RecyclerView.ViewHolder {

	private final ImageView clockIconImageView;
	private final TextView daysTextView;
	private final LinearLayout timeListContainer;
	private final ImageButton deleteItemImageButton;
	private final Button addTimeSpanButton;
	private final Activity activity;

	public OpenTimeListHolder(@NonNull View itemView, @NonNull Activity activity) {
		super(itemView);
		this.activity = activity;
		clockIconImageView = itemView.findViewById(R.id.clockIconImageView);
		daysTextView = itemView.findViewById(R.id.daysTextView);
		timeListContainer = itemView.findViewById(R.id.timeListContainer);
		deleteItemImageButton = itemView.findViewById(R.id.deleteItemImageButton);
		addTimeSpanButton = itemView.findViewById(R.id.addTimeSpanButton);
	}

	public void bindView(@NonNull OpenHoursItem openHoursItem, @NonNull OpeningHoursAdapter openingHoursAdapter,
	                     @NonNull EditPoiAdapterListener editPoiAdapterListener, @NonNull EditPoiListener editPoiListener) {
		timeListContainer.removeAllViews();
		OpeningHoursParser.OpeningHours openingHours = openingHoursAdapter.getOpeningHours();
		int position = openHoursItem.position();
		clockIconImageView.setImageDrawable(openingHoursAdapter.getClockDrawable());

		if(openingHours.getRules().size() > position) {
			if (openingHours.getRules().get(position) instanceof OpeningHoursParser.BasicOpeningHourRule rule) {
				StringBuilder stringBuilder = new StringBuilder();
				rule.appendDaysString(stringBuilder);

				daysTextView.setText(stringBuilder.toString());
				daysTextView.setOnClickListener(v -> {
					FragmentManager fragmentManager = editPoiListener.getChildFragmentManager();
					OpeningHoursDaysDialogFragment.showInstance(fragmentManager, rule, position);
				});

				TIntArrayList startTimes = rule.getStartTimes();
				TIntArrayList endTimes = rule.getEndTimes();
				for (int i = 0; i < startTimes.size(); i++) {
					View timeFromToLayout = LayoutInflater.from(activity)
							.inflate(R.layout.time_from_to_layout, timeListContainer, false);
					TextView openingTextView = timeFromToLayout.findViewById(R.id.openingTextView);
					openingTextView.setText(Algorithms.formatMinutesDuration(startTimes.get(i)));

					TextView closingTextView = timeFromToLayout.findViewById(R.id.closingTextView);
					closingTextView.setText(Algorithms.formatMinutesDuration(endTimes.get(i)));

					openingTextView.setTag(i);
					openingTextView.setOnClickListener(v -> {
						int index = (int) v.getTag();
						FragmentManager fragmentManager = editPoiListener.getChildFragmentManager();
						OpeningHoursHoursDialogFragment.showInstance(fragmentManager, rule, position, true, index);
					});
					closingTextView.setTag(i);
					closingTextView.setOnClickListener(v -> {
						int index = (int) v.getTag();
						FragmentManager fragmentManager = editPoiListener.getChildFragmentManager();
						OpeningHoursHoursDialogFragment.showInstance(fragmentManager, rule, position, false, index);
					});

					ImageButton deleteTimeSpanImageButton = timeFromToLayout
							.findViewById(R.id.deleteTimespanImageButton);
					deleteTimeSpanImageButton.setImageDrawable(openingHoursAdapter.getDeleteDrawable());
					int timeSpanPosition = i;
					deleteTimeSpanImageButton.setOnClickListener(v -> {
						if (startTimes.size() == 1) {
							openingHours.getRules().remove(position);
						} else {
							rule.deleteTimeRange(timeSpanPosition);
						}
						openingHoursAdapter.updateHoursData();
						editPoiAdapterListener.dataChanged();
					});
					timeListContainer.addView(timeFromToLayout);
				}

				deleteItemImageButton.setVisibility(View.GONE);
				addTimeSpanButton.setVisibility(View.VISIBLE);
				addTimeSpanButton.setOnClickListener(v -> {
					FragmentManager fragmentManager = editPoiListener.getChildFragmentManager();
					OpeningHoursHoursDialogFragment.showInstance(
							fragmentManager, rule, position, true, startTimes.size());
				});
			} else if (openingHours.getRules().get(position) instanceof OpeningHoursParser.UnparseableRule) {
				daysTextView.setText(openingHours.getRules().get(position).toRuleString());
				timeListContainer.removeAllViews();

				deleteItemImageButton.setVisibility(View.VISIBLE);
				deleteItemImageButton.setImageDrawable(openingHoursAdapter.getDeleteDrawable());
				deleteItemImageButton.setOnClickListener(v -> {
					openingHours.getRules().remove(position);
					openingHoursAdapter.updateHoursData();
					editPoiAdapterListener.dataChanged();
				});
				addTimeSpanButton.setVisibility(View.GONE);
			}
		}
	}
}