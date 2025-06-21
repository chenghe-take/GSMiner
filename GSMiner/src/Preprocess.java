import java.io.*;
import java.util.*;
public class Preprocess {
    /** naive discretization */
    private static String[] trends = new String[] {"-", "=", "+"};

    /** map from attribute type(integer) -> attribute name */
    static Map<Integer, String> attrMapping;
    /** map from event type(integer) -> enent type name(String) */
    static Map<Integer, String> eventTypeMapping = new LinkedHashMap<>();
    /** map from event type name(String) -> event type(integer) */
    static Map<String, Integer> eventTypeMappingRe = new LinkedHashMap<>();
    /** item dynamic attributed graph(ItDyAG) deriving from original dynamic attributed graph(DyAG) */
    static Map<Integer, ItemAttributedGraph> itDyAG = new LinkedHashMap<>();

    /** store path of mapping of attribute form integer to string */
    private static String ATTRI_MAPPING_PATH = ParametersSetting.ATTRI_MAPPING_PATH;

    /** this flag indicate how to do discretization */
    private static int DISCRET_FLAG = ParametersSetting.DISCRE_FLAG;

    private static int PASS_FLAG = 1;

    private static String EVENTTYPE_MAPPING_PATH = ParametersSetting.EVENTTYPE_MAPPING_PATH;


    private static void repeatGraph(Map<Integer, ItemAttributedGraph> tempItemDyAG) {
        int repeatNum = ParametersSetting.REPEAT;
        int oriSize = tempItemDyAG.size();
        for (int timeStamp = 0; timeStamp < oriSize; timeStamp++) {
            ItemAttributedGraph itemAG = tempItemDyAG.get(timeStamp);
            for (int i = 1; i < repeatNum; i++) {
                tempItemDyAG.put(oriSize * i + timeStamp, itemAG);
            }
        }
    }


    /** */
    private static Map<Integer, Map<Integer, List<Double>>> vertexMapAttrMapVals;

    static {
        switch (DISCRET_FLAG) {
            case 0: {
                trends = new String[] {"-", "0", "+"};
                break;
            }
            case 1: {
                trends = new String[] {"--", "-", "0", "+", "++"};
                break;
            }
        }
    }

    public static void main(String[] args) throws IOException {
        findEventTypeMapping();
        writeEventTypeMapping();

//        System.out.println(itDyAG.get(0).getItemV(0).getAllItems());
    }

    /**
     * This method read attribute mapping from integer to attribute name
     * @return attribute mapping
     * @throws IOException
     */
    public static Map<Integer, String> readAttrMapping() throws IOException {
        //use a map to store relationship between attribute type and integer
        Map<Integer, String> attrMap = new LinkedHashMap<>();
//        System.out.println(ATTRI_MAPPING_PATH);
        File attrMapFile = new File(ATTRI_MAPPING_PATH);
        if (! attrMapFile.exists())
            attrMapFile.createNewFile();
        BufferedReader br = new BufferedReader(new FileReader(ATTRI_MAPPING_PATH));
        int count = 1;
        String line = br.readLine();
        while (line != null) {
            attrMap.put(count++, line);
            line = br.readLine();
        }
        //test if read successfully
//        for (Integer i : attrMap.keySet()) {
//            System.out.println(i + " " + attrMap.get(i));
//        }
        return attrMap;
    }

    /**
     * This method do naive discretization for original attribute types
     * @throws IOException
     */
    private static void findEventTypeMapping() throws IOException {
        attrMapping = readAttrMapping();
        int count = 1;
        for (int attrType : attrMapping.keySet()) {
            String attrName = attrMapping.get(attrType);
            for (String trend: trends){
                String eventName = attrName + trend;
                // eg: key=1 value=DMKD-; key=2 value=DMKD0; key=3 value=DMKD+
                eventTypeMapping.put(count, eventName);
                eventTypeMappingRe.put(eventName, count);
                count++;
            }
        }
    }

    /**
     * This method write result of discretization to file
     * @throws IOException
     */
    public static void writeEventTypeMapping() throws IOException {
        BufferedWriter bw = new BufferedWriter(new FileWriter(EVENTTYPE_MAPPING_PATH));
        for (int eventType : eventTypeMapping.keySet()) {
            bw.write(eventTypeMapping.get(eventType));
            bw.newLine();
        }
        bw.close();
    }


    private static void acquireAllVals(Map<Integer, AttributedGraph> oriDyAG) {
        //compute mean and standard deviation for each vertex
        vertexMapAttrMapVals = new HashMap<>();
        for (int i = 0; i < oriDyAG.size() - 1; i++) {
            AttributedGraph aG = oriDyAG.get(i);
            if (i == 0) {
                for( Integer vId : aG.getAllVerticeId()) {
                    Map<Integer, List<Double>> attrMapVal = new HashMap<>();
                    vertexMapAttrMapVals.put(vId, attrMapVal);
                    Map<Integer, Double> attrMap = aG.getVertex(vId).getAttrDouMap();
                    for (int attrType : attrMap.keySet()) {
                        attrMapVal.put(attrType, new LinkedList<>());
                    }
                }
            }
            for (Integer vId : aG.getAllVerticeId()) {
                Map<Integer, Double> attrMapVal = aG.getVertex(vId).getAttrDouMap();
                for (Integer attrType: attrMapVal.keySet()) {
                    vertexMapAttrMapVals.get(vId).get(attrType).add(attrMapVal.get(attrType));
                }
            }
        }
    }


