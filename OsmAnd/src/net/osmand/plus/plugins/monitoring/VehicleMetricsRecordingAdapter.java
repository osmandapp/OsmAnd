package net.osmand.plus.plugins.monitoring;

import static net.osmand.plus.plugins.monitoring.VehicleMetricsRecordingFragment.*;

import android.annotation.SuppressLint;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.DimenRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.shared.obd.OBDCommand;

import java.util.ArrayList;
import java.util.List;

public class VehicleMetricsRecordingAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

	public static final int DESCRIPTION_TYPE = 0;
	public static final int ITEM_TYPE = 1;
	public static final int DIVIDER_TYPE = 2;
	public static final int CATEGORY_TYPE = 3;

	private final LayoutInflater themedInflater;
	private final boolean nightMode;
	private final MapActivity mapActivity;
	private final OsmandApplication app;

	private final List<Object> items = new ArrayList<>();
	private final VehicleMetricsRecordingListener listener;

	public VehicleMetricsRecordingAdapter(@NonNull MapActivity mapActivity, @NonNull VehicleMetricsRecordingListener listener, boolean nightMode) {
		this.listener = listener;
		this.nightMode = nightMode;
		this.mapActivity = mapActivity;
		this.app = mapActivity.getApp();
		themedInflater = UiUtilities.getInflater(mapActivity, nightMode);
	}

	@SuppressLint("NotifyDataSetChanged")
	public void setItems(@NonNull List<Object> items) {
		this.items.clear();
		this.items.addAll(items);
		notifyDataSetChanged();
	}

	public List<Object> getItems() {
		return items;
	}

	@NonNull
	@Override
	public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		View itemView;
		return switch (viewType) {
			case ITEM_TYPE -> {
				itemView = themedInflater.inflate(R.layout.bottom_sheet_item_with_descr_and_checkbox_and_divider_56dp, parent, false);
				yield new ItemHolder(itemView);
			}
			case CATEGORY_TYPE -> {
				itemView = themedInflater.inflate(R.layout.preference_category_title, parent, false);
				yield new CategoryHolder(itemView);
			}
			case DIVIDER_TYPE -> {
				itemView = themedInflater.inflate(R.layout.divider, parent, false);
				yield new DividerHolder(itemView);
			}
			case DESCRIPTION_TYPE -> {
				itemView = themedInflater.inflate(R.layout.description_article_item, parent, false);
				yield new DescriptionHolder(itemView);
			}
			default -> throw new IllegalArgumentException("Unsupported view type");
		};
	}

	@Override
	public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
		Object item = items.get(position);
		if (holder instanceof ItemHolder viewHolder) {
			VehicleMetricsItem command = (VehicleMetricsItem) item;
			boolean lastItemInCategory = (position + 1 < items.size() && !(items.get(position + 1) instanceof VehicleMetricsItem)) || position == items.size() - 1;
			viewHolder.bindView(mapActivity, command, nightMode, lastItemInCategory);
		} else if (holder instanceof CategoryHolder viewHolder) {
			if (item instanceof VehicleMetricsRecordingCategory category) {
				viewHolder.bindView(category);
			}
		} else if (holder instanceof DescriptionHolder descriptionHolder) {
			descriptionHolder.bindView(nightMode);
		}
	}

	@Override
	public int getItemCount() {
		return items.size();
	}

	@Override
	public int getItemViewType(int position) {
		Object object = items.get(position);
		if (object instanceof VehicleMetricsItem) {
			return ITEM_TYPE;
		} else if (object instanceof VehicleMetricsRecordingCategory) {
			return CATEGORY_TYPE;
		} else if (object instanceof Integer integer) {
			if (integer == DESCRIPTION_TYPE) {
				return DESCRIPTION_TYPE;
			} else if (integer == DIVIDER_TYPE) {
				return DIVIDER_TYPE;
			}
		}
		throw new IllegalArgumentException("Unsupported view type");
	}


	public interface VehicleMetricsRecordingListener {

		void onCommandClick(@NonNull OBDCommand command);

		boolean isCommandSelected(@NonNull OBDCommand command);
	}

	public static int getDimen(@NonNull OsmandApplication app, @DimenRes int id) {
		return app.getResources().getDimensionPixelSize(id);
	}

	class ItemHolder extends RecyclerView.ViewHolder {

		private final View itemView;
		private final TextView titleView;
		private final TextView description;
		private final CheckBox checkBoxView;
		private final ImageView imageView;
		private final View divider;

		public ItemHolder(@NonNull View itemView) {
			super(itemView);
			this.itemView = itemView;
			titleView = itemView.findViewById(R.id.title);
			description = itemView.findViewById(R.id.description);
			checkBoxView = itemView.findViewById(R.id.compound_button);
			imageView = itemView.findViewById(R.id.icon);
			divider = itemView.findViewById(R.id.divider_bottom);
		}

		public void bindView(@NonNull MapActivity mapActivity, @NonNull VehicleMetricsItem commandItem, boolean nightMode, boolean lastItemInCategory) {
			int dp48 = getDimen(mapActivity.getApp(), R.dimen.bottom_sheet_list_item_height);
			itemView.setMinimumHeight(dp48);

			titleView.setText(commandItem.nameId);
			boolean checked = listener.isCommandSelected(commandItem.command);
			checkBoxView.setChecked(checked);
			updateIcon(checked, commandItem.iconId);

			itemView.setOnClickListener(v -> {
				listener.onCommandClick(commandItem.command);
				boolean newState = listener.isCommandSelected(commandItem.command);
				checkBoxView.setChecked(newState);
				updateIcon(newState, commandItem.iconId);
			});

			UiUtilities.setupCompoundButton(nightMode, ColorUtilities.getActiveColor(app, nightMode), checkBoxView);
			AndroidUiHelper.updateVisibility(divider, !lastItemInCategory);
			AndroidUiHelper.updateVisibility(description, false);
		}

		private void updateIcon(boolean checked, @DrawableRes int iconId) {
			int iconColor = checked ? ColorUtilities.getActiveIconColor(app, nightMode) : ColorUtilities.getDefaultIconColor(app, nightMode);
			imageView.setImageDrawable(app.getUIUtilities().getPaintedIcon(iconId, iconColor));
		}
	}

	class CategoryHolder extends RecyclerView.ViewHolder {
		private final View itemView;
		private final TextView textView;
		private final ImageView imageView;

		public CategoryHolder(@NonNull View itemView) {
			super(itemView);
			this.itemView = itemView;
			textView = itemView.findViewById(android.R.id.title);
			imageView = itemView.findViewById(android.R.id.icon);
		}

		public void bindView(@NonNull VehicleMetricsRecordingCategory category) {
			RecyclerView.LayoutParams linearLayout = (RecyclerView.LayoutParams) itemView.getLayoutParams();
			linearLayout.width = LinearLayout.LayoutParams.MATCH_PARENT;
			linearLayout.height = LinearLayout.LayoutParams.WRAP_CONTENT;
			int dp36 = getDimen(app, R.dimen.showAllButtonHeight);
			itemView.setMinimumHeight(dp36);
			textView.setText(category.titleId);

			LinearLayout.LayoutParams currentImageParams = (LinearLayout.LayoutParams) imageView.getLayoutParams();
			int size = app.getResources().getDimensionPixelSize(R.dimen.standard_icon_size);
			LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
			params.gravity = Gravity.CENTER;
			params.setMargins(currentImageParams.leftMargin, currentImageParams.topMargin, AndroidUtils.dpToPx(app, 8), currentImageParams.bottomMargin);
			imageView.setLayoutParams(params);
		}
	}

	static class DividerHolder extends RecyclerView.ViewHolder {
		public DividerHolder(@NonNull View itemView) {
			super(itemView);
		}
	}

	class DescriptionHolder extends RecyclerView.ViewHolder {
		private final TextView textView;

		public DescriptionHolder(@NonNull View itemView) {
			super(itemView);
			textView = itemView.findViewById(R.id.title);
		}

		public void bindView(boolean nightMode) {
			textView.setText(R.string.vehicle_metrics_recording_description);
			textView.setTextColor(ColorUtilities.getPrimaryTextColor(app, nightMode));
		}
	}
}
