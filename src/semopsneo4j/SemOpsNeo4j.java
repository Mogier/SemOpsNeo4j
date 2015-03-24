package semopsneo4j;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

public class SemOpsNeo4j {

	/**
	 * @param args
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException {
		SemDistance semDistance = new SemDistance("/home/mael/Documents/Neo4j/neo4j-community-2.1.7/data/test.db");
		DataReader reader = new DataReader();
		
		//Create map
		Map<String, ArrayList<String>> imagesTags = reader.mapFromFile("example1.cvs");
//		Map imagesTags = reader.mapFromDatabase(url, user, password);
		
		//Foreach image create matrix
		ArrayList<String> currentTags;
		double[][] distanceMatrix;
		Iterator itMap = imagesTags.entrySet().iterator();

		while (itMap.hasNext()) {
			Map.Entry pairFromMap = (Map.Entry) itMap.next();
			currentTags = (ArrayList<String>) pairFromMap.getValue();
			distanceMatrix = semDistance.createMatrix(currentTags);
			printCSV(distanceMatrix,pairFromMap);			
		}		
	}

	private static void printCSV(double[][] distanceMatrix, Map.Entry pairFromMap) throws FileNotFoundException {
		PrintStream file = new PrintStream(new FileOutputStream("matrixes/"+pairFromMap.getKey()+ ".csv", false));

        int size = distanceMatrix.length;
        //Header
        file.print(", ");
        for (int i = 0; i < size; i++) {
			file.print(((ArrayList<String>) pairFromMap.getValue()).get(i) + ", ");
		}
        file.println("");
        
        //Content
		for (int j = 0; j < size; j++) {
			file.print(((ArrayList<String>) pairFromMap.getValue()).get(j) + ", ");

			for (int i = 0; i < size; i++) {
				file.print(distanceMatrix[i][j] + ", ");
			}

			file.println("");
		}
		
        file.close();
	}
	
}
