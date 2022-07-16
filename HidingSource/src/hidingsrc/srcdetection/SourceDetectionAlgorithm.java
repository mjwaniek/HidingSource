package hidingsrc.srcdetection;

import hidingsrc.core.Coalition;
import hidingsrc.core.Graph;

/**
 * Representation of an algorithm detecting the source of diffusion.
 * 
 * @author Marcin Waniek
 */
public abstract class SourceDetectionAlgorithm {
	
	public abstract String getName();
	public abstract int detectSource(Coalition active, Graph g);

}
