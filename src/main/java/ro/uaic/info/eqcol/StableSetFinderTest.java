/*
 * Copyright (C) 2026 Cristian Frăsinaru and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package ro.uaic.info.eqcol;

import org.graph4j.Graph;
import org.graph4j.GraphUtils;
import org.graph4j.generators.GraphGenerator;

/**
 *
 * @author Cristian Frăsinaru
 */
public class StableSetFinderTest {

    public static void main(String args[]) {
        //simpleTest();
        compareVsGurobi();
        //compareVsIterator();
    }

    private static void simpleTest() {
        int n = 200;
        double p = 0.7;
        Graph g = GraphGenerator.randomGnp(n, p);
        //Graph g = new Dataset().getInstance("C2000.5").getGraph();
        int size = 10;
        var finder = new StableSetFinder(g, size, 10 * 1000);
        System.out.println(finder.find());
    }

    private static void compareVsGurobi() {
        int n = 200;
        double p = 0.1;
        Graph g = GraphGenerator.randomGnp(n, p);
        //Graph g = new DimacsIO().read("d:/datasets/coloring/instances/DSJC1000.5.col");
        g = GraphUtils.shuffle(g);

        long timeLimit = 1 * 60 * 1000;
        for (int size = 1; size <= n; size++) {
            long start1 = System.currentTimeMillis();
            //var it = new StableSetIterator(g, size, size, timeLimit);
            //boolean found1 = it.hasNext();
            var finder1 = new StableSetFinder(g, size, timeLimit);
            boolean found1 = finder1.find() != null;
            long time1 = System.currentTimeMillis() - start1;

            long start2 = System.currentTimeMillis();
            var finder2 = new GurobiStableSetFinder(g, size, 0);
            boolean found2 = finder2.find() != null;
            long time2 = System.currentTimeMillis() - start2;

            if (found1 != found2) {
                throw new RuntimeException("Ooops....");
            }

            System.out.println(">>>>>>>>>> " + size);
            if (Math.abs(time1 - time2) > 100) {
                System.out.println("StableSetFinder: \t"
                        + found1 + " " + time1 + " ms");
                System.out.println("GurobiStableSetFinder: \t"
                        + found2 + " " + time2 + " ms");
            }
        }
    }

    private static void compareVsIterator() {
        int n = 300;
        double p = 0.5;
        Graph g = GraphGenerator.randomGnp(n, p);
        //Graph g = new DimacsIO().read("d:/datasets/coloring/instances/DSJC1000.5.col");
        g = GraphUtils.shuffle(g);

        long timeLimit = 1 * 60 * 1000;
        for (int size = 1; size <= n; size++) {
            long start1 = System.currentTimeMillis();
            //var it = new StableSetIterator(g, size, size, timeLimit);
            //boolean found1 = it.hasNext();
            var finder1 = new StableSetFinder(g, size, timeLimit);
            boolean found1 = finder1.find() != null;
            long time1 = System.currentTimeMillis() - start1;

            long start2 = System.currentTimeMillis();
            var finder2 = new StableSetIterator(g, size, size, timeLimit);
            boolean found2 = finder2.hasNext();
            long time2 = System.currentTimeMillis() - start2;

            if (found1 != found2) {
                throw new RuntimeException("Ooops....");
            }

            System.out.println(">>>>>>>>>> " + size);
            if (Math.abs(time1 - time2) > 100) {
                System.out.println("StableSetFinder: \t"
                        + found1 + " " + time1 + " ms");
                System.out.println("StableSetIterator: \t"
                        + found2 + " " + time2 + " ms");
            }
        }
    }

}
