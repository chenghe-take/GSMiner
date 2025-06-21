import java.util.LinkedList;
import java.util.List;
public class ItemVertex {
//    /** unique vertex id */
//    private int id;
    /** a set of item*/
    private List<Integer> items;
    /**
     * construct vertex obejct by unique vertex id
     * @param id the unique id to identify different vertices
     */
    public ItemVertex(int id) {
//        this.id = id;
        items = new LinkedList<>();
    }

    /**
     * This method add a set of items for vertex
     * @param items a set of item
     */
    public void addItems(List<Integer> items) {
        this.items.addAll(items);
    }

    /** This method get all items of the vertex
     * @return all items of the vertex
     */
    public Iterable<Integer> getAllItems() {
        return items;
    }
}
