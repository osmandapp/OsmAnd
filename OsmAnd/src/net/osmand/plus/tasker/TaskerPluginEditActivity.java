package net.osmand.plus.tasker;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.twofortyfouram.locale.sdk.client.ui.activity.AbstractAppCompatPluginActivity;

import net.osmand.plus.R;

import java.util.Random;

public class TaskerPluginEditActivity extends AbstractAppCompatPluginActivity {

	@NonNull
	public static final Intent INTENT_REQUEST_REQUERY = new Intent(
			com.twofortyfouram.locale.api.Intent.ACTION_REQUEST_QUERY).putExtra(
			com.twofortyfouram.locale.api.Intent.EXTRA_STRING_ACTIVITY_CLASS_NAME,
			TaskerPluginEditActivity.class.getName());

	private Random random = new Random();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_tasker_plugin_edit);
	}

	@Override
	public boolean isBundleValid(@NonNull Bundle bundle) {
		return PluginBundleValues.isBundleValid(bundle);
	}

	@Override
	public void onPostCreateWithPreviousResult(@NonNull Bundle previousBundle, @NonNull String previousBlurb) {
	}

	@Nullable
	@Override
	public Bundle getResultBundle() {
		return PluginBundleValues.generateBundle(random.nextBoolean());
	}

	@NonNull
	@Override
	public String getResultBlurb(@NonNull Bundle bundle) {
		return "testValue=" + bundle.getBoolean(PluginBundleValues.BUNDLE_EXTRA_BOOLEAN_TEST);
	}
}
