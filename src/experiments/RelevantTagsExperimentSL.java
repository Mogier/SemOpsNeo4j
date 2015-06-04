package experiments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Transaction;

import semopsneo4j.PairNodeScore;

/*
 * This class implement an experiment with the use of sublists of input tags
 */
public class RelevantTagsExperimentSL extends RelevantTagsExperiment {
	protected int sizeSubLists;

	public RelevantTagsExperimentSL(int maxLenthBetweenNodes, int nbCandidates,	int sizeSubLists, ArrayList<String> inputTags) {
		super(maxLenthBetweenNodes, nbCandidates, inputTags);
		this.sizeSubLists = sizeSubLists;
	}

	public int getSizeSubLists() {
		return sizeSubLists;
	}

	public void setSizeSubLists(int sizeSubLists) {
		this.sizeSubLists = sizeSubLists;
	}
	
	@Override
	protected double computeScores(Node currentNode, HashMap<Node, ArrayList<PairNodeScore>> ilots) {
		double globalScore = 0.0;
		ArrayList<Double> scores = new ArrayList<Double>();
		for(Node currentInputNode : ilots.keySet()){
			double currentScore=maxLenthBetweenNodes;
			Path shortPath = findShortestPath(currentNode,currentInputNode);
			if(shortPath!=null)
				currentScore = shortPath.length();
			scores.add(currentScore);
		}
		Collections.sort(scores);
		for(int i=0; i<sizeSubLists; i++)
			globalScore+=1/scores.get(i);
		return globalScore;
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
		result.append("subList size : " + sizeSubLists + NEW_LINE);
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
