package net.osmand.plus.myplaces;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.ListPopupWindow;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import net.osmand.AndroidUtils;
import net.osmand.plus.GPXUtilities;
import net.osmand.plus.GpxSelectionHelper;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayGroup;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayItemType;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayItem;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.TrackActivity;
import net.osmand.plus.base.OsmAndListFragment;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import gnu.trove.list.array.TIntArrayList;

import static net.osmand.plus.myplaces.TrackSegmentFragment.ARG_TO_FILTER_SHORT_TRACKS;

public class SplitSegmentFragment extends OsmAndListFragment{

    private OsmandApplication app;

    private SplitSegmentsAdapter adapter;
    private View headerView;

    private GpxDisplayItemType[] filterTypes = { GpxDisplayItemType.TRACK_SEGMENT };
    private List<String> options = new ArrayList<>();
    private List<Double> distanceSplit = new ArrayList<>();
    private TIntArrayList timeSplit = new TIntArrayList();
    private int selectedSplitInterval;
    private boolean updateEnable;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.app = getMyApplication();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getListView().setBackgroundColor(getResources().getColor(
                getMyApplication().getSettings().isLightContent() ? R.color.ctx_menu_info_view_bg_light
                        : R.color.ctx_menu_info_view_bg_dark));
    }

    @Override
    public ArrayAdapter<?> getAdapter() {
        return adapter;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        View view = getActivity().getLayoutInflater().inflate(R.layout.update_index, container, false);
        view.findViewById(R.id.header_layout).setVisibility(View.GONE);

        ListView listView = (ListView) view.findViewById(android.R.id.list);
        listView.setDivider(null);
        listView.setDividerHeight(0);

        adapter = new SplitSegmentsAdapter(new ArrayList<GpxDisplayItem>());
        headerView = getActivity().getLayoutInflater().inflate(R.layout.gpx_split_segments_header, null, false);

        listView.addHeaderView(headerView);
        listView.addFooterView(getActivity().getLayoutInflater().inflate(R.layout.list_shadow_footer, null, false));
        updateHeader();

        setListAdapter(adapter);
        return view;
    }

    private void updateHeader() {
        final View splitIntervalView = headerView.findViewById(R.id.split_interval_view);

        if (getGpx() != null && !getGpx().showCurrentTrack && adapter.getCount() > 0) {
            prepareSplitIntervalAdapterData();
            setupSplitIntervalView(splitIntervalView);
            updateSplitIntervalView(splitIntervalView);
            splitIntervalView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final ListPopupWindow popup = new ListPopupWindow(getActivity());
                    popup.setAnchorView(splitIntervalView);
                    popup.setContentWidth(AndroidUtils.dpToPx(app, 200f));
                    popup.setModal(true);
                    popup.setDropDownGravity(Gravity.RIGHT | Gravity.TOP);
                    popup.setVerticalOffset(AndroidUtils.dpToPx(app, -48f));
                    popup.setHorizontalOffset(AndroidUtils.dpToPx(app, -6f));
                    popup.setAdapter(new ArrayAdapter<>(getMyActivity(),
                            R.layout.popup_list_text_item, options));
                    popup.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            selectedSplitInterval = position;
                            GpxSelectionHelper.SelectedGpxFile sf = app.getSelectedGpxHelper().selectGpxFile(getGpx(), true, false);
                            final List<GpxDisplayGroup> groups = getDisplayGroups();
                            if (groups.size() > 0) {
                                updateSplit(groups, sf);
                                updateContent();
                            }
                            popup.dismiss();
                            updateSplitIntervalView(splitIntervalView);
                        }
                    });
                    popup.show();
                }
            });
            splitIntervalView.setVisibility(View.VISIBLE);
        } else {
            splitIntervalView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateContent();
        updateEnable = true;
    }

    @Override
    public void onPause() {
        super.onPause();
        updateEnable = false;
    }

    public void updateContent() {
        adapter.clear();
        adapter.setNotifyOnChange(false);
        List<GpxDisplayGroup> originalGroups = getOriginalGroups();
        List<GpxDisplayItem> splitSegments = getSplitSegments();
        adapter.addAll(splitSegments);
        adapter.setNotifyOnChange(true);
        adapter.notifyDataSetChanged();
        updateHeader();
    }

    protected List<GpxDisplayItem> flatten(List<GpxDisplayGroup> groups) {
        ArrayList<GpxDisplayItem> list = new ArrayList<>();
        for(GpxDisplayGroup g : groups) {
            list.addAll(g.getModifiableList());
        }
        return list;
    }

    private List<GpxDisplayGroup> getOriginalGroups() {
        return filterGroups(false);
    }

    private void updateSplit(List<GpxDisplayGroup> groups, GpxSelectionHelper.SelectedGpxFile sf) {
        new SplitTrackAsyncTask(sf, groups).execute((Void) null);
    }

    private void setupSplitIntervalView(View view) {
        final TextView title = (TextView) view.findViewById(R.id.split_interval_title);
        final TextView text = (TextView) view.findViewById(R.id.split_interval_text);
        final ImageView img = (ImageView) view.findViewById(R.id.split_interval_arrow);
        int colorId;
        final List<GpxDisplayGroup> groups = getDisplayGroups();
        if (groups.size() > 0) {
            colorId = app.getSettings().isLightContent() ?
                    R.color.primary_text_light : R.color.primary_text_dark;
        } else {
            colorId = app.getSettings().isLightContent() ?
                    R.color.secondary_text_light : R.color.secondary_text_dark;
        }
        int color = app.getResources().getColor(colorId);
        title.setTextColor(color);
        text.setTextColor(color);
        img.setImageDrawable(app.getIconsCache().getIcon(R.drawable.ic_action_arrow_drop_down, colorId));
    }

    private void updateSplitIntervalView(View view) {
        final TextView text = (TextView) view.findViewById(R.id.split_interval_text);
        if (selectedSplitInterval == 0) {
            text.setText(getString(R.string.shared_string_none));
        } else {
            text.setText(options.get(selectedSplitInterval));
        }
    }

    private GPXUtilities.GPXFile getGpx() {
        return getMyActivity().getGpx();
    }

    public TrackActivity getMyActivity() {
        return (TrackActivity) getActivity();
    }

    private void prepareSplitIntervalAdapterData() {
        final List<GpxDisplayGroup> groups = getDisplayGroups();

        options.add(app.getString(R.string.shared_string_none));
        distanceSplit.add(-1d);
        timeSplit.add(-1);
        addOptionSplit(30, true, groups); // 50 feet, 20 yards, 20
        // m
        addOptionSplit(60, true, groups); // 100 feet, 50 yards,
        // 50 m
        addOptionSplit(150, true, groups); // 200 feet, 100 yards,
        // 100 m
        addOptionSplit(300, true, groups); // 500 feet, 200 yards,
        // 200 m
        addOptionSplit(600, true, groups); // 1000 feet, 500 yards,
        // 500 m
        addOptionSplit(1500, true, groups); // 2000 feet, 1000 yards, 1 km
        addOptionSplit(3000, true, groups); // 1 mi, 2 km
        addOptionSplit(6000, true, groups); // 2 mi, 5 km
        addOptionSplit(15000, true, groups); // 5 mi, 10 km

        addOptionSplit(15, false, groups);
        addOptionSplit(30, false, groups);
        addOptionSplit(60, false, groups);
        addOptionSplit(120, false, groups);
        addOptionSplit(150, false, groups);
        addOptionSplit(300, false, groups);
        addOptionSplit(600, false, groups);
        addOptionSplit(900, false, groups);
        addOptionSplit(1800, false, groups);
        addOptionSplit(3600, false, groups);
    }

    private List<GpxDisplayGroup> getDisplayGroups() {
        return filterGroups(true);
    }

    private void addOptionSplit(int value, boolean distance, List<GpxDisplayGroup> model) {
        if (distance) {
            double dvalue = OsmAndFormatter.calculateRoundedDist(value, app);
            options.add(OsmAndFormatter.getFormattedDistance((float) dvalue, app));
            distanceSplit.add(dvalue);
            timeSplit.add(-1);
            if (Math.abs(model.get(0).getSplitDistance() - dvalue) < 1) {
                selectedSplitInterval = distanceSplit.size() - 1;
            }
        } else {
            if (value < 60) {
                options.add(value + " " + app.getString(R.string.int_seconds));
            } else if (value % 60 == 0) {
                options.add((value / 60) + " " + app.getString(R.string.int_min));
            } else {
                options.add((value / 60f) + " " + app.getString(R.string.int_min));
            }
            distanceSplit.add(-1d);
            timeSplit.add(value);
            if (model.get(0).getSplitTime() == value) {
                selectedSplitInterval = distanceSplit.size() - 1;
            }
        }
    }

    private List<GpxDisplayGroup> filterGroups(boolean useDisplayGroups) {
        List<GpxDisplayGroup> result = getMyActivity().getGpxFile(useDisplayGroups);
        List<GpxDisplayGroup> groups = new ArrayList<>();
        for (GpxDisplayGroup group : result) {
            boolean add = hasFilterType(group.getType());
            if (isArgumentTrue(ARG_TO_FILTER_SHORT_TRACKS)) {
                Iterator<GpxDisplayItem> item = group.getModifiableList().iterator();
                while (item.hasNext()) {
                    GpxDisplayItem it2 = item.next();
                    if (it2.analysis != null && it2.analysis.totalDistance < 100) {
                        item.remove();
                    }
                }
                if (group.getModifiableList().isEmpty()) {
                    add = false;
                }
            }
            if (add) {
                groups.add(group);
            }

        }
        return groups;
    }

    private List<GpxDisplayItem> getSplitSegments() {
        List<GpxDisplayGroup> result = getMyActivity().getGpxFile(true);
        List<GpxDisplayItem> splitSegments = new ArrayList<>();
        for (GpxDisplayGroup group : result) {
            splitSegments.addAll(group.getModifiableList());
        }
        return splitSegments;
    }

    private boolean isArgumentTrue(@NonNull String arg) {
        return getArguments() != null && getArguments().getBoolean(arg);
    }

    protected boolean hasFilterType(GpxDisplayItemType filterType) {
        for (GpxDisplayItemType type : filterTypes) {
            if (type == filterType) {
                return true;
            }
        }
        return false;
    }

    private class SplitSegmentsAdapter extends ArrayAdapter<GpxDisplayItem> {

        public SplitSegmentsAdapter(List<GpxDisplayItem> items) {
            super(getActivity(), 0, items);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            GpxDisplayItem currentGpxDisplayItem = getItem(position);
            if (convertView == null) {
                convertView = getMyActivity().getLayoutInflater().inflate(R.layout.gpx_split_segment_fragment, parent, false);
            }
            return convertView;
        }
    }

    private class SplitTrackAsyncTask extends AsyncTask<Void, Void, Void> {
        @Nullable
        private final GpxSelectionHelper.SelectedGpxFile mSelectedGpxFile;
        @NonNull private final TrackActivity mActivity;

        private final List<GpxDisplayGroup> groups;

        SplitTrackAsyncTask(@Nullable GpxSelectionHelper.SelectedGpxFile selectedGpxFile, List<GpxDisplayGroup> groups) {
            mSelectedGpxFile = selectedGpxFile;
            mActivity = getMyActivity();
            this.groups = groups;
        }

        protected void onPostExecute(Void result) {
            if (mSelectedGpxFile != null) {
                mSelectedGpxFile.setDisplayGroups(getDisplayGroups());
            }
            if (!mActivity.isFinishing()) {
                mActivity.setProgressBarIndeterminateVisibility(false);
            }
        }

        protected void onPreExecute() {
            mActivity.setProgressBarIndeterminateVisibility(true);
        }

        @Override
        protected Void doInBackground(Void... params) {
            for (GpxDisplayGroup model : groups) {
                OsmandApplication application = mActivity.getMyApplication();
                if (selectedSplitInterval == 0) {
                    model.noSplit(application);
                } else if (distanceSplit.get(selectedSplitInterval) > 0) {
                    model.splitByDistance(application, distanceSplit.get(selectedSplitInterval));
                } else if (timeSplit.get(selectedSplitInterval) > 0) {
                    model.splitByTime(application, timeSplit.get(selectedSplitInterval));
                }
            }

            return null;
        }
    }
}
