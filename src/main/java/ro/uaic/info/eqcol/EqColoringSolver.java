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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import org.graph4j.Graph;
import org.graph4j.clique.MaximalCliqueFinder;
import org.graph4j.coloring.Coloring;
import org.graph4j.coloring.eq.EquitableColoringAlgorithm;
import org.graph4j.coloring.eq.GreedyEquitableColoring;
import org.graph4j.exceptions.TimeLimitExceededException;
import org.graph4j.generators.RandomMultipartiteGenerator;
import org.graph4j.measures.GraphMeasures;
import org.graph4j.util.Clique;
import org.graph4j.util.VertexSet;

/**
 * A solver for equitable dense graph coloring.
 *
 * @author Cristian Frăsinaru
 */
public class EqColoringSolver implements EquitableColoringAlgorithm {

    private final Instance instance;
    private final Graph graph;
    private final long timeLimit;
    private Integer lb0, ub0;

    private boolean outputEnabled;
    private List<Clique> cliques;
    private int[][] foundStableOfSize; //-1(not),0(unknown),1(found)

    private final InstanceResults instanceResults;

    public EqColoringSolver(Graph graph, long timeLimit) {
        this(new Instance(graph), timeLimit);
    }

    public EqColoringSolver(Instance instance, long timeLimit) {
        this(instance, 0, 0, timeLimit);
    }

    public EqColoringSolver(Instance instance, int lb0, int ub0, long timeLimit) {
        this.instance = instance;
        this.graph = instance.getGraph();
        this.timeLimit = timeLimit;
        this.lb0 = lb0;
        this.ub0 = ub0;
        instanceResults = new InstanceResults(instance, timeLimit);
        instanceResults.setInitialLowerBound(lb0);
        instanceResults.setInitialUpperBound(ub0);
    }

    @Override
    public Graph getGraph() {
        return graph;
    }

    public Instance getInstance() {
        return instance;
    }

    public InstanceResults getInstanceResults() {
        return instanceResults;
    }

    @Override
    public Coloring findColoring() {
        instanceResults.setStartTime(new Date());
        var col = computeColoring();
        instanceResults.setEndTime(new Date());
        return col;
    }

    @Override
    public Coloring findColoring(int numColors) {
        instanceResults.setInitialLowerBound(numColors);
        instanceResults.setInitialUpperBound(numColors);
        var col = computeColoring(numColors);
        instanceResults.setEndTime(new Date());
        return col;
    }

    private Coloring computeColoring() {
        if (graph.isEdgeless()) {
            return new Coloring(graph, new int[graph.numVertices()]);
        }
        if (cliques == null) {
            computeCliquePartition();
        }
        if (lb0 == null || lb0 <= 0) {
            lb0 = cliques.isEmpty() ? 2 : cliques.get(0).size();
            instanceResults.setInitialLowerBound(lb0);
        }
        if (ub0 == null || ub0 <= 0) {
            ub0 = 1 + GraphMeasures.maxDegree(graph);
            long t0 = System.currentTimeMillis();
            int greedyEqColumber = new GreedyEquitableColoring(graph)
                    .findColoring().numUsedColors();
            instanceResults.incPresolveTime(System.currentTimeMillis() - t0);
            if (greedyEqColumber < ub0) {
                ub0 = greedyEqColumber;
            }
            instanceResults.setInitialUpperBound(ub0);
        }

        if (outputEnabled) {
            System.out.println("Solving ECP, lb0=" + lb0 + ", ub0=" + ub0);
        }
        int lb = lb0;
        int p = lb0;
        Coloring col = null;
        try {
            while (p <= ub0) {
                col = computeColoring(p);
                if (col != null) {
                    return col;
                } else {
                    p = p + 1;
                }
            }
        } catch (TimeLimitExceededException | TooManySetsException e) {
            lb = p + 1;
        }

        int ub = ub0, a = p + 1, b = ub;
        while (a <= b) {
            p = (a + b) / 2;
            try {
                col = computeColoring(p);
                if (col != null) {
                    ub = p;
                    b = p - 1;
                } else {
                    a = p + 1;
                }
            } catch (TimeLimitExceededException | TooManySetsException e) {
                a = p + 1;
            }
        }
        return col;
    }

