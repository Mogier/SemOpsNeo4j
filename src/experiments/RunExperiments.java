package experiments;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Random;

import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

import semopsneo4j.DataReader;
import semopsneo4j.PairNodeScore;
import treegenerator.services.Inflector;

/*
 * This class will launch all the experiments and collect results
 */
public class RunExperiments {
	
	// Step I : Define parameters perimeter	
	// C : number of candidates tags we are looking for
	final static int[] C = {2,4,6,8,10};
	// D : maximal distance between 2 nodes in the case there's no path between them
	final static int[] D = {10,16,24,32,50};
	// K : a number to lower the importance of nodes far from the base tags in the globalScore formula
	final static int[] K = {1,2,3};		
	// T : Number of tags that must be tested in the case we work with sublists (=sublists size)
	final static int[] T = {2,3};
	// P : percentage of input tags that will be removed
	final static int P = 20;
	
	static HashMap<String, Node> nodes;
	static GraphDatabaseService graphDb;
	static String DBname="";
	static String NEW_LINE="";
	private static String OUTPUT_FOLDER="";
	
	public static void main(String[] args) throws FileNotFoundException, IOException {
		Properties p = new Properties();
	    p.load(new FileInputStream("config.ini"));
		DBname = p.getProperty("NEO4JDB_PATH","");
		OUTPUT_FOLDER = p.getProperty("EXPERIMENTS_DIR_PATH");
		NEW_LINE = System.getProperty("line.separator");
		
		graphDb = connectDB(DBname);
		DataReader reader = new DataReader();
		Map<String, ArrayList<String>> imagesTags = reader.mapFromDatabase("jdbc:postgresql://localhost:5432/ImagesPFE", "mogier", "olouise38");
//		Map<String, ArrayList<String>> imagesTags = reader.mapFromFile("example1.cvs");
		System.out.println(imagesTags.size());
		Iterator<Entry<String, ArrayList<String>>> entries = imagesTags.entrySet().iterator();
		long total =0;
		for(int i=0;i<10; i++){
			long t1 = System.currentTimeMillis();
			nodes = new HashMap<String, Node>();
			Entry<String, ArrayList<String>> entry = entries.next();
			ArrayList<String> inputTags = entry.getValue();
			ArrayList<String> notFoundTags = findNodes(inputTags);
			//ArrayList<String> removedTags = removePercentageofTags(inputTags,P);

			// Step II : Initialize experiments
			// 1st case : GlobalScore take account of all initial tags
			ArrayList<RelevantTagsExperimentWL> expsWholeList = new ArrayList<RelevantTagsExperimentWL>();
			// 2nd case : GlobalScore take account of sublists, using the t closest distance to compute global score
			ArrayList<RelevantTagsExperimentSL> expsSubLists = new ArrayList<RelevantTagsExperimentSL>();
			// 3rd case : GlobalScore take account of all initial tags, looking for direct neighbors
			RelevantTagsExperimentDN dn = new RelevantTagsExperimentDN(nodes.size());
			// 4th case : Using wikipedia web pages
			RelevantTagsExperimentWiki expWiki = new RelevantTagsExperimentWiki(nodes.size());

			for(int c : C){
				for(int d : D){
					for(int k : K)
						expsWholeList.add(new RelevantTagsExperimentWL(d, c, k));
					for(int t : T)
						expsSubLists.add(new RelevantTagsExperimentSL(d, c, t));			
				}
			}
			
			// Step III : Run experiments
			StringBuilder textFileBuilder = new StringBuilder();
			StringBuilder csvFileBuilder = new StringBuilder();
			ArrayList<RelevantTagsExperiment> testsForHTML = new ArrayList<RelevantTagsExperiment>();
			csvFileBuilder.append(entry.getKey() +","+ inputTags + NEW_LINE);
			csvFileBuilder.append(NEW_LINE);
			csvFileBuilder.append("TypeTest NbCand MaxDist k/SubListSize (WL/SL)" + NEW_LINE);
			textFileBuilder.append("Initial tags : " + inputTags + NEW_LINE);
			textFileBuilder.append(notFoundTags.size()*100/inputTags.size()+"% " + "tags not found : " + notFoundTags + NEW_LINE);
			textFileBuilder.append("Tags considered : " + nodes.keySet().toString() + NEW_LINE);
			textFileBuilder.append(NEW_LINE);
			
			expWiki.findNewTags();
			textFileBuilder.append(expWiki.toString());
			csvFileBuilder.append(expWiki.getClass().getName()+" " + expWiki.getNbCandidates()  +",");
			csvFileBuilder.append(candidatesLabels(expWiki.getCandidates())+NEW_LINE);
			testsForHTML.add(expWiki);
			
			dn.findNewTags();
			testsForHTML.add(dn);
			textFileBuilder.append(dn.toString());
			csvFileBuilder.append(dn.getClass().getName()+" " + dn.getNbCandidates() +",");
			csvFileBuilder.append(candidatesLabels(dn.getCandidates())+NEW_LINE);

			for(RelevantTagsExperimentWL test : expsWholeList){
				test.findNewTags();
				textFileBuilder.append(test.toString());
				
				csvFileBuilder.append(test.getClass().getName()+" " + test.getNbCandidates() + " " + test.getMaxLenthBetweenNodes() + " " + test.getK() +",");
				csvFileBuilder.append(candidatesLabels(test.getCandidates())+NEW_LINE);
				
				if(test.getNbCandidates()==10)
					testsForHTML.add(test);
			}
			
			for(RelevantTagsExperimentSL test : expsSubLists){
				test.findNewTags();
				textFileBuilder.append(test.toString());
				
				csvFileBuilder.append(test.getClass().getName()+" " + test.getNbCandidates() + " " + test.getMaxLenthBetweenNodes() + " " + test.getSizeSubLists()  +",");
				csvFileBuilder.append(candidatesLabels(test.getCandidates())+NEW_LINE);
				
				if(test.getNbCandidates()==10)
					testsForHTML.add(test);
			}
			

				
			PrintStream textFile = new PrintStream(new FileOutputStream(OUTPUT_FOLDER+entry.getKey()+".txt", false));
			textFile.println(textFileBuilder.toString());
			textFile.close();
			
			PrintStream csvFile = new PrintStream(new FileOutputStream(OUTPUT_FOLDER+entry.getKey()+".csv", false));
			csvFile.println(csvFileBuilder.toString());
			csvFile.close();
			
			exportToHTML(p.getProperty("IMAGES_PATH"), entry.getKey(), inputTags, notFoundTags,testsForHTML);
			// Step IV : Evaluate results
			
			// Step V : Export ?
			long totalTime = System.currentTimeMillis()-t1;
			System.out.println("Image " + i + " : " + totalTime +"ms");
			total+=totalTime;
		}
		System.out.println("Total tests time : " + total + "ms");
		graphDb.shutdown();
	}
	
