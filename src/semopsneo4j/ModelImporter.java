package semopsneo4j;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;

import gexfparserforneo4jdb.neo4j.RelTypes;

import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.event.TransactionData;
import org.neo4j.graphdb.event.TransactionEventHandler;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

import treegenerator.model.OnlineConcept;
import treegenerator.services.Inflector;

public class ModelImporter {
	protected GraphDatabaseService graphDb;
	

	public ModelImporter(String DBname) {
		this.graphDb = startDB(DBname);
	}

	public ModelImporter(GraphDatabaseService graphDb) {
		this.graphDb = graphDb; 
	}

	public void importDataModel(Hashtable<String, OnlineConcept> dataModel, String DBname, boolean shutdownDB) {
		
		Transaction tx = graphDb.beginTx();
		try 
		{
			//Nodes creation
			Label label = DynamicLabel.label( "Concept" );
			int nodeSuccess =0;
			int nodeFail = 0;
			
			Enumeration<OnlineConcept> e = dataModel.elements();
			OnlineConcept currentConcept;
			while(e.hasMoreElements()) {
				currentConcept = e.nextElement();
				if(findConceptByURI(currentConcept.getUri(), graphDb)==null){
		    		//create and set attributes to the node pushed in the DB
			    	org.neo4j.graphdb.Node currentNeoNode = graphDb.createNode(label);
			    	currentNeoNode.setProperty("uri", currentConcept.getUri());
			    	currentNeoNode.setProperty("startingConcept", currentConcept.getStartingConcept());
			    	
			    	nodeSuccess++;
		    	} 
		    	else {
		    		nodeFail++;
		    	}	
			}
			System.out.println("Nodes creation completed.");
		    System.out.println("Created : " + nodeSuccess);
		    System.out.println("Already existing : " + nodeFail);
		    
		    //Edges creation
		    int edgesCreated=0;
	    	int edgesExists=0;
			OnlineConcept sourceConcept;
	    	Enumeration<OnlineConcept> e2 = dataModel.elements();
	    	while(e2.hasMoreElements()){
	    		sourceConcept = e2.nextElement();
	    		Node sourceNode = findConceptByURI(sourceConcept.getUri(), graphDb);
	    		for(OnlineConcept targetConcept : sourceConcept.getParents()) {
	    			Node targetNode = findConceptByURI(targetConcept.getUri(), graphDb);	    			
	    			if(sourceNode!=null && targetNode!=null) {
	    				RelTypes edgeType = sourceConcept.getType().equals(targetConcept.getType()) ? RelTypes.PARENT : RelTypes.EQUIV;
	    				if(!relationExists(sourceNode, targetNode, edgeType)){
	    		    		sourceNode.createRelationshipTo(targetNode, edgeType);
	    		    		edgesCreated++;
	    		    	} else {
	    		    		edgesExists++;
	    		    	}
	    			}
	    		}
	    	}
	    	System.out.println("Edges creation completed.");
		    System.out.println("Created : " + edgesCreated);
		    System.out.println("Already existing : " + edgesExists);
			
			tx.success();
		} finally {
			tx.close();
			if(shutdownDB)
				graphDb.shutdown();
		}
	}
	
	
	private GraphDatabaseService startDB(String DBname) {
		GraphDatabaseService graphDb = new GraphDatabaseFactory().newEmbeddedDatabase( DBname );
		final Node bottomNode = findConceptByURI("virtual:bottom", graphDb);
		final Node topNode = findConceptByURI("virtual:top", graphDb);
		graphDb.registerTransactionEventHandler(new TransactionEventHandler<Void>() {
			 public Void beforeCommit(TransactionData data) throws Exception {
				 for(Node createdNode : data.createdNodes()){
					 if(((String)createdNode.getProperty("uri")).startsWith("base:")){
						 bottomNode.createRelationshipTo(createdNode, RelTypes.VIRTUAL);
					 } else if (createdNode.getProperty("uri").equals("Wordnet:entity")|| createdNode.getProperty("uri").equals("http://www.w3.org/2002/07/owl#Thing")) {
						 createdNode.createRelationshipTo(topNode, RelTypes.VIRTUAL);
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
	
	public ArrayList<String> checkTags(ArrayList<String> currentTags) {
		Inflector inf = Inflector.getInstance();
		ArrayList<String> tagsToCreate = new ArrayList<>();
		String tag;
		for(int i=0; i<currentTags.size(); i++) {
			tag = currentTags.get(i);
			if (findConceptByURI("base:"+inf.singularize(tag), graphDb)==null){
				tagsToCreate.add(inf.singularize(tag));
			}
		}
		return tagsToCreate;
	}
	
	public void closeDB(){
		this.graphDb.shutdown();
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
	
	private static boolean relationExists(Node sourceNode, Node targetNode, RelTypes type) {
		boolean exist = false;
		if(sourceNode.getRelationships(type)!=null){
			for(Relationship r : sourceNode.getRelationships(type)){
				if(r.getOtherNode(sourceNode).equals(targetNode)){
					exist = true;
					break;
				}
			}
		}	
		return exist;
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
