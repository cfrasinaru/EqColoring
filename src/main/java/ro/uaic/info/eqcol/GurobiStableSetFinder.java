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

import com.gurobi.gurobi.GRB;
import com.gurobi.gurobi.GRBEnv;
import com.gurobi.gurobi.GRBException;
import com.gurobi.gurobi.GRBLinExpr;
import com.gurobi.gurobi.GRBModel;
import com.gurobi.gurobi.GRBVar;
import org.graph4j.Graph;
import org.graph4j.clique.MaximalCliqueFinder;
import org.graph4j.util.StableSet;

/**
 *
 * @author Cristian Frăsinaru
 */
public class GurobiStableSetFinder {

    private final Graph graph;
    private final int size;
    private final long timeLimit;
    protected boolean timeExpired;

    protected GRBEnv env;
    protected GRBModel model;
    protected GRBVar x[];

    public GurobiStableSetFinder(Graph graph, int size, long timeLimit) {
        this.graph = graph;
        this.size = size;
        this.timeLimit = timeLimit;
    }

    public StableSet find() {
        try {
            env = new GRBEnv(true);
            env.set(GRB.IntParam.OutputFlag, 0);
            env.start();

            model = new GRBModel(env);
            model.set(GRB.DoubleParam.MIPGapAbs, 0);
            model.set(GRB.DoubleParam.MIPGap, 0);
            if (timeLimit > 0) {
                model.set(GRB.DoubleParam.TimeLimit, timeLimit / 1000);
            }

            createModel();

            // Optimize model
            model.optimize();

            StableSet stableSet = null;
            if (model.get(GRB.IntAttr.Status) == GRB.Status.OPTIMAL) {
                // Get the solution
                stableSet = createStableSet();
            } else {
                if (model.get(GRB.IntAttr.Status) == GRB.Status.TIME_LIMIT) {
                    timeExpired = true;
                }
            }
            model.dispose();
            env.dispose();
            return stableSet;
        } catch (GRBException ex) {
            System.out.println(ex);
            return null;
        }
    }

    protected void createModel() throws GRBException {
        int n = graph.numVertices();
        x = new GRBVar[n];
        for (int i = 0; i < n; i++) {
            x[i] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "x[" + i + "]");
        }

        //two adjacent nodes cannot be in the set
        for (int v : graph.vertices()) {
            int vi = graph.indexOf(v);
            for (var it = graph.neighborIterator(v); it.hasNext();) {
                int u = it.next();
                if (v > u) {
                    continue;
                }
                int ui = graph.indexOf(u);
                GRBLinExpr sum = new GRBLinExpr();
                sum.addTerm(1, x[ui]);
                sum.addTerm(1, x[vi]);
                model.addConstr(sum, GRB.LESS_EQUAL, 1, "adj_" + u + "," + v);
            }
        }

        //size constraints
        GRBLinExpr sumAll = new GRBLinExpr();
        for (int i = 0; i < n; i++) {
            sumAll.addTerm(1, x[i]);
        }
        model.addConstr(sumAll, GRB.GREATER_EQUAL, size, "size_geq_" + size);
        model.addConstr(sumAll, GRB.LESS_EQUAL, n, "size_leq_" + n);

        // clique constraints
        for (int v : graph.vertices()) {
            var q = new MaximalCliqueFinder(graph).getMaximalClique(v);
            if (q.size() < 3) {
                continue;
            }
            GRBLinExpr sum = new GRBLinExpr();
            for (int u : q.vertices()) {
                sum.addTerm(1, x[graph.indexOf(u)]);
            }
            model.addConstr(sum, GRB.LESS_EQUAL, 1, "clique_" + v);
        }

    }

    protected StableSet createStableSet() throws GRBException {
        StableSet set = new StableSet(graph);
        for (int i = 0, n = graph.numVertices(); i < n; i++) {
            int v = graph.vertexAt(i);
            if (x[i].get(GRB.DoubleAttr.X) > .00001) {
                set.add(v);
            }
        }
        return set;
    }

    public boolean isTimeExpired() {
        return timeExpired;
    }

}
