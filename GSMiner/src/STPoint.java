/**
 *  This class is a spatio temporal point
 */

public class STPoint {
    /** represent identifier of item attributed graph */
    private int timestamp;
    /** represent identifier of item vertex */
    private int vId;

    /**
     * This method construct spatio-temporal point using identifiers of attributed graph and vertex
     * @param timestamp identitier of attributed graph
     * @param vId identifier of vertex
     */
    public STPoint(int timestamp, int vId) {
        this.timestamp = timestamp;
        this.vId = vId;
    }

    /**
     * This method get timestamp ot this spatio-temporal point
     * @return timestamp of the the ST point
     */
    public int getTimestamp() {
        return timestamp;
    }

    /**
     *  This method get vertex identifier of the ST point
     * @return vertex identifier of the ST point
     */
    public int getvId() {
        return vId;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("(").append(timestamp).append(",").append(vId).append(")");
        return sb.toString();
    }
}
