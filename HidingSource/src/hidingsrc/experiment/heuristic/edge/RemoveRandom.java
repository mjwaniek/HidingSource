package hidingsrc.experiment.heuristic.edge;

import hidingsrc.core.Coalition;
import hidingsrc.core.Graph;
import hidingsrc.utils.Utils;

public class RemoveRandom extends RemoveHeuristic {
	
	@Override
	public String getName() {
		return "rem-random";
	}

	@Override
	protected double score(int i, Graph g, int evader, Coalition infected) {
		return Utils.RAND.nextDouble();
	}
}
