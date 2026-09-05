package net.osmand.plus.settings.fragments.customizable;

import static net.osmand.plus.base.dialog.data.DialogExtra.TITLE;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.R;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.utils.AndroidUtils;

public class CustomizableSingleSelectionDialogFragment extends CustomizableDialogFragment {

	public static final String TAG = CustomizableSingleSelectionDialogFragment.class.getSimpleName();

	private SingleSelectionAdapter adapter;

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		updateNightMode();
		View view = inflate(getLayoutId(), container);
		if (displayData != null) {
			adapter = new SingleSelectionAdapter(app, requireContext(), getController());
			setupToolbar(view);
			setupContent(view);
		} else {
			dismiss();
		}
		return view;
	}

	protected void setupToolbar(@NonNull View view) {
		Toolbar toolbar = view.findViewById(R.id.toolbar);
		ViewCompat.setElevation(view.findViewById(R.id.appbar), 5.0f);

		TextView title = toolbar.findViewById(R.id.toolbar_title);
		title.setText((String) displayData.getExtra(TITLE));

		ImageView closeButton = toolbar.findViewById(R.id.close_button);
		closeButton.setImageDrawable(getIcon(R.drawable.ic_action_close));
		closeButton.setOnClickListener(v -> dismiss());

		ImageView actionButton = toolbar.findViewById(R.id.action_button);
		AndroidUiHelper.updateVisibility(actionButton, false);
	}

	private void setupContent(@NonNull View view) {
		RecyclerView recyclerView = view.findViewById(R.id.recycler_view);
		recyclerView.setLayoutManager(new LinearLayoutManager(app));
		recyclerView.setAdapter(adapter);
	}

	public void askUpdateContent() {
		View view = getView();
		if (view != null) {
			updateContent(view);
		}
	}

	@Override
	protected void updateContent(@NonNull View view) {
		adapter.updateDisplayData();
	}

	@LayoutRes
	protected int getLayoutId() {
		return R.layout.fragment_customizable_single_selection;
	}

	public static boolean showInstance(@NonNull FragmentManager fragmentManager,
	                                   @NonNull String processId) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			CustomizableSingleSelectionDialogFragment fragment =
					new CustomizableSingleSelectionDialogFragment();
			fragment.setProcessId(processId);
			fragment.show(fragmentManager, TAG);
			return true;
		}
		return false;
	}
}
