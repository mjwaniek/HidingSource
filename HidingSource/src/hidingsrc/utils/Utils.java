package hidingsrc.utils;

import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A set of useful methods
 * 
 * @author Marcin Waniek
 */
public class Utils {

	public static final Random RAND = new Random();
	public static final String WHITESPACE = "\\s+";
	
	/**
	 * Current time in the yyMMdd-HHmmss format.
	 */
	public static String timestamp(){
		return new SimpleDateFormat("yyMMdd-HHmmss-SSS").format(new Date());
	}
	
	public static String capitalize(String s) {
		return s.substring(0,1).toUpperCase() + s.substring(1);
	}
	
	/**
	 * Readable format of milliseconds.
	 */
	public static String timeDesc(long ms){
		if (ms < 1000)
			return ms + "ms";
		long s = ms / 1000;
		long m = s / 60;
		long h = m / 60;
		String res = s + "s";
		if (m > 0)
			res = m + "m " + (s % 60) + "s";
		if (h > 0)
			res = h + "h " + (m % 60) + "m " + (s % 60) + "s";
		return res;
	}
	
	public static <T> T getRandom(List<T> l) {
		return l.size() > 0 ? l.get(RAND.nextInt(l.size())) : null;
	}
	
	public static <T> T getRandom(Stream<T> s) {
		return getRandom(s.collect(Collectors.toList()));
	}
	
	public static <T> T getRandom(Stream<T> s, int size) {
		return s.skip(RAND.nextInt(size)).findFirst().orElse(null);
	}
	
	/**
	 * Finds the element of collection that maximizes given function.
	 */
	public static <T> T argmax(Iterable<T> it, Function<T,Number> f){
		ArgMaxCounter<T> counter = new ArgMaxCounter<>();
		it.forEach(t -> counter.update(t, f.apply(t).doubleValue()));
		return counter.res;
	}
	
	/**
	 * Finds the element of stream that maximizes given function.
	 */
	public static <T> T argmax(Stream<T> s, Function<T,Number> f){
		ArgMaxCounter<T> counter = new ArgMaxCounter<>();
		s.forEach(t -> counter.update(t, f.apply(t).doubleValue()));
		return counter.res;
	}
	
	private static class ArgMaxCounter<T> {
		private T res;
		private double resVal;
		private int equalCount;
		
		public ArgMaxCounter() {
			this.res = null;
			this.resVal = 0;
			this.equalCount = 0;
		}
		
		public void update(T elem, double val) {
			if (res == null || val > resVal){
				equalCount = 0;
				res = elem;
				resVal = val;
			} else if (val == resVal){
				++equalCount;
				if (RAND.nextDouble() >= (double)equalCount/(equalCount + 1)){
					res = elem;
					resVal = val;
				}
			}
		}
	}
	
	/**
	 * Finds the element of collection that maximizes given function.
	 */
	public static <T> T argmin(Iterable<T> it, final Function<T,Number> f){
		return argmax(it, t -> -f.apply(t).doubleValue());
	}
	
	/**
	 * Finds the element of stream that maximizes given function.
	 */
	public static <T> T argmin(Stream<T> s, final Function<T,Number> f){
		return argmax(s, t -> -f.apply(t).doubleValue());
	}
	
	public static <T> T last(T[] a) {
		return a[a.length - 1];
	}
	
	public static <T> T last(List<T> l) {
		return l.get(l.size() - 1);
	}

	/**
	 * Concatenate two arrays.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T[] concat(T[] a, T[] b){
		T[] res = (T[]) Array.newInstance(a.getClass().getComponentType(), a.length + b.length);
		int i = 0;
		for (T t : a)
			res[i++] = t;
		for (T t : b)
			res[i++] = t;
		return res;
	}

	@SafeVarargs
	public static <T> List<T> concat(List<T>... lists){
		List<T> res = new ArrayList<>();
		for (List<T> l : lists)
			res.addAll(l);
		return res;
	}
	
	/**
	 * Vector multiplication.
	 */
	public static double mult(double[] a, double[] b) {
		double res = 0.;
		for (int i = 0; i < Math.min(a.length, b.length); ++i)
			res += a[i] * b[i];
		return res;
	}
	
	/**
	 * Saves iterable elements to a list.
	 */
	public static <T> List<T> toList(Iterable<T> it){
		List<T> res = new ArrayList<>();
		for (T t : it)
			res.add(t);
		return res;
	}
	
	/**
	 * Creates list in place.
	 */
	@SafeVarargs
	public static <T> List<T> aList(T... elems){
		List<T> res = new ArrayList<>();
		for (T elem : elems)
			res.add(elem);
		return res;
	}
	
	public static <T> List<T> asList(Stream<T> elems){
		List<T> res = new ArrayList<>();
		elems.forEach(elem -> res.add(elem));
		return res;
	}
	
	public static <T> List<T> asList(T[] elems){
		List<T> res = new ArrayList<>();
		for (T elem : elems)
			res.add(elem);
		return res;
	}
	
	public static List<Integer> asList(int[] elems){
		List<Integer> res = new ArrayList<>();
		for (int elem : elems)
			res.add(elem);
		return res;
	}
	
	public static List<Double> asList(double[] elems){
		List<Double> res = new ArrayList<>();
		for (double elem : elems)
			res.add(elem);
		return res;
	}
	
	public static List<Boolean> asList(boolean[] elems){
		List<Boolean> res = new ArrayList<>();
		for (boolean elem : elems)
			res.add(elem);
		return res;
	}
	
    /**
     * Rounds d to the multiplicity of delta.
     */
    public static double round(double d, double delta) {
    	return delta * Math.round(d / delta);
    }
	
	/**
	 * Computing mean value.
	 */
	public static Double mean(Collection<Double> data){
		Double res = data.stream().reduce(0., Double::sum); 
		if (data.size() > 0)
			res /= data.size();
		return res;
	}
	
	/**
	 * Computing standard deviation.
	 */
	public static Double sd(Collection<Double> data){
		Double res = 0.;
		if (data.size() > 0){
			Double mean = mean(data);
			for (Double x : data)
				res += (x - mean)*(x - mean);
			res /= data.size();
			res = Math.sqrt(res);
		}
		return res;
	}
	
	/**
	 * Computing 95% confidence interval.
	 */
	public static Double conf95(Collection<Double> data){
		Double res = 0.;
		if (data.size() > 0){
			Double sd = sd(data);
			res = 1.96 * sd / Math.sqrt(data.size());
		}
		return res;
	}
	
	/**
	 * Hurwicz zeta function
	 */
	public static double hurwiczZeta(double s, double q, double prec) {
		double res = 0.;
		int n = 0;
		double d = Double.POSITIVE_INFINITY;
		while (d > prec) {
			d = Math.pow(q + n++, -s);
			res += d;
		}
		return res; 
	}
}
