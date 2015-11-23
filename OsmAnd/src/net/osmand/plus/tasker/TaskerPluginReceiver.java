package net.osmand.plus.tasker;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.widget.Toast;

import com.twofortyfouram.locale.sdk.client.receiver.AbstractPluginSettingReceiver;

public class TaskerPluginReceiver extends AbstractPluginSettingReceiver {
	public TaskerPluginReceiver() {
	}

	@Override
	protected boolean isBundleValid(@NonNull Bundle bundle) {
		return PluginBundleValues.isBundleValid(bundle);
	}

	@Override
	protected boolean isAsync() {
		return false;
	}

	@Override
	protected void firePluginSetting(@NonNull Context context, @NonNull Bundle bundle) {
		Toast.makeText(context,
				"All good value=" + bundle.getBoolean(PluginBundleValues.BUNDLE_EXTRA_BOOLEAN_TEST),
				Toast.LENGTH_LONG).show();
	}
}
