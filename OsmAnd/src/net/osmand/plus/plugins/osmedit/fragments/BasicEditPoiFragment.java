package net.osmand.plus.plugins.osmedit.fragments;

import static net.osmand.plus.plugins.osmedit.dialogs.EditPoiDialogFragment.AMENITY_TEXT_LENGTH;
import static net.osmand.plus.plugins.osmedit.fragments.EditPoiContentAdapter.TYPE_ADD_OPENING_HOURS;
import static net.osmand.plus.plugins.osmedit.fragments.EditPoiContentAdapter.TYPE_BASIC_INFO;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.osm.edit.OSMSettings;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.BaseFullScreenFragment;
import net.osmand.plus.helpers.LocaleHelper;
import net.osmand.plus.plugins.osmedit.data.EditPoiData;
import net.osmand.plus.plugins.osmedit.dialogs.EditPoiDialogFragment;
import net.osmand.plus.plugins.osmedit.dialogs.EditPoiDialogFragment.OnFragmentActivatedListener;
import net.osmand.plus.plugins.osmedit.dialogs.OpeningHoursDaysDialogFragment;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.util.OpeningHoursParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BasicEditPoiFragment extends BaseFullScreenFragment implements OnFragmentActivatedListener {

	private static final String OPENING_HOURS = "opening_hours";
	private OpeningHoursAdapter openingHoursAdapter;

	private boolean basicTagsInitialized;
	private EditPoiContentAdapter contentAdapter;

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		updateNightMode();
		View view = inflate(R.layout.fragment_edit_poi_advanced_new, container, false);

		RecyclerView recyclerView = view.findViewById(R.id.content_recycler_view);
		InputFilter[] lengthLimit = {
				new InputFilter.LengthFilter(AMENITY_TEXT_LENGTH)
		};

		int iconColor = ColorUtilities.getSecondaryTextColor(app, nightMode);
		Drawable clockDrawable = getPaintedIcon(R.drawable.ic_action_time, iconColor);
		Drawable deleteDrawable = getPaintedIcon(R.drawable.ic_action_remove_dark, iconColor);
		if (savedInstanceState != null && savedInstanceState.containsKey(OPENING_HOURS)) {
			OpeningHoursParser.OpeningHours openingHours = AndroidUtils.getSerializable(savedInstanceState, OPENING_HOURS, OpeningHoursParser.OpeningHours.class);
			openingHoursAdapter = new OpeningHoursAdapter(app, openingHours,
					getData(), clockDrawable, deleteDrawable);
			openingHoursAdapter.updateHoursData();
		} else {
			openingHoursAdapter = new OpeningHoursAdapter(app, new OpeningHoursParser.OpeningHours(),
					getData(), clockDrawable, deleteDrawable);
		}

		EditPoiContentAdapter.EditPoiListener editPoiListener = new EditPoiContentAdapter.EditPoiListener() {
			@Override
			public void onAddNewItem(int position, int buttonType) {
				if (buttonType == TYPE_ADD_OPENING_HOURS) {
					OpeningHoursParser.BasicOpeningHourRule rule = new OpeningHoursParser.BasicOpeningHourRule();
					rule.setStartTime(9 * 60);
					rule.setEndTime(18 * 60);
					if (openingHoursAdapter.openingHours.getRules().isEmpty()) {
						rule.setDays(new boolean[]{true, true, true, true, true, false, false});
					}
					FragmentManager fragmentManager = getChildFragmentManager();
					OpeningHoursDaysDialogFragment.showInstance(fragmentManager, rule, -1);
				}
			}

			@Override
			public void onDeleteItem(int position) {

			}

			@Override
			public InputFilter[] getLengthLimit() {
				return lengthLimit;
			}

			@Override
			public FragmentManager getChildFragmentManager() {
				return BasicEditPoiFragment.this.getChildFragmentManager();
			}

			@Override
			public boolean isFragmentResumed() {
				return BasicEditPoiFragment.this.isResumed();
			}

			@Override
			public boolean isBasicTagsInitialized() {
				return BasicEditPoiFragment.this.basicTagsInitialized;
			}
		};

		contentAdapter = new EditPoiContentAdapter(requireActivity(), getContentList(),
				null, null, openingHoursAdapter, nightMode, getEditPoiFragment(), editPoiListener);
		recyclerView.setLayoutManager(new LinearLayoutManager(app));
		recyclerView.setAdapter(contentAdapter);
		onFragmentActivated();

		return view;
	}

	@Nullable
	private EditPoiDialogFragment getEditPoiFragment() {
		return (EditPoiDialogFragment) getParentFragment();
	}

	private void updateViews() {
		if (contentAdapter != null) {
			contentAdapter.setItems(getContentList());
		}
	}

	public record OpenHoursItem(int position, long id) {
	}

	private List<Object> getContentList() {
		List<Object> list = new ArrayList<>();
		EditPoiData data = getData();
		if (data == null) {
			return list;
		}

		list.add(TYPE_BASIC_INFO);
		for (int i = 0; i < openingHoursAdapter.openingHours.getRules().size(); i++) {
			list.add(new OpenHoursItem(i, System.currentTimeMillis()));
		}
		list.add(TYPE_ADD_OPENING_HOURS);

		return list;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putSerializable(OPENING_HOURS, openingHoursAdapter.openingHours);
		super.onSaveInstanceState(outState);
	}

	public void setBasicOpeningHoursRule(OpeningHoursParser.BasicOpeningHourRule item, int position) {
		if (item.getStartTime() == 0 && item.getEndTime() == 0 && item.isOpenedEveryDay()) {
			item.setEndTime(24 * 60);
		}
		openingHoursAdapter.setOpeningHoursRule(item, position);
	}

	public void removeUnsavedOpeningHours() {
		EditPoiData data = getData();
		if (data != null) {
			OpeningHoursParser.OpeningHours openingHours = OpeningHoursParser.parseOpenedHoursHandleErrors(data.getTagValues()
					.get(OSMSettings.OSMTagKey.OPENING_HOURS.getValue()));
			if (openingHours == null) {
				openingHours = new OpeningHoursParser.OpeningHours();
			}
			openingHoursAdapter.replaceOpeningHours(openingHours);
			updateViews();
			openingHoursAdapter.updateHoursData();
		}
		contentAdapter.notifyDataSetChanged();
	}

	@Nullable
	private EditPoiData getData() {
		EditPoiDialogFragment fragment = getEditPoiFragment();
		if (fragment == null) {
			return null;
		}
		return fragment.getEditPoiData();
	}

	@Override
	public void onFragmentActivated() {
		EditPoiData data = getData();
		if (data == null) {
			return;
		}
		basicTagsInitialized = false;

		Map<String, String> tagValues = data.getTagValues();
		OpeningHoursParser.OpeningHours openingHours = OpeningHoursParser.parseOpenedHoursHandleErrors(tagValues.get(OSMSettings.OSMTagKey.OPENING_HOURS.getValue()));
		if (openingHours == null) {
			openingHours = new OpeningHoursParser.OpeningHours();
		}
		openingHoursAdapter.replaceOpeningHours(openingHours);
		updateViews();
		openingHoursAdapter.updateHoursData();

		basicTagsInitialized = true;
	}

	public class OpeningHoursAdapter {

		private final OsmandApplication app;
		private OpeningHoursParser.OpeningHours openingHours;

		private final EditPoiData data;
		private final Drawable clockDrawable;
		private final Drawable deleteDrawable;

		public OpeningHoursAdapter(@NonNull OsmandApplication app,
		                           @NonNull OpeningHoursParser.OpeningHours openingHours,
		                           @NonNull EditPoiData data,
		                           @NonNull Drawable clockDrawable,
		                           @NonNull Drawable deleteDrawable) {
			this.app = app;
			this.openingHours = openingHours;
			this.data = data;
			this.clockDrawable = clockDrawable;
			this.deleteDrawable = deleteDrawable;
		}

		public Drawable getClockDrawable() {
			return clockDrawable;
		}

		public Drawable getDeleteDrawable() {
			return deleteDrawable;
		}

		public OpeningHoursParser.OpeningHours getOpeningHours() {
			return openingHours;
		}

		public void setOpeningHoursRule(OpeningHoursParser.BasicOpeningHourRule rule, int position) {
			if (position == -1) {
				openingHours.addRule(rule);
			} else {
				openingHours.getRules().set(position, rule);
			}
			updateViews();
			updateHoursData();
			contentAdapter.notifyDataSetChanged();
		}

		public void replaceOpeningHours(OpeningHoursParser.OpeningHours openingHours) {
			this.openingHours = openingHours;
		}

		public void updateHoursData() {
			if (!data.isInEdit()) {
				LocaleHelper helper = app.getLocaleHelper();
				helper.updateTimeFormatting(false, Locale.getDefault());
				String openingHoursString = openingHours.toString();
				helper.updateTimeFormatting();

				if (!TextUtils.isEmpty(openingHoursString)) {
					if (openingHours.getOriginal() == null ||
							!OpeningHoursParser.parseOpenedHoursHandleErrors(openingHours.getOriginal()).toString().equals(openingHoursString)) {
						data.putTag(OSMSettings.OSMTagKey.OPENING_HOURS.getValue(), openingHoursString);
					}
				} else if (basicTagsInitialized && isResumed()) {
					data.removeTag(OSMSettings.OSMTagKey.OPENING_HOURS.getValue());
				}
			}
		}
	}
}
