package hidingsrc.experiment.heuristic.edge;

import hidingsrc.core.Coalition;
import hidingsrc.core.Graph;

/**
 * Hiding the source of diffusion by adding edges from source to nodes with minimal degree.
 * 
 * @author Marcin Waniek
 */
public class AddMinDegree extends AddHeuristic {

	@Override
	public String getName() {
		return "add-mindegr";
	}
	
	@Override
	protected double score(int i, Graph g, int evader, Coalition infected) {
		return -g.getDegree(i);
	}
}
