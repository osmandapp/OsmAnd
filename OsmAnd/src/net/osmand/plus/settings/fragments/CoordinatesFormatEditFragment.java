package net.osmand.plus.settings.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.R;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.coordinates.CoordinateFormat;
import net.osmand.plus.settings.coordinates.CoordinateFormatIds;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.controls.ReorderItemTouchHelperCallback;
import net.osmand.plus.widgets.dialogbutton.DialogButton;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class CoordinatesFormatEditFragment extends BaseCoordinateFormatFragment {

	public static final String TAG = CoordinatesFormatEditFragment.class.getSimpleName();

	private static final String STATE_EDIT_IDS = "edit_ids";

	private final List<String> editableIds = new ArrayList<>();

	private RecyclerView recyclerView;
	private View bottomButtonsContainer;
	private DialogButton applyButton;
	private ItemTouchHelper touchHelper;
	private EditFormatsAdapter editAdapter;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		ArrayList<String> savedIds = savedInstanceState != null
				? savedInstanceState.getStringArrayList(STATE_EDIT_IDS) : null;
		editableIds.addAll(Objects.requireNonNullElseGet(savedIds, () -> formatPreferences.getPreferredIds(appMode)));

		getParentFragmentManager().setFragmentResultListener(AddCoordinateFormatFragment.REQUEST_ADD_TO_EDIT, this,
				(requestKey, result) -> {
					String id = result.getString(AddCoordinateFormatFragment.REQUEST_ADD_TO_EDIT);
					if (!Algorithms.isEmpty(id)) {
						addFormatToEditDraft(id);
					}
				});

		requireMyActivity().getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
			@Override
			public void handleOnBackPressed() {
				closeScreen();
			}
		});
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
	                         @Nullable Bundle savedInstanceState) {
		updateNightMode();
		View view = LayoutInflater.from(getMaterialThemedContext())
				.inflate(R.layout.coordinate_format_edit_fragment, container, false);
		recyclerView = view.findViewById(R.id.recycler_view);
		bottomButtonsContainer = view.findViewById(R.id.bottom_buttons_container);
		applyButton = view.findViewById(R.id.apply_button);
		setupToolbar(view);
		renderEditScreen();
		return view;
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		outState.putStringArrayList(STATE_EDIT_IDS, new ArrayList<>(editableIds));
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onApplyInsets(@NonNull WindowInsetsCompat insets) {
		super.onApplyInsets(insets);
		int bottomInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
		if (bottomButtonsContainer != null) {
			bottomButtonsContainer.setPadding(0, 0, 0, bottomInset);
		}
		if (recyclerView != null) {
			recyclerView.setPadding(0, 0, 0, dp(88) + bottomInset);
		}
	}

	private void setupToolbar(@NonNull View view) {
		TextView title = view.findViewById(R.id.toolbar_title);
		title.setText(R.string.coordinates_format);
		ImageButton closeButton = view.findViewById(R.id.close_button);
		closeButton.setImageDrawable(getIcon(R.drawable.ic_action_close, ColorUtilities.getDefaultIconColorId(nightMode)));
		closeButton.setOnClickListener(v -> closeScreen());
		ImageButton addButton = view.findViewById(R.id.action_add);
		addButton.setOnClickListener(v -> AddCoordinateFormatFragment.show(
				requireMyActivity(), appMode, true, false, new ArrayList<>(editableIds)));
	}

	private void renderEditScreen() {
		editAdapter = new EditFormatsAdapter();
		recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
		recyclerView.setAdapter(editAdapter);
		recyclerView.setPadding(0, 0, 0, dp(88) + getBottomSystemInset());

		touchHelper = new ItemTouchHelper(new ReorderItemTouchHelperCallback(editAdapter));
		touchHelper.attachToRecyclerView(recyclerView);

		applyButton.setOnClickListener(v -> applyEditChanges());
		updateApplyButton();
	}

	private int getBottomSystemInset() {
		WindowInsetsCompat insets = getLastRootInsets();
		return insets != null ? insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom : 0;
	}

	private void addFormatToEditDraft(@NonNull String id) {
		String normalizedId = CoordinateFormatIds.normalize(id);
		if (Algorithms.isEmpty(normalizedId) || editableIds.contains(normalizedId)) {
			return;
		}
		editableIds.add(normalizedId);
		if (editAdapter != null) {
			editAdapter.notifyItemInserted(editableIds.size() - 1);
		}
		updateApplyButton();
	}

	private void applyEditChanges() {
		if (!isEditChanged()) {
			return;
		}
		List<String> previousIds = formatPreferences.getPreferredIds(appMode);
		formatPreferences.setPreferredIds(appMode, editableIds);
		for (String id : editableIds) {
			if (!previousIds.contains(id)) {
				formatPreferences.addRecentId(id);
			}
		}
		dismiss();
	}

	private boolean isEditChanged() {
		return !editableIds.equals(formatPreferences.getPreferredIds(appMode));
	}

	private void updateApplyButton() {
		applyButton.setEnabled(isEditChanged());
	}

	private void closeScreen() {
		if (isEditChanged()) {
			new AlertDialog.Builder(UiUtilities.getThemedContext(requireContext(), nightMode))
					.setTitle(R.string.coordinate_format_cancel_changes_title)
					.setMessage(R.string.coordinate_format_cancel_changes_message)
					.setPositiveButton(R.string.coordinate_format_discard_changes, (dialog, which) -> dismiss())
					.setNegativeButton(R.string.shared_string_cancel, null)
					.show();
		} else {
			dismiss();
		}
	}

	private void dismiss() {
		FragmentActivity activity = getActivity();
		if (activity == null) {
			return;
		}
		FragmentManager fragmentManager = activity.getSupportFragmentManager();
		if (!fragmentManager.isStateSaved()) {
			fragmentManager.popBackStack();
		}
	}

	public static void show(@NonNull FragmentActivity activity, @NonNull ApplicationMode appMode) {
		CoordinatesFormatEditFragment fragment = new CoordinatesFormatEditFragment();
		Bundle args = new Bundle();
		args.putString(APP_MODE_KEY, appMode.getStringKey());
		fragment.setArguments(args);
		activity.getSupportFragmentManager().beginTransaction()
				.replace(R.id.fragmentContainer, fragment, TAG)
				.addToBackStack(TAG)
				.commitAllowingStateLoss();
	}

	private class EditFormatsAdapter extends RecyclerView.Adapter<EditFormatsAdapter.FormatViewHolder>
			implements ReorderItemTouchHelperCallback.OnItemMoveCallback {

		@NonNull
		@Override
		public FormatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			View view = inflate(R.layout.coordinate_format_edit_item, parent, false);
			return new FormatViewHolder(view);
		}

		@Override
		public void onBindViewHolder(@NonNull FormatViewHolder holder, int position) {
			CoordinateFormat format = resolveFormats(Collections.singletonList(editableIds.get(position))).get(0);
			holder.title.setText(format.getTitle());
			String summary = getFormatSummary(format);
			if (position == 0) {
				summary = summary + " • " + getString(R.string.coordinate_format_primary);
			}
			holder.summary.setText(summary);
			holder.divider.setVisibility(position < getItemCount() - 1 ? View.VISIBLE : View.GONE);
			holder.removeButton.setEnabled(editableIds.size() > 1);
			holder.removeButton.setAlpha(editableIds.size() > 1 ? 1.0f : 0.35f);
			holder.removeButton.setBackground(null);
			holder.removeButton.setOnClickListener(v -> removeItem(holder.getBindingAdapterPosition()));
			holder.dragHandle.setOnTouchListener((v, event) -> {
				if (event.getActionMasked() == MotionEvent.ACTION_DOWN && touchHelper != null) {
					touchHelper.startDrag(holder);
				}
				return false;
			});
		}

		@Override
		public int getItemCount() {
			return editableIds.size();
		}

		@Override
		public boolean onItemMove(int from, int to) {
			if (from < 0 || to < 0 || from >= editableIds.size() || to >= editableIds.size()) {
				return false;
			}
			Collections.swap(editableIds, from, to);
			notifyItemMoved(from, to);
			updateApplyButton();
			return true;
		}

		@Override
		public void onItemDismiss(@NonNull RecyclerView.ViewHolder holder) {
			updateApplyButton();
		}

		private void removeItem(int position) {
			if (position == RecyclerView.NO_POSITION) {
				return;
			}
			if (editableIds.size() <= 1) {
				app.showShortToastMessage(R.string.coordinate_format_last_item_warning);
				return;
			}
			editableIds.remove(position);
			notifyItemRemoved(position);
			notifyItemRangeChanged(position, editableIds.size() - position);
			updateApplyButton();
		}

		private static class FormatViewHolder extends RecyclerView.ViewHolder {
			private final TextView title;
			private final TextView summary;
			private final View divider;
			private final ImageButton removeButton;
			private final ImageButton dragHandle;

			FormatViewHolder(@NonNull View itemView) {
				super(itemView);
				title = itemView.findViewById(android.R.id.title);
				summary = itemView.findViewById(android.R.id.summary);
				divider = itemView.findViewById(R.id.divider);
				removeButton = itemView.findViewById(R.id.removeButton);
				dragHandle = itemView.findViewById(R.id.dragHandle);
			}
		}
	}
}
