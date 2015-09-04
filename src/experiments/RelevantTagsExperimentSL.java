package experiments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Transaction;

import semopsneo4j.SemDistance;

/*
 * This class implement an experiment with the use of sublists of input tags
 */
public class RelevantTagsExperimentSL extends RelevantTagsExperimentLists {
	protected int sizeSubLists;

	public RelevantTagsExperimentSL(int maxLenthBetweenNodes, int nbCandidates,	int sizeSubLists, String imageID,Set<String> tags) {
		super(maxLenthBetweenNodes, nbCandidates, imageID, tags);
		this.sizeSubLists = sizeSubLists;
		this.label = "SL";
	}

	public int getSizeSubLists() {
		return sizeSubLists;
	}

	public void setSizeSubLists(int sizeSubLists) {
		this.sizeSubLists = sizeSubLists;
	}
	
	@Override
	protected double computeScores(Node currentNode, HashMap<Node, ArrayList<Node>> ilots, ExecutionEngine engine) {
		double globalScore = 0.0;
		SemDistance semDist = new SemDistance(RunExperiments.DBname, RunExperiments.graphDb);
		ArrayList<Double> scores = new ArrayList<Double>();
		for(Node currentInputNode : ilots.keySet()){
//			double currentScore = semDist.wuPalmerEvolvedMeasure(currentInputNode, currentNode, engine);
			double currentScore=maxLenthBetweenNodes;
			Path shortPath = findShortestPath(currentNode,currentInputNode);
			if(shortPath!=null)
				currentScore = shortPath.length();
			scores.add(currentScore);
		}
		Collections.sort(scores);
		for(int i=0; i<Math.min(sizeSubLists, scores.size()); i++)
			globalScore+=1/scores.get(i);
		return globalScore;
	}
	
	@Override
	public String toString(){
		StringBuilder result = new StringBuilder();
		String NEW_LINE = System.getProperty("line.separator");
		result.append("Type : " + this.getClass().getName() + NEW_LINE);
		result.append("================== Parameters =================="+NEW_LINE);
//		result.append("InputTags : " + inputTags+ NEW_LINE);
		result.append("Nb candidates : " + nbCandidates + NEW_LINE);
		result.append("Max dist : " + maxLenthBetweenNodes + NEW_LINE);
		result.append("subList size : " + sizeSubLists + NEW_LINE);
		result.append("================== Results =================="+NEW_LINE);
		Transaction tx = RunExperiments.graphDb.beginTx();
		try {
			result.append(candidates + NEW_LINE);
			tx.success();
		} finally {
			tx.close();			
		}
		result.append("Exec time : " + execTime + "ms" + NEW_LINE);
		result.append(NEW_LINE);
		return result.toString();		
	}
}