    public Coloring computeColoring(int numColors) {
        Result result = new Result(instance, numColors);
        instanceResults.add(result);
        //
        int n = graph.numVertices();
        int p = numColors;
        int kmin = n / p;
        int kmax = n % p == 0 ? n / p : 1 + n / p;
        int r = n % p; // of size kmax; (p-r) of size kmin

        if (outputEnabled) {
            System.out.println("---------------------------------------------");
            System.out.println("Attempting: " + numColors + " colors");
            System.out.println((p - r) + " classes of size: " + kmin
                    + ", " + r + " classes of size: " + kmax);
            System.out.println("Expected stable sets: " + estimateStableSetCount(kmin, kmax));
        }
        // Maximal clique partition
        if (cliques == null) {
            computeCliquePartition();
        }
        if (!cliques.isEmpty()) {
            int maxCliqueSize = cliques.get(0).size();
            if (numColors < maxCliqueSize) {
                if (outputEnabled) {
                    System.out.println("Max clique size: " + cliques.get(0).size()
                            + " larger than number of colors: " + numColors);
                }
                result.setStatus(Status.UNFEASIBLE);
                return null;
            }
        }

        // Initial check
        try {
            long t0 = System.currentTimeMillis();
            var status = presolve(kmax);
            result.incPresolveTime(System.currentTimeMillis() - t0);
            if (status == Status.UNFEASIBLE) {
                result.setStatus(Status.UNFEASIBLE);
                if (outputEnabled) {
                    System.out.println("No stable set of size " + kmax + " exists!");
                }
                return null;
            }
        } catch (TimeLimitExceededException e) {
            result.setStatus(Status.TIMEOUT);
            if (outputEnabled) {
                System.out.println("Time expired. No stable set of size " + kmax + " found in the given time");
            }
            throw e;
        }

        // Gurobi set covering solver
        try {
            if (outputEnabled) {
                System.out.println(">>> GurobiSetCoveringSolver");
            }
            long t0 = System.currentTimeMillis();
            var alg = new GurobiSetCoveringSolver(graph, timeLimit);
            alg.setOutputEnabled(outputEnabled);
            Coloring coloring = alg.findColoring(numColors);
            result.setStableSetCount(alg.getStableSets().size());
            result.setSolveTime(System.currentTimeMillis() - t0);
            if (coloring == null) {
                result.setStatus(Status.UNFEASIBLE);
            } else {
                result.setStatus(Status.FEASIBLE);
                result.setColoring(coloring);
            }
            if (outputEnabled) {
                if (coloring != null) {
                    System.out.println("Success!");
                } else {
                    System.out.println("No solution.");
                }
            }
            return coloring;
        } catch (TimeLimitExceededException e) {
            System.err.println(e.getMessage());
            result.setStatus(Status.TIMEOUT);
            throw e;
        } catch (TooManySetsException e) {
            System.err.println(e.getMessage());
            result.setStableSetCount(e.getStableSetCount());
            result.setStatus(Status.TOO_MANY_SETS);
            throw e;
        }
    }

    private Status presolve(int stableSetSize) {
        if (outputEnabled) {
            System.out.println("Looking for a stable set of size: " + stableSetSize);
        }
        int cliqueCount = cliques.size();
        if (cliqueCount < stableSetSize) {
            return Status.UNFEASIBLE;
        }
        int n = graph.numVertices();
        int t = cliques.size();
        var vertices = new VertexSet(graph);
        int i = t - 1;
        while (i >= 0) {
            vertices.addAll(cliques.get(i).vertices());
            Graph temp2 = graph.subgraph(vertices);
            int k = stableSetSize - i;
            if (k <= 2 || foundStableOfSize[i][k] == 1) {
                i--;
                continue;
            }
            if (foundStableOfSize[i][k] == -1) {
                return Status.UNFEASIBLE;
            }
            if (outputEnabled) {
                System.out.println("\tLooking for a stable set of size: " + k
                        + " in a graph with " + temp2.numVertices() + " vertices"
                        + " and density: " + GraphMeasures.density(temp2));
            }
            //
            var finder = new StableSetFinder(temp2, k, timeLimit);
            //var alg = new GurobiStableSetFinder(temp2, k, timeLimit);
            var found = finder.find() != null;
            if (!found) {
                for (int j = k; j < n; j++) {
                    foundStableOfSize[i][j] = -1;
                }
                for (int q = i; q < t; q++) {
                    foundStableOfSize[q][k] = -1;
                }
                if (outputEnabled) {
                    System.out.println("\tNot found!");
                }
                return Status.UNFEASIBLE;
            } else {
                // found it
                for (int j = 0; j <= k; j++) {
                    foundStableOfSize[i][j] = 1;
                }
                for (int q = 0; q <= i; q++) {
                    foundStableOfSize[q][k] = 1;
                }
            }
            i--;
        }
        return Status.UNKNOWN;
    }

    private void computeCliquePartition() {
        long t0 = System.currentTimeMillis();
        //maxClique = new MaximalCliqueFinder(graph).getMaximalClique();
        Graph temp = graph.copy();
        this.cliques = new ArrayList<>();
        while (!temp.isEmpty()) {
            var clique = new MaximalCliqueFinder(temp).getMaximalClique();
            cliques.add(clique);
            temp.removeVertices(clique.vertices());
        }
        Collections.sort(cliques, (q1, q2) -> q2.size() - q1.size());
        if (!cliques.isEmpty()) {
            instanceResults.setMaxCliqueSize(cliques.get(0).size());
        }
        instanceResults.incPresolveTime(System.currentTimeMillis() - t0);
        if (outputEnabled) {
            System.out.println("Cliques: \t" + cliques.size());
            System.out.println("Clique sizes: \t" + cliques.stream()
                    .map(q -> String.valueOf(q.size()))
                    .collect(Collectors.joining(", ")));
        }
        foundStableOfSize = new int[cliques.size()][graph.numVertices()];
    }

    private BigInteger estimateStableSetCount(int kmin, int kmax) {
        BigInteger count = BigInteger.ZERO;
        for (int k = kmin; k <= kmax; k++) {
            count = count.add(GraphMeasures.estimateStableSetCount(graph, k));
        }
        return count;
    }

    public boolean isOutputEnabled() {
        return outputEnabled;
    }

    public void setOutputEnabled(boolean outputEnabled) {
        this.outputEnabled = outputEnabled;
    }

    public static void main(String args[]) {
        int numColors = 10;
        int r = 5;
        int kmin = 3;
        int kmax = 4;
        int[] numVertices = new int[numColors];
        for (int i = 0; i < r; i++) {
            numVertices[i] = kmax;
        }
        for (int i = r; i < numColors; i++) {
            numVertices[i] = kmin;
        }
        var g = new RandomMultipartiteGenerator(numVertices, 0.5).create();

        var alg = new EqColoringSolver(g, 0);
        alg.setOutputEnabled(true);
        var col = alg.findColoring(numColors);
        System.out.println(col.numUsedColors());
        System.out.println(col);
        System.out.println("-------------------------------------------------");
        var best = alg.findColoring();
        System.out.println(best.numUsedColors());
        System.out.println(best);
    }

}
