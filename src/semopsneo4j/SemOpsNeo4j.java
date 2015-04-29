package semopsneo4j;

import gexfparserforneo4jdb.GexfParserForNeo4jDB;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.schema.Schema;
import org.sqlite.SQLiteConfig.TempStore;

import treegenerator.TreeGenerator;
import treegenerator.model.OnlineConcept;

public class SemOpsNeo4j {

	private static String NEO4JDB_PATH;
	private static String SEPARATOR;
	private static String TREE_INI_FILE_PATH;
	private static String GEXF_DIR_PATH;
	private static String MATRIXES_DIR_PATH;
	
	//Do you need a .gexf file with all new concepts/relationships ?
	static final boolean GENERATE_GEXF_FILE = false;
	
	//Should we generate a global similarity matrix with all base concepts ?
	static final boolean GENERATE_FULL_MATRIX = false;
	
	//Should we generate each image's similarity matrix ?
	static final boolean GENERATE_IMAGES_MATRIX = true;
	
	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		loadProperties("config.ini");
		
		TreeGenerator treeGen;
		GexfParserForNeo4jDB gexfParser;
		
		SemDistance semDistance = new SemDistance(NEO4JDB_PATH);
		DataReader reader = new DataReader();
		//Create Neo4j DB if absent
		if(!new File(NEO4JDB_PATH).isDirectory()) {
			createDB(NEO4JDB_PATH);
		}
		
		
		//Create map
		Map<String, ArrayList<String>> imagesTags = reader.mapFromFile("example1.cvs");
//		Map imagesTags = reader.mapFromDatabase("jdbc:postgresql://localhost:5432/ImagesPFE", "mogier", "xxx");
		
		//Foreach image create matrix
		ArrayList<String> currentTags;
		ArrayList<String> tagsToCreate;
		double[][] distanceMatrix;
		Iterator itMap = imagesTags.entrySet().iterator();
		String stringTags="";
		while (itMap.hasNext()) {
			treeGen = new TreeGenerator();
			gexfParser = new GexfParserForNeo4jDB();
			
			Map.Entry pairFromMap = (Map.Entry) itMap.next();
			currentTags = (ArrayList<String>) pairFromMap.getValue();
			
			//Check if all tags exist in the DB
			tagsToCreate = semDistance.checkTags(currentTags);

			//Eventually create tags
			stringTags = StringUtils.join(tagsToCreate,SEPARATOR);
			if(stringTags!=""){
				
				if(GENERATE_GEXF_FILE){
					String newGexfFile = treeGen.runAndGenerateGEXF(new String[]{stringTags,SEPARATOR,TREE_INI_FILE_PATH,GEXF_DIR_PATH,(String)pairFromMap.getKey()});
					gexfParser.main(new String[]{newGexfFile, NEO4JDB_PATH});
				}
				else {
					Hashtable<String, OnlineConcept> dataModel = treeGen.run(new String[]{stringTags,SEPARATOR,TREE_INI_FILE_PATH,GEXF_DIR_PATH,(String)pairFromMap.getKey()});
					ModelImporter modelImp = new ModelImporter();
					modelImp.importDataModel(dataModel, NEO4JDB_PATH);
				}
			}
			
			//Compute and print distance matrix for each image
			if(GENERATE_IMAGES_MATRIX){
				distanceMatrix = semDistance.createMatrix(currentTags);
				printCSV(distanceMatrix,pairFromMap,MATRIXES_DIR_PATH);	
			}	
		}		
		
		//Compute and print distance matrix for all base's tags
		if(GENERATE_FULL_MATRIX){
			ArrayList<String> allBaseConcepts = semDistance.findAllBaseTags();
			double[][] allTagsDistanceMatrix = semDistance.createMatrixAllBase(allBaseConcepts);
			printCSV(allTagsDistanceMatrix,allBaseConcepts,MATRIXES_DIR_PATH);	
		}
		
		//Generate ranked lists of interesting tags
		Iterator itMapSubLists = imagesTags.entrySet().iterator();
		ArrayList<ArrayList<String>> currentImageSubTagLists;
		ArrayList<String> currentImageTags;
		
