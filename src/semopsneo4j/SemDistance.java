package semopsneo4j;

import gexfparserforneo4jdb.neo4j.RelTypes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphalgo.GraphAlgoFactory;
import org.neo4j.graphalgo.PathFinder;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.PathExpanders;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.event.TransactionData;
import org.neo4j.graphdb.event.TransactionEventHandler;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

import treegenerator.services.Inflector;

public class SemDistance {
	protected String DBname;
	protected Node rootNode;
	protected Node bottomNode;
	protected GraphDatabaseService graphDb;
	HashMap<String, Node> nodes;
	
	public SemDistance(String DBname) {
		this.DBname = DBname;
		this.graphDb = connectDB(DBname);
		this.nodes = new HashMap<String, Node>();
	}
	
	public double[][] createMatrix(ArrayList<String> currentTags) {
		Inflector inf = Inflector.getInstance();
		//Matrix size
		int nbTags = currentTags.size();
		double[][] resultMatrix = new double[nbTags][nbTags];
		
		// Generate matrix
		// Nodes are pulled out DB only once : for the matrix "first line"
		Node node1=null;
		Node node2=null;
		double currentDistance=-1.0;
		for(int i=0;i<nbTags; i++) {
			String singularizedBaseURI1 = "base:"+inf.singularize(currentTags.get(i));
			if(i==0){
				node1 = findConceptByURI(singularizedBaseURI1, graphDb);
				nodes.put(singularizedBaseURI1, node1);
			}
			else {
				node1=nodes.get(singularizedBaseURI1);
			}
			for(int j=i; j<nbTags; j++){
				String singularizedBaseURI2 = "base:"+inf.singularize(currentTags.get(j));
				if(i==0 && j>0) {
					node2 = findConceptByURI(singularizedBaseURI2, graphDb);
					nodes.put(singularizedBaseURI2, node2);
				}
				else 
					node2 = nodes.get(singularizedBaseURI2);		
				
				if(node1!=null && node2!=null)
					currentDistance = wuPalmerEvolvedMeasure(node1, node2, graphDb);
				else
					currentDistance = -1.0;
				resultMatrix[j][i] = currentDistance;
			}
		}
		return resultMatrix;
	}
	
	public double[][] createMatrixAllBase(ArrayList<String> allBaseTags) {
		Inflector inf = Inflector.getInstance();
		
		//Matrix size
		int nbTags = allBaseTags.size();
		double[][] resultMatrix = new double[nbTags][nbTags];
		
		//Iterate through arraylist
		Node node1;
		Node node2;
		double currentDistance=-1.0;
		for(int i=0;i<nbTags; i++) {
			String singularizedBaseURI1 = "base:"+inf.singularize(allBaseTags.get(i));
			node1 = nodes.get(singularizedBaseURI1);
//			node1 = findConceptByURI(allBaseTags.get(i), graphDb);
			for(int j=i; j<nbTags; j++){
				String singularizedBaseURI2 = "base:"+inf.singularize(allBaseTags.get(j));
				node2 = nodes.get(singularizedBaseURI2);
//				node2 = findConceptByURI(allBaseTags.get(j), graphDb);
				if(node1!=null && node2!=null)
					currentDistance = wuPalmerEvolvedMeasure(node1, node2, graphDb);
				else
					currentDistance = -1.0;
				resultMatrix[j][i] = currentDistance;
			}
		}
		return resultMatrix;
	}
	
	public ArrayList<String> findAllBaseTags() {
		
		ArrayList<String>  nodesResult= new ArrayList<String>();
		ExecutionEngine engine = new ExecutionEngine( graphDb );
		String query = "start n = node(*) where n.uri =~ 'base:.*' return n.uri;";
		ExecutionResult result = (ExecutionResult) engine.execute( query);
		
		ResourceIterator<String> it = result.columnAs("n.uri");
		while (it.hasNext()){
			nodesResult.add(it.next().substring(5));
		}
		return nodesResult;
	}
	
	public void closeDB(){
		this.graphDb.shutdown();
	}

//	private double wuPalmerMeasure(Node node1, Node node2, Node rootNode, GraphDatabaseService graphDb){
//		double result = -1.0;
//		Transaction tx = graphDb.beginTx();hah, 
//		try {	
//			Node lca = findLCA(node1, node2, graphDb);
//			System.out.println("LCA for " + node1.getProperty("uri") + " and "+node2.getProperty("uri") + " is : " + lca.getProperty("uri"));
//			PathFinder<Path> finder = GraphAlgoFactory.shortestPath(
//					PathExpanders.forTypesAndDirections( RelTypes.PARENT, Direction.INCOMING, RelTypes.EQUIV, Direction.BOTH ), 20);
//			int lcaToRoot = finder.findSinglePath(rootNode,lca).length();
//			//Substract 1 cause base:xxx count as 0
//			int node1toLCA = finder.findSinglePath(lca, node1).length()-1;
//			int node2toLCA = finder.findSinglePath(lca, node2).length()-1;
//			result = (double) (2*lcaToRoot)/(2*lcaToRoot+node1toLCA+node2toLCA);
//			tx.success();
//		} finally {
//			tx.close();			
//		}
//		return result;
//	}
	
