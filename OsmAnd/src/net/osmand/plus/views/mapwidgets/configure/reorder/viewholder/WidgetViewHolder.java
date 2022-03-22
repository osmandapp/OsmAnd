package net.osmand.plus.views.mapwidgets.configure.reorder.viewholder;

import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.views.controls.ReorderItemTouchHelperCallback.UnmovableItem;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;

public class WidgetViewHolder extends ViewHolder implements UnmovableItem {

	public TextView title;
	public ImageView icon;
	public ImageView moveIcon;

	private final ItemMovableCallback itemMovableCallback;

	public WidgetViewHolder(@NonNull View itemView, @Nullable ItemMovableCallback callback) {
		super(itemView);
		this.itemMovableCallback = callback;
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
		int mapIconId = widgetInfo.getMapIconId(nightMode);
		int settingsIconId = widgetInfo.getSettingsIconId();

		OsmandApplication app = (OsmandApplication) imageView.getContext().getApplicationContext();
		if (mapIconId != 0) {
			imageView.setImageResource(mapIconId);
			WidgetViewHolder.setImageFilter(imageView, !selected);
		} else {
			Drawable drawable = app.getUIUtilities().getPaintedIcon(settingsIconId, selected ? profileColor : defaultIconColor);
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

	public static class WidgetUiInfo {

		public String key;
		public String title;
		public MapWidgetInfo info;
		public int order;
		public int iconId;
		public boolean isActive;

		public void toggleActive() {
			isActive = !isActive;
		}
	}

	public interface ItemMovableCallback {
		boolean isListItemMovable(int position);
	}
}
