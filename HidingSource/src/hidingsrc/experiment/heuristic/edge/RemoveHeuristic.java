package hidingsrc.experiment.heuristic.edge;

import hidingsrc.core.Coalition;
import hidingsrc.core.Graph;
import hidingsrc.utils.Utils;

/**
 * Hiding the source of diffusion by removing edges from the network.
 * 
 * @author Marcin Waniek
 */
public abstract class RemoveHeuristic extends EdgeHeuristic {
	
	@Override
	public String getType() {
		return "edge-rem";
	}

	@Override
	public Graph hideEvader(Graph g, int evader, Coalition infected, int step) {
		Coalition pot = getPotential(g, evader, infected);
		if (pot.size() > 0)
			g.removeEdge(evader, Utils.argmax(pot, i -> score(i, g, evader, infected)));
		return g;
	}
	
	@Override
	protected Coalition getPotential(Graph g, int evader, Coalition infected) {
		Coalition forbidden = getForbidden(g, evader, infected);
		return new Coalition(g.getNeighs(evader)).filter(i -> infected.contains(i) && !forbidden.contains(i));
	}
}