		while(itMapSubLists.hasNext()) {
			Map.Entry pairFromMap = (Map.Entry) itMapSubLists.next();
			
			//Split tags into sublists of 2 elements
			currentImageTags = (ArrayList<String>) pairFromMap.getValue();
			currentImageSubTagLists = splitTags(currentImageTags);
			
			//Find the 10 most interesting concepts for each pair of tags
			for(int i=0;i<currentImageSubTagLists.size();i++) {
				ArrayList<String> tenTagsForThisSubList = semDistance.findTenConcepts(currentImageSubTagLists.get(i));
			}
		}
	}

	private static ArrayList<ArrayList<String>> splitTags(ArrayList<String> currentImageTags) {
		ArrayList<ArrayList<String>> returnList = new ArrayList<ArrayList<String>>();
		ArrayList<String> tempList;
		for(int i=0; i<currentImageTags.size()-1;i++){
			for(int j=i+1; j<currentImageTags.size();j++){
				tempList = new ArrayList<String>(2);
				tempList.add(currentImageTags.get(i));
				tempList.add(currentImageTags.get(j));
				returnList.add(tempList);
			}
		}		
		return returnList;
	}

	private static void loadProperties(String fileConfig) throws FileNotFoundException, IOException {
		Properties p = new Properties();
	    p.load(new FileInputStream(fileConfig));
	    
	    GEXF_DIR_PATH=p.getProperty("GEXF_DIR_PATH", "");
	    NEO4JDB_PATH=p.getProperty("NEO4JDB_PATH", "");
		SEPARATOR =p.getProperty("SEPARATOR", "");
		TREE_INI_FILE_PATH =p.getProperty("TREE_INI_FILE_PATH", "");
		MATRIXES_DIR_PATH=p.getProperty("MATRIXES_DIR_PATH", "");
	}

	private static void createDB(String neo4jdbPath) {
		GraphDatabaseService graphDb = new GraphDatabaseFactory().newEmbeddedDatabase( neo4jdbPath );
		final Node bottomNode;
		final Node topNode;
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
			
			topNode = graphDb.createNode(label);
			topNode.setProperty("uri", "virtual:top");
			topNode.setProperty("startingConcept", "false");
			tx2.success();
		} finally {
			tx2.close();
		}		
		graphDb.shutdown();
	}

	private static void printCSV(double[][] distanceMatrix,	ArrayList<String> allBaseConcepts, String matrixesDirPath) throws FileNotFoundException {
		PrintStream file = new PrintStream(new FileOutputStream(matrixesDirPath+"allTags.csv", false));

        int size = distanceMatrix.length;
        //Header
        file.print(",");
        for (int i = 0; i < size; i++) {
			file.print(allBaseConcepts.get(i).substring(5)+ ",");
		}
        file.println("");
        
        //Content
		for (int j = 0; j < size; j++) {
			file.print(allBaseConcepts.get(j).substring(5) + ",");
			for (int i = 0; i < size; i++) {
				file.print(distanceMatrix[i][j] + ",");
			}

			file.println("");
		}
		
        file.close();
	}
	
	private static void printCSV(double[][] distanceMatrix, Map.Entry pairFromMap, String pathDirMatrix) throws FileNotFoundException {
		PrintStream file = new PrintStream(new FileOutputStream(pathDirMatrix+pairFromMap.getKey()+ ".csv", false));

        int size = distanceMatrix.length;
        //Header
        file.print(",");
        for (int i = 0; i < size; i++) {
			file.print(((ArrayList<String>) pairFromMap.getValue()).get(i) + ",");
		}
        file.println("");
        
        //Content
		for (int j = 0; j < size; j++) {
			file.print(((ArrayList<String>) pairFromMap.getValue()).get(j) + ",");

			for (int i = 0; i < size; i++) {
				file.print(distanceMatrix[i][j] + ",");
			}

			file.println("");
		}
		
        file.close();
	}
	
}
