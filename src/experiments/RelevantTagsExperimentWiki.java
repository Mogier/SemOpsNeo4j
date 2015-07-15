package experiments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;

import semopsneo4j.PairNodeScore;
import semopsneo4j.WikipediaCrawler;

public class RelevantTagsExperimentWiki extends RelevantTagsExperiment {

	public RelevantTagsExperimentWiki(int nbCandidates) {
		super(nbCandidates);
	}

	@Override
	void findNewTags() {
		long startTime = System.currentTimeMillis();
		WikipediaCrawler crawler = new WikipediaCrawler("https://en.wikipedia.org/wiki/");
		candidates = new ArrayList<PairNodeScore>();
		HashMap<String,Integer> tagsCandidats = new HashMap<String, Integer>();
		
		for(String tag : RunExperiments.nodes.keySet()){
			Elements currentElements = crawler.getWikilinks(tag);
			if(currentElements !=null){
				for(Element elem : currentElements){
					Integer previousValue = tagsCandidats.get(elem.attr("title"));
					tagsCandidats.put(elem.attr("title"), previousValue == null ? 1 : previousValue + 1);
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
