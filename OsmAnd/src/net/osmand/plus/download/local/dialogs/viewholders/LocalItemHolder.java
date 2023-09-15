package net.osmand.plus.download.local.dialogs.viewholders;

import static net.osmand.plus.download.local.ItemType.CACHE;
import static net.osmand.plus.download.local.ItemType.REGULAR_MAPS;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.download.local.ItemType;
import net.osmand.plus.download.local.LocalItem;
import net.osmand.plus.download.local.dialogs.LocalItemsAdapter.LocalItemListener;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;

import org.apache.commons.logging.Log;

import java.io.File;
import java.text.DateFormat;
import java.util.Date;
import java.util.Map;

public class LocalItemHolder extends RecyclerView.ViewHolder {

	private final Log log = PlatformUtil.getLog(LocalItemHolder.class);

	private final OsmandApplication app;
	private final UiUtilities uiUtilities;
	private final LocalItemListener listener;

	private final TextView title;
	private final TextView description;
	private final ImageView icon;
	private final View options;
	private final CompoundButton compoundButton;
	private final View bottomShadow;
	private final View bottomDivider;

	public LocalItemHolder(@NonNull View itemView, @Nullable LocalItemListener listener, boolean nightMode) {
		super(itemView);
		app = (OsmandApplication) itemView.getContext().getApplicationContext();
		uiUtilities = app.getUIUtilities();
		this.listener = listener;

		icon = itemView.findViewById(R.id.icon);
		title = itemView.findViewById(R.id.title);
		description = itemView.findViewById(R.id.description);
		options = itemView.findViewById(R.id.options);
		compoundButton = itemView.findViewById(R.id.compound_button);
		bottomShadow = itemView.findViewById(R.id.bottom_shadow);
		bottomDivider = itemView.findViewById(R.id.bottom_divider);

		int activeColor = ColorUtilities.getActiveColor(app, nightMode);
		Drawable drawable = UiUtilities.getColoredSelectableDrawable(app, activeColor, 0.3f);
		UiUtilities.setupCompoundButton(nightMode, activeColor, compoundButton);
		AndroidUtils.setBackground(itemView.findViewById(R.id.selectable_list_item), drawable);
	}

	public void bindView(@NonNull LocalItem item, boolean selectionMode, boolean lastItem) {
		ItemType type = item.getType();

		title.setText(item.getName());

		String descr = getDescription(item);
		String size = AndroidUtils.formatSize(app, item.getSize());
		String text;
		if (descr != null) {
			text = app.getString(R.string.ltr_or_rtl_combine_via_bold_point, size, descr);
		} else {
			text = size;
		}
		if (item.isBackupedData(app)) {
			text += " (" + app.getString(R.string.local_indexes_cat_backup) + ")";
		}
		description.setText(text);
		icon.setImageDrawable(uiUtilities.getThemedIcon(type.getIconId()));

		boolean selected = listener != null && listener.isItemSelected(item);
		compoundButton.setChecked(selected);

		options.setOnClickListener(v -> {
			if (listener != null) {
				listener.onItemOptionsSelected(item, options);
			}
		});
		itemView.setOnClickListener(v -> {
			if (listener != null) {
				listener.onItemSelected(item);
			}
		});
		AndroidUiHelper.updateVisibility(options, !selectionMode);
		AndroidUiHelper.updateVisibility(compoundButton, selectionMode);
		AndroidUiHelper.updateVisibility(bottomShadow, lastItem);
		AndroidUiHelper.updateVisibility(bottomDivider, !lastItem);
	}

	@Nullable
	private String getDescription(@NonNull LocalItem item) {
		File file = item.getFile();
		ItemType type = item.getType();

		if (type == REGULAR_MAPS) {
			Map<String, String> indexFileNames = app.getResourceManager().getIndexFileNames();
			String fileModifiedDate = indexFileNames.get(file.getName());
			if (fileModifiedDate != null) {
				try {
					Date date = app.getResourceManager().getDateFormat().parse(fileModifiedDate);
					if (date != null) {
						return getInstalledDate(date);
					}
				} catch (Exception e) {
					log.error(e);
				}
			}
		}
		return type != CACHE ? getInstalledDate(new Date(file.lastModified())) : null;
	}

	@NonNull
	private String getInstalledDate(@NonNull Date date) {
		return DateFormat.getDateInstance(DateFormat.SHORT).format(date);
	}
}