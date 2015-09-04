package experiments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

import semopsneo4j.NLPParser;
import semopsneo4j.PairNodeScore;
import semopsneo4j.WikipediaCrawler;

public class RelevantTagsExperimentWikiContent extends RelevantTagsExperiment {
	protected NLPParser parser;
	
	public RelevantTagsExperimentWikiContent(int nbCandidates, String imageID, Set<String> tags, NLPParser parser) {
		super(nbCandidates, imageID, tags);
		this.label = "WikiContent";
		this.parser = parser;
	}
	
	void findNewTags() {
		long startTime = System.currentTimeMillis();
		WikipediaCrawler crawler = new WikipediaCrawler("https://en.wikipedia.org/wiki/");
		candidates = new ArrayList<PairNodeScore>();
		HashMap<String,Integer> tagsCandidats = new HashMap<String, Integer>();
		
		for(String tag : RunExperiments.nodes.keySet()){
			String para = crawler.getContentText(tag);
			if(para !=null){
				Set<String> tokens = parser.getTokens(para);
				for(String token : tokens){
					Integer previousValue = tagsCandidats.get(token);
					tagsCandidats.put(token, previousValue == null ? 1 : previousValue + 1);
				}
			}
		}
		for(String tag : tagsCandidats.keySet()){
			if(!RunExperiments.nodes.keySet().contains(tag))
				candidates.add(new PairNodeScore(tag, tagsCandidats.get(tag).doubleValue()));				
		}
		
		Collections.sort(candidates);
		candidates = candidates.subList(0, nbCandidates);
		execTime = System.currentTimeMillis() - startTime;
	}
	
	@Override
	public String toString(){
		StringBuilder result = new StringBuilder();
		String NEW_LINE = System.getProperty("line.separator");
		result.append("Type : " + this.getClass().getName() + NEW_LINE);
		result.append("================== Parameters =================="+NEW_LINE);
		result.append("Nb candidates : " + nbCandidates + NEW_LINE);
		result.append("================== Results =================="+NEW_LINE);
		result.append(candidates + NEW_LINE);
		result.append("Exec time : " + execTime + "ms" + NEW_LINE);
		result.append(NEW_LINE);
		return result.toString();		
	}

}
