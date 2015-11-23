package net.osmand.plus.osmedit;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public abstract class OsmPoint  implements Serializable {

	public static enum Group {BUG, POI};

	public static enum Action {CREATE, MODIFY, DELETE, REOPEN};

	public static final Map<Action, String> stringAction = new HashMap<Action, String>();
	public static final Map<String, Action> actionString = new HashMap<String, Action>();
	static {
		stringAction.put(Action.CREATE, "create");
		stringAction.put(Action.MODIFY, "modify");
		stringAction.put(Action.DELETE, "delete");
		stringAction.put(Action.REOPEN, "reopen");

		actionString.put("create", Action.CREATE);
		actionString.put("modify", Action.MODIFY);
		actionString.put("reopen", Action.REOPEN);
		actionString.put("delete", Action.DELETE);
	};

	private Action action;

	public OsmPoint(){
	}

	public abstract long getId();

	public abstract double getLatitude();

	public abstract double getLongitude();

	public abstract Group getGroup();

	public Action getAction() {
		return action;
	}


	public void setAction(String action) {
		this.action = actionString.get(action);
	}

	public void setAction(Action action) {
		this.action = action;
	}


	@Override
	public String toString() {
		return new StringBuffer("Osm Point ").append(this.getAction()).append(" ")
			.toString();
	}

}
