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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;
import org.graph4j.Graph;
import org.graph4j.SimpleGraphAlgorithm;
import org.graph4j.util.IntArrays;
import org.graph4j.util.StableSet;
import org.graph4j.util.VertexSet;

/**
 * Iterates over all stable sets of a specified size in a graph.
 *
 * @author Cristian Frăsinaru
 */
public class StableSetIterator extends SimpleGraphAlgorithm {

    private final int minSize, maxSize;
    private final int[][] adjMatrix;
    private final int[] degrees;
    private final int[] order;
    private final int numVertices;
    private final Timer timer;

    private final Deque<Node> stack;
    private StableSet currentStable;
    private AtomicBoolean stopFlag;

    public StableSetIterator(Graph graph, int minSize, int maxSize, long timeLimit) {
        this(graph, minSize, maxSize, null, timeLimit);
    }

    public StableSetIterator(Graph graph, int minSize, int maxSize, int[] order, long timeLimit) {
        super(graph);
        this.minSize = minSize;
        this.maxSize = maxSize;
        this.timer = new Timer(timeLimit);
        this.order = order != null ? order : IntStream.range(0, graph.numVertices()).toArray();
        stack = new ArrayDeque<>((int) graph.numEdges());
        adjMatrix = graph.adjacencyMatrix();
        degrees = graph.degrees();
        numVertices = graph.numVertices();
        if (!graph.isEmpty()) {
            int[] vertices = graph.vertices();
            var vertexSet = new VertexSet(graph,
                    IntArrays.sort(vertices, (v, u) -> this.order[graph.indexOf(u)] - this.order[graph.indexOf(v)]));
            for (int v : vertices) {
                if (graph.degree(v) > numVertices - minSize) {
                    vertexSet.remove(v);
                }
            }
            stack.push(new Node(new StableSet(graph), vertexSet));
        }
    }

    public StableSet next() {
        if (currentStable != null) {
            var temp = currentStable;
            currentStable = null;
            return temp;
        }
        if (hasNext()) {
            return currentStable;
        }
        throw new NoSuchElementException();
    }

    public boolean hasNext() {
        if (currentStable != null) {
            return true;
        }
        while (!stack.isEmpty()) {
            timer.check();
            if (stopFlag != null && stopFlag.get()) {
                return false;
            }
            var node = stack.peek();
            if (node.cand == null || node.cand.isEmpty()) {
                stack.pop();
                continue;
            }

            //make a new stable set
            int v = node.cand.pop();
            var newStableSet = new StableSet(node.stableSet);
            newStableSet.add(v);
            int newSize = newStableSet.size();

            VertexSet newCand = null;
            if (newSize < maxSize) {
                newCand = nonNeighbors(v, node.cand.vertices());
                int dif = newCand.size() - (minSize - newSize);
                if (dif < 0) {
                    newCand = null;
                } else if (dif == 0) {
                    if (isEdgeless(newCand)) {
                        newStableSet.addAll(newCand.vertices());
                        newSize = minSize;
                    }
                    newCand = null;
                }
            }
            stack.push(new Node(newStableSet, newCand));

            if (newSize >= minSize) {
                currentStable = newStableSet;
                assert currentStable.isValid();
                return true;
            }
        }
        return false;
    }

    //find the non-neighbors with higher order
    private VertexSet nonNeighbors(int v, int[] cand) {
        var set = new VertexSet(graph, cand.length);
        int vi = graph.indexOf(v);
        for (int u : cand) {
            //if (u > v && !graph.containsEdge(v, u)) {
            int ui = graph.indexOf(u);
            if (order[ui] > order[vi] && adjMatrix[vi][ui] == 0 && degrees[ui] <= numVertices - minSize) {
                set.add(u);
            }
        }
        return set;
    }

    private boolean isEdgeless(VertexSet cand) {
        int candVertices[] = cand.vertices();
        for (int i = 0, k = candVertices.length; i < k - 1; i++) {
            int vi = graph.indexOf(candVertices[i]);
            for (int j = i + 1; j < k; j++) {
                int ui = graph.indexOf(candVertices[j]);
                if (adjMatrix[vi][ui] > 0) {
                    return false;
                }
            }
        }
        return true;
    }

    public AtomicBoolean getStopFlag() {
        return stopFlag;
    }

    public void setStopFlag(AtomicBoolean stopFlag) {
        this.stopFlag = stopFlag;
    }

    private class Node {

        final StableSet stableSet;
        final VertexSet cand;

        public Node(StableSet stableSet, VertexSet cand) {
            this.stableSet = stableSet;
            this.cand = cand;
        }
    }

}
