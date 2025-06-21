import java.io.*;
import java.text.NumberFormat;
import java.util.*;

public class AlgoGSMiner {
	// minimum threshold for JS divergence
	private static double minJS = 0.5;

	private static final double LN2 = Math.log(2);

	// Smooth processing of small value to avoid KL -> infinite
	private static final double EPSILON = 1e-10;

	/**
	 * item dynamic attributed graph acquired from preprocessing of original dynamic
	 * attributed graph
	 */
	private Map<Integer, ItemAttributedGraph> timestampMapItemAG;
	/** total number of vertex in this timestampMapItemAG */
	private int totalNumVertex;

	private Map<Integer, String> eventTypeMapName;

	/** associate itemset with its supporting points */
	private Map<Itemset, STPSet> itemsetMapSTPSet;
	/** associate itemset with its support in whole space */
	private Map<Itemset, Double> itemsetMapSup;
	/** total number of frequent itemset */
	private int totalItemsetNum;

//    private static Itemset startItemset;

	/** partitioning itemsets according to size */
	private static Map<Integer, Set<Itemset>> levelMapItemsets;
	/** store direct superset for each itemset */
	private static Map<Itemset, List<Itemset>> itemsetMapSuperset;
	/** minimal support among all supersets */
	private static Map<Itemset, Double> itemsetMapMinSup;

	/** path of file storing mined patterns */
//    private static String PATTERN_PATH = ParametersSetting.PATTERN_PATH;
	private static BufferedWriter bw;

	/** user specified minimal significance */
//    private static double SIGNIFICANCE = ParametersSetting.MIN_SIGNIFICANCE;
	/**
	 * user specified minimal support of tail itemset allowing to be extended
	 * further
	 */
//    private static int MIN_TAIL_SUP = ParametersSetting.MIN_TAIL_SUP;

	/**
	 * indicate whether overlapping of neighboring space is allowed, generally
	 * speaking, allowing overlapping will overestimate significance
	 */
//    private static boolean ALLOW_OVERLAPPING = ParametersSetting.ALLOW_OVERLAPPING;

	/** indicate whether to interpret sequence */
	private boolean INTERPRET_RESULT = true;
    private static boolean EXHIBIT_SUPPORTING_POINTS = false;

	/** mapping from vertex index to real entity name */
	private Map<Integer, String> vertexMapName;
//    private static String VERTEX_MAP_NAME_PATH = ParametersSetting.VERTEX_MAP_NAME_PATH;

	/**
	 * parameters for testing efficiency of pruning and different traversal methods
	 */
//    public static boolean ADOPT_PRUNING = ParametersSetting.ADOPTING_PRUNING;

	/** associate itemset with its supersets that are dominated by it */
	private static Map<Itemset, Set<Itemset>> itemsetMapDominantsSuperset;

	/**
	 * this flag specify traversal behavior case 1: complete DFS case 2: process
	 * outer and inner separately, outer DFS, inner BFS case 3: process outer and
	 * inner separately, outer DFS, inner DFS
	 */
//    private static int TRAVERSAL_FLAG = ParametersSetting.TRAVERSAL_FLAG;

	private long totalMiningTime = 0;

	private int patternCount = 0;

	private static NumberFormat nf = NumberFormat.getInstance();

	private double prefixSup = 0;

	static {
		nf.setMaximumFractionDigits(5); // set the maximum number of decimal places to 5
	}


