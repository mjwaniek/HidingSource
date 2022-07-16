package hidingsrc.core;

import java.util.function.BiFunction;

/**
 * Keeping track of shortest paths when the length of and edge is defined by a function. 
 * 
 * @author Marcin Waniek
 */
public class WeightedShortestPaths extends ShortestPaths {

	private BiFunction<Integer, Integer, Double> length;
	
	protected WeightedShortestPaths(Graph g, BiFunction<Integer, Integer, Double> length) {
		super(g);
		this.length = length;
	}
	
	public static WeightedShortestPaths construct(Graph g, BiFunction<Integer,Integer,Double> length){
		WeightedShortestPaths res = new WeightedShortestPaths(g, length);
		g.subscribe(res);
		return res;
	}

	@Override
	protected double edgeLength(int i, int j) {
		return length.apply(i, j);
	}
}
