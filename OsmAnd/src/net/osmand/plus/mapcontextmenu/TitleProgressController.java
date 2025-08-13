package net.osmand.plus.mapcontextmenu;

import android.content.Context;

import androidx.annotation.NonNull;

import net.osmand.plus.R;

public abstract class TitleProgressController {

	public String caption = "";
	public float progress;
	public boolean indeterminate;
	public boolean visible;
	public boolean progressVisible;
	public boolean buttonVisible;

	public void setIndexesDownloadMode(@NonNull Context ctx) {
		caption = ctx.getString(R.string.downloading_list_indexes);
		indeterminate = true;
		progressVisible = true;
		buttonVisible = false;
	}

	public void setNoInternetConnectionMode(@NonNull Context ctx) {
		caption = ctx.getString(R.string.no_index_file_to_download);
		progressVisible = false;
		buttonVisible = false;
	}

	public void setMapDownloadMode() {
		indeterminate = false;
		progressVisible = true;
		buttonVisible = true;
	}

	public abstract void buttonPressed();
}