	/**
	 * Run the algorithm
	 * 
	 * @param inputDirectory          the input directory
	 * @param outputPath              the path of the output file

	 * @throws IOException if an error reading or writing files
	 */
	public void runAlgorithm(String inputDirectory, String outputPath) throws IOException {
		// Save parameters
		ParametersSetting.EDGE_FILE_PATH = inputDirectory + "graph.txt";
		ParametersSetting.ATTRI_MAPPING_PATH = inputDirectory + "attributes_mapping.txt";
//		System.out.println(ParametersSetting.ATTRI_MAPPING_PATH);
		ParametersSetting.ATTR_FILE_PATH = inputDirectory + "attributes.txt";
		ParametersSetting.VERTEX_MAP_NAME_PATH = inputDirectory + "vertices_mapping.txt";

		ParametersSetting.PATTERN_PATH = outputPath;

//        System.out.println("\nstart to run sequence miner algorithm...\n");
		MemoryLogger.getInstance().reset();
		MemoryLogger.getInstance().checkMemory();
//        System.out.println("memory usage after preprocessing: " + MemoryLogger.getInstance().getMaxMemory());

		long t1 = System.currentTimeMillis();
		// get all frequent itemset and their support points

		readFrequentItemsetFromEclat();
		// get item dynamic attributed graph

		getItemDyAGFromPreprocessing();
		// compute

		getBasicMappings();
		// construct search structure for DFS
		getItemsetMapDominants();

		long t2 = System.currentTimeMillis();
//        System.out.println("*************************** time for preparation: " + (t2 - t1) + "ms");
		ParametersSetting.PREPARE = t2 - t1;

		// create an object for writing to file
		File patternPathFile = new File(ParametersSetting.PATTERN_PATH);
		if (!patternPathFile.exists())
			patternPathFile.createNewFile();
		System.out.println(ParametersSetting.PATTERN_PATH);
		bw = new BufferedWriter(new FileWriter(ParametersSetting.PATTERN_PATH));
		String head = "sequence --- (conditional) probability --- tail JS divergence\n";
		bw.write(head);

		MemoryLogger.getInstance().checkMemory();
//        System.out.println("memory usage after all preparation: " + MemoryLogger.getInstance().getMaxMemory());

		seqMinerDFS();

		bw.close();

		totalMiningTime = System.currentTimeMillis() - t2;

//        System.out.println("total " + patternCount + " patterns are found");
//        patternCount = 0;
	}

	/**
	 * This method apply DFS completely to mine sequential patterns. Main part here
	 * is to prepare initial objects for recursive method seqMinerDFSHelper.
	 * @throws IOException
	 */
	private void seqMinerDFS() throws IOException {
		// this list record a sequence of itemset that is based to form sequential pattern
		// the list can be reused by removing last itemset after returning from deeper step
		// because we only append an itemset when going to deeper step
		List<Itemset> prefix = new LinkedList<>();
		List<STPSet> prefixSTPSet = new LinkedList<>();
		List<Double> prefixProbList = new LinkedList<>();
		// this part can be written in a more consistent way at cost of little loss of
		// efficiency
		// for each frequent itemset
//        System.out.println(itemsetMapSTPSet.size());
		for (Map.Entry<Itemset, STPSet> entry : itemsetMapSTPSet.entrySet()) {
			// find its supporting points
			Itemset itemset = entry.getKey();
			STPSet stpSet = entry.getValue();
			double p = itemsetMapSup.get(itemset);
			// compute neighboring space
			List<STPSet> neighboringPointsSet = acquireNeighboringPointsSet(stpSet);
			prefix.add(itemset);
			prefixSTPSet.add(itemsetMapSTPSet.get(itemset));
			prefixProbList.add(p);
			// for each itemset of size 1
			for (Itemset nextItemset : levelMapItemsets.get(1)) {
				// modify prefix and go deeper
				prefixSup = itemsetMapSup.get(prefix.get(0));
				seqMinerDFSHelper(prefix, prefixSTPSet, prefixProbList, nextItemset, neighboringPointsSet);
			}
			prefix.remove(prefix.size() - 1);
			prefixSTPSet.remove(prefixSTPSet.size() - 1);
			prefixProbList.remove(prefixProbList.size() - 1);
		}
	}

