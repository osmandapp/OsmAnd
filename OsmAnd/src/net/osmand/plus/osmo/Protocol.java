package net.osmand.plus.osmo;

public class Protocol {
	public static class CreateGroupData {
		public final String name;
		public final boolean onlyByInvite;
		public final String description;
		public final String policy;

		public CreateGroupData(String name, boolean onlyByInvite, String description, String policy) {
			this.name = name;
			this.onlyByInvite = onlyByInvite;
			this.description = description;
			this.policy = policy;
		}
	}
}
