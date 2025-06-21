import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;

public class MainGSMiner {

	public static void main(String [] arg) throws IOException{

//		String inputDirectory = "/Users/hecheng/Documents/DYNAMIC_GRAPH_DATASETS_REDUCED/TSEQMINER_datasets/GrayValue4000/Graph03/Interval120/";
		String inputDirectory = "Data/USFlight/";

		// The output file path
		String outputPath = "output.txt";

		// Apply the algorithm 
		AlgoGSMiner algo = new AlgoGSMiner();
		algo.runAlgorithm(inputDirectory, outputPath);
		
		// Print statistics about the algorithm execution
		algo.printStats();

	}

	public static String fileToPath(String filename) throws UnsupportedEncodingException {
		URL url = MainGSMiner.class.getResource(filename);
		 return java.net.URLDecoder.decode(url.getPath(),"UTF-8");
	}
}
