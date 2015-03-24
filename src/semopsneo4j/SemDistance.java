package semopsneo4j;

import gexfparserforneo4jdb.neo4j.RelTypes;

import java.util.ArrayList;
import java.util.Iterator;

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
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

public class SemDistance {
	protected GraphDatabaseService graphDb;
	protected Node rootNode;
	protected Node bottomNode;
	
	public SemDistance(String DBname) {
		this.graphDb = connectDB(DBname);
		this.rootNode = findConceptByURI("Wordnet:entity");
		this.bottomNode = findConceptByURI("virtual:bottom");
		
	}
	
	public double[][] createMatrix(ArrayList<String> currentTags) {
		//Matrix size
		int nbTags = currentTags.size();
		double[][] resultMatrix = new double[nbTags][nbTags];
		
		//Iterate through arraylist
		Node node1;
		Node node2;
		for(int i=0; i<nbTags; i++) {
			node1 = findConceptByURI("base:"+currentTags.get(i));
			for (int j=0;j<nbTags; j++) {
				node2 = findConceptByURI("base:"+currentTags.get(j));
				resultMatrix[i][j] = wuPalmerEvolvedMeasure(node1, node2, rootNode, bottomNode, graphDb);
			}
		}
		return resultMatrix;
	}
	
	private double wuPalmerMeasure(Node node1, Node node2, Node rootNode, GraphDatabaseService graphDb){
		double result = -1.0;
		Transaction tx = graphDb.beginTx();
		try {	
			Node lca = findLCA(node1, node2, graphDb);
			System.out.println("LCA for " + node1.getProperty("uri") + " and "+node2.getProperty("uri") + " is : " + lca.getProperty("uri"));
			PathFinder<Path> finder = GraphAlgoFactory.shortestPath(
					PathExpanders.forTypesAndDirections( RelTypes.PARENT, Direction.INCOMING, RelTypes.EQUIV, Direction.BOTH ), 20);
			int lcaToRoot = finder.findSinglePath(rootNode,lca).length();
			//Substract 1 cause base:xxx count as 0
			int node1toLCA = finder.findSinglePath(lca, node1).length()-1;
			int node2toLCA = finder.findSinglePath(lca, node2).length()-1;
			result = (double) (2*lcaToRoot)/(2*lcaToRoot+node1toLCA+node2toLCA);
			tx.success();
		} finally {
			tx.close();			
		}
		return result;
	}
	
	private double wuPalmerEvolvedMeasure(Node node1, Node node2, Node rootNode, Node bottomNode, GraphDatabaseService graphDb) {
		double result = -1.0;
		Transaction tx = graphDb.beginTx();
		try {
			Node lca = findLCA(node1, node2, graphDb);
			System.out.println("LCA for " + node1.getProperty("uri") + " and "+node2.getProperty("uri") + " is : " + lca.getProperty("uri"));
			//Wu-Palmer data
			PathFinder<Path> finder = GraphAlgoFactory.shortestPath(
					PathExpanders.forTypesAndDirections( RelTypes.PARENT, Direction.INCOMING, RelTypes.EQUIV, Direction.BOTH ), 20);
			int lcaToRoot = finder.findSinglePath(rootNode,lca).length();
			//Substract 1 cause base:xxx count as 0
			int node1toLCA = finder.findSinglePath(lca, node1).length()-1;
			int node2toLCA = finder.findSinglePath(lca, node2).length()-1;
			
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
	
	private GraphDatabaseService connectDB(String DBname) {
		GraphDatabaseService graphDb = new GraphDatabaseFactory().newEmbeddedDatabase( DBname );
		registerShutdownHook( graphDb );
		
		return graphDb;
	}
	
	private Node findConceptByURI(String URI) {
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
