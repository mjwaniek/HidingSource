package hidingsrc.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Class for sorting elements based on scores.
 * 
 * @author Marcin Waniek
 *
 * @param <T> type of elements that are sorted
 */
public class Ranking<T> implements Iterable<T> {
	
	protected Map<T, Double> scores;
	protected List<T> ranking;

	public Ranking(){
		this.scores = new HashMap<T, Double>();
		this.ranking = null;
	}
	
	public Ranking(Map<T, Double> scores){
		this.scores = scores;
		this.ranking = null;
	}
	
	public Ranking(Iterable<T> elements, Function<T, Number> f){
		this();
		elements.forEach(t -> setScore(t, f.apply(t).doubleValue()));
	}
	
	public double getScore(T v){
		return scores.get(v);
	}
	
	public void setScore(T v, double score){
		scores.put(v, score);
		ranking = null;
	}

	public void incScore(T v, double increment) {
		scores.putIfAbsent(v, 0.);
		setScore(v, getScore(v) + increment);
	}
	
	public void remove(T v){
		scores.remove(v);
		ranking = null;
	}
	
	public int size(){
		return scores.size();
	}
	
	public Set<T> getKeySet(){
		return new HashSet<>(scores.keySet());
	}
	
	public T getBest(){
		return get(1);
	}
	
	public T getLast(){
		return get(scores.size());
	}
	
	public T get(int i){
		if (ranking == null)
			ranking = createRanking();
		return ranking.get(i - 1);
	}
	
	public Integer getPosition(T v){
		if (ranking == null)
			ranking = createRanking();
		return ranking.indexOf(v) + 1;
	}
	
	public Integer getExAequoPosition(T v, double delta){
		return getTopBlockPosition(v, delta);
	}
	
	public Double getAvgPosition(T v, double delta){
		return ((double)getTopBlockPosition(v, delta) + getBotBlockPosition(v, delta)) / 2;
	}
	
	/**
	 * Get highest position with almost the same score as v.
	 */
	private int getTopBlockPosition(T v, double delta) {
		int top = getPosition(v);
		while (top > 1 && Math.abs(scores.get(v) - scores.get(get(top - 1))) <= delta)
			--top;
		return top;
	}
	
	/**
	 * Get lowest position with almost the same score as v.
	 */
	private int getBotBlockPosition(T v, double delta) {
		int bot = getPosition(v);
		while (bot < size() && Math.abs(scores.get(v) - scores.get(get(bot + 1))) <= delta)
			++bot;
		return bot;
	}

	public List<T> getList() {
		if (ranking == null)
			ranking = createRanking();
		return new ArrayList<>(ranking);
	}
	
	public List<T> getTop(int k) {
		if (ranking == null)
			ranking = createRanking();
		return new ArrayList<>(ranking.subList(0, k));
	}
	
	public Stream<T> stream() {
		if (ranking == null)
			ranking = createRanking();
		return ranking.stream();
	}

	private List<T> createRanking(){
		List<T> res = new ArrayList<>(scores.keySet());
		res.sort((v1,v2) -> scores.get(v2).compareTo(scores.get(v1)));
		return res;
	}
	
	public void printRanking(){
		printRanking(scores.keySet().size());
	}
	
	public void printRanking(int n){
		if (ranking == null)
			ranking = createRanking();
		int i = 1;
		for (T v : ranking)
			if (i <= n)
				System.out.println((i++) + ". " + v + " " + scores.get(v));
	}

	@Override
	public Iterator<T> iterator() {
		if (ranking == null)
			ranking = createRanking();
		return ranking.iterator();
	}
}
