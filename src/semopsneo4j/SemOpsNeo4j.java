package semopsneo4j;

import gexfparserforneo4jdb.GexfParserForNeo4jDB;
import gexfparserforneo4jdb.neo4j.RelTypes;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.event.TransactionData;
import org.neo4j.graphdb.event.TransactionEventHandler;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.schema.Schema;

import treegenerator.TreeGenerator;

public class SemOpsNeo4j {

	final static String NEO4JDB_PATH = "/home/mael/Documents/Neo4j/neo4j-community-2.1.7/data/test.db";
	final static String SEPARATOR = ",";
	final static String INI_FILE_PATH = "/home/mael/Documents/WorkplaceEclipse/TreeGenerator/config.ini";
	final static String GEXF_DIR_PATH="/home/mael/Documents/WorkplaceEclipse/TreeGenerator/generatedFiles/";
	final static String MATRIXES_DIR_PATH="/home/mael/Documents/WorkplaceEclipse/SemOpsNeo4j/matrixes/";
	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		TreeGenerator treeGen;
		GexfParserForNeo4jDB gexfParser;
		
		SemDistance semDistance = new SemDistance(NEO4JDB_PATH);
		DataReader reader = new DataReader();
		//Create Neo4j DB if absent
		if(!new File(NEO4JDB_PATH).isDirectory()) {
			createDB(NEO4JDB_PATH);
		}
		
		
		//Create map
//		Map<String, ArrayList<String>> imagesTags = reader.mapFromFile("example8.cvs");
		Map imagesTags = reader.mapFromDatabase("jdbc:postgresql://localhost:5432/ImagesPFE", "mogier", "xxx");
		
		//Foreach image create matrix
		ArrayList<String> currentTags;
		ArrayList<String> tagsToCreate;
		double[][] distanceMatrix;
		Iterator itMap = imagesTags.entrySet().iterator();

		while (itMap.hasNext()) {
			treeGen = new TreeGenerator();
			gexfParser = new GexfParserForNeo4jDB();
			
			Map.Entry pairFromMap = (Map.Entry) itMap.next();
			currentTags = (ArrayList<String>) pairFromMap.getValue();
			
			//Check if all tags exist in the DB
			tagsToCreate = semDistance.checkTags(currentTags);

			//Eventually create tags
			String stringTags = StringUtils.join(tagsToCreate,SEPARATOR);
			if(stringTags!=""){
				String newGexfFile = treeGen.run(new String[]{stringTags,SEPARATOR,INI_FILE_PATH,GEXF_DIR_PATH,(String)pairFromMap.getKey()});
				gexfParser.main(new String[]{newGexfFile, NEO4JDB_PATH});
			}
						
			distanceMatrix = semDistance.createMatrix(currentTags);
			printCSV(distanceMatrix,pairFromMap,MATRIXES_DIR_PATH);			
		}		
	}

	private static void createDB(String neo4jdbPath) {
		GraphDatabaseService graphDb = new GraphDatabaseFactory().newEmbeddedDatabase( neo4jdbPath );
		final Node bottomNode;
		Transaction tx = graphDb.beginTx();
		try
		{
			Schema schema = graphDb.schema();
			//Create unicity constraint on uris
			schema.constraintFor( DynamicLabel.label( "Concept" ) )
					.assertPropertyIsUnique( "uri" )
					.create();
			tx.success();
		} finally {
			tx.close();
		}
		
		Transaction tx2 = graphDb.beginTx();	
		try
		{
			Label label = DynamicLabel.label( "Concept" );
			bottomNode = graphDb.createNode(label);
			bottomNode.setProperty("uri", "virtual:bottom");
			bottomNode.setProperty("startingConcept", "false");
			tx2.success();
		} finally {
			tx2.close();
		}		
		graphDb.shutdown();
	}

	private static void printCSV(double[][] distanceMatrix, Map.Entry pairFromMap, String pathDirMatrix) throws FileNotFoundException {
		PrintStream file = new PrintStream(new FileOutputStream(pathDirMatrix+pairFromMap.getKey()+ ".csv", false));

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
