package net.osmand.access.tasker;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.twofortyfouram.locale.sdk.client.ui.activity.AbstractAppCompatPluginActivity;

import net.osmand.plus.R;

import java.util.Random;

public class TaskerPluginEditActivity extends AbstractAppCompatPluginActivity {

	@NonNull
	public static final Intent INTENT_REQUEST_REQUERY =
			new Intent(com.twofortyfouram.locale.api.Intent.ACTION_REQUEST_QUERY)
					.putExtra(com.twofortyfouram.locale.api.Intent.EXTRA_STRING_ACTIVITY_CLASS_NAME,
							TaskerPluginEditActivity.class.getName());
	public static final String TAG = "TaskerPluginEditActivit";

	private Random random = new Random();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_tasker_plugin_edit);
		mIsCancelled = false;
		Log.v(TAG, "onCreate()");
	}

	@Override
	public boolean isBundleValid(@NonNull Bundle bundle) {
		final boolean bundleValid = PluginBundleValues.isBundleValid(bundle);
		Log.v(TAG, "isBundleValid = " + bundleValid);
		return bundleValid;
	}

	@Override
	public void onPostCreateWithPreviousResult(@NonNull Bundle previousBundle, @NonNull String previousBlurb) {
	}

	@Nullable
	@Override
	public Bundle getResultBundle() {
		Log.v(TAG, "getResultBundle()");
		return PluginBundleValues.generateBundle(random.nextBoolean());
	}

	@NonNull
	@Override
	public String getResultBlurb(@NonNull Bundle bundle) {
		final String blurp = "testValue=" + bundle.getBoolean(PluginBundleValues.BUNDLE_EXTRA_BOOLEAN_TEST);
		Log.v(TAG, "getResultBlurb()" + blurp);
		return blurp;
	}
}
