package net.osmand.plus.settings;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import net.osmand.AndroidUtils;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BaseOsmAndFragment;
import net.osmand.plus.profiles.ProfilesAdapter;
import net.osmand.plus.profiles.ProfilesItemTouchHelperCallback;

import java.util.ArrayList;
import java.util.List;

public class EditProfilesFragment extends BaseOsmAndFragment {

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		MapActivity mapActivity = (MapActivity) getActivity();
		View mainView = inflater.inflate(R.layout.edit_profiles_list_fragment, container, false);
		AndroidUtils.addStatusBarPadding21v(getContext(), mainView);

		ImageButton closeButton = mainView.findViewById(R.id.close_button);
		closeButton.setImageResource(R.drawable.ic_action_remove_dark);
		closeButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				FragmentActivity fragmentActivity = getActivity();
				if (fragmentActivity != null) {
					fragmentActivity.onBackPressed();
				}
			}
		});

		TextView toolbarTitle = mainView.findViewById(R.id.toolbar_title);
		toolbarTitle.setText(R.string.edit_profiles);

		RecyclerView recyclerView = mainView.findViewById(R.id.profiles_list);
		recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

		final List<ApplicationMode> applicationModes = new ArrayList<>(ApplicationMode.allPossibleValues());
		final ProfilesAdapter adapter = new ProfilesAdapter(mapActivity, applicationModes);
		final ItemTouchHelper touchHelper = new ItemTouchHelper(new ProfilesItemTouchHelperCallback(adapter));

		touchHelper.attachToRecyclerView(recyclerView);
		adapter.setAdapterListener(new ProfilesAdapter.ProfilesAdapterListener() {

			private int fromPosition;
			private int toPosition;

			@Override
			public void onItemClick(View view) {

			}

			@Override
			public void onDragStarted(RecyclerView.ViewHolder holder) {
				fromPosition = holder.getAdapterPosition();
				touchHelper.startDrag(holder);
			}

			@Override
			public void onDragOrSwipeEnded(RecyclerView.ViewHolder holder) {
				toPosition = holder.getAdapterPosition();
				if (toPosition >= 0 && fromPosition >= 0 && toPosition != fromPosition) {
					adapter.notifyDataSetChanged();
				}
			}
		});

		recyclerView.setAdapter(adapter);

		View cancelButton = mainView.findViewById(R.id.cancel_button);
		UiUtilities.setupDialogButton(false, cancelButton, UiUtilities.DialogButtonType.SECONDARY, R.string.shared_string_cancel);
		cancelButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				FragmentActivity fragmentActivity = getActivity();
				if (fragmentActivity != null) {
					fragmentActivity.onBackPressed();
				}
			}
		});

		View applyButton = mainView.findViewById(R.id.apply_button);
		UiUtilities.setupDialogButton(false, applyButton, UiUtilities.DialogButtonType.PRIMARY, R.string.shared_string_apply);
		applyButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				MapActivity mapActivity = (MapActivity) getActivity();
				if (mapActivity != null) {
					OsmandSettings settings = mapActivity.getMyApplication().getSettings();
					for (int i = 0; i < applicationModes.size(); i++) {
						ApplicationMode mode = applicationModes.get(i);
						mode.setOrder(i);
					}
					ApplicationMode.reorderAppModes();
					ApplicationMode.saveAppModesToSettings(settings);
					mapActivity.onBackPressed();
				}
			}
		});

		return mainView;
	}
}