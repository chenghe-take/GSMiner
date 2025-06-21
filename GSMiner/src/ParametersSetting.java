public class ParametersSetting {

    /** This flag specify different dataset
     *  case 0: DBLP
     *  case 1: USA flight
     */
    public static int TASK_FLAG = 0;
    public static String projectPath = System.getProperty("user.dir");

    /** threshold for determine increase/decrase (used in process of naive dicretization)*/
    public static double INCRE_THRESHOLD;

    //input and output file path
    /** path of file recording attribute mapping */
    public static String ATTRI_MAPPING_PATH;
    /** path of file describing attributes of vertices*/
    public static String ATTR_FILE_PATH;
    /** path of file describing edges of vertices */
    public static String EDGE_FILE_PATH;
    /** path of file recording mined patterns */
    public static String PATTERN_PATH;
    /** specify total number attribute when reading attribute file (depend on dataset) */
    public static String EVENTTYPE_MAPPING_PATH;

    public static String TRANSACTION_PATH;
    public static String FRE_ITEMSET_PATH;
    public static String VERTEX_MAP_NAME_PATH;


    //following parameters are common for different datasets
    /** This flag indicate if we allow overlapping when compute neighboring space.
     * Generally, sequence will have larger threshold when allowing overlapping.
     */
    public static boolean ALLOW_OVERLAPPING = false;

    /** This flag indicate if we will use pruning technique */
    public static boolean ADOPTING_PRUNING = true;

    /** this flag specify traversal behavior
     * case 0: complete DFS
     * case 1: process outer and inner separately, outer DFS, inner BFS
     * case 2: process outer and inner separately, outer DFS, inner DFS
     */
    public static int TRAVERSAL_FLAG = 0;

    public static boolean LARGE_GRAINED_PRUNING = true;
    public static boolean MINI_GRAINED_PRUNING = true;

    public static boolean EXHIBIT_SUPPORTING_POINTS = false;

    public static boolean OUTPUT_PATTERNS = true;

    /** This flag specify discretization strategy
     * case 0: '-', '0', '+'
     * case 1: '--', '-', '0', '+', '++'
     * GSMiner only considers case 0
     */
    public static int DISCRE_FLAG = 0;

    /** This parameter specify the number of repetition of data*/
    public static int REPEAT = 1;

    /** Runtime of preparation (load graph, preprocess)*/
    public static long PREPARE;
    public static double SCALE = 0.5;

    /** This flag indicate what neighboring relationship we will use
     * case 0: for p=(t,id), if point (t,id1) is connected with p at time t, then (t+1,id1) is a neighbor.
     * case 1: for p=(t,id), point (t+1,id) is a neighbor.
     */
    public static int NEIGHBOR_FLAG = 0;

}
