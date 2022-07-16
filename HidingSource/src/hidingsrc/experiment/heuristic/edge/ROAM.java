package hidingsrc.experiment.heuristic.edge;

import hidingsrc.core.Coalition;
import hidingsrc.core.Graph;
import hidingsrc.experiment.heuristic.Heuristic;
import hidingsrc.utils.Utils;

public class ROAM extends Heuristic {

	private int edgesToAdd;
	private Integer v0;
	
	public ROAM(int edgesToAdd) {
		this.edgesToAdd = edgesToAdd;
		this.v0 = null;
	}

	@Override
	public String getName() {
		return "ROAM-" + edgesToAdd;
	}

	@Override
	public String getType() {
		return "edge-other";
	}

	@Override
	public Graph hideEvader(Graph g, int evader, Coalition infected, int step) {
		if (step % (edgesToAdd + 1) == 1) {
			v0 = Utils.argmax(g.getNeighs(evader).stream().filter(i -> infected.contains(i)).boxed(),
					i -> g.getDegree(i));
			if (v0 != null)
				g.removeEdge(evader, v0);
		} else
			if (v0 != null) {
				Integer v1 = Utils.argmin(g.getNeighs(evader).stream()
								.filter(i -> infected.contains(i) && !g.getNeighs(v0).contains(i)).boxed(),
						i -> g.getDegree(i)); 
				if (v1 != null)
					g.addEdge(v0, v1);
			}
		return g;
	}
}
