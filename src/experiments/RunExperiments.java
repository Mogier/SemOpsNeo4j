package experiments;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
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
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

import semopsneo4j.DataReader;
import semopsneo4j.NLPParser;
import semopsneo4j.PairNodeScore;
import semopsneo4j.SemDistance;
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
	final static int P = 30;
	
	static HashMap<String, Node> nodes;
	static GraphDatabaseService graphDb;
	static String DBname="";
	static String NEW_LINE="";
	private static String OUTPUT_FOLDER="";
	
	public static void main(String[] args) throws Exception {
		Properties p = new Properties();
	    p.load(new FileInputStream("config.ini"));
		DBname = p.getProperty("NEO4JDB_PATH","");
		OUTPUT_FOLDER = p.getProperty("EXPERIMENTS_DIR_PATH");
		NEW_LINE = System.getProperty("line.separator");
		// eval = true if you want to delete P% of tags and evaluate the results.
		boolean eval = true;
		
		graphDb = connectDB(DBname);
		ExecutionEngine engine = new ExecutionEngine( graphDb );
		NLPParser parser = new NLPParser();
		DataReader reader = new DataReader();
		Map<String, ArrayList<String>> imagesTags = reader.mapFromDatabase("jdbc:postgresql://localhost:5432/ImagesPFE", "mogier", "olouise38");
//		Map<String, ArrayList<String>> imagesTags = reader.mapFromFile("example1.cvs");
		System.out.println(imagesTags.size());
		Iterator<Entry<String, ArrayList<String>>> entries = imagesTags.entrySet().iterator();
		long total =0;
		
		// Store experiments for user's evaluation
		ArrayList<RelevantTagsExperiment> expsWholeList = new ArrayList<RelevantTagsExperiment>();
		ArrayList<RelevantTagsExperiment> expsSubLists = new ArrayList<RelevantTagsExperiment>();
		ArrayList<RelevantTagsExperiment> expsDirectN = new ArrayList<RelevantTagsExperiment>();
		ArrayList<RelevantTagsExperiment> expsWikiLinks = new ArrayList<RelevantTagsExperiment>();
		ArrayList<RelevantTagsExperiment> expsWikiContent = new ArrayList<RelevantTagsExperiment>();
		for(int i=0;i<100; i++){
			long t1 = System.currentTimeMillis();
			nodes = new HashMap<String, Node>();
			Entry<String, ArrayList<String>> entry = entries.next();
			String imageID = entry.getKey();
			ArrayList<String> inputTags = entry.getValue();
			ArrayList<String> notFoundTags = findNodes(inputTags);
			if(nodes.keySet().size()>7){
				HashMap<String, Node> removedTags = null;
				if(eval){
					System.out.println("Avant remove");
					System.out.println(nodes.keySet());
					System.out.println();
					removedTags = removePercentageofTags(P);
					System.out.println("Après remove");
					System.out.println(nodes.keySet());
					System.out.println("Removed");
					System.out.println(removedTags.keySet());
				}
				
				// Step II : Initialize experiments
				// 1st case : GlobalScore take account of all initial tags
				// 2nd case : GlobalScore take account of sublists, using the t closest distance to compute global score
				// 3rd case : GlobalScore take account of all initial tags, looking for direct neighbors
				
				// 4th case : Using wikipedia web pages
				RelevantTagsExperimentWL expWL = new RelevantTagsExperimentWL(50, 10, 3,imageID, nodes.keySet());
				RelevantTagsExperimentSL expSL = new RelevantTagsExperimentSL(50, 10, 3,imageID, nodes.keySet());
				RelevantTagsExperimentDN dn = new RelevantTagsExperimentDN(nodes.size(),imageID, nodes.keySet());
				RelevantTagsExperimentWikiLinks expWiki = new RelevantTagsExperimentWikiLinks(nodes.size(),imageID, nodes.keySet());
				RelevantTagsExperimentWikiContent expWikiC = new RelevantTagsExperimentWikiContent(nodes.size(),imageID, nodes.keySet(), parser);

//				for(int c : C){
//					for(int d : D){
//						for(int k : K)
//							expsWholeList.add(new RelevantTagsExperimentWL(d, c, k));
//						for(int t : T)
//							expsSubLists.add(new RelevantTagsExperimentSL(d, c, t));			
//					}
//				}
				
				// Step III : Run experiments
				ArrayList<RelevantTagsExperiment> testsForHTML = new ArrayList<RelevantTagsExperiment>();
				
				expWiki.findNewTags();
				testsForHTML.add(expWiki);
				expsWikiLinks.add(expWiki);
				
				expWikiC.findNewTags();
				testsForHTML.add(expWikiC);
				expsWikiContent.add(expWikiC);
				
				dn.findNewTags();
				testsForHTML.add(dn);
				expsDirectN.add(dn);
				
				expWL.findNewTags(engine);
				testsForHTML.add(expWL);
				expsWholeList.add(expWL);
				
				expSL.findNewTags(engine);
				testsForHTML.add(expSL);
				expsSubLists.add(expSL);
				
				//Add wiki results to the graph
//				ModelImporter modelImp = new ModelImporter(graphDb);
//				ArrayList<String> wikiCandidates = generateWikiCand(expWiki.getCandidates(), expWikiC.getCandidates());
//				ArrayList<String> tagsToCreate = modelImp.checkTags(wikiCandidates);
//				String stringTags = StringUtils.join(tagsToCreate,p.getProperty("SEPARATOR", ""));
//				System.out.println(stringTags);
//				if(stringTags!=""){
//					Hashtable<String, OnlineConcept> dataModel = TreeGenerator.run(new String[]{stringTags,p.getProperty("SEPARATOR", ""),p.getProperty("TREE_INI_FILE_PATH", ""),p.getProperty("GEXF_DIR_PATH", ""),entry.getKey()});
//					modelImp.importDataModel(dataModel, DBname, false);
//				}
//				for(PairNodeScore pair : expWiki.getCandidates())
//					pair.setNode(findConceptByURI("base:"+pair.getLabel()));
//				for(PairNodeScore pair2 : expWikiC.getCandidates())
//					pair2.setNode(findConceptByURI("base:"+pair2.getLabel()));
				
				// Step IV : Evaluate results
				if(eval) {
					for(RelevantTagsExperiment test : testsForHTML){
						double avgDist = distanceOldNewTags(removedTags.values(), test.candidates, DBname);
						test.setAvgDistance(avgDist);
					}
				}
				
				
				// Step V : Export ?
				StringBuilder textFileBuilder = new StringBuilder();
				StringBuilder csvFileBuilder = new StringBuilder();
				
				csvFileBuilder.append(imageID +","+ inputTags + NEW_LINE);
				csvFileBuilder.append(NEW_LINE);
				csvFileBuilder.append("TypeTest NbCand MaxDist k/SubListSize (WL/SL), avgDist" + NEW_LINE);
				textFileBuilder.append("Initial tags : " + inputTags + NEW_LINE);
				textFileBuilder.append(notFoundTags.size()*100/inputTags.size()+"% " + "tags not found : " + notFoundTags + NEW_LINE);
				textFileBuilder.append("Tags considered : " + nodes.keySet().toString() + NEW_LINE);
				if(eval)
					textFileBuilder.append("Tags removed : " + P +"% : " + removedTags.keySet().toString() + NEW_LINE);
				textFileBuilder.append(NEW_LINE);
				
				textFileBuilder.append(expWiki.toString());
				csvFileBuilder.append(expWiki.getLabel()+" " + expWiki.getNbCandidates()  +"," + expWiki.getAvgDistance()+",");
				csvFileBuilder.append(candidatesLabels(expWiki.getCandidates())+NEW_LINE);
				
				textFileBuilder.append(expWikiC.toString());
				csvFileBuilder.append(expWikiC.getLabel()+" " + expWikiC.getNbCandidates()  +"," + expWikiC.getAvgDistance()+",");
				csvFileBuilder.append(candidatesLabels(expWikiC.getCandidates())+NEW_LINE);
				
				textFileBuilder.append(dn.toString());
				csvFileBuilder.append(dn.getLabel()+" " + dn.getNbCandidates() +"," + dn.getAvgDistance()+",");
				csvFileBuilder.append(candidatesLabels(dn.getCandidates())+NEW_LINE);

				textFileBuilder.append(expWL.toString());
				csvFileBuilder.append(expWL.getLabel()+" " + expWL.getNbCandidates() + " " + expWL.getMaxLenthBetweenNodes() + " " + expWL.getK() +"," + expWL.getAvgDistance()+",");
				csvFileBuilder.append(candidatesLabels(expWL.getCandidates())+NEW_LINE);
		
				textFileBuilder.append(expSL.toString());
				csvFileBuilder.append(expSL.getLabel()+" " + expSL.getNbCandidates() + " " + expSL.getMaxLenthBetweenNodes() + " " + expSL.getSizeSubLists()  +"," + expSL.getAvgDistance()+",");
				csvFileBuilder.append(candidatesLabels(expSL.getCandidates())+NEW_LINE);
				
				PrintStream textFile = new PrintStream(new FileOutputStream(OUTPUT_FOLDER+imageID+".txt", false));
				textFile.println(textFileBuilder.toString());
				textFile.close();
				
				PrintStream csvFile = new PrintStream(new FileOutputStream(OUTPUT_FOLDER+imageID+".csv", false));
				csvFile.println(csvFileBuilder.toString());
				csvFile.close();
				
				exportImageToHTML(p.getProperty("IMAGES_PATH"), imageID, inputTags, notFoundTags, removedTags, testsForHTML);
				
				long totalTime = System.currentTimeMillis()-t1;
				System.out.println("Image " + i + " : " + totalTime +"ms");
				total+=totalTime;
			}		
			else {
				System.err.println("Trop peu de tags initiaux : " + nodes.keySet().size());
			}
		}
		if(eval) {
			System.out.println("DN avg : " + avgDistance(expsDirectN));
			System.out.println("WL avg : " + avgDistance(expsWholeList));
			System.out.println("SL avg : " + avgDistance(expsSubLists));
		}
		
		// If you want to generate lots for user evaluation
//		generateTestsLots(p.getProperty("IMAGES_PATH"), expsWikiContent);
//		generateTestsLots(p.getProperty("IMAGES_PATH"), expsDirectN);
//		generateTestsLots(p.getProperty("IMAGES_PATH"), expsSubLists);
//		generateTestsLots(p.getProperty("IMAGES_PATH"), expsWholeList);
//		generateTestsLots(p.getProperty("IMAGES_PATH"), expsWikiLinks);
		System.out.println("Total tests time : " + total + "ms");
		graphDb.shutdown();
	}
	
	private static double avgDistance(ArrayList<RelevantTagsExperiment> list){
		double total = 0.0;
		for(RelevantTagsExperiment test : list)
			total+=test.getAvgDistance();
		return (double) total/list.size();
	}
	private static ArrayList<String> generateWikiCand(List<PairNodeScore> candidates, List<PairNodeScore> candidates2) {
		ArrayList<String> cands = new ArrayList<String>();
		for(PairNodeScore p : candidates)
			cands.add(p.getLabel());
		for(PairNodeScore p2 : candidates2)
			cands.add(p2.getLabel());
		return cands;
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
	
	private static void exportImageToHTML(String imagesPath, String imageID, ArrayList<String> inputTags, ArrayList<String> notFoundTags, HashMap<String, Node> removedTags, ArrayList<RelevantTagsExperiment> tests) throws FileNotFoundException{
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
		if(removedTags!=null)
			htmlBuilder.append("<div> Removed tags : " + removedTags.keySet().toString() + "</div>");
		
		// New tags
		htmlBuilder.append("<h3> New tags </h3>" + NEW_LINE);
		htmlBuilder.append("<table cellpadding='5px'>" + NEW_LINE);
		for (RelevantTagsExperiment test : tests){
			htmlBuilder.append("<tr>" + NEW_LINE);
			htmlBuilder.append("<td>" +test.label +"</td>" + NEW_LINE);
			for(PairNodeScore p : test.getCandidates())
				htmlBuilder.append("<td><a href='https://en.wikipedia.org/wiki/" + p.getLabel()+ "'>"+ p.getLabel() + "</a></td>" + NEW_LINE);
			htmlBuilder.append("</tr>" + NEW_LINE);
		}
		htmlBuilder.append("</table>" + NEW_LINE);
		htmlBuilder.append("</body>" + NEW_LINE);
		htmlBuilder.append("</html>");
		
		PrintStream htmlFile = new PrintStream(new FileOutputStream(OUTPUT_FOLDER+imageID+".html", false));
		htmlFile.println(htmlBuilder.toString());
		htmlFile.close();
	}
	
	private static void exportTestsToHTML(ArrayList<RelevantTagsExperiment> tests, String fileName) throws FileNotFoundException{
		StringBuilder htmlBuilder = new StringBuilder();
		// Head
		htmlBuilder.append("<html>" + NEW_LINE + "<head>" + NEW_LINE + "<title>");
		htmlBuilder.append(tests.get(0).getLabel() + "</title>" + NEW_LINE + "</head>");
		
		// Body
		htmlBuilder.append("<body>" + NEW_LINE);
		for(RelevantTagsExperiment test : tests){
			htmlBuilder.append("<h1> Image " + test.getImageID() + "</h1>" + NEW_LINE);
			htmlBuilder.append("<img src='"+ test.getImageID()+".jpg'>");
			htmlBuilder.append("<div> Initial tags : " + test.getTagsConsidered().toString() + "</div>");
			
			// New tags
			htmlBuilder.append("<h3> New tags </h3>" + NEW_LINE);
			htmlBuilder.append("<table cellpadding='5px'>" + NEW_LINE);
			htmlBuilder.append("<tr>" + NEW_LINE);
			for(PairNodeScore p : test.getCandidates())
				htmlBuilder.append("<td><a href='https://en.wikipedia.org/wiki/" + p.getLabel()+ "'>"+ p.getLabel() + "</a></td>" + NEW_LINE);
				
			htmlBuilder.append("</tr>" + NEW_LINE);	
			htmlBuilder.append("</table>" + NEW_LINE);
			htmlBuilder.append("</body>" + NEW_LINE);
			htmlBuilder.append("</html>");
		}
		PrintStream htmlFile = new PrintStream(new FileOutputStream(fileName+".html", false));
		htmlFile.println(htmlBuilder.toString());
		htmlFile.close();
	}
	
	private static void exportTestsToCSV(ArrayList<RelevantTagsExperiment> tests, String fileName) throws FileNotFoundException {
		StringBuilder csvFileBuilder = new StringBuilder();
		csvFileBuilder.append("Image annotation evaluation" + NEW_LINE);
		csvFileBuilder.append(NEW_LINE);
		csvFileBuilder.append("In addition to this evaluation file I sent you several HTML files containing the pictures themselves as well as their initial tags." + NEW_LINE);
		csvFileBuilder.append("Your role here is to evaluate each tag which has been generated by one of my algorithms." + NEW_LINE);
		csvFileBuilder.append("To achieve that simply answer this question : « Does this tag describe the image ? »" + NEW_LINE);
		csvFileBuilder.append("Scale : " + NEW_LINE);
		csvFileBuilder.append("1 : Strongly Agree" + NEW_LINE);
		csvFileBuilder.append("2 : Agree" + NEW_LINE);
		csvFileBuilder.append("3 : Undecided" + NEW_LINE);
		csvFileBuilder.append("4 : Disagree" + NEW_LINE);
		csvFileBuilder.append("5 : Strongly Disagree" + NEW_LINE);
		csvFileBuilder.append(NEW_LINE);
		csvFileBuilder.append(",Example,Example,Example,Example,Example,Example,Example,Example" + NEW_LINE);
		csvFileBuilder.append(",ID_image, tag1, tag2, tag3, tag4, tag5, tag6, tag7"+NEW_LINE);
		csvFileBuilder.append(",,5,3,2,5,1,1,2" + NEW_LINE);
		csvFileBuilder.append(NEW_LINE);
		csvFileBuilder.append(",---------------------------------------------------------" + NEW_LINE);
		csvFileBuilder.append(",---------------------------------------------------------" + NEW_LINE);
		
		for(RelevantTagsExperiment test : tests){
			csvFileBuilder.append("," + test.getImageID());
			for(PairNodeScore p : test.getCandidates())
				csvFileBuilder.append(","+p.getLabel());
			csvFileBuilder.append(NEW_LINE);
			csvFileBuilder.append(NEW_LINE);
			csvFileBuilder.append(NEW_LINE);
		}
		
		PrintStream csvFile = new PrintStream(new FileOutputStream(fileName+".csv", false));
		csvFile.println(csvFileBuilder.toString());
		csvFile.close();
	}
	
	private static void generateTestsLots(String imagePath, ArrayList<RelevantTagsExperiment> tests) throws IOException {
		int lotsSize = 5;
		int numLot = 1;
		Random rand = new Random();
		String testType = tests.get(0).getLabel();
		
		while(numLot<4){
			String fileFolder = OUTPUT_FOLDER+"userTests/"+testType+"/"+testType+numLot+"/";
			for(File file: new File(fileFolder).listFiles()) 
				file.delete();
			String fileName = fileFolder+testType+numLot;
			ArrayList<RelevantTagsExperiment> subListTests = new ArrayList<RelevantTagsExperiment>();
			ArrayList<Integer> indexes = new ArrayList<Integer>();
			for(int i=0; i<lotsSize; i++){
				int currentIndex = rand.nextInt(tests.size());
				while(indexes.contains(currentIndex))
					currentIndex = rand.nextInt(tests.size());
				indexes.add(currentIndex);
				subListTests.add(tests.get(currentIndex));
			}
			
			// Copy img files
			for(RelevantTagsExperiment test : subListTests){
				String cpCmd = "cp " + imagePath+test.getImageID() + ".jpg " + fileFolder;
				Runtime.getRuntime().exec(cpCmd);
			}
			
			exportTestsToHTML(subListTests, fileName);
			exportTestsToCSV(subListTests, fileName);
			numLot++;
		}
	}
	
	private static HashMap<String, Node> removePercentageofTags(int p2) {
		HashMap<String, Node> removedTags = new HashMap<String, Node>();
		ArrayList<Integer> indexes = new ArrayList<Integer>();
		int size = nodes.size();
		int totalToRemove = Math.round(size*p2/100);
		Random random = new Random();
		
		for(int i=0; i<totalToRemove; i++){
			int index=random.nextInt(size);
			while(indexes.contains(index)){
				index = random.nextInt(size);
			}
			indexes.add(index);
			int j = 0;
			for(Entry<String, Node> e : nodes.entrySet()){
				if(j==index){
					removedTags.put(e.getKey(), e.getValue());
					break;
				} else {
					j++;
				}
				
			}
		}
		for(String key : removedTags.keySet()){
			nodes.remove(key);
		}
		
		return removedTags;
	}
	
	/*
	 * Mathematical evaluation function for a list of new tags given a chosen method
	 * Dist_moy= Somme_pour_les_mots_prpoposés (distmin(mot_proposé_i,mots_enlevés)/|mots_proposés| 
	 */
	private static double distanceOldNewTags(Collection<Node> collection, List<PairNodeScore> newTags, String DBname){
		SemDistance semDist = new SemDistance(DBname, graphDb);
		int sumDist = 0;
		Transaction tx = graphDb.beginTx();
		for(PairNodeScore pair : newTags){
			int currentMinDist=100;
			if(pair.getNode()!=null){
				for(Node del : collection){
					Path p = semDist.findShortestPathAll(pair.getNode(), del);
					if(p!=null){
						int spLength = p.length();
						if(spLength < currentMinDist)
							currentMinDist = spLength;
					}
				}
			}
			sumDist +=currentMinDist;
		}
		tx.close();
		return (double) sumDist/newTags.size();
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
