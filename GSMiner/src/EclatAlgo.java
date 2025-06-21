import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class EclatAlgo {

    private static boolean writeToFile = false;

    // minimum initial support ensures that the support is larger than minInitSup in Eclat Algorithm
    static int minInitSup = 1;

    private static BufferedWriter bw;

    private static int count = 0;

    public static STPSet wholeSpace;

    private static String FRE_ITEMSET_PATH = ParametersSetting.FRE_ITEMSET_PATH;


    /**
     * This method find all frequent items and their supporting points
     * result are stored in class variable supSTPMapping
     * @throws IOException
     */
    private static Map<Integer, STPSet> findAllFrequentItems() throws IOException {

//        System.out.println("@@@ start to find all frequent items ...");
        // Associate frequent item with its supporting points
        Map<Integer, STPSet> supSTPMapping = new HashMap<>();

        wholeSpace = new STPSet();

        // Get event type mapping and item dynamic attributed graph by loading Preprocess class and running its method
        Map<Integer, ItemAttributedGraph> itemDyAG = Preprocess.convertToItDyAGCase();

        // For each timestamp in item dynamic attributed graph
        for (int timestamp : itemDyAG.keySet()) {
            // Get its corresponding item attributed graph
            ItemAttributedGraph itAG = itemDyAG.get(timestamp);
            // For each id of vertex in this attributed graph
            for (int vId : itAG.getAllVId()) {
                wholeSpace.addSTP(timestamp, vId);
                // For each event type in this vertex's items
                for (int eventType : itAG.getAllItems4V(vId)) {
                    // Get total set spatio-temporal supporting points, it is a object of class STPoint
                    STPSet stpSet =  supSTPMapping.get(eventType);
                    //supporting set of ST points maybe empty
                    //if empty, create a new one
                    if (stpSet == null) {
                        stpSet = new STPSet();
                        supSTPMapping.put(eventType, stpSet);
                    }
                    //add this new supporting point
                    stpSet.addSTP(timestamp, vId);
                }
            }
        }
//
//        // test if successfully remove infrequent event type
//        for (int eventType: supSTPMapping.keySet()) {
//            int sup = supSTPMapping.get(eventType).getSize();
//            System.out.println(eventType + " " + sup);
//        }

        //remove those item with support smaller than minSup in way of iteration
        Iterator<Integer> iter = supSTPMapping.keySet().iterator();
        while (iter.hasNext()) {
            int it = iter.next();
            int sup = supSTPMapping.get(it).getSize();
            if (sup <= minInitSup) {
                iter.remove();
            }
        }

//        // test if successfully remove infrequent event type
//        System.out.println();
//        System.out.println();
//        for (int eventType: supSTPMapping.keySet()) {
//            int sup = supSTPMapping.get(eventType).getSize();
//            System.out.println(eventType + " " + sup);
//        }

        System.out.println("find total " + supSTPMapping.size() + " frequent items");
        return supSTPMapping;

    }

    /**
     * This method find all frequent itemset with supporting points according to all frequent items with supporting points
     * @throws IOException
     */
    public static Map<Itemset, STPSet> extendFreItems() throws IOException {
//        s.println("@@@ start to find all frequent itemsets...");
        Map<Integer, STPSet> supSTPMapping = findAllFrequentItems();

        Map<Itemset, STPSet> itemsetSTPSetMap = new LinkedHashMap<>();

        if (writeToFile) {
            bw = new BufferedWriter(new FileWriter(FRE_ITEMSET_PATH));
        }

        //get frequent items and their support points
        List<Integer> frequentEvents = new ArrayList<>(supSTPMapping.size());
        List<STPSet> supPoints = new ArrayList<>(supSTPMapping.size());
        //sort items according to total increasing order of support
        //it is an important step for improving performance due to our processing method in recursive method
        //the item precessing first will explore most itemsets, thus precessing item with small support will generally
        //result in better pruning processing
        List<Map.Entry<Integer, STPSet>> list = new ArrayList<>(supSTPMapping.entrySet());

        Collections.sort(list, new Comparator<Map.Entry<Integer, STPSet>>() {
            @Override
            public int compare(Map.Entry<Integer, STPSet> o1, Map.Entry<Integer, STPSet> o2) {
                return supSTPMapping.get(o1.getKey()).getSize() - supSTPMapping.get(o2.getKey()).getSize();
            }
        });

        for (Map.Entry<Integer, STPSet> entry : list) {
//            System.out.println(entry.getKey() + " " + entry.getValue().getSize());
            frequentEvents.add(entry.getKey());
            supPoints.add(entry.getValue());
        }

//        for (int i = 0; i < frequentEvents.size(); i++) {
//            System.out.println(frequentEvents.get(i) + " " + supPoints.get(i).getSize());
//        }

        processProjectedPart(new LinkedList<Integer>(), frequentEvents, supPoints, itemsetSTPSetMap);

        if (bw != null) {
            bw.close();
        }

//        System.out.println("find total " + itemsetSTPSetMap.size() + " frequent itemsets");

        return itemsetSTPSetMap;

//        //record prefix used by projection
//        List<Integer> prefix = new LinkedList<>();
//        for (int i = 0; i < frequentEvents.size(); i++) {
//            //add frequent item i as prefix
//            prefix.add(i);
//            List<Integer> projectedFreItems = new LinkedList<>();
//            List<STPSet> projectedSupPoints = new LinkedList<>();
//            STPSet supPointsI = supPoints.get(i);
//            for (int j = i+1; j < frequentEvents.size(); j++) {
//                STPSet supPointsJ = supPoints.get(j);
//                STPSet supPointsIJ = supPointsI.intersect(supPointsJ);
//                if (supPointsIJ.getSize() > minSupRelative) {
//                    projectedFreItems.add(j);
//                    projectedSupPoints.add(supPointsIJ);
//                }
//                if (projectedFreItems.size() > 0) {
//                    processProjectedPart(prefix, projectedFreItems, projectedSupPoints);
//                }
//            }

//        }

    }

    private static void processProjectedPart(List<Integer> prefix, List<Integer> projectedFreItems, List<STPSet> projectedSupPoints, Map<Itemset, STPSet> itemsetSTPSetMap) throws IOException {

        for (int i = 0; i < projectedFreItems.size(); i++) {
            List<Integer> newPrefix = new LinkedList<>();
            newPrefix.addAll(prefix);
            Integer itemI = projectedFreItems.get(i);
            STPSet stpSetI = projectedSupPoints.get(i);
//            System.out.println(prefix + " " + itemI + " " +stpSetI);
            if (writeToFile) {
                savePattern(newPrefix, itemI, stpSetI);
            }
            else {
                itemsetSTPSetMap.put(new Itemset(newPrefix, itemI), stpSetI);
            }

            //create a set of projection with respect to item i
            List<Integer> itemsBasedOnI = new LinkedList<>();
            List<STPSet> supPointsBasedOnI = new LinkedList<>();

            for (int j = i+1; j < projectedFreItems.size(); j++) {
                Integer itemJ = projectedFreItems.get(j);
                STPSet stpSetJ = projectedSupPoints.get(j);

                STPSet stpSetIJ = stpSetI.intersect(stpSetJ);

                if (stpSetIJ.getSize() > minInitSup) {
                    itemsBasedOnI.add(itemJ);
                    supPointsBasedOnI.add(stpSetIJ);
                }
            }

            if (itemsBasedOnI.size() > 0) {
                newPrefix.add(itemI);
                processProjectedPart(newPrefix, itemsBasedOnI, supPointsBasedOnI, itemsetSTPSetMap);
            }
        }
    }

    private static void savePattern(List<Integer> prefix, Integer newItem, STPSet stpSet) throws IOException {
        count++;
        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(count).append("\n");
        sb.append("(");
        for (int item : prefix) {
            sb.append(item).append(",");
        }
        sb.append(newItem);
        sb.append(")\n");
        sb.append(stpSet);
        sb.append("\n");
        bw.write(sb.toString());
    }

    public static void main(String[] args) throws IOException {
//        findAllFrequentItems();
        extendFreItems();
    }
}
