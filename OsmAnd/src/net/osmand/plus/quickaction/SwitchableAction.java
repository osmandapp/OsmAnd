package net.osmand.plus.quickaction;

import static net.osmand.plus.utils.AndroidUtils.isLayoutRtl;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.core.util.Pair;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.quickaction.QuickActionListFragment.OnStartDragListener;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.views.controls.ReorderItemTouchHelperCallback;
import net.osmand.plus.views.controls.ReorderItemTouchHelperCallback.OnItemMoveCallback;
import net.osmand.plus.views.controls.maphudbuttons.QuickActionButton;
import net.osmand.plus.views.layers.MapQuickActionLayer;
import net.osmand.plus.views.mapwidgets.configure.buttons.QuickActionButtonState;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class SwitchableAction<T> extends QuickAction {

	public static final String KEY_ID = "id";

	protected static final String KEY_DIALOG = "dialog";

	private transient EditText title;

	private transient Adapter adapter;
	private transient ItemTouchHelper touchHelper;

	protected SwitchableAction(QuickActionType type) {
		super(type);
	}

	public SwitchableAction(QuickAction quickAction) {
		super(quickAction);
	}

	@Override
	public void setAutoGeneratedTitle(EditText title) {
		this.title = title;
	}

	@Override
	public void drawUI(@NonNull ViewGroup parent, @NonNull MapActivity mapActivity) {
		View view = LayoutInflater.from(parent.getContext())
				.inflate(R.layout.quick_action_switchable_action, parent, false);

		SwitchCompat showDialog = view.findViewById(R.id.saveButton);
		if (!getParams().isEmpty()) {
			showDialog.setChecked(Boolean.valueOf(getParams().get(KEY_DIALOG)));
		}
		view.findViewById(R.id.saveButtonContainer).setOnClickListener(v -> {
			boolean selected = showDialog.isChecked();
			showDialog.setChecked(!selected);
		});

		RecyclerView list = view.findViewById(R.id.list);
		adapter = new Adapter(mapActivity, viewHolder -> touchHelper.startDrag(viewHolder));

		ReorderItemTouchHelperCallback touchHelperCallback = new ReorderItemTouchHelperCallback(adapter);
		touchHelper = new ItemTouchHelper(touchHelperCallback);
		touchHelper.attachToRecyclerView(list);

		if (!getParams().isEmpty()) {
			adapter.addItems(loadListFromParams());
		}

		list.setAdapter(adapter);

		TextView dscrTitle = view.findViewById(R.id.textDscrTitle);
		TextView dscrHint = view.findViewById(R.id.textDscrHint);
		Button addBtn = view.findViewById(R.id.btnAdd);

		dscrTitle.setText(parent.getContext().getString(getDiscrTitle()) + ":");
		dscrHint.setText(getDiscrHint());
		addBtn.setText(getAddBtnText());
		addBtn.setOnClickListener(getOnAddBtnClickListener(mapActivity, adapter));

		parent.addView(view);
	}

	@Override
	public String getActionText(@NonNull OsmandApplication app) {
		String rtlArrow = "\u25c0", ltrArrow = "\u25b6";
		String arrow = isLayoutRtl(app) ? rtlArrow : ltrArrow;
		String selectedItem = getSelectedItem(app);
		String itemName;
		if (loadListFromParams().size() == 1) {
			String mainItem = getItemIdFromObject(loadListFromParams().get(0));
			boolean selectedMain = Algorithms.stringsEqual(mainItem, selectedItem);
			// RTL: A  <| MAIN (selected), MAIN <| B (selected)
			// LTR: A (selected) |> MAIN , MAIN (selected) |>  B
			itemName = (selectedMain ? "" : arrow) + getTranslatedItemName(app, mainItem) +
					(selectedMain ? arrow : "");
		} else {
			String disabledItem = getDisabledItem(app);
			String nextItem = getNextSelectedItem(app);
			String mainItem = Algorithms.stringsEqual(nextItem, disabledItem) ? selectedItem : nextItem;
			boolean selectedMain = Algorithms.stringsEqual(mainItem, selectedItem);
			// RTL: A  <| MAIN (selected), MAIN <| B (selected)
			// LTR: A (selected) |> MAIN , MAIN (selected) |>  B
			itemName = (selectedMain ? "" : ("\u2026" + arrow)) + getTranslatedItemName(app, mainItem) +
					(selectedMain ? (arrow + "\u2026") : "");
		}
		return itemName;
	}

	@Override
	public boolean fillParams(@NonNull View root, @NonNull MapActivity mapActivity) {
		RecyclerView list = root.findViewById(R.id.list);
		Adapter adapter = (Adapter) list.getAdapter();

		boolean hasParams = adapter.itemsList != null && !adapter.itemsList.isEmpty();

		if (hasParams) saveListToParams(adapter.itemsList);

		return hasParams;
	}

	protected Adapter getAdapter() {
		return adapter;
	}

	public abstract String getItemIdFromObject(T object);

	public abstract List<T> loadListFromParams();

	public abstract void executeWithParams(@NonNull MapActivity activity, String params);

	public abstract String getTranslatedItemName(Context context, String item);

	public abstract String getDisabledItem(OsmandApplication app);

	public abstract String getSelectedItem(OsmandApplication app);

	public abstract String getNextSelectedItem(OsmandApplication app);

	protected void showChooseDialog(@NonNull MapActivity mapActivity) {
		OsmandApplication app = mapActivity.getMyApplication();
		MapQuickActionLayer layer = mapActivity.getMapLayers().getMapQuickActionLayer();

		QuickActionButton button = layer.getSelectedButton();
		QuickActionButtonState buttonState = button != null ? button.getButtonState() : null;
		if (buttonState == null) {
			buttonState = app.getMapButtonsHelper().getButtonStateByAction(this);
		}
		if (buttonState != null) {
			FragmentManager manager = mapActivity.getSupportFragmentManager();
			SelectMapViewQuickActionsBottomSheet.showInstance(manager, buttonState, id);
		}
	}

	public String getNextItemFromSources(@NonNull OsmandApplication app,
										 @NonNull List<Pair<String, String>> sources,
										 @NonNull String defValue) {
		if (!Algorithms.isEmpty(sources)) {
			String currentSource = getSelectedItem(app);
			if (sources.size() > 1) {
				int index = -1;
				for (int idx = 0; idx < sources.size(); idx++) {
					if (Algorithms.stringsEqual(sources.get(idx).first, currentSource)) {
						index = idx;
						break;
					}
				}
				Pair<String, String> nextSource = sources.get(0);
				if (index >= 0 && index + 1 < sources.size()) {
					nextSource = sources.get(index + 1);
				}
				return nextSource.first;
			} else {
				String source = sources.get(0).first;
				return Algorithms.stringsEqual(source, currentSource) ? defValue : source;
			}
		}
		return null;
	}

	protected class Adapter extends RecyclerView.Adapter<Adapter.ItemHolder> implements OnItemMoveCallback {

		private final Context context;
		private final List<T> itemsList = new ArrayList<>();
		private final OnStartDragListener dragListener;

		public Adapter(@NonNull Context context, @NonNull OnStartDragListener dragListener) {
			this.context = context;
			this.dragListener = dragListener;
		}

		@Override
		public Adapter.ItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			LayoutInflater inflater = LayoutInflater.from(parent.getContext());
			return new Adapter.ItemHolder(inflater.inflate(R.layout.quick_action_switchable_item, parent, false));
		}

		@Override
		public void onBindViewHolder(Adapter.ItemHolder holder, int position) {
			T item = itemsList.get(position);

			OsmandApplication app = (OsmandApplication) context.getApplicationContext();

			setIcon(app, item, holder.icon, holder.iconProgressBar);
			holder.title.setText(getItemName(context, item));

			holder.handleView.setOnTouchListener((v, event) -> {
				if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
					dragListener.onStartDrag(holder);
				}
				return false;
			});

			holder.closeBtn.setOnClickListener(v -> {
				String oldTitle = getTitle(itemsList);
				String defaultName = holder.handleView.getContext().getString(getNameRes());

				deleteItem(holder.getAdapterPosition());

				if (oldTitle.equals(title.getText().toString()) || title.getText().toString().equals(defaultName)) {
					String newTitle = getTitle(itemsList);
					title.setText(newTitle);
				}
			});
		}

		@Override
		public int getItemCount() {
			return itemsList.size();
		}

		public List<T> getItemsList() {
			return itemsList;
		}

		public void deleteItem(int position) {
			if (position == -1) {
				return;
			}
			itemsList.remove(position);
			notifyItemRemoved(position);
		}

		public void addItems(List<T> data) {
			if (!itemsList.containsAll(data)) {
				itemsList.addAll(data);
				notifyDataSetChanged();
			}
		}

		public void addItem(T item, Context context) {
			if (!itemsList.contains(item)) {
				String oldTitle = getTitle(itemsList);
				String defaultName = context.getString(getNameRes());

				int oldSize = itemsList.size();
				itemsList.add(item);

				notifyItemRangeInserted(oldSize, itemsList.size() - oldSize);

				if (oldTitle.equals(title.getText().toString()) || title.getText().toString().equals(defaultName)) {
					String newTitle = getTitle(itemsList);
					title.setText(newTitle);
				}
			}
		}

		@Override
		public boolean onItemMove(int selectedPosition, int targetPosition) {
			String oldTitle = getTitle(itemsList);
			String defaultName = context.getString(getNameRes());

			Collections.swap(itemsList, selectedPosition, targetPosition);
			if (selectedPosition - targetPosition < -1) {

				notifyItemMoved(selectedPosition, targetPosition);
				notifyItemMoved(targetPosition - 1, selectedPosition);

			} else if (selectedPosition - targetPosition > 1) {

				notifyItemMoved(selectedPosition, targetPosition);
				notifyItemMoved(targetPosition + 1, selectedPosition);

			} else {

				notifyItemMoved(selectedPosition, targetPosition);
			}

			notifyItemChanged(selectedPosition);
			notifyItemChanged(targetPosition);

			if (oldTitle.equals(title.getText().toString()) || title.getText().toString().equals(defaultName)) {

				String newTitle = getTitle(itemsList);
				title.setText(newTitle);
			}

			return true;
		}

		@Override
		public void onItemDismiss(@NonNull ViewHolder holder) {

		}

		public class ItemHolder extends RecyclerView.ViewHolder {
			public TextView title;
			public ImageView handleView;
			public ImageView closeBtn;
			public ImageView icon;
			public ProgressBar iconProgressBar;

			public ItemHolder(View itemView) {
				super(itemView);

				title = itemView.findViewById(R.id.title);
				handleView = itemView.findViewById(R.id.handle_view);
				closeBtn = itemView.findViewById(R.id.closeImageButton);
				icon = itemView.findViewById(R.id.imageView);
				iconProgressBar = itemView.findViewById(R.id.iconProgressBar);
			}
		}
	}

	protected abstract String getTitle(List<T> filters);

	protected abstract void saveListToParams(List<T> list);

	protected abstract String getItemName(Context context, T item);

	protected void setIcon(@NonNull OsmandApplication app, T item, @NonNull ImageView imageView, @NonNull ProgressBar iconProgressBar) {
		imageView.setImageDrawable(app.getUIUtilities().getPaintedIcon(
				getItemIconRes(app, item), getItemIconColor(app, item)));
	}

	@DrawableRes
	protected int getItemIconRes(Context context, T item) {
		return R.drawable.ic_map;
	}

	@ColorInt
	protected int getItemIconColor(OsmandApplication app, T item) {
		boolean nightMode = !app.getSettings().isLightContent();
		int colorRes = ColorUtilities.getDefaultIconColorId(nightMode);
		return ContextCompat.getColor(app, colorRes);
	}

	@StringRes
	protected abstract int getAddBtnText();

	@StringRes
	protected abstract int getDiscrHint();

	@StringRes
	protected abstract int getDiscrTitle();

	protected abstract String getListKey();

	protected abstract View.OnClickListener getOnAddBtnClickListener(MapActivity activity, Adapter adapter);

	protected void onItemsSelected(Context ctx, List<T> selectedItems) {

	}
}