	/**
	 * calculation for ALLCONF
	 * This recursive method process one itemset at a time given prefix of itemset
	 * and projected space This function is called when we want to go deeper(next
	 * step). This case we need to append itemset to current prefix. This function
	 * is called also when we want to go broader. This case we do not modify prefix.
	 *
	 * @param prefix       a sequence of itemset
	 * @param itemset      itemset to be investigated in this part
	 * @param prefixProbList sequence of (conditional) probability
	 * @param neighborings range of space determined by tail itemset of prefix and
	 *                     within it we investigate current itemset
	 * @throws IOException
	 */
	private void seqMinerDFSHelper(List<Itemset> prefix, List<STPSet> prefixSTPSet, List<Double> prefixProbList,
			Itemset itemset, List<STPSet> neighborings) throws IOException {
		// Check memory usage
		MemoryLogger.getInstance().checkMemory();
//        System.out.println(MemoryLogger.getInstance().getMaxMemory());

		// Get the probability in the whole space
		double p1 = itemsetMapSup.get(prefix.get(prefix.size()-1));
		double p2 = itemsetMapSup.get(itemset);

		// This list is just used to pass STPSet object, so it always contains only one item
		List<STPSet> candidateList = new LinkedList<>();

		// Find the conditional probability
		double q2 = computeConditionalP(neighborings, itemset, candidateList);

		STPSet candidateSTP = candidateList.get(0);

		// Find the last (conditional) probability
		double q1 = prefixProbList.get(prefixProbList.size()-1);

		// calculate the distribution of the next itemset
		double[] p2D = {p2, 1 - p2};
		double[] q2D = {q2, 1 - q2};

		double js = jsDivergence(p2D,q2D);

		if (q2 >= q1) {
			// entropyGain(p2D,q2D) >= 0 &&
			if (entropyGain(p2D,q2D) >= 0 && js >= minJS) {
				if (ParametersSetting.OUTPUT_PATTERNS) {
					saveSequencePattern(prefix, prefixSTPSet, prefixProbList, itemset, q2, candidateSTP, js);
					patternCount++;
//					if (patternCount > 1000000) {
//						System.out.println("Warning: Pattern count exceeded 1,000,000.");
//					}
				}

				// Then we modify prefix and go deeper
				prefix.add(itemset);
				prefixSTPSet.add(candidateSTP);
				prefixProbList.add(q2);
				List<STPSet> nextNeighbors = acquireNeighboringPointsSet(candidateSTP);

				// All itemset of size 1 is enough for us to find all itemsets in next step
				for (Itemset nextItemset : levelMapItemsets.get(1)) {
					// This function call will go to next step
					seqMinerDFSHelper(prefix, prefixSTPSet, prefixProbList, nextItemset, nextNeighbors);
				}
				prefix.remove(prefix.size() - 1);
				prefixProbList.remove(prefixProbList.size() - 1);
				prefixSTPSet.remove(prefixSTPSet.size() - 1);
			}


			for (Itemset nextItemsetSameStep : itemsetMapDominantsSuperset.get(itemset)) {
				// This function call will not go to next step, it just investigates dominated itemsets at this step
				// Here we see function call that go deeper is before that go broader
				// That we prefer to go deeper illustrate it is a DFS
				seqMinerDFSHelper(prefix, prefixSTPSet, prefixProbList, nextItemsetSameStep, neighborings);
			}

		}
		// Check memory
		MemoryLogger.getInstance().checkMemory();
	}