	private double wuPalmerEvolvedMeasure(Node node1, Node node2, GraphDatabaseService graphDb) {
		double result = -1.0;
		Transaction tx = graphDb.beginTx();
		try {
			if(node1.equals(node2))
				return 1.0;
			
			Node lca = findLCA(node1, node2, graphDb);
			if(lca==null){
				return result;
			}
								
			System.out.println("LCA for " + node1.getProperty("uri") + " and "+node2.getProperty("uri") + " is : ");
			System.out.println(lca.getProperty("uri"));
			//Wu-Palmer data
			Path lcaToRootPath = findShortestPath(rootNode,lca);
			if(lcaToRootPath==null)
				return result;
			
			int lcaToRoot = lcaToRootPath.length()-1;
			//Substract 1 cause base:xxx count as 0
			int node1toLCA = findShortestPath(lca, node1).length()-1;
			int node2toLCA = findShortestPath(lca, node2).length()-1;
			
			//Evolved
			PathFinder<Path> finderLong = GraphAlgoFactory.allPaths(
					PathExpanders.forTypesAndDirections(
							RelTypes.PARENT, Direction.OUTGOING,
							RelTypes.EQUIV, Direction.BOTH, 
							RelTypes.VIRTUAL, Direction.OUTGOING), 20);
			int bottomtoLCA = 0;
			Iterator<Path> it = finderLong.findAllPaths(bottomNode, lca).iterator();			
			while(it.hasNext()) {
				Path current = it.next();
				if (current.length()-1 > bottomtoLCA) {
					bottomtoLCA = current.length()-1;
				}
			}			
					
			result = (double) (2*lcaToRoot)/(2*lcaToRoot+node1toLCA+node2toLCA+(bottomtoLCA*node1toLCA*node2toLCA));
			tx.success();
		} finally {
			tx.close();			
		}
		
		return result;
	}
	
	private Path findShortestPath(Node node1, Node node2) {
		PathFinder<Path> finder = GraphAlgoFactory.shortestPath(
				PathExpanders.forTypesAndDirections( 
						RelTypes.PARENT, Direction.INCOMING, 
						RelTypes.EQUIV, Direction.BOTH, 
						RelTypes.VIRTUAL, Direction.INCOMING ), 20);
		return finder.findSinglePath(node1,node2);
	}
	
	private GraphDatabaseService connectDB(String DBname) {
		graphDb = new GraphDatabaseFactory().newEmbeddedDatabase( DBname );
		rootNode = findConceptByURI("virtual:top",graphDb);
		bottomNode = findConceptByURI("virtual:bottom",graphDb);
		graphDb.registerTransactionEventHandler(new TransactionEventHandler<Void>() {
			 public Void beforeCommit(TransactionData data) throws Exception {
				 for(Node createdNode : data.createdNodes()){
					 if(((String)createdNode.getProperty("uri")).startsWith("base:")){
						 bottomNode.createRelationshipTo(createdNode, RelTypes.VIRTUAL);
					 }
				 }				 
			 return null;
			 }
			  
			 public void afterCommit(TransactionData data, Void state) {
			 }
			  
			 public void afterRollback(TransactionData data, Void state) {
			 }
		});
		
		registerShutdownHook( graphDb );
		
		return graphDb;
	}
	
	private Node findConceptByURI(String URI, GraphDatabaseService graphDb) {
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

	private Node findLCA(Node node1, Node node2, GraphDatabaseService graphDb){
		Node nodeResult=null;
		ExecutionEngine engine = new ExecutionEngine( graphDb );
		String query = "MATCH path=(node1 {uri:\""+node1.getProperty("uri")+"\"})" +
						"-[:PARENT|:EQUIV*]->lca<-[:PARENT|:EQUIV*]-"+
						"(node2 {uri:\""+node2.getProperty("uri")+"\"})"+
						"RETURN lca "+
						"order by length(path)"+
						"LIMIT 1";
		ExecutionResult result = (ExecutionResult) engine.execute( query);
		
		ResourceIterator<Node> it = result.columnAs("lca");
		if (it.hasNext()){
			nodeResult = it.next();
		}
		return nodeResult;
	}

	private void registerShutdownHook( final GraphDatabaseService graphDb )
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
