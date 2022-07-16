package hidingsrc.experiment.heuristic.edge;

import hidingsrc.core.Coalition;
import hidingsrc.core.Graph;
import hidingsrc.utils.Utils;

/**
 * Hiding the source of diffusion by adding edges to the network.
 * 
 * @author Marcin Waniek
 */
public abstract class AddHeuristic extends EdgeHeuristic {

	@Override
	public String getType() {
		return "edge-add";
	}
	
	@Override
	public Graph hideEvader(Graph g, int evader, Coalition infected, int step) {
		Coalition pot = getPotential(g, evader, infected);
		if (pot.size() > 0)
			g.addEdge(evader, Utils.argmax(pot, i -> score(i, g, evader, infected)));
		return g;
	}
	
	@Override
	protected Coalition getPotential(Graph g, int evader, Coalition infected) {
		Coalition forbidden = getForbidden(g, evader, infected);
		return g.getNeighs(evader).stream().flatMap(i -> g.getNeighs(i).stream()).distinct()
				.filter(i -> i != evader && !g.getNeighs(evader).contains(i) && infected.contains(i)
						&& !forbidden.contains(i))
				.boxed().collect(Coalition.getCollector());
	}
}
