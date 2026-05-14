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

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import org.graph4j.Graph;
import org.graph4j.GraphBuilder;
import org.graph4j.exceptions.TimeLimitExceededException;
import org.graph4j.measures.GraphMeasures;
import org.graph4j.util.StableSet;
import static ro.uaic.info.eqcol.GurobiSetCoveringSolver.MAX_STABLE_SET_COUNT;

/**
 *
 * @author Cristian Frăsinaru
 */
public class Tools {

    //graph vertices must be 1,2,...,n
    public static void createKamisGraphFile(String name) throws IOException {
        var graph = new Dataset().getInstance(name).getGraph();
        createKamisGraphFile(graph);
    }

    public static void createKamisGraphFile(Graph graph) throws IOException {
        String name = graph.getName();
        if (name == null || name.isBlank()) {
            name = "input";
        }
        File file = new File(name + ".graph");
        try (var out = new PrintWriter(file)) {
            out.println(graph.numVertices() + " " + graph.numEdges());
            for (int v : graph.vertices()) {
                var adjList = new StringBuilder();
                for (var it = graph.neighborIterator(v); it.hasNext();) {
                    adjList.append(it.next()).append(" ");
                }
                out.println(adjList.toString().trim());
            }
        }
        System.out.println("File created: " + file.getCanonicalPath());
    }

    public static Graph createGraphOfSets(Graph graph, int numColors, long timeLimit) {
        long t0 = System.currentTimeMillis();
        List<StableSet> stableSets = new ArrayList<>();
        int n = graph.numVertices();
        int p = numColors;
        int n1 = n % p == 0 ? n : (1 + (n / p)) * p;
        int x = n1 - n;
        System.out.println("Extra nodes: " + x);
        Graph copy = graph;
        int max = graph.maxVertexNumber() + 1;
        if (x > 0) {
            copy = graph.copy();
            for (int i = 0; i < x; i++) {
                copy.addVertex(max + i);
            }
            for (int i = 0; i < x - 1; i++) {
                for (int j = i + 1; j < x; j++) {
                    copy.addEdge(max + i, max + j);
                }
            }
        }
        int k = n1 / p; //target
        var it = new StableSetIterator(copy, k, k, 0);
        int count = 0;
        mainLoop:
        while (it.hasNext()) {
            var set = it.next();
            stableSets.add(set);
            if (++count > MAX_STABLE_SET_COUNT) {
                throw new TooManySetsException(count);
            }
            if (timeLimit > 0 && System.currentTimeMillis() - t0 > timeLimit) {
                throw new TimeLimitExceededException(timeLimit);
            }
        }
        System.out.println("Stable sets to consider: " + stableSets.size()
                + ", of size " + k);

        int numSets = stableSets.size();
        var work = GraphBuilder.vertexRange(1, numSets).buildGraph();
        for (int i = 0; i < numSets - 1; i++) {
            var s1 = stableSets.get(i);
            for (int j = i + 1; j < numSets; j++) {
                var s2 = stableSets.get(j);
                if (!s1.intersection(s2).isEmpty()) {
                    work.addEdge(i + 1, j + 1);
                }
            }
        }
        System.out.println(work + ", " + GraphMeasures.density(work));
        return work;
    }

    public static void main(String args[]) throws IOException {
        //createKamisGraphFile("C2000.5");
        var g = new Dataset().getInstance("latin_square_10").getGraph();
        var h = createGraphOfSets(g, 90, 0);
        Tools.createKamisGraphFile(h);
    }
}
