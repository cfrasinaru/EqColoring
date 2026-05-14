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
import org.graph4j.Graph;
import org.graph4j.clique.MaximalCliqueFinder;
import org.graph4j.coloring.Coloring;
import org.graph4j.coloring.eq.EquitableColoringAlgorithm;
import org.graph4j.util.IntArrays;

/**
 * ILP Assignment model. Requires a valid Gurobi installation.
 *
 * @author Cristian Frăsinaru
 */
public class GurobiAssignmentSolver extends GurobiColoringBase
        implements EquitableColoringAlgorithm {

    private GRBVar x[][];

    public GurobiAssignmentSolver(Graph graph) {
        super(graph);
    }

    public GurobiAssignmentSolver(Graph graph, long timeLimit) {
        super(graph, timeLimit);
    }

    @Override
    protected GurobiAssignmentSolver getInstance(Graph graph, long timeLimit) {
        return new GurobiAssignmentSolver(graph, timeLimit);
    }

    @Override
    protected boolean createModel(int numColors) throws GRBException {
        int n = graph.numVertices();
        int p = numColors;
        int n1 = n % p == 0 ? n : (1 + (n / p)) * p;
        //variables in the range [n,n1) are fake (vertices connected with everybody else)

        x = new GRBVar[n1][p];
        for (int i = 0; i < n1; i++) {
            for (int c = 0; c < p; c++) {
                x[i][c] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "x[" + i + ", " + c + "]");
            }
        }

        //each node must have a color
        for (int i = 0; i < n1; i++) {
            GRBLinExpr sum = new GRBLinExpr();
            for (int c = 0; c < p; c++) {
                sum.addTerm(1, x[i][c]);
            }
            model.addConstr(sum, GRB.EQUAL, 1, "color_" + i);
        }

        //two adjacent nodes cannot have the same color
        for (int v : graph.vertices()) {
            int vi = graph.indexOf(v);
            for (var it = graph.neighborIterator(v); it.hasNext();) {
                int u = it.next();
                if (v < u) {
                    int ui = graph.indexOf(u);
                    for (int c = 0; c < p; c++) {
                        GRBLinExpr sum = new GRBLinExpr();
                        sum.addTerm(1, x[ui][c]);
                        sum.addTerm(1, x[vi][c]);
                        model.addConstr(sum, GRB.LESS_EQUAL, 1, "diffcolor_" + u + "," + v + " - " + c);
                    }
                }
            }
        }

        //fake nodes are all connected with each other
        for (int u = n; u < n1 - 1; u++) {
            for (int v = u + 1; v < n1; v++) {
                for (int c = 0; c < p; c++) {
                    GRBLinExpr sum = new GRBLinExpr();
                    sum.addTerm(1, x[u][c]);
                    sum.addTerm(1, x[v][c]);
                    model.addConstr(sum, GRB.LESS_EQUAL, 1, "diffcolor_" + u + "," + v + " - " + c);
                }
            }
        }

        //coloring class sizes
        for (int c = 0; c < p; c++) {
            GRBLinExpr sum = new GRBLinExpr();
            for (int u = 0; u < n1; u++) {
                sum.addTerm(1, x[u][c]);
            }
            model.addConstr(sum, GRB.EQUAL, n1 / p, "size_eq_" + c);
        }

        //symmetry breaking        
        //Set initial colors for some max clique
        int color = 0;
        var maxClique = getMaximalClique();
        int[] cliqueVertices = IntArrays.sort(maxClique.vertices());
        for (int u : cliqueVertices) {
            if (color >= numColors) {
                return false;
            }
            model.addConstr(x[graph.indexOf(u)][color++], GRB.EQUAL, 1, "maxclique_" + u);
        }

        //add aditional clique-based constraints 
        for (int v : graph.vertices()) {
            var q = new MaximalCliqueFinder(graph).getMaximalClique(v);
            if (q.size() < 3) {
                continue;
            }
            for (int c = 0; c < p; c++) {
                GRBLinExpr sum = new GRBLinExpr();
                for (int u : q.vertices()) {
                    sum.addTerm(1, x[graph.indexOf(u)][c]);
                }
                model.addConstr(sum, GRB.LESS_EQUAL, 1, "clique_" + v + "_" + c);
            }
        }

        //Exploit symmetries (vertices having the same set of neighbors)
        //for normal nodes and mocks
        //forces colors to be used in order if nodes have same neighbors
        int[] vertices = graph.vertices();
        int[] emptyArr = new int[0];
        for (int i = 0; i < n1 - 1; i++) {
            int v = i < n ? vertices[i] : (n + i);
            if (maxClique.contains(v)) {
                continue;
            }
            var vn = i < n ? graph.neighbors(v) : emptyArr;
            for (int j = i + 1; j < n1; j++) {
                int u = j < n ? vertices[j] : (n + j);
                if (maxClique.contains(u)) {    
                    continue;
                }
                var un = j < n ? graph.neighbors(u) : emptyArr;
                if (IntArrays.haveSameValues(vn, un)) {
                    //if i is colored with ci and j with cj
                    //ci must be smaller than cj (or equal)
                    GRBLinExpr sumi = new GRBLinExpr();
                    GRBLinExpr sumj = new GRBLinExpr();
                    for (int c = 0; c < p; c++) {
                        sumi.addTerm(1, x[i][c]);
                        sumj.addTerm(1, x[j][c]);
                    }
                    model.addConstr(sumi, GRB.LESS_EQUAL, sumj, "colororder_" + v + "_" + u);
                }
            }
        }
        return true;
    }

    @Override
    protected Coloring createColoring(int numColors) throws GRBException {
        Coloring coloring = new Coloring(graph);
        for (int i = 0, n = graph.numVertices(); i < n; i++) {
            int v = graph.vertexAt(i);
            for (int c = 0; c < numColors; c++) {
                if (x[i][c].get(GRB.DoubleAttr.X) > .00001) {
                    coloring.setColor(v, c);
                    break;
                }
            }
        }
        return coloring;
    }

}
