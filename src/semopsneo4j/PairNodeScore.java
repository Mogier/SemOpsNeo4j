package semopsneo4j;

import org.neo4j.graphdb.Node;

public class PairNodeScore {
	private Node node;
	private double score;
	
	public PairNodeScore(Node node, double score) {
		super();
		this.node = node;
		this.score = score;
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
		return this.node.getProperty("uri")+"|"+this.score;
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
	
	
}
