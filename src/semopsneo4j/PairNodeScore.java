package semopsneo4j;

import org.neo4j.graphdb.Node;

public class PairNodeScore implements Comparable<PairNodeScore> {
	private Node node;
	private String label;
	private double score;
	
	public PairNodeScore(Node node, double score) {
		super();
		this.node = node;
		this.score = score;
		
		if(((String)node.getProperty("uri")).startsWith("Wordnet"))
			label = ((String)node.getProperty("uri")).substring(8).toLowerCase();
		else if(((String)node.getProperty("uri")).startsWith("http")){
			int lastSlash = ((String)node.getProperty("uri")).lastIndexOf('/');
			label = ((String)node.getProperty("uri")).substring(lastSlash+1).toLowerCase();
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((node == null) ? 0 : node.hashCode());
		long temp;
		temp = Double.doubleToLongBits(score);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	@Override
    public boolean equals(Object object)
    {
        boolean sameSame = false;

        if (object != null && object instanceof PairNodeScore)
        {
            sameSame = this.node.getProperty("uri") == ((PairNodeScore) object).node.getProperty("uri");
        }

        return sameSame;
    }
	
	@Override
	public String toString()
	{		
		return this.label+"|"+this.score;
	}

	public Node getNode() {
		return node;
	}

	public void setNode(Node node) {
		this.node = node;
	}

	public double getScore() {
		return score;
	}

	public void setScore(double score) {
		this.score = score;
	}

	
	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	@Override
	public int compareTo(PairNodeScore arg0) {
		if(this.getScore()<arg0.getScore())
			return 1;
		else if(this.getScore()>arg0.getScore())
			return -1;
		return 0;
	}
	
	
}
