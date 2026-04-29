
import org.graph4j.Graph;
import org.graph4j.GraphUtils;
import org.graph4j.generators.GraphGenerator;
import org.graph4j.util.Tools;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import ro.uaic.info.eqcol.GurobiStableSetFinder;
import ro.uaic.info.eqcol.StableSetIterator;

/**
 *
 * @author Cristian Frăsinaru
 */
public class StableSetIteratorTest {

    @Test
    public void random() {
        int n = 20;
        double p = 0.5;
        Graph g = GraphGenerator.randomGnp(n, p);
        g = GraphUtils.shuffle(g);

        for (int k = 1; k <= n; k++) {
            var it = new StableSetIterator(g, k, k, 0);
            boolean found1 = it.hasNext();

            var alg = new GurobiStableSetFinder(g, k, 0);
            boolean found2 = alg.find() != null;

            assertEquals(found1, found2);
        }
    }

    @Test
    public void countEmpty() {
        int n = 10;
        Graph g = GraphGenerator.empty(n);
        g = GraphUtils.shuffle(g);
        for (int k = 1; k <= n; k++) {
            var it = new StableSetIterator(g, k, k, 0);
            int count = 0;
            while (it.hasNext()) {
                it.next();
                count++;
            }
            assertEquals(Tools.combinations(n, k).intValue(), count);
        }
    }

    @Test
    public void countComplete() {
        int n = 10;
        Graph g = GraphGenerator.complete(n);
        assertEquals(n, count(g, 1));
        for (int k = 2; k <= n; k++) {
            assertEquals(0, count(g, k));
        }
    }

    private int count(Graph g, int k) {
        var it = new StableSetIterator(g, k, k, 0);
        int count = 0;
        while (it.hasNext()) {
            it.next();
            count++;
        }
        return count;
    }
}
