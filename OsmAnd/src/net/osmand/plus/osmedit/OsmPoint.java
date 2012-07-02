package net.osmand.plus.osmedit;

import java.util.HashMap;
import java.util.Map;

public abstract class OsmPoint {
	private static final long serialVersionUID = 729654300829771469L;

	public static enum Group {BUG, POI};

	public static enum Action {CREATE, MODIFY, DELETE};

	public static final Map<Action, String> stringAction = new HashMap<Action, String>();
	public static final Map<String, Action> actionString = new HashMap<String, Action>();
	static {
		stringAction.put(Action.CREATE, "create");
		stringAction.put(Action.MODIFY, "modify");
		stringAction.put(Action.DELETE, "delete");

		actionString.put("create", Action.CREATE);
		actionString.put("modify", Action.MODIFY);
		actionString.put("delete", Action.DELETE);
	};

	private Action action;
	private boolean stored = false;

	public OsmPoint(){
	}

	public abstract long getId();

	public abstract double getLatitude();

	public abstract double getLongitude();

	public abstract Group getGroup();

	public Action getAction() {
		return action;
	}

	public boolean isStored() {
		return stored;
	}

	public void setAction(String action) {
		this.action = actionString.get(action);
	}

	public void setAction(Action action) {
		this.action = action;
	}

	public void setStored(boolean stored) {
		this.stored = stored;
	}

	@Override
	public String toString() {
		return new StringBuffer("Osm Point ").append(this.getAction()).append(" ")
			.toString();
	}
}