	/**
	 * This method is used to compute conditional probalility and lower bound
	 * space
	 * 
	 * @param neiboringSTPSetList donote neighboring points. if not allow
	 *                            overlapping, it is just a STPSet object rather
	 *                            than a list.
	 * @param itemset             the itemset to be compute support ratio
	 * @param candidateSTPSetList the list supporting points set
	 * @return average local support ratio
	 */
	private double computeConditionalP(List<STPSet> neiboringSTPSetList, Itemset itemset,
										 List<STPSet> candidateSTPSetList) {
		STPSet stpSet = itemsetMapSTPSet.get(itemset);

		List<STPSet> tempSTPSetList = new LinkedList<>();
		// Sum up all support ratio
		double totalLocalSupRaio = 0;
		// For each set of neighboring points(neighboring space)
		for (STPSet neibor : neiboringSTPSetList) {
			// Size of single neighboring size
			int neiboringSize = neibor.getSize();
			// Compute intersection of this neighboring space and the itemset's set of
			// supporting points
			STPSet tempSTPSet = neibor.intersect(stpSet);
			// Record this possible tail points
			tempSTPSetList.add(tempSTPSet);
			// Compute support ratio in this neighboring space
			int localSup = tempSTPSet.getSize();
			if (neiboringSize != 0) {
				double localSupRatio = (double) localSup / neiboringSize;
//                        System.out.println(localSup + "!!!!!!!!!!!!!" + neiboringSize + "!!!!!!!!!" + localSupRatio);
				totalLocalSupRaio += localSupRatio;
			}
		}
		candidateSTPSetList.add(STPSet.mergeSTPSets(tempSTPSetList));
		// compute average support ratio in all neighboring space
//		double avgLocalSupRaio = ;
		return totalLocalSupRaio / neiboringSTPSetList.size();
	}

	/**
	 * This method is used to acquire neighboring points given a point set
	 * 
	 * @param stpSet based point set
	 * @return all neighboring point set
	 */
	private List<STPSet> acquireNeighboringPointsSet(STPSet stpSet) {
		// Constructing a set of neighboring space for this new itemset according to its
		// tail points
		List<STPSet> neighboringSTPSetList = new LinkedList<>();

		// For each spatio-temporal point, find a set of neighboring points
		for (int timestamp : stpSet.getTimestamps()) {
			for (int vId : stpSet.getVIdSet4Timestamp(timestamp)) {
				STPSet neighborSTPSet = null;
				if (ParametersSetting.NEIGHBOR_FLAG == 0) {
					neighborSTPSet = findNeighbors0(timestamp, vId);
				} else if (ParametersSetting.NEIGHBOR_FLAG == 1) {
					neighborSTPSet = findNeighbors1(timestamp, vId);
				}
				neighboringSTPSetList.add(neighborSTPSet);
			}
		}
		if (!ParametersSetting.ALLOW_OVERLAPPING) {
			// if we do not allow repeated appearance of neighboring points,
			// a merge process must be performed.
			List<STPSet> tempSTPSet = new LinkedList<>();
			tempSTPSet.add(STPSet.mergeSTPSets(neighboringSTPSetList));
			neighboringSTPSetList = tempSTPSet;
		}
		return neighboringSTPSetList;
	}

	/**
	 * This method define what neighborhood is and find neighbors for a vertex with
	 * specified timestamp and id
	 * 
	 * @param timestamp used to located itemAG in timestampMapItemAG
	 * @param vId       used to located vertex in an itemAG
	 * @return a set of neighboring points
	 */
	private STPSet findNeighbors0(int timestamp, int vId) {
		List<Integer> nextTimestamps = new LinkedList<>();
		List<Set<Integer>> vIdSets = new LinkedList<>();
		ItemAttributedGraph itemAG = timestampMapItemAG.get(timestamp);
		nextTimestamps.add(timestamp + 1);
//        nextTimestamps.add(timestamp + 2);
		Set<Integer> neighbors = itemAG.findAllNeighbors4V(vId);

		vIdSets.add(neighbors);
		return new STPSet(nextTimestamps, vIdSets);
	}

	private STPSet findNeighbors1(int timestamp, int vId) {
		List<Integer> nextTimestamps = new LinkedList<>();
		List<Set<Integer>> vIdSets = new LinkedList<>();
		nextTimestamps.add(timestamp + 1);
		Set<Integer> neighbors = new HashSet<>();
		neighbors.add(vId);
		vIdSets.add(neighbors);
		return new STPSet(nextTimestamps, vIdSets);
	}

