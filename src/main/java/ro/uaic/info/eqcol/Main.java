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
import org.graph4j.coloring.Coloring;
import org.graph4j.coloring.eq.EquitableColoringAlgorithm;
import org.graph4j.measures.GraphMeasures;
import org.graph4j.util.IntArrays;

/**
 *
 * @author Cristian Frăsinaru
 */
public class Main {

    public static void main(String[] args) {
        var app = new Main();
        app.demo();
    }

    private void demo() {
        long timeLimit = 1 * 60 * 60 * 1000;
        var dataset = new Dataset();

        String name = "DSJC500.9";
        var instance = dataset.getInstance(name);
        var graph = instance.getGraph();

        System.out.println("Graph\t: " + name);
        System.out.println("numVertices\t: " + graph.numVertices());
        System.out.println("numEdges\t: " + graph.numEdges());
        System.out.println("density\t: " + GraphMeasures.density(graph));
        System.out.println("minDegree\t: " + GraphMeasures.minDegree(graph));
        System.out.println("maxDegree\t: " + GraphMeasures.maxDegree(graph));
        System.out.println("isolated\t: " + countIsolated(graph));
        System.out.println("symmetries\t: " + countSymmetries(graph));

        System.out.println("-------------------------------------------------");
        var alg = new EqColoringSolver(instance, 124, 124, timeLimit);
        alg.setOutputEnabled(true);
        run(alg);
        var results = alg.getInstanceResults();
        dataset.writeResults(results);
    }

    private void run(EquitableColoringAlgorithm alg) {
        System.out.println("Running " + alg.getClass().getSimpleName());
        long t0 = System.currentTimeMillis();
        Coloring col = alg.findColoring();
        if (col != null) {
            System.out.println("col:" + col);
            col.checkProper();
            col.checkEquitable();
        } else {
            System.out.println("No solution.");
        }
        System.out.println((System.currentTimeMillis() - t0) + " ms");
    }

    private int countIsolated(Graph g) {
        int isolated = 0;
        for (int v : g.vertices()) {
            if (g.degree(v) == 0) {
                isolated++;
            }

        }
        return isolated;
    }

    private int countSymmetries(Graph g) {
        int symmetries = 0;
        for (int v : g.vertices()) {
            var vn = g.neighbors(v);
            for (int u : g.vertices()) {
                if (u == v) {
                    continue;
                }
                var un = g.neighbors(u);
                if (IntArrays.haveSameValues(vn, un)) {
                    symmetries++;
                }
            }
        }
        return symmetries;
    }

}
