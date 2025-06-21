import java.util.HashMap;
import java.util.List;
import java.util.Map;
public class Vertex {
    /** mapping record attribute types and values */
    private Map<Integer, Double> attrDouMap;

    /**
     * construct vertex obejct by unique vertex id
     * @param id the unique id to identify different vertices
     */
    public Vertex(int id) {
        attrDouMap = new HashMap<>();
    }


    /**
     * This method set attribute types and values for vertex
     * @param attrL the attribute type list
     * @param valL the attribute value list
     */
    public void addAttrsValsForV(List<Integer> attrL, List<Double> valL) {
        for (int i = 0; i < attrL.size(); i++) {
            addAttrValForV(attrL.get(i), valL.get(i));
        }
    }

    /**
     * This method set single attribute type and value for vertex
     * @param attr single attribute type
     * @param val single attribute value
     */
    public void addAttrValForV(int attr, Double val) {
        attrDouMap.put(attr, val);
    }

    /**
     * This method get number of attribute type
     * @return number of attribute
     */
    public int getAttrNum() {
        return attrDouMap.size();
    }

    /**
     * This method get attribute map for the vertex
     * @return attribute map
     */
    public Map<Integer, Double> getAttrDouMap() {
        return attrDouMap;
    }
}
