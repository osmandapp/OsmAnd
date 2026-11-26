package net.osmand.plus.measurementtool;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.measurementtool.MeasurementToolFragment.OnUpdateInfoListener;
import net.osmand.plus.measurementtool.adapter.MeasurementToolAdapter;
import net.osmand.plus.routepreparationmenu.cards.MapBaseCard;
import net.osmand.plus.views.controls.ReorderItemTouchHelperCallback;

public class PointsCard extends MapBaseCard implements OnUpdateInfoListener {

	private MeasurementToolAdapter adapter;
	private final MeasurementToolFragment fragment;

	public PointsCard(@NonNull MapActivity mapActivity, MeasurementToolFragment fragment) {
		super(mapActivity);
		this.fragment = fragment;
	}

	@Override
	public void onUpdateInfo() {
		if (adapter != null) {
			adapter.notifyDataSetChanged();
		}
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.measurement_tool_points_card;
	}

	@Override
	protected void updateContent() {
		MeasurementEditingContext editingCtx = fragment.getEditingCtx();
		adapter = new MeasurementToolAdapter(mapActivity, editingCtx.getPoints());
		RecyclerView pointsRv = view.findViewById(R.id.measure_points_recycler_view);
		ItemTouchHelper touchHelper = new ItemTouchHelper(new ReorderItemTouchHelperCallback(adapter));
		touchHelper.attachToRecyclerView(pointsRv);
		adapter.setAdapterListener(fragment.createMeasurementAdapterListener(touchHelper));
		pointsRv.setLayoutManager(new LinearLayoutManager(app));
		pointsRv.setAdapter(adapter);
	}
}
