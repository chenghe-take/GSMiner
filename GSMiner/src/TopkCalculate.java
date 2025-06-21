import java.io.*;
import java.util.*;
import java.util.regex.*;

//calculate the same records, and the length and size of records
public class TopkCalculate {
    public static void main(String[] args) {
        int topk = 306;
        String path = "/Users/hecheng/Documents/Paper/GSMiner/experiments/";
        String isMinerName = "USFlight_TSeqMiner_306.txt";
        String gsMinerName = "USFlight_GSMiner_306.txt";
        String isMinerFile = path + isMinerName;
        String gsMinerFile = path + gsMinerName;

        try {
            // Compare the sequences and output the results
            Set<String> isMinerSequences = readAndNormalizeSequences(isMinerFile);
            int matchCount = countMatches(gsMinerFile, isMinerSequences);
            double probability = (double) matchCount / topk;
            System.out.println("The number of the same sequence: " + matchCount);
            System.out.printf("The same probability: %.4f%n", probability);

            // Output statistical information
            System.out.println("\n==== Dataset statistical information ====");
            printStatistics(isMinerFile, isMinerName);
            printStatistics(gsMinerFile, gsMinerName);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void printStatistics(String filename, String title) throws IOException {
        Stats stats = calculateStats(filename);
        System.out.println("\n" + title + " statistics：");
        System.out.printf("Average sequence length (number of itemsets): %.2f%n", stats.avgSequenceLength);
        System.out.printf("The average number of items (total number of items/number of sequences): %.2f%n", stats.avgItemsPerSequence);
        System.out.println("Sequence length distribution：");
        System.out.println("  The number of sequences of length 2: " + stats.countLen2);
        System.out.println("  The number of sequences of length 3: " + stats.countLen3);
        System.out.println("  The number of sequences of length 4: " + stats.countLen4);
        System.out.println("  The number of sequences of length ≥5: " + stats.countLen5Plus);
    }

    private static Stats calculateStats(String filename) throws IOException {
        int totalSequences = 0;
        int totalItemsets = 0;
        int totalItems = 0;
        int countLen2 = 0, countLen3 = 0, countLen4 = 0, countLen5Plus = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            br.readLine(); // Skip the title line
            String line;
            while ((line = br.readLine()) != null) {
                ItemsetInfo info = parseSequence(line);
                if (info != null) {
                    // Count the total
                    totalItemsets += info.itemsetCount;
                    totalItems += info.totalItems;
                    totalSequences++;

                    // Count length distribution
                    switch (info.itemsetCount) {
                        case 2: countLen2++; break;
                        case 3: countLen3++; break;
                        case 4: countLen4++; break;
                        default:
                            if (info.itemsetCount >= 5) {
                                countLen5Plus++;
                            }
                            break;
                    }
                }
            }
        }

        return new Stats(
                totalSequences > 0 ? (double) totalItemsets / totalSequences : 0,
                totalSequences > 0 ? (double) totalItems / totalSequences : 0,
                countLen2, countLen3, countLen4, countLen5Plus
        );
    }

    private static ItemsetInfo parseSequence(String line) {
        Pattern pattern = Pattern.compile("^\\s*(\\{.*?\\})");
        Matcher matcher = pattern.matcher(line);
        if (!matcher.find()) return null;

        String sequence = matcher.group(1)
                .replaceAll("[{}]", "")
                .trim();

        int itemsetCount = 0;
        int totalItems = 0;

        // Split itemset
        String[] itemsets = sequence.split("\\),\\s*\\(");
        for (String itemset : itemsets) {
            itemsetCount++;
            itemset = itemset.replaceAll("[\\(\\)]", "");
            totalItems += itemset.split(",\\s*").length;
        }

        return new ItemsetInfo(itemsetCount, totalItems);
    }

    static class Stats {
        final double avgSequenceLength;
        final double avgItemsPerSequence;
        final int countLen2, countLen3, countLen4, countLen5Plus;

        Stats(double a, double b, int c2, int c3, int c4, int c5p) {
            avgSequenceLength = a;
            avgItemsPerSequence = b;
            countLen2 = c2;
            countLen3 = c3;
            countLen4 = c4;
            countLen5Plus = c5p;
        }
    }

    static class ItemsetInfo {
        final int itemsetCount;
        final int totalItems;

        ItemsetInfo(int a, int b) {
            itemsetCount = a;
            totalItems = b;
        }
    }

    private static Set<String> readAndNormalizeSequences(String filename) throws IOException {
        Set<String> sequences = new HashSet<>(3062);
        Pattern pattern = Pattern.compile("^\\s*(\\{.*?\\})");

        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            br.readLine(); // Skip the title line
            String line;
            while ((line = br.readLine()) != null) {
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    String seq = matcher.group(1).replaceAll("\\s+", "");
                    sequences.add(seq);
                }
            }
        }
        return sequences;
    }

    private static int countMatches(String filename, Set<String> targetSet) throws IOException {
        int count = 0;
        Pattern pattern = Pattern.compile("^\\s*(\\{.*?\\})");

        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            br.readLine(); // Skip the title line
            String line;
            while ((line = br.readLine()) != null) {
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    String seq = matcher.group(1).replaceAll("\\s+", "");
                    if (targetSet.contains(seq)) count++;
                }
            }
        }
        return count;
    }
}