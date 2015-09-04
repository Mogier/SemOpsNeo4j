package experiments;

import java.util.List;
import java.util.Set;

import semopsneo4j.PairNodeScore;

public abstract class RelevantTagsExperiment {
	protected String imageID;
	protected String label;
	protected Set<String> tagsConsidered;
	protected int nbCandidates;
	protected List<PairNodeScore> candidates;
	protected long execTime;
	protected double avgDistance;
	
	public RelevantTagsExperiment(int nbCandidates, String id, Set<String> tags) {
		super();
		this.nbCandidates = nbCandidates;
		this.imageID = id;
		this.tagsConsidered = tags;
	}
	
	public int getNbCandidates() {
		return nbCandidates;
	}

	public void setNbCandidates(int nbCandidates) {
		this.nbCandidates = nbCandidates;
	}
	
	public List<PairNodeScore> getCandidates() {
		return candidates;
	}

	public void setCandidates(List<PairNodeScore> candidates) {
		this.candidates = candidates;
	}

	public long getExecTime() {
		return execTime;
	}

	public void setExecTime(long execTime) {
		this.execTime = execTime;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public double getAvgDistance() {
		return avgDistance;
	}

	public void setAvgDistance(double avgDistance) {
		this.avgDistance = avgDistance;
	}

	public String getImageID() {
		return imageID;
	}

	public void setImageID(String imageID) {
		this.imageID = imageID;
	}

	public Set<String> getTagsConsidered() {
		return tagsConsidered;
	}

	public void setTagsConsidered(Set<String> tagsConsidered) {
		this.tagsConsidered = tagsConsidered;
	}
	
	
}
