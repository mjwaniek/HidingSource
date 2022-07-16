package hidingsrc.core;

import java.util.Map;
import java.util.function.Function;

/**
 * Ranking that only stores the top elements.
 * 
 * @author Marcin Waniek
 *
 * @param <T> type of elements that are sorted
 */
public class LimitedMemoryRanking<T> extends Ranking<T> {

	private int sizeLimit;
	private T worst;
	
	public LimitedMemoryRanking(int sizeLimit){
		super();
		this.sizeLimit = sizeLimit;
		this.worst = null;
	}
	
	public LimitedMemoryRanking(Map<T, Double> scores, int sizeLimit){
		this(sizeLimit);
		for (T t : scores.keySet())
			setScore(t, scores.get(t));
		if (size() > 0)
			worst = get(size());
	}

	public LimitedMemoryRanking(Iterable<T> elements, Function<T, Number> f, int sizeLimit){
		this(sizeLimit);
		elements.forEach(t -> setScore(t, f.apply(t).doubleValue()));
		if (size() > 0)
			worst = get(size());
	}
	
	@Override
	public T getLast() {
		return worst;
	}
	
	@Override
	public void setScore(T v, double score){
		if (size() < sizeLimit || scores.containsKey(v) || score > scores.get(worst)){
			super.setScore(v, score);
			if (size() > sizeLimit)
				remove(worst);
			if (!scores.containsKey(worst) || v.equals(worst))
				worst = get(size());
			worst = score < scores.get(worst) ? v : worst;
		}
	}
}
