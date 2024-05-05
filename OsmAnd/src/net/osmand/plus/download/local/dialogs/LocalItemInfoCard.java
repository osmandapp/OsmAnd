package net.osmand.plus.download.local.dialogs;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.R;
import net.osmand.plus.download.local.BaseLocalItem;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.routepreparationmenu.cards.BaseCard;
import net.osmand.plus.utils.AndroidUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class LocalItemInfoCard extends BaseCard {

	private final BaseLocalItem localItem;

	public LocalItemInfoCard(@NonNull FragmentActivity activity, @NonNull BaseLocalItem localItem) {
		super(activity, false);
		this.localItem = localItem;
	}

	@Override
	public int getCardLayoutId() {
		return R.layout.local_item_info_card;
	}

	@Override
	protected void updateContent() {
		ViewGroup container = view.findViewById(R.id.container);

		String type = localItem.getType().toHumanString(app);
		setupRow(container.findViewById(R.id.type), getString(R.string.shared_string_type), type, false);

		DateFormat format = new SimpleDateFormat("dd.MM.yyyy, HH:mm", Locale.getDefault());
		String date = format.format(localItem.getLastModified());
		setupRow(container.findViewById(R.id.data), getString(R.string.shared_string_created), date, false);

		String size = AndroidUtils.formatSize(app, localItem.getSize());
		setupRow(container.findViewById(R.id.size), getString(R.string.shared_string_size), size, true);
	}

	private void setupRow(@NonNull View view, @Nullable String title, @Nullable String description, boolean lastItem) {
		TextView tvTitle = view.findViewById(R.id.title);
		tvTitle.setText(title);

		TextView tvDescription = view.findViewById(R.id.description);
		tvDescription.setText(description);

		AndroidUiHelper.updateVisibility(view.findViewById(R.id.bottom_divider), !lastItem);
	}
}