package net.osmand.plus.backup.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.backup.trash.TrashScreenAdapter;
import net.osmand.plus.backup.trash.controller.TrashScreenController;
import net.osmand.plus.backup.trash.TrashUtils;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;

public class CloudTrashFragment extends BaseOsmAndFragment {

	public static final String TAG = CloudTrashFragment.class.getSimpleName();

	private TrashScreenAdapter adapter;
	private TrashScreenController controller;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		TrashUtils trashUtils = new TrashUtils(app);
		controller = new TrashScreenController(app, trashUtils);
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		updateNightMode();
		View view = themedInflater.inflate(R.layout.fragment_osmand_cloud_trash, container, false);
		AndroidUtils.addStatusBarPadding21v(requireMyActivity(), view);

		adapter = new TrashScreenAdapter(app, controller, isUsedOnMap());
		RecyclerView recyclerView = view.findViewById(R.id.recycler_view);
		recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
		recyclerView.setAdapter(adapter);
		recyclerView.setItemAnimator(null);
		recyclerView.setLayoutAnimation(null);
		updateViewContent();

		setupToolbar(view);
		return view;
	}

	private void setupToolbar(@NonNull View view) {
		Toolbar toolbar = view.findViewById(R.id.toolbar);
		ViewCompat.setElevation(view.findViewById(R.id.appbar), 5.0f);

		TextView toolbarTitle = toolbar.findViewById(R.id.toolbar_title);
		toolbarTitle.setText(R.string.shared_string_trash);

		ImageView closeButton = toolbar.findViewById(R.id.close_button);
		closeButton.setImageDrawable(getIcon(AndroidUtils.getNavigationIconResId(view.getContext())));
		closeButton.setOnClickListener(v -> {
			FragmentActivity activity = getActivity();
			if (activity != null) {
				activity.onBackPressed();
			}
		});
	}

	private void updateViewContent() {
		adapter.setScreenItems(controller.populateScreenItems());
	}

	@Override
	public void onResume() {
		super.onResume();
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.disableDrawer();
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.enableDrawer();
		}
	}

	@Nullable
	private MapActivity getMapActivity() {
		return (MapActivity) getActivity();
	}

	@Override
	public int getStatusBarColorId() {
		return ColorUtilities.getStatusBarColorId(nightMode);
	}

	public static void showInstance(@NonNull FragmentManager manager) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			CloudTrashFragment fragment = new CloudTrashFragment();
			manager.beginTransaction()
					.replace(R.id.fragmentContainer, fragment, TAG)
					.addToBackStack(TAG)
					.commitAllowingStateLoss();
		}
	}
}
