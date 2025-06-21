import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
public class ReadGraph {
    /** indicate whether to store all attribute values as type of double **/
    private static boolean ALLASDOUBLE = true;
    /** set maximal number of attribute */
    private static int TOTAL_NUM_ATTR;
    /** store path of file that record attributes of vertices each time */
    private static String ATTR_FILE_PATH = ParametersSetting.ATTR_FILE_PATH;
    /** store path of file that record edges of vertices each time */
    private static String EDGE_FILE_PATH = ParametersSetting.EDGE_FILE_PATH;


    public static void main(String[] args) throws IOException {
        readGraph();
//        readAttrMapping();
        statGraph();
    }

    /**
     * This method output statistical information of the constructed dynamic attributed graph,
     * including number of vertices, edges, and average number of edges for each vertex.
     * @throws IOException
     */
    public static void statGraph() throws IOException {
        Map<Integer, AttributedGraph> dyAG = readGraph();
        int numTimestamps = dyAG.size();
        int numVertices = dyAG.get(0).getVerNum();
        int totalCount4E = 0;
        for (int i : dyAG.keySet()) {
            AttributedGraph aG = dyAG.get(i);
            for (Map.Entry<Integer, Set<Integer>> edgeLinkEntry: aG.getEdgesMap().entrySet()) {
                totalCount4E += edgeLinkEntry.getValue().size();
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append("total timestamps: ").append(numTimestamps);
        sb.append("\ntotal vertices: ").append(numVertices);
        sb.append("\naverage edges for each vertex each timestamp: ").append(1.0 * totalCount4E/(numTimestamps * numVertices));
//        System.out.println(sb.toString());
    }

    /**
     * This method create dynamic attributed graph (DyAG) and add attributes and edges for each vertex
     * @throws IOException
     */
    public static Map<Integer, AttributedGraph> readGraph() throws IOException {
//        System.out.println("@@@ start to read original graph ...");
        //create an empty DyAG, use a map denote this DyAG
        Map<Integer, AttributedGraph> DyAG = new HashMap<>();

//        System.out.println(ATTR_FILE_PATH);
        //add vertices and attributes for an empty DyAG according to file "attributes.txt"
        BufferedReader brAttr = new BufferedReader(new FileReader(ATTR_FILE_PATH));
        String line1 = brAttr.readLine();
        int count = 0;
        //while still has unprocessed line
        while (line1 != null) {
            //if it indicates a new attributed graph
            if (line1.startsWith("T")) {
                AttributedGraph aG = new AttributedGraph(count);
                while ((line1 = brAttr.readLine()) != null && ! line1.startsWith("T")) {
                    attrLineProcess(aG, line1);
                }
                DyAG.put(count, aG);
            }
            count++;
        }

        //add edges for DyAG according to file "graph.txt"
        //same with previous process except here we do not need to create new attributed graph
        //and use sub method of edgeLineProcess
//        System.out.println(EDGE_FILE_PATH);
        BufferedReader brEdges = new BufferedReader(new FileReader(EDGE_FILE_PATH));
        String line2 = brEdges.readLine();
        while (line2 != null) {
            if (line2.startsWith("T") | line2.startsWith("-1")) {
                int aGId = Integer.parseInt(line2.split("T")[1]);
                AttributedGraph aG = DyAG.get(aGId);
                while ((line2 = brEdges.readLine()) != null && !line2.startsWith("T")) {
                    edgeLineProcess(aG, line2);
                }
            }
        }

        // output some statistic information
        int numTimestamps = DyAG.size();
        int numVertices = DyAG.get(0).getVerNum();
        int totalCount4E = 0;
        for (int i : DyAG.keySet()) {
            AttributedGraph aG = DyAG.get(i);
            for (Map.Entry<Integer, Set<Integer>> edgeLinkEntry: aG.getEdgesMap().entrySet()) {
                totalCount4E += edgeLinkEntry.getValue().size();
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append("total timestamps: ").append(numTimestamps);
        sb.append("\ntotal vertices: ").append(numVertices);
        sb.append("\naverage edges for each vertex each timestamp: ").append(1.0 * totalCount4E/(numTimestamps * numVertices));
//        System.out.println(sb.append("\n").toString());

//        System.out.println("reading graph finish !");

        //test whether read attributes and edges successfully

        return DyAG;

    }

    /**
     * This method process each edge line from "graph.txt" to add edges
     * @param aG the attributed graph
     * @param line the edge line to be processed
     */
    private static void edgeLineProcess(AttributedGraph aG, String line) {
        String[] items = line.split(" ");
        //value of first position denote id of the common vertex linking to rest vertices in the line

        Integer vId = Integer.parseInt(items[0]);
        // store ids of all other neighboring vertices
        List<Integer> neighbors = new LinkedList<>();
        //for each item other than the first one
        for (int i = 1; i < items.length; i++) {
            //parse it to integer and add it to id list
            neighbors.add(Integer.parseInt(items[i]));
        }
        if (aG!=null){
            aG.addEdges(vId, neighbors);
        }
    }

    /**
     * This method process each attribute line from "attributes.txt" to create vertex and add it to DyAG
     * @param aG the attributed graph associated with this line
     * @param line the attribute line to be processed
     */
    private static void attrLineProcess(AttributedGraph aG, String line) {
        String[] items = line.split(" ");
        //value of first position denote id of the vertex
        Integer vId = Integer.parseInt(items[0]);
        aG.addVertex(vId);
        if (ALLASDOUBLE) {
            //store all attribute values as type of double
            //attribute type list
            List<Integer> attrTypes = new LinkedList<>();
            //attribute value list
            List<Double> attrVals = new LinkedList<>();

            int max_attr_num = items.length;
            TOTAL_NUM_ATTR = max_attr_num-1;

            for (int i = 1; i < max_attr_num; i++) {
                Double val = Double.parseDouble(items[i]);
                attrTypes.add(i);
                attrVals.add(val);
            }
            //add attribute types and values
            aG.addAttrValForV(vId, attrTypes, attrVals);
        }
    }
}
