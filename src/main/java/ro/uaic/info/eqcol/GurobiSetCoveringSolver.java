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
import com.gurobi.gurobi.GRBException;
import com.gurobi.gurobi.GRBLinExpr;
import com.gurobi.gurobi.GRBVar;
import java.util.ArrayList;
import java.util.List;
import org.graph4j.Graph;
import org.graph4j.coloring.Coloring;
import org.graph4j.coloring.eq.EquitableColoringAlgorithm;
import org.graph4j.exceptions.TimeLimitExceededException;
import org.graph4j.util.VertexSet;

/**
 * ILP Stable set model. Requires a valid Gurobi installation.
 *
 * @author Cristian Frăsinaru
 */
public class GurobiSetCoveringSolver extends GurobiColoringBase
        implements EquitableColoringAlgorithm {

    public static final int MAX_STABLE_SET_COUNT = 10_000_000;
    private GRBVar x[];
    private List<VertexSet> stableSets;

    public GurobiSetCoveringSolver(Graph graph) {
        this(graph, 0);
    }

    public GurobiSetCoveringSolver(Graph graph, long timeLimit) {
        super(graph, timeLimit);
    }

    @Override
    protected GurobiSetCoveringSolver getInstance(Graph graph, long timeLimit) {
        return new GurobiSetCoveringSolver(graph, timeLimit);
    }

    public List<VertexSet> getStableSets() {
        return stableSets;
    }

    //stable sets with size n/p or between n/p and 1+n/p
    public void createStableSets(int kmin, int kmax) {
        long t0 = System.currentTimeMillis();

        stableSets = new ArrayList<>();
        var it = new StableSetIterator(graph, kmin, kmax, 0);
        int count = 0;
        while (it.hasNext()) {
            stableSets.add(it.next());
            if (++count > MAX_STABLE_SET_COUNT) {
                throw new TooManySetsException(count);
            }
            if (timeLimit > 0 && System.currentTimeMillis() - t0 > timeLimit) {
                throw new TimeLimitExceededException(timeLimit);
            }
        }
        if (outputEnabled) {
            System.out.println("Stable sets to consider: " + stableSets.size()
                    + ": " + kmin + " <= size <= " + kmax);
        }
    }

    @Override
    protected boolean createModel(int numColors) throws GRBException {
        int n = graph.numVertices();
        int p = numColors;

        int kmin = n / p;
        int kmax = n % p == 0 ? n / p : 1 + n / p;
        if (stableSets == null) {
            createStableSets(kmin, kmax);
        }

        int s = stableSets.size();
        if (s < numColors) {
            return false;
        }
        x = new GRBVar[s];
        for (int i = 0; i < s; i++) {
            x[i] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "x[" + i + "]");
        }

        //stable sets must cover all nodes exactly once
        for (int u : graph.vertices()) {
            GRBLinExpr sum = new GRBLinExpr();
            for (int i = 0; i < s; i++) {
                if (stableSets.get(i).contains(u)) {
                    sum.addTerm(1, x[i]);
                }
            }
            model.addConstr(sum, GRB.EQUAL, 1, "cover_" + u);
        }

        int r = n % p;
        if (r == 0) {
            //k color classes
            GRBLinExpr sum = new GRBLinExpr();
            for (int i = 0; i < s; i++) {
                sum.addTerm(1, x[i]);
            }
            model.addConstr(sum, GRB.EQUAL, p, "number_" + p);

        } else {
            //k-r lb classes, r ub classes
            GRBLinExpr lbSum = new GRBLinExpr();
            GRBLinExpr ubSum = new GRBLinExpr();
            for (int i = 0; i < s; i++) {
                if (stableSets.get(i).size() == kmin) {
                    lbSum.addTerm(1, x[i]);
                } else {
                    ubSum.addTerm(1, x[i]);
                }
            }
            model.addConstr(lbSum, GRB.EQUAL, p - r, "lb_classes_" + kmin);
            model.addConstr(ubSum, GRB.EQUAL, r, "ub_classes_" + kmax);
        }

        //all nodes are covered (redundant)
        GRBLinExpr sumAll = new GRBLinExpr();
        for (int i = 0; i < s; i++) {
            sumAll.addTerm(stableSets.get(i).size(), x[i]);
        }
        model.addConstr(sumAll, GRB.EQUAL, n, "sum_all");

        return true;
    }

    @Override
    protected Coloring createColoring(int numColors) throws GRBException {
        Coloring coloring = new Coloring(graph);
        int col = 0;
        for (int j = 0; j < stableSets.size(); j++) {
            if (x[j].get(GRB.DoubleAttr.X) > .00001) {
                for (int v : stableSets.get(j)) {
                    coloring.setColor(v, col);
                }
                col++;
            }
        }
        return coloring;
    }
}
