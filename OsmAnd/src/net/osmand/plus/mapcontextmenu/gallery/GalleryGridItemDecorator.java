package net.osmand.plus.mapcontextmenu.gallery;

import static net.osmand.plus.mapcontextmenu.gallery.holders.GalleryImageHolder.*;

import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.mapcontextmenu.gallery.holders.GalleryImageHolder;
import net.osmand.plus.mapcontextmenu.gallery.holders.MapillaryContributeHolder;
import net.osmand.plus.utils.AndroidUtils;

public class GalleryGridItemDecorator extends RecyclerView.ItemDecoration {
	public static final int GRID_SCREEN_ITEM_SPACE_DP = 3;

	private final OsmandApplication app;

	public GalleryGridItemDecorator(@NonNull OsmandApplication app) {
		this.app = app;
	}

	@Override
	public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
		super.getItemOffsets(outRect, view, parent, state);
		int position = parent.getChildAdapterPosition(view);
		RecyclerView.ViewHolder viewHolder = parent.getChildViewHolder(view);
		if (viewHolder instanceof GalleryImageHolder holder) {
			ImageHolderType type = holder.getHolderType();
			switch (type) {
				case SPAN_RESIZABLE -> {
					int gridSpace = AndroidUtils.dpToPx(app, GRID_SCREEN_ITEM_SPACE_DP);
					outRect.right = gridSpace;
					outRect.bottom = gridSpace;
					outRect.left = gridSpace;
					outRect.top = gridSpace;
				}
				case MAIN -> {
					outRect.right = AndroidUtils.dpToPx(app, 6);
				}
				case STANDARD -> {
					outRect.right = AndroidUtils.dpToPx(app, 6);
					outRect.left = AndroidUtils.dpToPx(app, 6);
					if (position % 2 == 0) {
						outRect.top = AndroidUtils.dpToPx(app, 6);
						outRect.bottom = 0;
					} else {
						outRect.bottom = AndroidUtils.dpToPx(app, 6);
						outRect.top = 0;
					}
				}
			}
		} else if (viewHolder instanceof MapillaryContributeHolder) {
			outRect.right = AndroidUtils.dpToPx(app, 16);
		}
	}
}
