package hidingsrc.centrality;

import java.util.function.Function;

import hidingsrc.core.Graph;
import hidingsrc.core.Ranking;
import hidingsrc.core.ShortestPaths;

/**
 * Betweenness centrality measure - percentage of controlled shortest between pairs of other nodes.
 * 
 * @author Marcin Waniek
 */
public class BetweennessCentrality extends Centrality {
	
	private Function<Graph,ShortestPaths> spProducer;
	
	public BetweennessCentrality(Function<Graph, ShortestPaths> spProducer) {
		this.spProducer = spProducer;
	}
	
	public BetweennessCentrality() {
		this(g -> g.sp());
	}

	@Override
	public String getName() {
		return "betweenness";
	}

	@Override
	protected void recountCentrality() {
		ShortestPaths sp = spProducer.apply(g);
		double[] acc = new double[g.size()];
		for (int from : g.nodes()){
			double[] controlled = new double[g.size()];
			Ranking<Integer> closest = new Ranking<>();
			for (int v : g.nodes())
				if (sp.getDistance(from, v) < Double.POSITIVE_INFINITY)
					closest.setScore(v, sp.getDistance(from, v));
			for (int v : closest)
				for (int w : g.getPreds(v))
					if (w != from && sp.getDistance(from, w) < sp.getDistance(from, v))
						controlled[w] += (double) sp.getNumberOfShortestPaths(from, w)
								/ sp.getNumberOfShortestPaths(from, v) * (1. + controlled[v]);
			for (int v : g.nodes())
				acc[v] += controlled[v];
		}
		for (int v : g.nodes())
			values.put(v, acc[v] /((g.size() - 1) * (g.size() - 2)));
	}

	@Override
	public double computeSingleCentrality(int v, Graph g) {
		return getCentrality(v, g);
	}
}
