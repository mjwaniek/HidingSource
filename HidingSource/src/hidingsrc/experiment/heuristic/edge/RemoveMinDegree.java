package hidingsrc.experiment.heuristic.edge;

import hidingsrc.core.Coalition;
import hidingsrc.core.Graph;

/**
 * Hiding the source of diffusion by disconnecting the source from nodes with minimal degree.
 * 
 * @author Marcin Waniek
 */
public class RemoveMinDegree extends RemoveHeuristic {

	@Override
	public String getName() {
		return "rem-mindegr";
	}
	
	@Override
	protected double score(int i, Graph g, int evader, Coalition infected) {
		return -g.getDegree(i);
	}
}
