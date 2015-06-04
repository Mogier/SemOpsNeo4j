package experiments;

import gexfparserforneo4jdb.neo4j.RelTypes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

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

import semopsneo4j.BFSTraverser;
import semopsneo4j.PairNodeScore;
import treegenerator.services.Inflector;

public abstract class RelevantTagsExperiment {	
	int maxLenthBetweenNodes;
	int nbCandidates;
	protected ArrayList<String> inputTags;
	protected List<PairNodeScore> candidates;
	
	public RelevantTagsExperiment(int maxLenthBetweenNodes, int nbCandidates, ArrayList<String> inputTags) {
		super();
		this.maxLenthBetweenNodes = maxLenthBetweenNodes;
		this.nbCandidates = nbCandidates;
		this.inputTags = inputTags;
	}
	
	public void findNewTags(){
		HashMap<Node,BFSTraverser> traversers = new HashMap<Node, BFSTraverser>();
		HashMap<Node, ArrayList<PairNodeScore>> ilots = new HashMap<Node, ArrayList<PairNodeScore>>();
		ArrayList<PairNodeScore> tagsCandidats = new ArrayList<PairNodeScore>();
		boolean tousParcoursTermines = false;
		Inflector inf = Inflector.getInstance();
		
		// Init
		for(String tag : inputTags){
			String baseTag = "base:"+inf.singularize(tag);
			Node tagNode;
			if(RunExperiments.nodes.containsKey(baseTag))
				tagNode=RunExperiments.nodes.get(baseTag);
			else
				tagNode=findConceptByURI(baseTag);
			traversers.put(tagNode, new BFSTraverser(tagNode));
			ilots.put(tagNode, new ArrayList<PairNodeScore>());
		}
		
		// Parcours
		Transaction tx = RunExperiments.graphDb.beginTx();
		try {
			while(tagsCandidats.size()<nbCandidates && !tousParcoursTermines){
				tousParcoursTermines=true;
				for(BFSTraverser traverser : traversers.values()){
					if(traverser.hasNext()){
						Node currentNode = traverser.next();
						Node currentRoot = traverser.getRootNode();
						// Wu-Palmer or simple distance
						//double score = wuPalmerEvolvedMeasure(traverser.getRootNode(), currentNode, graphDb);
						Path shortPath = findShortestPath(currentNode,traverser.getRootNode());
						double score =shortPath.length();
						ilots.get(currentRoot).add(new PairNodeScore(currentNode, score));
						
						if(!containsNode(currentNode, tagsCandidats)){
							List<Node> occurences = intersection(ilots, currentNode);
							if(occurences.size()>1){
								double globalScore = computeScores(currentNode,ilots);
								tagsCandidats.add(new PairNodeScore(currentNode, globalScore));
							}
						}
						if(traverser.hasNext())
							tousParcoursTermines=false;
					}
				}
			}
			tx.success();
		} finally {
			tx.close();			
		}
		candidates = tagsCandidats;
		Collections.sort(candidates);
	}
	
	abstract double computeScores(Node currentNode, HashMap<Node, ArrayList<PairNodeScore>> ilots);

	public int getMaxLenthBetweenNodes() {
		return maxLenthBetweenNodes;
	}

	public void setMaxLenthBetweenNodes(int maxLenthBetweenNodes) {
		this.maxLenthBetweenNodes = maxLenthBetweenNodes;
	}

	public int getNbCandidates() {
		return nbCandidates;
	}

	public void setNbCandidates(int nbCandidates) {
		this.nbCandidates = nbCandidates;
	}

	public ArrayList<String> getInputTags() {
		return inputTags;
	}

	public void setInputTags(ArrayList<String> inputTags) {
		this.inputTags = inputTags;
	}
	
	protected Node findConceptByURI(String URI) {
		Node nodeResult=null;
		ExecutionEngine engine = new ExecutionEngine( RunExperiments.graphDb );
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
	
	protected boolean containsNode(Node node, ArrayList<PairNodeScore> coll){
		for(PairNodeScore pair : coll){
			if(pair.getNode().equals(node))
				return true;
		}
		return false;
	}
	
	protected Path findShortestPath(Node node1, Node node2) {
		PathFinder<Path> finder = GraphAlgoFactory.shortestPath(
				PathExpanders.forTypesAndDirections( 
						RelTypes.PARENT, Direction.INCOMING, 
						RelTypes.EQUIV, Direction.BOTH, 
						RelTypes.VIRTUAL, Direction.INCOMING ), 20);
		return finder.findSinglePath(node1,node2);
	}
	
	protected List<Node> intersection(HashMap<Node, ArrayList<PairNodeScore>> ilots, Node currentNode) {
		List<Node> initialNodesIntersect = new ArrayList<Node>();
		for(java.util.Map.Entry<Node, ArrayList<PairNodeScore>> entry : ilots.entrySet()){
			if(containsNode(currentNode, entry.getValue())){
				initialNodesIntersect.add(entry.getKey());
			}
				
		}
		return initialNodesIntersect;
	}
}
