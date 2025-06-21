import java.util.*;
public class STPSet {

    private int size;
    /** Mapping from timestamp to set of spatio-temporal points */
    private Map<Integer, Set<Integer>> STPMapping;

    public STPSet() {
        size = 0;
        STPMapping = new HashMap<>();
    }

    public STPSet(List<Integer> timestamps, List<Set<Integer>> vIdSets) {
        STPMapping = new HashMap<>();
        for (int i = 0; i < timestamps.size(); i++) {
            int timestamp = timestamps.get(i);
            Set<Integer> vIdSet = vIdSets.get(i);
            size += (vIdSet == null? 0 : vIdSet.size());
            STPMapping.put(timestamp, vIdSets.get(i));
        }
    }

    public void setSize(int size) {
        this.size = size;
    }

    /**
     * This method merge a list of STPSet instances to form a new STPSet instance
     * @param stpSets
     * @return
     */
    public static STPSet mergeSTPSets(List<STPSet> stpSets) {
        STPSet retSTPSet = new STPSet();
        int sup = 0;
        for (STPSet stpSet : stpSets) {
            for (Integer timestamp : stpSet.getTimestamps()) {
                Set<Integer> vIdSet = stpSet.getVIdSet4Timestamp(timestamp);
                retSTPSet.addTimestampAndVIdSet(timestamp, vIdSet);
            }
            sup += stpSet.getSize();
        }
        retSTPSet.setSize(sup);
        return retSTPSet;
    }

    /**
     * This method add spatio-temporal point to STPSet
     * @param timestamp timestamp of this point
     * @param vId vertex's id of this point
     */
    public void addSTP(int timestamp, int vId) {
        Set<Integer> vIdSet = STPMapping.get(timestamp);
        if (vIdSet == null) {
            vIdSet = new HashSet<>();
            STPMapping.put(timestamp, vIdSet);
        }
        vIdSet.add(vId);
        size++;
    }
    private void addTimestampAndVIdSet(Integer timestamp, Set<Integer> newVIdSet) {
        Set<Integer> vIdSet = this.STPMapping.get(timestamp);
        if (vIdSet == null) {
            vIdSet = new HashSet<>();
            this.STPMapping.put(timestamp, vIdSet);
        }
        if (newVIdSet != null) {
            vIdSet.addAll(newVIdSet);
        }
    }

    /**
     * This method calculate intersection of current STPSet and another STPSet
     * @param another another STPSet instance
     * @return
     */
    public STPSet intersect(STPSet another) {
        List<Integer> timestamps = new LinkedList<>();
        List<Set<Integer>> vIdSets = new LinkedList<>();
        for (int timestamp : STPMapping.keySet()) {
            Set<Integer> retSet = new HashSet<>();
            Set<Integer> vIdSet1 = STPMapping.get(timestamp);
            Set<Integer> vIdSet2 = another.getVIdSet4Timestamp(timestamp);
            if (vIdSet1 != null && vIdSet2 != null) {
                retSet.addAll(vIdSet1);
                retSet.retainAll(vIdSet2);
                if (retSet.size() != 0) {
                    timestamps.add(timestamp);
                    vIdSets.add(retSet);
                }
            }
        }
        STPSet ret = new STPSet(timestamps, vIdSets);
        return ret;
    }


    public Iterable<Integer> getTimestamps() {return STPMapping.keySet();}

    public Set<Integer> getVIdSet4Timestamp(int timestamp) {
        return STPMapping.get(timestamp);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("#SUP:").append(size);
        sb.append("\nsupporting points:\n");
        for (int timestamp : STPMapping.keySet()) {
            sb.append("[").append(timestamp).append("] {");
            for (int vId : STPMapping.get(timestamp)) {
                sb.append(vId).append(",");
            }
            sb.deleteCharAt(sb.length()-1);
            sb.append("}").append("\n");
        }
        return sb.toString();
    }

    public int getSize() {
        return size;
    }
}