	/**
	 * This method find mapping that map itemset to superset that are larger than
	 * it.
	 */
	private static void getItemsetMapDominantsSuperset() {
		itemsetMapDominantsSuperset = new HashMap<>();
		for (int i = 1; i <= levelMapItemsets.keySet().size(); i++) {
			for (Itemset itemset : levelMapItemsets.get(i)) {
				Set<Itemset> dominantSupersets = new HashSet<>();
				itemsetMapDominantsSuperset.put(itemset, dominantSupersets);
				for (Itemset superset : itemsetMapSuperset.get(itemset)) {
					if (superset.compareTo(itemset) > 0) {
						dominantSupersets.add(superset);
					}
				}
			}
		}
//        wholeSpace = EclatAlgo.wholeSpace;
//        Set<Itemset> startDominants = new HashSet<>();
//        startItemset = new Itemset(Integer.MAX_VALUE);
//        startDominants.addAll(levelMapItemsets.get(1));
//        itemsetMapDominantsSuperset.put(startItemset, startDominants);
	}

	/**
	 * This method complete 3 tasks. These 3 mappings will always be used
	 * whatever traversal method you use and whether you use optimization or not
	 * first is constructing mapping from size of itemset -> all itemsets with that size
	 * second is constructing mapping from itemset -> all its supersets
	 * third is constructing mapping from itemset -> its relative support
	 */
	private void getBasicMappings() {
		// partitioning itemsets according to size
		// so that we can quickly find itemsets of certain size
		levelMapItemsets = new HashMap<>();
		for (Itemset itemset : itemsetMapSTPSet.keySet()) {
			int size = itemset.getSize();
			Set<Itemset> itemsets = levelMapItemsets.get(size);
			if (itemsets == null) {
				itemsets = new HashSet<>();
				levelMapItemsets.put(size, itemsets);
			}
			itemsets.add(itemset);
		}

		// find for each itemset its direct superset
		itemsetMapSuperset = new HashMap<>();
		for (int i = 1; i <= levelMapItemsets.size(); i++) {
			for (Itemset itemset1 : levelMapItemsets.get(i)) {
				itemsetMapSuperset.put(itemset1, new LinkedList<>());
				Set<Itemset> itemset2List = levelMapItemsets.get(i + 1);
				if (itemset2List != null) {
					for (Itemset itemset2 : itemset2List) {
						if (itemset2.isSupersetOf(itemset1)) {
							itemsetMapSuperset.get(itemset1).add(itemset2);
						}
					}
				}
			}
		}

		itemsetMapSup = new HashMap<>();
		for (int i = levelMapItemsets.size(); i > 0; i--) {
			for (Itemset itemset : levelMapItemsets.get(i)) {
				int sup = itemsetMapSTPSet.get(itemset).getSize();
				itemsetMapSup.put(itemset, (double) sup / totalNumVertex);
			}
		}

	}

	private void getItemsetMapDominants() {
		// if we plan to use DFS, then a mapping of itemset -> dominating supersets is
		// needed
		if (ParametersSetting.TRAVERSAL_FLAG == 0 || ParametersSetting.TRAVERSAL_FLAG == 1) {
			getItemsetMapDominantsSuperset();
		}

		// if we plan to adopt pruning technique
//		if (ParametersSetting.ADOPTING_PRUNING) {
//			// we need a mapping of itemset -> superMinSup
//			itemsetMapMinSup = new HashMap<>();
//			// if we do not adopt a specific optimization
//			if (ParametersSetting.LARGE_GRAINED_PRUNING) {
//				// we just acquire a general upper bound that may not so efficient for specific
//				// case
//				getItemsetMapSuperMinSlack();
//			}
//			// compute a search strategy based upper bound
//			else {
//				// this is for case of DFS
//				if (ParametersSetting.TRAVERSAL_FLAG == 0 || ParametersSetting.TRAVERSAL_FLAG == 1) {
//					getItemsetMapSuperMinTight4DFS();
//				}
//				// this for BFS
//				else {
//					getItemsetMapSuperMinTight4BFS();
//				}
//			}
//		}
	}

