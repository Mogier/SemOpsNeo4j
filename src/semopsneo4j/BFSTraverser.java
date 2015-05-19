package semopsneo4j;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

/*
 * Breadth First Search Traverser 
 */
public class BFSTraverser {
	protected Queue<Node> queue;// = new LinkedList<Node>();
	protected List<Node> visitedNodes;
	protected Node rootNode;
	
	
	
	public BFSTraverser(Node rootNode) {
		this.queue = new LinkedList<Node>();
		this.rootNode = rootNode;
		this.queue.add(this.rootNode);
		this.visitedNodes = new ArrayList<Node>();
	}

	public Queue<Node> getQueue() {
		return queue;
	}

	public void setQueue(Queue<Node> queue) {
		this.queue = queue;
	}

	public List<Node> getVisitedNodes() {
		return visitedNodes;
	}

	public void setVisitedNodes(List<Node> visitedNodes) {
		this.visitedNodes = visitedNodes;
	}

	public Node getRootNode() {
		return rootNode;
	}

	public void setRootNode(Node rootNode) {
		this.rootNode = rootNode;
	}

	public boolean hasNext(){
		return !queue.isEmpty();
	}
	
	/*
	 * Process BFS algorithm and return the last reached Node
	 */
	public Node next(){
		Node removedNode = queue.remove(); 
		visitedNodes.add(removedNode);
		
		Iterator<Relationship> itRel = removedNode.getRelationships(Direction.OUTGOING).iterator();
		while(itRel.hasNext()){
			Node currentParent = itRel.next().getEndNode();
			
			if(!visitedNodes.contains(currentParent)){
				queue.add(currentParent);
			}
		}
		return removedNode;
	}
}
