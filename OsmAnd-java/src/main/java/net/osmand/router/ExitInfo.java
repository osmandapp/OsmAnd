package net.osmand.router;


public class ExitInfo {

    private String ref;

    private String destinationName;
    private String destinationRef;

    public String getRef() {
        return ref;
    }

    public void setRef(String ref) {
        this.ref = ref;
    }

    public String getDestinationRef() {
        return destinationRef;
    }

    public void setDestinationRef(String destinationRef) {
        this.destinationRef = destinationRef;
    }

    public String getDestinationName() {
        return destinationName;
    }

    public void setDestinationName(String destinationName) {
        this.destinationName = destinationName;
    }
}
