package net.osmand.plus.importfiles.ui;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.BaseOsmAndDialogFragment;
import net.osmand.plus.base.OsmandBaseExpandableListAdapter;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.importfiles.ui.ExitImportBottomSheet.OnExitConfirmedListener;
import net.osmand.plus.track.helpers.DisplayPointsGroupsHelper;
import net.osmand.plus.track.helpers.DisplayPointsGroupsHelper.DisplayGroupsHolder;
import net.osmand.plus.track.helpers.GpxSelectionHelper.GpxDisplayGroup;
import net.osmand.plus.track.helpers.GpxSelectionHelper.GpxDisplayItem;
import net.osmand.plus.track.helpers.GpxSelectionHelper.GpxDisplayItemType;
import net.osmand.plus.track.helpers.TrackDisplayHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.utils.UiUtilities.DialogButtonType;
import net.osmand.plus.utils.UiUtilities.UpdateLocationViewCache;
import net.osmand.plus.views.PointImageDrawable;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SelectPointsFragment extends BaseOsmAndDialogFragment implements OnExitConfirmedListener {

	public static final String TAG = ImportTracksFragment.class.getSimpleName();

	private OsmandApplication app;

	private TrackItem trackItem;

	private final List<WptPt> points = new ArrayList<>();
	private final Set<WptPt> selectedPoints = new HashSet<>();

	private View applyButton;
	private View selectAllButton;
	private TextView toolbarTitle;

	private boolean nightMode;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = getMyApplication();
		nightMode = isNightMode(true);
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		return new Dialog(requireContext(), getTheme()) {
			@Override
			public void onBackPressed() {
				showSkipSelectionDialog();
			}
		};
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		LayoutInflater themedInflater = UiUtilities.getInflater(app, nightMode);
		View view = themedInflater.inflate(R.layout.select_track_points_fragment, container, false);

		setupToolbar(view);
		setupButtons(view);
		updateToolbar();
		updateButtonsState();


		return view;
	}

	protected void setupToolbar(@NonNull View view) {
		View appbar = view.findViewById(R.id.appbar);
		ViewCompat.setElevation(appbar, 5.0f);

		toolbarTitle = appbar.findViewById(R.id.toolbar_title);

		ImageView closeButton = appbar.findViewById(R.id.close_button);
		closeButton.setOnClickListener(v -> showSkipSelectionDialog());
		closeButton.setImageDrawable(getIcon(AndroidUtils.getNavigationIconResId(view.getContext())));
	}

	protected void updateToolbar() {
		String selected = getString(R.string.shared_string_selected);
		String count = getString(R.string.ltr_or_rtl_combine_via_slash, String.valueOf(selectedPoints.size()), String.valueOf(points.size()));
		toolbarTitle.setText(getString(R.string.ltr_or_rtl_combine_via_colon, selected, count));
	}

	private void setupButtons(@NonNull View view) {
		View buttonsContainer = view.findViewById(R.id.control_buttons);
		View container = buttonsContainer.findViewById(R.id.buttons_container);
		container.setBackgroundColor(ColorUtilities.getListBgColor(app, nightMode));

		applyButton = container.findViewById(R.id.right_bottom_button);
//		applyButton.setOnClickListener(v -> importTracks());

		selectAllButton = container.findViewById(R.id.dismiss_button);
		selectAllButton.setOnClickListener(v -> {
			if (selectedPoints.containsAll(points)) {
				selectedPoints.clear();
			} else {
				selectedPoints.addAll(points);
			}
			updateButtonsState();
			adapter.notifyDataSetChanged();
		});
		AndroidUiHelper.updateVisibility(applyButton, true);
		AndroidUiHelper.updateVisibility(view.findViewById(R.id.buttons_divider), true);

		TextView textView = selectAllButton.findViewById(R.id.button_text);
		FrameLayout.LayoutParams params = (LayoutParams) textView.getLayoutParams();
		params.gravity = Gravity.START | Gravity.CENTER_VERTICAL;
		textView.setLayoutParams(params);
	}

	protected void updateButtonsState() {
		boolean allSelected = selectedPoints.containsAll(points);
		String selectAllText = getString(allSelected ? R.string.shared_string_deselect_all : R.string.shared_string_select_all);
		UiUtilities.setupDialogButton(nightMode, selectAllButton, DialogButtonType.SECONDARY, selectAllText, R.drawable.ic_action_deselect_all);
		UiUtilities.setupDialogButton(nightMode, applyButton, DialogButtonType.PRIMARY, getString(R.string.shared_string_apply));

		TextView textView = selectAllButton.findViewById(R.id.button_text);
		textView.setCompoundDrawablePadding(AndroidUtils.dpToPx(app, 12));
	}

	private void showSkipSelectionDialog() {
		FragmentActivity activity = getActivity();
		if (activity != null) {
			SkipPointsSelectionBottomSheet.showInstance(activity.getSupportFragmentManager(), this, true);
		}
	}

	@Override
	public void onExitConfirmed() {
		dismiss();
	}

	public static void showInstance(@NonNull FragmentManager manager, @NonNull TrackItem trackItem, @NonNull List<WptPt> points) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			SelectPointsFragment fragment = new SelectPointsFragment();
			fragment.trackItem = trackItem;
			fragment.points.addAll(points);
			fragment.setRetainInstance(true);
			fragment.show(manager, TAG);
		}
	}
}