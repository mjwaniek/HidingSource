package hidingsrc.experiment.heuristic;

import hidingsrc.core.Coalition;
import hidingsrc.core.Graph;

/**
 * Heuristic algorithm of hiding the source of diffusion.
 * 
 * @author Marcin Waniek
 */
public abstract class Heuristic {

	public abstract String getName();
	public abstract String getType();
	public abstract Graph hideEvader(Graph g, int evader, Coalition infected, int step);
	
	/**
	 * Potentially more efficient implementation of performing multiple steps at the same time.
	 */
	public Graph hideEvaderMultipleSteps(Graph g, int evader, Coalition infected, int lastStep, int k) {
		Graph res = g;
		for (int step = lastStep - k + 1; step <= lastStep; ++step)
			res = hideEvader(res, evader, infected, step);
		return res;
	}
}
