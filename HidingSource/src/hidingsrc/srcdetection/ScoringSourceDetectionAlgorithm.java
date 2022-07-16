package hidingsrc.srcdetection;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import hidingsrc.core.Coalition;
import hidingsrc.core.Edge;
import hidingsrc.core.Graph;
import hidingsrc.core.GraphChangeListener;
import hidingsrc.core.Ranking;
import hidingsrc.utils.Utils;

/**
 * Representation of a source detection algorithm selecting source based on scoring function.
 * 
 * @author Marcin Waniek
 */
public abstract class ScoringSourceDetectionAlgorithm extends SourceDetectionAlgorithm implements GraphChangeListener {

	protected Map<Integer,Double> scores;
	private Coalition lastActive;
	private Graph lastG;
	
	@Override
	public int detectSource(Coalition active, Graph g) {
		prepare(active, g);
		return Utils.argmax(g.nodes(), i -> scores.get(i));
	}
	
	public double getScore(int i, Coalition active, Graph g) {
		prepare(active, g);
		return scores.get(i);
	}

	/**
	 * Computes scores of all nodes in the network.
	 */
	protected void recountScores(Coalition active, Graph g) {
		Coalition comparison = g.nodesCoalition();
		g.nodes().forEach(i -> scores.put(i, computeSingleScore(i, active, g, comparison)));
	}

	public abstract double computeSingleScore(int i, Coalition active, Graph g, Coalition comparison);

	public Ranking<Integer> getRanking(Coalition active, Graph g){
		prepare(active, g);
		return new Ranking<>(scores);
	}
	
	public Ranking<Integer> getRanking(Collection<Integer> active, Graph g){
		return getRanking(new Coalition(active), g);
	}
	
	protected void prepare(Coalition active, Graph g) {
		if (g != lastG) {
			if (lastG != null)
				lastG.unsubscribe(this);
			g.subscribe(this);
			lastG = g;
			scores = null;
		}
		if (!active.equals(lastActive)) {
			lastActive = new Coalition(active);
			scores = null;
		}
		if (scores == null) {
			scores = new HashMap<>();
			refreshStructures(active, g);
			recountScores(active, g);
		}
	}
	
	protected void refreshStructures(Coalition active, Graph g) {}

	@Override
	public void notifyAdd(Graph g, Edge e) {
		scores = null;
	}

	@Override
	public void notifyRemove(Graph g, Edge e) {
		scores = null;		
	}

	@Override
	public void notifyReset(Graph g) {
		scores = null;
	}
}
