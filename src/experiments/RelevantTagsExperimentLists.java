package experiments;

import gexfparserforneo4jdb.neo4j.RelTypes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.graphalgo.GraphAlgoFactory;
import org.neo4j.graphalgo.PathFinder;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.PathExpanders;
import org.neo4j.graphdb.Transaction;

import semopsneo4j.BFSTraverser;
import semopsneo4j.PairNodeScore;

public abstract class RelevantTagsExperimentLists extends RelevantTagsExperiment{	
	int maxLenthBetweenNodes;
	
	public RelevantTagsExperimentLists(int maxLenthBetweenNodes, int nbCandidates, String imageID, Set<String> tags) {
		super(nbCandidates,imageID, tags);
		this.maxLenthBetweenNodes = maxLenthBetweenNodes;
	}
	

	public void findNewTags(ExecutionEngine engine){
		long startTime = System.currentTimeMillis();
		HashMap<Node,BFSTraverser> traversers = new HashMap<Node, BFSTraverser>();
		HashMap<Node, ArrayList<Node>> ilots = new HashMap<Node, ArrayList<Node>>();
		ArrayList<PairNodeScore> tagsCandidats = new ArrayList<PairNodeScore>();
		boolean tousParcoursTermines = false;
		// Init
		for(Node tagNode : RunExperiments.nodes.values()){
			traversers.put(tagNode, new BFSTraverser(tagNode));
			ilots.put(tagNode, new ArrayList<Node>());
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

						ilots.get(currentRoot).add(currentNode);
						
						if(!containsNode(currentNode, tagsCandidats)){
							List<Node> occurences = intersection(ilots, currentNode);
							if(occurences.size()>1){
								PairNodeScore p = new PairNodeScore(currentNode, 0.0);
								if(!RunExperiments.nodes.containsKey(p.getLabel())){
									double globalScore = computeScores(currentNode,ilots, engine);
									p.setScore(globalScore);
									tagsCandidats.add(p);
								}
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
		execTime = System.currentTimeMillis() - startTime;
	}
	
	abstract double computeScores(Node currentNode, HashMap<Node, ArrayList<Node>> ilots, ExecutionEngine engine);

	public int getMaxLenthBetweenNodes() {
		return maxLenthBetweenNodes;
	}

	public void setMaxLenthBetweenNodes(int maxLenthBetweenNodes) {
		this.maxLenthBetweenNodes = maxLenthBetweenNodes;
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
	
	protected List<Node> intersection(HashMap<Node, ArrayList<Node>> ilots, Node currentNode) {
		List<Node> initialNodesIntersect = new ArrayList<Node>();
		for(java.util.Map.Entry<Node, ArrayList<Node>> entry : ilots.entrySet()){
			if(entry.getValue().contains(currentNode)){
				initialNodesIntersect.add(entry.getKey());
			}				
		}
		return initialNodesIntersect;
	}	
}
