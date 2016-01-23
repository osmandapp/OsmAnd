package net.osmand.access.tasker;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by joaom_000 on 11/10/2015.
 */
public class AutoAppsThirdParty {
	//Intent
	public static final String ACTION_AUTHORIZE = "com.joaomgcd.autoapps.ACTION_AUTHORIZE";
	public static final String ACTION_SEND_COMMAND = "com.joaomgcd.autoapps.ACTION_SEND_COMMAND";
	public static final String ACTION_REGISTER_COMMANDS = "com.joaomgcd.autoapps.ACTION_REGISTER_COMMANDS";
	public static final String EXTRA_PENDING_INTENT = "com.joaomgcd.autoapps.EXTRA_PENDING_INTENT";
	public static final String EXTRA_COMMAND = "com.joaomgcd.autoapps.EXTRA_COMMAND";

	//Service
	public static final String AUTOAPPS_PACKAGE = "com.joaomgcd.autoappshub";
	public static final String AUTOAPPS_SERVICE = AUTOAPPS_PACKAGE + ".service.ServiceThirdPartyCommands";

	public static void authorize(Context context) {
		Intent intentAuthorize = getServiceIntent(context, ACTION_AUTHORIZE);
		context.startService(intentAuthorize);
	}

	@NonNull
	private static Intent getServiceIntent(Context context, String action) {
		Intent intent = new Intent();
		intent.setComponent(new ComponentName(AUTOAPPS_PACKAGE, AUTOAPPS_SERVICE));
		intent.putExtra(EXTRA_PENDING_INTENT, PendingIntent.getService(context, 1, new Intent(), 0));
		intent.setAction(action);
		return intent;
	}

	public static void sendCommand(Context context, String command) {
		Intent intentCommand = getServiceIntent(context, ACTION_SEND_COMMAND);
		intentCommand.putExtra(EXTRA_COMMAND, command);
		context.startService(intentCommand);
	}

	public static void sendCommand(Context context, String commandId, String... parameters) {
		StringBuilder sb = new StringBuilder();
		sb.append(commandId);
		if (parameters != null) {
			for (String parameter : parameters) {
				sb.append("=:=");
				sb.append(parameter);
			}
		}
		sendCommand(context, sb.toString());
	}

	public static void registerCommands(Context context, RegisteredCommand... commands) {
		Intent intentCommand = getServiceIntent(context, ACTION_REGISTER_COMMANDS);
		ArrayList<RegisteredCommand> registeredCommands = new ArrayList<>(Arrays.asList(commands));
		intentCommand.putParcelableArrayListExtra(EXTRA_COMMAND, registeredCommands);
		context.startService(intentCommand);
	}

	public static class RegisteredCommand implements Parcelable {
		private String name;
		private String id;
		private String variables;
		private boolean isArray;

		public String getVariables() {
			return variables;
		}

		public void setVariables(String variables) {
			this.variables = variables;
		}

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public RegisteredCommand(String name, String id, boolean isArray, String... variables) {
			this(name, id, variables);
			this.isArray = isArray;
		}

		public RegisteredCommand(String name, String id, String... variables) {
			this.name = name;
			this.id = id;
			StringBuilder sb = new StringBuilder();
			int i = 0;
			for (String variable : variables) {
				if (i > 0) {
					sb.append(",");
				}
				sb.append(variable);
				i++;
			}
			this.variables = sb.toString();
		}

		protected RegisteredCommand(Parcel in) {
			name = in.readString();
			id = in.readString();
			variables = in.readString();
			isArray = in.readInt() == 1;
		}

		@Override
		public int describeContents() {
			return 0;
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			dest.writeString(name);
			dest.writeString(id);
			dest.writeString(variables);
			dest.writeInt(isArray ? 1 : 0);
		}

		@SuppressWarnings("unused")
		public static final Parcelable.Creator<RegisteredCommand> CREATOR = new Parcelable.Creator<RegisteredCommand>() {
			@Override
			public RegisteredCommand createFromParcel(Parcel in) {
				return new RegisteredCommand(in);
			}

			@Override
			public RegisteredCommand[] newArray(int size) {
				return new RegisteredCommand[size];
			}
		};

		public boolean isArray() {
			return isArray;
		}

		public void setIsArray(boolean isArray) {
			this.isArray = isArray;
		}
	}
}