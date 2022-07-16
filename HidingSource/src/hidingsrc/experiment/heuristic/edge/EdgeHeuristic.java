package hidingsrc.experiment.heuristic.edge;

import hidingsrc.core.Coalition;
import hidingsrc.core.Graph;
import hidingsrc.experiment.heuristic.Heuristic;

/**
 * Hiding the source of diffusion by rewiring the network.
 * 
 * @author Marcin Waniek
 */
public abstract class EdgeHeuristic extends Heuristic {
	
	protected abstract Coalition getPotential(Graph g, int evader, Coalition infected);

	protected abstract double score(int i, Graph g, int evader, Coalition infected);
	
	protected double absoluteScoreUpperBound(Graph g, int evader, Coalition infected) {
		return g.size();
	}
	
	protected Coalition getForbidden(Graph g, int evader, Coalition infected) {
		return g.getChanges().map(e -> e.getOther(evader)).collect(Coalition.getCollector());
	}
}
