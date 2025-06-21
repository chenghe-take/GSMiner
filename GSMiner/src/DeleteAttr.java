import java.io.*;

/**
 * delete the unnecessary attributes of DBLP and USFLight
 */
public class DeleteAttr {

    public static void main(String[] args) {
        String inputFile = "/Users/hecheng/Desktop/ISMiner/Data/USFlight/attributes.txt";
        String outputFile = "USFlightattributes.txt";

        try (BufferedReader br = new BufferedReader(new FileReader(inputFile));
             BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile))) {

            String line;
            while ((line = br.readLine()) != null) {
                String processedLine = processLine(line);
                bw.write(processedLine);
                bw.newLine();
            }

            System.out.println("Complete！");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 处理每一行的逻辑
     */
    private static String processLine(String line) {
        String[] attributes = line.split(" ");
        if (line.startsWith("T")) {
            return attributes[0]; // if it starts with 'T', only retains the first element
        } else {
            int length = Math.min(attributes.length, 9);
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < length; i++) {
                result.append(attributes[i]).append(" ");
            }
            return result.toString().trim(); // remove the redundent blank space
        }
    }
}