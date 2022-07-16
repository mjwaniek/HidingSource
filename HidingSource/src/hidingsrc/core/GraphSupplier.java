package hidingsrc.core;

import java.util.function.BiFunction;

/**
 * Producer of networks, generating them based on a given model.
 * 
 * @author Marcin Waniek
 */
public class GraphSupplier {
	
	private String modelName;
	private BiFunction<Integer,Integer,Graph> generator;

	public GraphSupplier(String typeName, BiFunction<Integer,Integer,Graph> generator) {
		this.modelName = typeName;
		this.generator = generator;
	}

	public String getName(){
		return modelName;
	}

	public Graph generate(int n, int avgDegree){
		return generator.apply(n, avgDegree);
	}
}
