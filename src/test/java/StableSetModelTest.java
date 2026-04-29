
import org.graph4j.Graph;
import org.graph4j.GraphUtils;
import org.graph4j.generators.RandomMultipartiteGenerator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ro.uaic.info.eqcol.GurobiSetCoveringSolver;

/**
 *
 * @author Cristian Frăsinaru
 */
public class StableSetModelTest {

    
    @Test
    public void random() {
        int numColors = 5;
        int colorClassSize = 5;
        double prob = Math.random();
        Graph g = new RandomMultipartiteGenerator(numColors, colorClassSize, prob).create();
        g = GraphUtils.shuffle(g);

        var alg1 = new GurobiSetCoveringSolver(g);
        var col1 = alg1.findColoring(numColors);
        Assertions.assertNotNull(col1);
    }

}