	private static ArrayList<String> findNodes(ArrayList<String> inputTags) {
		ArrayList<String> notFoundNodes = new ArrayList<String>();
		Inflector inf = Inflector.getInstance();
		Transaction tx = graphDb.beginTx();
		try{
			for(String tag : inputTags){
				Node current = findConceptByURI("base:"+inf.singularize(tag));
				if(current.getRelationships(Direction.OUTGOING).iterator().hasNext())
					nodes.put(tag,current);
				else
					notFoundNodes.add(tag);
			}
			tx.success();
		} finally {
			tx.close();
		}
		
		return notFoundNodes;
	}

	private static Node findConceptByURI(String URI) {
		Node nodeResult=null;
		ExecutionEngine engine = new ExecutionEngine( graphDb );
		String query = "MATCH (concept {uri:\""+URI+"\"})" +
						"RETURN concept "+
						"LIMIT 1";
		ExecutionResult result = (ExecutionResult) engine.execute( query);
		
		ResourceIterator<Node> it = result.columnAs("concept");
		if (it.hasNext()){
			nodeResult = it.next();
		}
		return nodeResult;
	}
	
	private static String candidatesLabels(List<PairNodeScore> list) {
		StringBuilder str = new StringBuilder();
		
		for(PairNodeScore p : list)
			str.append(p.getLabel()+",");
		
		return str.toString();
	}
	
	private static void exportToHTML(String imagesPath, String imageID, ArrayList<String> inputTags, ArrayList<String> notFoundTags, ArrayList<RelevantTagsExperiment> tests) throws FileNotFoundException{
		StringBuilder htmlBuilder = new StringBuilder();
		// Head
		htmlBuilder.append("<html>" + NEW_LINE + "<head>" + NEW_LINE + "<title>");
		htmlBuilder.append(imageID + "</title>" + NEW_LINE + "</head>");
		// Body
		htmlBuilder.append("<body>" + NEW_LINE);
		htmlBuilder.append("<h1> Image " + imageID + "</h1>" + NEW_LINE);
		htmlBuilder.append("<img src='"+ imagesPath+imageID+".jpg'>");
		htmlBuilder.append("<div> Initial tags : " + inputTags.toString() + "</div>");
		htmlBuilder.append("<div> Not found tags : " + notFoundTags.toString() + "</div>");
		
		// New tags
		htmlBuilder.append("<h3> New tags </h3>" + NEW_LINE);
		htmlBuilder.append("<table cellpadding='5px'>" + NEW_LINE);
		for (RelevantTagsExperiment test : tests){
			htmlBuilder.append("<tr>" + NEW_LINE);
			String type = test.getClass().getName().substring(test.getClass().getName().lastIndexOf('t')+1);
			htmlBuilder.append("<td>" + type + "</td>" + NEW_LINE);
			for(PairNodeScore p : test.getCandidates())
				htmlBuilder.append("<td>" + p.getLabel() + "</td>" + NEW_LINE);
			htmlBuilder.append("</tr>" + NEW_LINE);
		}
		htmlBuilder.append("</table>" + NEW_LINE);
		htmlBuilder.append("</body>" + NEW_LINE);
		htmlBuilder.append("</html>");
		
		PrintStream htmlFile = new PrintStream(new FileOutputStream(OUTPUT_FOLDER+imageID+".html", false));
		htmlFile.println(htmlBuilder.toString());
		htmlFile.close();
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