	private void getItemsetMapSuperMinTight4BFS() {
		for (int level = 1; level <= levelMapItemsets.size(); level++) {
			for (Itemset itemset : levelMapItemsets.get(level)) {
				double supRatioMin = itemsetMapSup.get(itemset);
				for (Itemset superset : itemsetMapSuperset.get(itemset)) {
					double superSup = itemsetMapSup.get(superset);
					if (superSup < supRatioMin) {
						supRatioMin = superSup;
					}
				}
				itemsetMapMinSup.put(itemset, supRatioMin);
			}
		}
	}

	private void getItemsetMapSuperMinTight4DFS() {
		int highestLevel = levelMapItemsets.size();
		for (Itemset itemset : levelMapItemsets.get(highestLevel)) {
			itemsetMapMinSup.put(itemset, itemsetMapSup.get(itemset));
		}
		for (int level = highestLevel - 1; level > 0; level--) {
			for (Itemset itemset : levelMapItemsets.get(level)) {
				double supRatioMin = itemsetMapSup.get(itemset);
				for (Itemset superset : itemsetMapDominantsSuperset.get(itemset)) {
					double superSup = itemsetMapMinSup.get(superset);
					if (superSup < supRatioMin) {
						supRatioMin = superSup;
					}
				}
				itemsetMapMinSup.put(itemset, supRatioMin);
			}
		}
	}

	private void getItemsetMapSuperMinSlack() {
		for (int i = levelMapItemsets.size(); i > 0; i--) {
			for (Itemset itemset : levelMapItemsets.get(i)) {
				double supRatioMin = itemsetMapSup.get(itemset);
				for (Itemset superset : itemsetMapSuperset.get(itemset)) {
					double superSup = itemsetMapMinSup.get(superset);
					if (superSup < supRatioMin)
						supRatioMin = superSup;
				}
				itemsetMapMinSup.put(itemset, supRatioMin);
			}
		}
	}

	/**
	 * This method just get timestampMapItemAG and event type mapping from
	 * preprocessing
	 * 
	 * @throws IOException
	 */
	private void getItemDyAGFromPreprocessing() throws IOException {
		timestampMapItemAG = Preprocess.itDyAG;

//        System.out.println("sequence miner input: timestamps-" + timestampMapItemAG.keySet().size() + "   vertex-" + timestampMapItemAG.get(0).getTotalSize());
		totalNumVertex = timestampMapItemAG.get(0).getTotalSize() * timestampMapItemAG.size();
		eventTypeMapName = Preprocess.eventTypeMapping;
		if (ParametersSetting.EXHIBIT_SUPPORTING_POINTS)
			getVerterMapName();
	}

	private void getVerterMapName() throws IOException {
		vertexMapName = new HashMap<>();
//        System.out.println(ParametersSetting.VERTEX_MAP_NAME_PATH);
		File vertexMapNameFile = new File(ParametersSetting.VERTEX_MAP_NAME_PATH);
		if (!vertexMapNameFile.exists())
			vertexMapNameFile.createNewFile();
		BufferedReader br = new BufferedReader(new FileReader(ParametersSetting.VERTEX_MAP_NAME_PATH));
		String line = null;
		while ((line = br.readLine()) != null) {
			try {
				String[] items = line.split(",");
				int vId = Integer.parseInt(items[0]);
				String name = items[1];
				vertexMapName.put(vId, name);
			} catch (ArrayIndexOutOfBoundsException e) {
				System.out.println(line);
			}
		}
		br.close();
	}

