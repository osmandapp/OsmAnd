package net.osmand.plus.plugins.development;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.R;
import net.osmand.plus.base.BaseLoadAsyncTask;
import net.osmand.plus.render.NativeOsmandLibrary;
import net.osmand.render.RenderingRulesStorage;

class NativeLibraryLoadTask extends BaseLoadAsyncTask<Void, Void, Void> {

	private final RenderingRulesStorage storage;

	public NativeLibraryLoadTask(@NonNull FragmentActivity activity, @NonNull RenderingRulesStorage storage) {
		super(activity);
		this.storage = storage;
	}

	@Override
	protected Void doInBackground(Void... voids) {
		NativeOsmandLibrary.getLibrary(storage, app);
		return null;
	}

	@Override
	protected void onPostExecute(Void result) {
		hideProgress();
		if (!NativeOsmandLibrary.isNativeSupported(storage, app)) {
			app.showToastMessage(R.string.native_library_not_supported);
		}
	}
}