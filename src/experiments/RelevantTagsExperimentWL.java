package experiments;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Transaction;

import semopsneo4j.PairNodeScore;
import semopsneo4j.SemDistance;

/*
 * This class implement an experiment with the use of the whole list of input tags
 */
public class RelevantTagsExperimentWL extends RelevantTagsExperimentLists {
	protected int k;

	public RelevantTagsExperimentWL(int maxLenthBetweenNodes, int nbCandidates, int k, String imageID, Set<String> tags) {
		super(maxLenthBetweenNodes, nbCandidates, imageID, tags); 
		this.k = k;
		this.label = "WL";
	}

	public int getK() {
		return k;
	}

	public void setK(int k) {
		this.k = k;
	}
	
	@Override
	protected double computeScores(Node currentNode, HashMap<Node, ArrayList<Node>> ilots, ExecutionEngine engine) {
		double globalScore = 0.0;
		SemDistance semDist = new SemDistance(RunExperiments.DBname, RunExperiments.graphDb);
		for(Node currentInputNode : ilots.keySet()){
//			double currentScore = semDist.wuPalmerEvolvedMeasure(currentInputNode, currentNode, engine);	
			double currentScore=maxLenthBetweenNodes;
			Path shortPath = findShortestPath(currentNode,currentInputNode);
			if(shortPath!=null)
				currentScore = shortPath.length();
			
			globalScore+=1/	Math.pow(currentScore, k);
		}
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
		result.append("k : " + k + NEW_LINE);
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
