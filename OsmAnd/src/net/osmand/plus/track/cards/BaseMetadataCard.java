package net.osmand.plus.track.cards;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import net.osmand.gpx.GPXUtilities.Metadata;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.mapcontextmenu.other.ShareMenu;
import net.osmand.plus.routepreparationmenu.cards.MapBaseCard;

public abstract class BaseMetadataCard extends MapBaseCard {

	protected final Metadata metadata;

	private ViewGroup itemsContainer;

	public BaseMetadataCard(@NonNull MapActivity mapActivity, @NonNull Metadata metadata) {
		super(mapActivity);
		this.metadata = metadata;
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.gpx_info_card;
	}

	@StringRes
	abstract protected int getTitleId();

	@Override
	public void updateContent() {
		TextView header = view.findViewById(R.id.header);
		header.setText(getTitleId());

		itemsContainer = view.findViewById(R.id.items_container);
		itemsContainer.removeAllViews();
	}

	@NonNull
	protected View createItemRow(@NonNull String title, @NonNull String description, @Nullable Drawable icon) {
		View view = themedInflater.inflate(R.layout.item_with_title_desc, itemsContainer, false);
		itemsContainer.addView(view);

		ImageView iconIv = view.findViewById(R.id.icon);
		TextView titleTv = view.findViewById(R.id.title);
		TextView descriptionTv = view.findViewById(R.id.description);

		titleTv.setText(title);
		descriptionTv.setText(description);
		iconIv.setImageDrawable(icon);
		AndroidUiHelper.updateVisibility(iconIv, icon != null);

		view.setOnLongClickListener(v -> {
			ShareMenu.copyToClipboardWithToast(activity, description, Toast.LENGTH_SHORT);
			return true;
		});

		return view;
	}
}