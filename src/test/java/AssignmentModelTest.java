
import org.graph4j.Graph;
import org.graph4j.GraphUtils;
import org.graph4j.generators.GraphGenerator;
import org.graph4j.generators.RandomMultipartiteGenerator;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;
import ro.uaic.info.eqcol.GurobiAssignmentSolver;

/**
 *
 * @author Cristian Frăsinaru
 */
public class AssignmentModelTest {

    @Test
    public void random() {
        int numColors = 5;
        int colorClassSize = 5;
        double prob = Math.random();
        Graph g = new RandomMultipartiteGenerator(numColors, colorClassSize, prob).create();
        g = GraphUtils.shuffle(g);

        var alg = new GurobiAssignmentSolver(g);
        var col = alg.findColoring(numColors);
        assertNotNull(col);
        col.checkProper();
        col.checkEquitable();
    }

    @Test
    public void complete() {
        int n = 10;
        Graph g = GraphGenerator.complete(n);
        g = GraphUtils.shuffle(g);

        var alg = new GurobiAssignmentSolver(g);
        var col = alg.findColoring();
        assertNotNull(col);
        assertEquals(col.numUsedColors(), n);
        col.checkProper();
        col.checkEquitable();
    }

}