    private static void computeMeanStdDev() {
        for (int vId : vertexMapAttrMapVals.keySet()) {
            Map<Integer, List<Double>> attrMapVals = vertexMapAttrMapVals.get(vId);
            for (int attrType : attrMapVals.keySet()) {
                List<Double> vals = attrMapVals.get(attrType);
                double mean = 0, preVal = 0, stdDev = 0;
                int count = 0;
                for (double val : vals) {
                    count++;
                    mean += (val - preVal)/count;
                }
                for (double val : vals) {
                    stdDev += (val - mean) * (val - mean);
                }
                stdDev = Math.sqrt(stdDev/vals.size());
                vals.clear();
                vals.add(mean);
                vals.add(stdDev);
            }
        }
    }
    /**
     * This method derive item dynamic attributed graph from original dynamic attributed graph
     * @return resulting item dynamic attributed graph
     * @throws IOException
     */
    public static Map<Integer, ItemAttributedGraph> convertToItDyAGCase() throws IOException {
//        System.out.println("@@@ start to preprocess...");
        findEventTypeMapping();

        //construct dynamic item attributed graph using DyAG which indicate trend of evolution
        Map<Integer, AttributedGraph> oriDyAG = ReadGraph.readGraph();

        if (DISCRET_FLAG == 1) {
            acquireAllVals(oriDyAG);
            computeMeanStdDev();
        }

        Map<Integer, ItemAttributedGraph> tempDyAG = new HashMap<>();

        //for each position in DyAG, other than the last
        for (int i = 0; i < oriDyAG.size() - 1; i++) {
            //get 2 consecutive attributed graphs that are needed to find trend
            AttributedGraph aG1 = oriDyAG.get(i), aG2 = oriDyAG.get(i+1);
            //construct a map of vertex id -> event types
            Map<Integer, ItemVertex> vMap = new HashMap<>();
            //for each vertex
            for (int vId : aG1.getAllVerticeId()) {
                //get attribute maps for these 2 attributed graphs
                Map<Integer, Double> attrMap1 = aG1.getVertex(vId).getAttrDouMap();
                Map<Integer, Double> attrMap2 = aG2.getVertex(vId).getAttrDouMap();
                List<Integer> eventTypeList = new LinkedList<>();
                //for each attribute type
                for (int attrType : attrMap1.keySet()) {
                    //find trend of the values
                    double val1 = attrMap1.get(attrType);
                    double val2 = attrMap2.get(attrType);

                    //***************************** key position of preprocessing ******************************
                    int trendFlag = findTrendFlag(DISCRET_FLAG, vId, attrType, val1, val2);
                    if (trendFlag == PASS_FLAG)
                        continue;

                    String eventName = attrMapping.get(attrType) + trends[trendFlag];
                    int eventType = eventTypeMappingRe.get(eventName);
                    //add it to event type list of the vertex
                    eventTypeList.add(eventType);
                    //***************************************************************************************
                }
                ItemVertex iV = new ItemVertex(vId);
                iV.addItems(eventTypeList);
                vMap.put(vId, iV);
            }
            //construct a new item attributed graph using identifier, vertex map, edge map
            ItemAttributedGraph iAG = new ItemAttributedGraph(i, vMap, aG1.getEdgesMap());
            //add it to ItDyAG
            tempDyAG.put(i, iAG);
        }
//        System.out.println("preprocessing finish !");
        repeatGraph(tempDyAG);
//        System.out.println("repeating finish !");
        Map<Integer, ItemAttributedGraph> subItDyAG = new HashMap<>();

        for (int j = 0; j < tempDyAG.size(); j++) {
            subItDyAG.put(j, tempDyAG.get(j));
        }
        itDyAG = subItDyAG;
        return subItDyAG;
    }

    private static int findTrendFlag(int discretFlag, int vId, int attrType, double val1, double val2) {
        int trendFlag = PASS_FLAG;
        switch (discretFlag) {
            case 0: {
                double diff = val2 - val1;
                if (diff > 0) trendFlag = 2;
                else if (diff < 0) trendFlag = 0;
                return trendFlag;
            }
            case 1: {
                double diff = val2 - val1;
                double scale = ParametersSetting.SCALE;
                double stdDev = vertexMapAttrMapVals.get(vId).get(attrType).get(1);
                if (diff > 2  *  scale * stdDev) trendFlag = 4;
                else if (diff > scale * stdDev) trendFlag = 3;
                else if (diff < - 2  * scale * stdDev) trendFlag = 0;
                else if (diff < scale * stdDev) trendFlag = 1;
                else trendFlag = PASS_FLAG;
                return trendFlag;
            }
        }
        return 999;
    }



}
