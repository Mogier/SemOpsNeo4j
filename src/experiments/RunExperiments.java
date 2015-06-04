package experiments;

import gexfparserforneo4jdb.neo4j.RelTypes;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.Random;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.event.TransactionData;
import org.neo4j.graphdb.event.TransactionEventHandler;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

import semopsneo4j.SemDistance;

/*
 * This class will launch all the experiments and collect results
 */
public class RunExperiments {
	// Step I : Define parameters perimeter	
	// C : number of candidates tags we are looking for
	final static int[] C = {2,4,6};//,8,10};
	// D : maximal distance between 2 nodes in the case there's no path between them
	final static int[] D = {10,16};//,24,32,50};
	// K : a number to lower the importance of nodes far from the base tags in the globalScore formula
	final static int[] K = {1,2,3};		
	// T : Number of tags that must be tested in the case we work with sublists (=sublists size)
	final static int[] T = {2,3};
	// P : percentage of input tags that will be removed
	final static int P = 20;
	
	static HashMap<String, Node> nodes;
	static GraphDatabaseService graphDb;
	static String DBname="";
	
	public static void main(String[] args) throws FileNotFoundException, IOException {
		Properties p = new Properties();
	    p.load(new FileInputStream("config.ini"));
		DBname = p.getProperty("NEO4JDB_PATH","");
		
		nodes = new HashMap<String, Node>();
		ArrayList<String> inputTags = new ArrayList<String>();
		inputTags.add("cat");
		inputTags.add("poodle");
		inputTags.add("dog");
		//ArrayList<String> removedTags = removePercentageofTags(inputTags,P);
		graphDb = connectDB(DBname);
		
		// Step II : Initialize experiments
		// 1st case : GlobalScore take account of all initial tags
		ArrayList<RelevantTagsExperiment> expsWholeList = new ArrayList<RelevantTagsExperiment>();
		// 2nd case : GlobalScore take account of sublists, using the t closest distance to compute global score
		ArrayList<RelevantTagsExperiment> expsSubLists = new ArrayList<RelevantTagsExperiment>();
		// 3rd case : GlobalScore take account of all initial tags, looking for direct neighboors
		ArrayList<RelevantTagsExperiment> expsWholeListsDN = new ArrayList<RelevantTagsExperiment>();
		for(int c : C){
			for(int d : D){
				expsWholeListsDN.add(new RelevantTagsExperimentDN(d, c, inputTags));
				for(int k : K)
					expsWholeList.add(new RelevantTagsExperimentWL(d, c, k, inputTags));
				for(int t : T)
					expsSubLists.add(new RelevantTagsExperimentSL(d, c, t, inputTags));			
			}
		}
		// Step III : Run experiments
		for(RelevantTagsExperiment test : expsWholeListsDN){
			test.findNewTags();
			System.out.println(test.toString());
		}
		
//		for(RelevantTagsExperiment test : expsWholeList){
//			test.findNewTags();
//			System.out.println(test.toString());
//		}
//		
//		for(RelevantTagsExperiment test : expsSubLists){
//			test.findNewTags();
//			System.out.println(test.toString());
//		}
		
		graphDb.shutdown();
		// Step IV : Evaluate results
		
		// Step V : Export ?
		
	}
	
	private static ArrayList<String> removePercentageofTags(ArrayList<String> inputTags, int p2) {
		ArrayList<String> removedTags = new ArrayList<String>();
		int totalToRemove = Math.round(inputTags.size()*p2/100);
		Random random = new Random();
		
		for(int i=0; i<totalToRemove; i++){
			int index = random.nextInt(inputTags.size());
			String current = inputTags.remove(index);
			removedTags.add(current);
		}
		
		return removedTags;
	}

	private double distanceOldNewTags(ArrayList<Node> deletedNode, ArrayList<Node> newTags){
		double distance = 0.0;
		
		return distance;
	}
	
	private static GraphDatabaseService connectDB(String DBname) {
		graphDb = new GraphDatabaseFactory().newEmbeddedDatabase( DBname );		
		registerShutdownHook( graphDb );		
		return graphDb;
	}
	
	private static void registerShutdownHook( final GraphDatabaseService graphDb )
	{
	    // Registers a shutdown hook for the Neo4j instance so that it
	    // shuts down nicely when the VM exits (even if you "Ctrl-C" the
	    // running application).
	    Runtime.getRuntime().addShutdownHook( new Thread()
	    {
	        @Override
	        public void run()
	        {
	            graphDb.shutdown();
	        }
	    } );
	}
}
