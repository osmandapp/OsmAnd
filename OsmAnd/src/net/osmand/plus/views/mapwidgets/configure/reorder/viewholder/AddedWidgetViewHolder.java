package net.osmand.plus.views.mapwidgets.configure.reorder.viewholder;

import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.controls.ReorderItemTouchHelperCallback.UnmovableItem;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.configure.reorder.viewholder.AvailableWidgetViewHolder.AvailableWidgetUiInfo;

public class AddedWidgetViewHolder extends ViewHolder implements UnmovableItem {

	public final ImageButton deleteWidgetButton;
	public final TextView title;
	public final ImageView icon;
	public final ImageView moveIcon;

	private final ItemMovableCallback itemMovableCallback;

	public AddedWidgetViewHolder(@NonNull View itemView, @Nullable ItemMovableCallback callback) {
		super(itemView);
		this.itemMovableCallback = callback;

		deleteWidgetButton = itemView.findViewById(R.id.delete_widget_button);
		title = itemView.findViewById(R.id.title);
		icon = itemView.findViewById(R.id.icon);
		moveIcon = itemView.findViewById(R.id.move_icon);
	}

	@Override
	public boolean isMovingDisabled() {
		int position = getAdapterPosition();
		if (position != RecyclerView.NO_POSITION && itemMovableCallback != null) {
			return !itemMovableCallback.isListItemMovable(position);
		}
		return false;
	}

	public static void updateWidgetIcon(@NonNull ImageView imageView, @NonNull MapWidgetInfo widgetInfo,
	                                    int profileColor, int defaultIconColor, boolean selected, boolean nightMode) {
		boolean shouldPaintIcon = !widgetInfo.isIconPainted();
		int mapIconId = widgetInfo.getMapIconId(nightMode);
		int settingsIconId = widgetInfo.getSettingsIconId(nightMode);

		OsmandApplication app = (OsmandApplication) imageView.getContext().getApplicationContext();
		if (mapIconId != 0) {
			imageView.setImageResource(mapIconId);
			if (shouldPaintIcon) {
				AddedWidgetViewHolder.setImageFilter(imageView, !selected);
			}
		} else {
			UiUtilities iconsCache = app.getUIUtilities();
			Drawable drawable;
			if (shouldPaintIcon) {
				drawable = iconsCache.getPaintedIcon(settingsIconId, selected ? profileColor : defaultIconColor);
			} else {
				drawable = iconsCache.getIcon(settingsIconId);
			}
			imageView.setImageDrawable(drawable);
		}
	}

	public static void setImageFilter(@NonNull ImageView imageView, boolean applyFilter) {
		ColorFilter colorFilter = null;
		if (applyFilter) {
			ColorMatrix matrix = new ColorMatrix();
			matrix.setSaturation(0);
			colorFilter = new ColorMatrixColorFilter(matrix);
		}
		imageView.setColorFilter(colorFilter);
	}

	public static class AddedWidgetUiInfo {

		public String key;
		public String title;
		public MapWidgetInfo info;
		public int page;
		public int order;
		public int iconId;

		public AddedWidgetUiInfo() {
		}

		public AddedWidgetUiInfo(@NonNull AvailableWidgetUiInfo availableWidgetInfo, int page, int order) {
			this.key = availableWidgetInfo.key;
			this.title = availableWidgetInfo.title;
			this.info = availableWidgetInfo.info;
			this.page = page;
			this.order = order;
			this.iconId = availableWidgetInfo.iconId;
		}
	}

	public interface ItemMovableCallback {
		boolean isListItemMovable(int position);
	}
}