	/**
	 * This method get frequent itemset and their supporting points from static
	 * method of class EclatAlgo, not from file
	 * 
	 * @throws IOException
	 */
	private void readFrequentItemsetFromEclat() throws IOException {
		itemsetMapSTPSet = EclatAlgo.extendFreItems();
		totalItemsetNum = itemsetMapSTPSet.size();
//        System.out.println("load all frequent itemsets and their supporting points");
		System.out.println("total " + totalItemsetNum + " frequent itemsets");

//        print all itemsets and their support
//        System.out.println(itemsetMapSTPSet.size());
//        for (Itemset itemset: itemsetMapSTPSet.keySet()) {
//            System.out.println(itemset + "\n" +itemsetMapSTPSet.get(itemset));
//        }
	}

	/**
	 * This method save all frequent patterns to file
	 * 
	 * @param prefix        sequence of itemset which is used to extend
	 * @param prefixSTPSet  sequence of STPSet(support points) corresponding to
	 *                      prefix itemset
	 * @param prefixProbList sequence of (conditional) probability
	 * @param newItemset    new added itemset
	 * @param newConditional  new itemset conditional probability
	 * @param tailSTPSet    support of newItemset in neighboring space of prefix
	 * @param js        Jensen-Shannon entropy
	 * @throws IOException
	 */
	private void saveSequencePattern(List<Itemset> prefix, List<STPSet> prefixSTPSet, List<Double> prefixProbList,
			Itemset newItemset, double newConditional, STPSet tailSTPSet, double js) throws IOException {
		StringBuilder sb = new StringBuilder();
		sb.append("\n");
		sb.append("{");
		if (INTERPRET_RESULT) {
			for (Itemset itemset : prefix) {
				sb.append(interpretItemset(itemset)).append(",");
			}
			sb.append(interpretItemset(newItemset));
		} else {
			for (Itemset itemset : prefix) {
				sb.append(itemset).append(",");
			}
			sb.append(newItemset);
		}
		sb.append("} {");
//		for (STPSet stpSet : prefixSTPSet) {
//			sb.append(stpSet.getSize()).append(",");
//		}
//		sb.append(tailSTPSet.getSize());
//		sb.append("} {");
		for (int i = 0; i < prefixProbList.size(); i++) {
			sb.append(nf.format(prefixProbList.get(i))).append(",");
		}
		sb.append(nf.format(newConditional));
		sb.append("} {");
		sb.append(nf.format(js));
		sb.append("}\n");
		if (ParametersSetting.EXHIBIT_SUPPORTING_POINTS) {
			for (STPSet stpSet : prefixSTPSet) {
				for (Integer timestamp : stpSet.getTimestamps()) {
					for (Integer vId : stpSet.getVIdSet4Timestamp(timestamp)) {
//						sb.append("(").append(timestamp).append(",").append(vertexMapName.get(vId)).append(") ");
						sb.append("(").append(timestamp).append(",").append(vId).append(") ");
					}
				}
				sb.append("\n");
			}
			for (Integer timestamp : tailSTPSet.getTimestamps()) {
				for (Integer vId : tailSTPSet.getVIdSet4Timestamp(timestamp)) {
					sb.append("(").append(timestamp).append(",").append(vId).append(") ");
				}
			}
			sb.append("\n");
		}
		bw.write(sb.toString());
	}

	/**
	 * This method convert item denoted by Integer to String
	 * 
	 * @param itemset the itemset whose item will be decoded
	 * @return Itemset indicating real meaning
	 */
	private String interpretItemset(Itemset itemset) {
		StringBuilder sb = new StringBuilder();
		sb.append("(");
		for (Integer item : itemset.getItems()) {
			sb.append(eventTypeMapName.get(item)).append(",");
		}
		sb.deleteCharAt(sb.length() - 1);
		sb.append(")");
		return sb.toString();
	}

