package semopsneo4j;

import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.PathExpanders;
import org.neo4j.graphdb.RelationshipExpander;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphalgo.GraphAlgoFactory;
import org.neo4j.graphalgo.PathFinder;
import org.neo4j.graphalgo.impl.ancestor.AncestorsUtil;
import org.neo4j.kernel.Traversal;

import gexfparserforneo4jdb.neo4j.RelTypes;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
public class SemOpsNeo4j {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String DBname = "/home/mael/Documents/Neo4j/neo4j-community-2.1.7/data/test.db";
		
		GraphDatabaseService graphDb = connectDB(DBname);
		Node rootNode = findConceptByURI("Wordnet:entity", graphDb);
		Node node1 = findConceptByURI("base:dog", graphDb);
		Node node2 = findConceptByURI("base:poodle", graphDb);
		
		double measure = wuPalmerMeasure(node1, node2, rootNode, graphDb);
		System.out.println(measure);
	}
	
	public static double wuPalmerMeasure(Node node1, Node node2, Node rootNode, GraphDatabaseService graphDb){
		double result = -1.0;
		Transaction tx = graphDb.beginTx();
		try {
			List<Node> listOfTwoNodes = new ArrayList<Node>();
			listOfTwoNodes.add(node1);
			listOfTwoNodes.add(node2);
			RelationshipExpander expander = Traversal.expanderForTypes(RelTypes.PARENT, Direction.OUTGOING);
			Node lca = AncestorsUtil.lowestCommonAncestor(listOfTwoNodes, expander);
			System.out.println("Root is : " + rootNode.getProperty("uri"));
			System.out.println("LCA for " + node1.getProperty("uri") + " and "+node2.getProperty("uri") + " is : " + lca.getProperty("uri"));
			PathFinder<Path> finder = GraphAlgoFactory.shortestPath(
					PathExpanders.forTypesAndDirections( RelTypes.PARENT, Direction.INCOMING, RelTypes.EQUIV, Direction.BOTH ), 20);
			int lcaToRoot = finder.findSinglePath(rootNode,lca).length();				
			int node1toLCA = finder.findSinglePath(lca, node1).length();
			int node2toLCA = finder.findSinglePath(lca, node2).length();
			result = (double) (2*lcaToRoot)/(2*lcaToRoot+node1toLCA+node2toLCA);
			tx.success();
		} finally {
			tx.close();			
		}
		return result;
	}
	
	private static Node findConceptByURI(String URI, GraphDatabaseService graphDb) {
		Node nodeResult=null;
		ExecutionEngine engine = new ExecutionEngine( graphDb );
		String query = "MATCH (concept {uri:\""+URI+"\"})" +
						"RETURN concept "+
						"LIMIT 1";
		ExecutionResult result = (ExecutionResult) engine.execute( query);
		
		ResourceIterator<Node> it = result.columnAs("concept");
		while (it.hasNext()){
			nodeResult = it.next();
		}
		return nodeResult;
	}
	
	private static GraphDatabaseService connectDB(String DBname) {
		GraphDatabaseService graphDb = new GraphDatabaseFactory().newEmbeddedDatabase( DBname );
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
