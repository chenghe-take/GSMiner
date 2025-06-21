import java.io.*;
import java.util.*;
import java.util.regex.*;

// get the top-k records
public class TopkFilter {
    public static void main(String[] args) {
        int topk = 58;
        String inputFile = "/Users/hecheng/Documents/Paper/GSMiner/experiments/World_65.txt";
        String outputFile = "/Users/hecheng/Documents/Paper/GSMiner/experiments/World_ISMiner_58.txt";

        try {
            List<SequenceRecord> records = new ArrayList<>();
            BufferedReader reader = new BufferedReader(new FileReader(inputFile));

            // Read and save the title line
            String header = reader.readLine();

            // Define a regular expression to match three columns of data
            Pattern pattern = Pattern.compile("^(.+?)\\s+(\\{.*?\\})\\s+(\\{.*?\\})$");

            String line;
            while ((line = reader.readLine()) != null) {
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    //Extract the headSup column data
                    String headSupStr = matcher.group(3).replaceAll("[{}]", "").trim();

                    // Split multiple support values and take the last one
                    String[] headSupValues = headSupStr.split(",\\s*");
                    if (headSupValues.length == 0) continue;

                    try {
                        double headSup = Double.parseDouble(
                                headSupValues[headSupValues.length - 1].trim()
                        );
                        records.add(new SequenceRecord(line, headSup));
                    } catch (NumberFormatException e) {
                        System.err.println("Abnormal numerical format: " + line);
                    }
                }
            }
            reader.close();

            // Sort headSup in ascending order
            Collections.sort(records, Comparator.comparingDouble(SequenceRecord::getHeadSup));

            // Determine the number of records to be retained
            int outputSize = Math.min(topk, records.size());

            // Write to the result file
            BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));
            writer.write(header);
            writer.newLine();
            writer.newLine();

            for (int i = 0; i < outputSize; i++) {
                writer.write(records.get(i).getLine());
                writer.newLine();
                writer.newLine();
            }
            writer.close();

            System.out.println("Complete！Save " + outputSize + " records");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class SequenceRecord {
        private final String line;
        private final double headSup;

        public SequenceRecord(String line, double headSup) {
            this.line = line;
            this.headSup = headSup;
        }

        public String getLine() { return line; }
        public double getHeadSup() { return headSup; }
    }
}