	/**
	 * This method read all frequent itemset and their supporting points into memory
	 * from intermediate file
	 * 
	 * @throws IOException
	 */
	private void readFrequentItemsetFromFile() throws IOException {
		EclatAlgo.extendFreItems();

		String freqItemsetPath = "dataset/DBLP/result/frequent_itemset.txt";
		BufferedReader br = new BufferedReader(new FileReader(freqItemsetPath));

		// use list to store frequent itemset, support and supporting points
		itemsetMapSTPSet = new LinkedHashMap<>();

		String line = br.readLine();
		while (line != null) {
			// when we find a line start with "#", we find a frequent itemset.
			// next is information about components, support, and supporting points
			if (line.startsWith("#")) {
				line = br.readLine();
				// get information about components and support from this line
				Itemset itemset = new Itemset(line);
				br.readLine();
				br.readLine();
				// get information about supporting points from next lines
				// use timestamp list and list of supporting point set to create a new STPSet
				// object
				List<Integer> timestampList = new LinkedList<>();
				List<Set<Integer>> vIdSetList = new LinkedList<>();
				while ((line = br.readLine()).startsWith("[")) {
					String[] splitItems = line.split(" ");
					timestampList.add(Integer.parseInt(splitItems[0].substring(1, splitItems[0].length() - 1)));
					Set<Integer> vIdSet = new HashSet<>();
					String idStr = splitItems[1].substring(1, splitItems[1].length() - 1);
					for (String str : idStr.split(",")) {
						vIdSet.add(Integer.parseInt(str));
					}
					vIdSetList.add(vIdSet);
				}
				itemsetMapSTPSet.put(itemset, new STPSet(timestampList, vIdSetList));
				totalItemsetNum++;
			} else {
				line = br.readLine();
			}
		}

//        System.out.println("load all frequent itemsets and their supporting points");
//        System.out.println("total " + totalItemsetNum + " frequent itemsets");
//        for (Itemset itemset: itemsetMapSTPSet.keySet()) {
//            System.out.println(itemset + "\n" + itemsetMapSTPSet.get(itemset));
//        }
	}

	public static double log2(double x) {
		return Math.log(x) / LN2;
	}

	// KL divergence
	public static double klDivergence(double[] p, double[] q) {
		double divergence = 0.0;
		for (int i = 0; i < p.length; i++) {
			// Smooth process to ensure that q[i] is not 0
			double qSmoothed = q[i] + EPSILON;
			if (p[i] > 0) {
				divergence += p[i] * log2(p[i] / qSmoothed);
			}
		}
		return divergence;
	}

	// Jensen-Shannon Divergence
	public static double jsDivergence(double[] p, double[] q) {
		// 计算平均分布 M
		double[] m = new double[p.length];
		for (int i = 0; i < p.length; i++) {
			m[i] = 0.5 * (p[i] + q[i]);
		}

		// 计算 KL 散度
		double klPM = klDivergence(p, m);
		double klQM = klDivergence(q, m);

		// 返回 JSD
		return 0.5 * (klPM + klQM);
	}

	public static double entropy(double[] p) {
		if (p[0] == 0.0 || p[0] == 1.0) {
			return 0.0;
		}
		return -p[0] * log2(p[0]) - p[1] * log2(p[1]);
	}

	public static double entropyGain(double[] p, double[] q) {
		if (p.length != q.length) {
			throw new IllegalArgumentException("Input arrays must have the same length");
		}

		double gain = entropy(p)-entropy(q);
		return gain;
	}
	public void printStats() {
		long runtime = ParametersSetting.PREPARE + totalMiningTime;
		System.out.println("=============  GSMiner v2.40 - STATS =============");
		System.out.println(" Time to prepare the data: " + ParametersSetting.PREPARE + " ms");
		System.out.println(" Time to mine patterns from data: " + totalMiningTime + " ms");
		System.out.println(" Time to run the procedure: "+ runtime + " ms");
		System.out.println(" Maximum memory usage : " + MemoryLogger.getInstance().getMaxMemory() + " MB");
		System.out.println(" Pattern count: " + patternCount);
		System.out.println("====================================================");
	}
}
