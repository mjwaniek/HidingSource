package hidingsrc.centrality;

import java.util.function.Function;

import hidingsrc.core.Graph;
import hidingsrc.core.ShortestPaths;

/**
 * Closeness centrality measure - average distance to other nodes.
 * 
 * @author Marcin Waniek
 */
public class ClosenessCentrality extends Centrality {

	private Function<Graph,ShortestPaths> spProducer;
	
	public ClosenessCentrality(Function<Graph, ShortestPaths> spProducer) {
		this.spProducer = spProducer;
	}
	
	public ClosenessCentrality() {
		this(g -> g.sp());
	}

	@Override
	public String getName() {
		return "closeness";
	}

	@Override
	public double computeSingleCentrality(int v, Graph g) {
		ShortestPaths sp = spProducer.apply(g);
		if (g.isDirected()){
			double sum = 0.;
			for (int w : g.nodes())
				if (v != w && sp.getDistance(v, w) < Double.POSITIVE_INFINITY)
					sum += 1. / sp.getDistance(v, w);
			return sum / (g.size() - 1);				
		} else {
			double splSum = 0.;
			for (int w : g.nodes())
				if (v != w) {
					if (sp.getDistance(v, w) < Double.POSITIVE_INFINITY)
						splSum += sp.getDistance(v, w);
					else
						splSum += g.size() - 1;
				}
			return splSum == 0. ? 0. : (double)(g.size() - 1) / splSum;
		}
	}
}
