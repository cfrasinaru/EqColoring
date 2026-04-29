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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;
import org.graph4j.Graph;
import org.graph4j.SimpleGraphAlgorithm;
import org.graph4j.exceptions.TimeLimitExceededException;
import org.graph4j.util.IntArrays;
import org.graph4j.util.StableSet;

/**
 *
 * @author Cristian Frăsinaru
 */
public class StableSetFinder extends SimpleGraphAlgorithm {

    private final int size;
    private final long timeLimit;
    private final AtomicBoolean stopFlag = new AtomicBoolean();
    private StableSet solution;

    public StableSetFinder(Graph graph, int size, long timeLimit) {
        super(graph);
        this.size = size;
        this.timeLimit = timeLimit;
    }

    public StableSet find() {
        int threads = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        List<IteratorTask> tasks = new ArrayList<>();
        for (int v : graph.vertices()) {
            tasks.add(new IteratorTask(v));
        }
        try {
            long timeout = timeLimit == 0 ? Long.MAX_VALUE : timeLimit;
            var futures = executor.invokeAll(tasks, timeout, TimeUnit.MILLISECONDS);
            for (var f : futures) {
                if (f.isCancelled()) {
                    throw new TimeLimitExceededException(timeLimit);
                }
                f.get();
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for tasks", ex);
        } catch (ExecutionException ex) {
            throw new RuntimeException("Task failed", ex.getCause());
        } finally {
            executor.shutdownNow();
        }
        return solution;
    }

    // A StableSetIterator running from a specific vertex
    private class IteratorTask implements Callable<StableSet> {

        private final int startVertex;

        public IteratorTask(int startVertex) {
            this.startVertex = startVertex;
        }

        @Override
        public StableSet call() {
            int[] order = IntArrays.shuffle(IntStream.rangeClosed(1, graph.numVertices()).toArray());
            order[graph.indexOf(startVertex)] = 0;

            var it = new StableSetIterator(graph, size, size, order, 0);
            it.setStopFlag(stopFlag);
            StableSet stableSet = null;
            if (it.hasNext()) {
                stableSet = it.next();
                synchronized (graph) {
                    solution = stableSet;
                }
            }
            stopFlag.set(true);
            return stableSet;
        }
    }

}
