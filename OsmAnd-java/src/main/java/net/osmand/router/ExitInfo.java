package net.osmand.router;


public class ExitInfo {

    private String ref;

    private String exitStreetName;

    public String getRef() {
        return ref;
    }

    public void setRef(String ref) {
        this.ref = ref;
    }

    public String getExitStreetName() {
        return exitStreetName;
    }

    public void setExitStreetName(String exitStreetName) {
        this.exitStreetName = exitStreetName;
    }

    public boolean isEmpty() {
        return ref == null && exitStreetName == null;
    }
}
