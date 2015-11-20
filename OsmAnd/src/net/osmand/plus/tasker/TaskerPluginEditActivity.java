package net.osmand.plus.tasker;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.twofortyfouram.locale.sdk.client.ui.activity.AbstractAppCompatPluginActivity;

import net.osmand.plus.R;

public class TaskerPluginEditActivity extends AbstractAppCompatPluginActivity {


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_tasker_plugin_edit);
	}

	@Override
	public boolean isBundleValid(@NonNull Bundle bundle) {
		return false;
	}

	@Override
	public void onPostCreateWithPreviousResult(@NonNull Bundle previousBundle, @NonNull String previousBlurb) {

	}

	@Nullable
	@Override
	public Bundle getResultBundle() {
		return null;
	}

	@NonNull
	@Override
	public String getResultBlurb(@NonNull Bundle bundle) {
		return "Empty implementation";
	}
}
