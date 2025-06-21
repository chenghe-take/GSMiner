import java.io.*;
import java.util.*;
import java.util.regex.*;

// transform the output sequence into .dot file
public class GraphToDot {
    private static final String GRAPH_PATH = "Data/USFlight/graph.txt";
    private static final String OUTPUT_PATH = "output.txt";
    private static final String DOT_PATH = "vizgraph.dot";

    // Store graph structure: Time -> Vertex -> Neighbor set
    private static final Map<Integer, Map<Integer, Set<Integer>>> graph = new HashMap<>();
    // Store rules: Each rule contains attribute labels and a list of vertices grouped by parentheses
    private static final List<Rule> rules = new ArrayList<>();

    public static void main(String[] args) {
        parseGraphFile();
        parseOutputFile();
        generateDotFile();
    }

    // read graph.txt
    private static void parseGraphFile() {
        try (BufferedReader br = new BufferedReader(new FileReader(GRAPH_PATH))) {
            String line;
            Integer currentTime = null;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("T")) {
                    currentTime = Integer.parseInt(line.substring(1));
                    graph.putIfAbsent(currentTime, new HashMap<>());
                } else if (!line.isEmpty() && currentTime != null) {
                    String[] parts = line.split("\\s+");
                    if (parts.length == 0) continue;
                    int node = Integer.parseInt(parts[0]);
                    Set<Integer> neighbors = new HashSet<>();
                    for (int i = 1; i < parts.length; i++) {
                        int neighbor = Integer.parseInt(parts[i]);
                        neighbors.add(neighbor);
                        // Undirected graph, bidirectional addition
                        graph.get(currentTime).computeIfAbsent(neighbor, k -> new HashSet<>()).add(node);
                    }
                    graph.get(currentTime).put(node, neighbors);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // read output.txt
    private static void parseOutputFile() {
        try (BufferedReader br = new BufferedReader(new FileReader(OUTPUT_PATH))) {
            String line;
            Rule currentRule = null;
            List<List<TimeNode>> currentGroups = null;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("{")) {
                    if (currentRule != null) {
                        rules.add(currentRule);
                    }
                    Matcher m = Pattern.compile("\\{([^}]+)\\}").matcher(line);
                    if (m.find()) {
                        String[] labels = m.group(1).split("\\),\\s*\\(");
                        int groupCount = labels.length;
                        currentGroups = new ArrayList<>(groupCount);
                        for (int i = 0; i < groupCount; i++) {
                            currentGroups.add(new ArrayList<>());
                        }
                        currentRule = new Rule(labels[0], currentGroups);
                    }
                } else if (line.startsWith("(")) {
                    if (currentGroups == null) continue;
                    String[] entries = line.split("\\)\\s*\\(");
                    for (int i = 0; i < entries.length; i++) {
                        String entry = entries[i].replaceAll("[()]", "");
                        String[] parts = entry.split(",");
                        if (parts.length != 2) continue;
                        int t = Integer.parseInt(parts[0].trim());
                        int node = Integer.parseInt(parts[1].trim());
                        if (i < currentGroups.size()) {
                            currentGroups.get(i).add(new TimeNode(t, node));
                        }
                    }
                }
            }
            if (currentRule != null) {
                rules.add(currentRule);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // generate .dot
    private static void generateDotFile() {
        try (PrintWriter writer = new PrintWriter(DOT_PATH)) {
            writer.println("digraph G {");
            writer.println("  edge [dir=none, color=black, style=solid];");
            writer.println("  node [color=black, shape=circle];");

            // Generate subgraphs at all times (black vertices and edges)
            for (Integer time : graph.keySet()) {
                writer.printf("  subgraph cluster_T%d {%n", time);
                writer.printf("    label=\"T%d\";%n", time);
                Map<Integer, Set<Integer>> nodes = graph.get(time);
                for (Integer node : nodes.keySet()) {
                    writer.printf("    T%d_%d;%n", time, node);
                }
                for (Map.Entry<Integer, Set<Integer>> entry : nodes.entrySet()) {
                    int from = entry.getKey();
                    for (int to : entry.getValue()) {
                        if (from <= to) { // Avoid duplicate edges
                            writer.printf("    T%d_%d -> T%d_%d;%n", time, from, time, to);
                        }
                    }
                }
                writer.println("  }");
            }

            // rule: red vertices and blue dashed arrows
            writer.println("  edge [dir=forward, color=blue, style=dashed];");
            for (Rule rule : rules) {
                List<List<TimeNode>> groups = rule.groups;
                for (int i = 0; i < groups.size() - 1; i++) {
                    List<TimeNode> currentGroup = groups.get(i);
                    List<TimeNode> nextGroup = groups.get(i + 1);
                    for (TimeNode src : currentGroup) {
                        for (TimeNode dest : nextGroup) {
                            writer.printf("  T%d_%d -> T%d_%d [color=blue, style=dashed];%n",
                                    src.time, src.node, dest.time, dest.node);
                        }
                    }
                }
            }

            // mark red vertices
            writer.println("  node [color=red];");
            for (Rule rule : rules) {
                for (List<TimeNode> group : rule.groups) {
                    for (TimeNode tn : group) {
                        writer.printf("  T%d_%d;%n", tn.time, tn.node);
                    }
                }
            }

            writer.println("}");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    // Rule definition
    static class Rule {
        String label;
        List<List<TimeNode>> groups;

        Rule(String label, List<List<TimeNode>> groups) {
            this.label = label;
            this.groups = groups;
        }
    }

    // Vertex with moment
    static class TimeNode {
        int time;
        int node;

        TimeNode(int time, int node) {
            this.time = time;
            this.node = node;
        }
    }
}