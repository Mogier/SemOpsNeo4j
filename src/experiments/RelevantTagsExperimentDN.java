package experiments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;

import semopsneo4j.BFSTraverser;
import semopsneo4j.PairNodeScore;
import treegenerator.services.Inflector;

public class RelevantTagsExperimentDN extends RelevantTagsExperiment {

	public RelevantTagsExperimentDN(int maxLenthBetweenNodes, int nbCandidates,ArrayList<String> inputTags) {
		super(maxLenthBetweenNodes, nbCandidates, inputTags);
	}

	@Override
	public void findNewTags(){
		candidates = new ArrayList<PairNodeScore>();
		HashMap<Node,Integer> tagsCandidats = new HashMap<Node, Integer>();
		ArrayList<Node> inputNodes = new ArrayList<Node>();
		Inflector inf = Inflector.getInstance();
		
		// Init
		for(String tag : inputTags){
			String baseTag = "base:"+inf.singularize(tag);
			Node tagNode;
			if(RunExperiments.nodes.containsKey(baseTag))
				tagNode=RunExperiments.nodes.get(baseTag);
			else
				tagNode=findConceptByURI(baseTag);
			inputNodes.add(tagNode);
		}
		
		// Parcours
		Transaction tx = RunExperiments.graphDb.beginTx();
		try {
			for(Node current : inputNodes){
				ArrayList<Node> directParents = getParentsNode(current, 0);
				for(Node parent : directParents){
					Integer previousValue = tagsCandidats.get(parent);
					tagsCandidats.put(parent, previousValue == null ? 1 : previousValue + 1);
				}
			}
			for(Node node : tagsCandidats.keySet()){
				candidates.add(new PairNodeScore(node, tagsCandidats.get(node).doubleValue()));				
			}
			tx.success();
		} finally {
			tx.close();			
		}	
		Collections.sort(candidates);
		candidates = candidates.subList(0, nbCandidates);
	}
	
	@Override
	double computeScores(Node currentNode,HashMap<Node, ArrayList<PairNodeScore>> ilots) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	/*
	 * This function returns the parent node of the one in input.
	 * 1st and 2nd parents are taking in account because of the virtual base tags (base:dog <=> Wordnet:dog)
	 */
	private ArrayList<Node> getParentsNode(Node node, int depth){
		ArrayList<Node> parents = new ArrayList<Node>();
		Iterator<Relationship> itRel = node.getRelationships(Direction.OUTGOING).iterator();
		while(itRel.hasNext()){
			Node currentParent = itRel.next().getEndNode();
			parents.add(currentParent);
			if(depth==0)
				parents.addAll(getParentsNode(currentParent, 1));			
		}
		
		return parents;
	}
	
	@Override
	public String toString(){
		StringBuilder result = new StringBuilder();
		String NEW_LINE = System.getProperty("line.separator");
		result.append("Type : " + this.getClass().getName() + NEW_LINE);
		result.append("================== Parameters =================="+NEW_LINE);
		result.append("InputTags : " + inputTags+ NEW_LINE);
		result.append("Nb candidates : " + nbCandidates + NEW_LINE);
		result.append("Max dist : " + maxLenthBetweenNodes + NEW_LINE);
		result.append("================== Results =================="+NEW_LINE);
		Transaction tx = RunExperiments.graphDb.beginTx();
		try {
			result.append(candidates);
			tx.success();
		} finally {
			tx.close();			
		}
		result.append(NEW_LINE);
		return result.toString();		
	}

}
