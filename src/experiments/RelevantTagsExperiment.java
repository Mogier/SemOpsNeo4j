package experiments;

import java.util.List;

import semopsneo4j.PairNodeScore;

public abstract class RelevantTagsExperiment {
	protected String label;
	int nbCandidates;
	protected List<PairNodeScore> candidates;
	protected long execTime;
	
	public RelevantTagsExperiment(int nbCandidates) {
		super();
		this.nbCandidates = nbCandidates;
	}
	
	abstract void findNewTags();
	
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
}
