package hidingsrc.centrality;

import java.util.HashMap;
import java.util.Map;

import hidingsrc.core.Edge;
import hidingsrc.core.Graph;
import hidingsrc.core.GraphChangeListener;
import hidingsrc.core.Ranking;

/**
 * Centrality measure on a graph.
 * 
 * @author Marcin Waniek
 */
public abstract class Centrality implements GraphChangeListener{

	protected Graph g;
	protected Map<Integer, Double> values;
	
	public Centrality(){
		this.g = null;
		this.values = null;
	}

	public abstract String getName();
	
	public Double getCentrality(int v, Graph g){
		prepare(g);
		return values.get(v);
	}
	
	// Either recountCentrality() (computing for all nodes) or computeSingleCentrality() has to be implemented.
	// If you implement the former, set computeSingleCentrality to return getCentrality().
	
	protected void recountCentrality() {
		for (int v : g.nodes())
			values.put(v, computeSingleCentrality(v, g));
	}
	
	/**
	 * If possible, efficiently computes the centrality score of only v and does not store it.
	 * Otherwise, computes scores of all nodes, stores them and returns the score of v. 
	 */
	public abstract double computeSingleCentrality(int v, Graph g); 
	
	public Ranking<Integer> getRanking(Graph g){
		prepare(g);
		return new Ranking<>(values);
	}
	
	private void prepare(Graph g) {
		if (this.g != g) {
			if (this.g != null)
				this.g.unsubscribe(this);
			g.subscribe(this);
			this.g = g;
			this.values = null;
		}
		if (this.values == null){
			this.values = new HashMap<>();
			recountCentrality();		
		}
	}

	@Override
	public void notifyAdd(Graph g, Edge e) {
		reactNotify();
	}

	@Override
	public void notifyRemove(Graph g, Edge e) {
		reactNotify();
	}

	@Override
	public void notifyReset(Graph g) {
		reactNotify();
	}

	private void reactNotify(){
		values = null;
	}
}
