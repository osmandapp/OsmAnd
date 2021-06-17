package net.osmand.router;

public class ExitInfo {

	private String ref;
	private String exitName;
	private String exitDestination;

	public String getRef() {
		return ref;
	}

	public void setRef(String ref) {
		this.ref = ref;
	}

	public String getExitName() {
		return exitName;
	}

	public void setExitName(String exitStreetName) {
		this.exitName = exitStreetName;
	}

	public String getExitDestination() {
		return exitDestination;
	}

	public void setExitDestination(String exitDestination) {
		this.exitDestination = exitDestination;
	}
}
