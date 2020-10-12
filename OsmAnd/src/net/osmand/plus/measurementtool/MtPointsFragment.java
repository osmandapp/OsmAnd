package net.osmand.plus.measurementtool;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.measurementtool.adapter.MeasurementToolAdapter;
import net.osmand.plus.views.controls.ReorderItemTouchHelperCallback;

public class MtPointsFragment extends Fragment
		implements MeasurementToolFragment.OnUpdateAdditionalInfoListener {

	private boolean nightMode;
	private MeasurementToolAdapter adapter;
	private MeasurementEditingContext editingCtx;
	private RecyclerView pointsRv;

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater,
	                         @Nullable ViewGroup container,
	                         @Nullable Bundle savedInstanceState) {

		final MapActivity mapActivity = (MapActivity) getActivity();
		final MeasurementToolFragment mtf = (MeasurementToolFragment) getParentFragment();
		if (mapActivity == null || mtf == null) {
			return null;
		}
		nightMode = mapActivity.getMyApplication().getDaynightHelper().isNightModeForMapControls();
		View view = UiUtilities.getInflater(getContext(), nightMode)
				.inflate(R.layout.fragment_measurement_tool_points_list, container, false);

		editingCtx = mtf.getEditingCtx();
		final GpxData gpxData = editingCtx.getGpxData();
		adapter = new MeasurementToolAdapter(mapActivity, editingCtx.getPoints(),
				gpxData != null ? gpxData.getActionType() : null);
		pointsRv = view.findViewById(R.id.measure_points_recycler_view);
		ItemTouchHelper touchHelper = new ItemTouchHelper(new ReorderItemTouchHelperCallback(adapter));
		touchHelper.attachToRecyclerView(pointsRv);
		adapter.setAdapterListener(mtf.createMeasurementAdapterListener(touchHelper));
		pointsRv.setLayoutManager(new LinearLayoutManager(getContext()));
		pointsRv.setAdapter(adapter);

		return view;
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		adapter.setAdapterListener(null);
	}

	@Override
	public void onUpdateAdditionalInfo() {
		adapter.notifyDataSetChanged();
	}
}
