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
import com.gurobi.gurobi.GRBModel;
import java.util.HashSet;
import org.graph4j.Graph;
import org.graph4j.coloring.Coloring;
import org.graph4j.coloring.ExactColoringBase;
import org.graph4j.exceptions.TimeLimitExceededException;

/**
 *
 * @author Cristian Frăsinaru
 */
public abstract class GurobiColoringBase extends ExactColoringBase {

    protected GRBEnv env;
    protected GRBModel model;

    public GurobiColoringBase(Graph graph) {
        super(graph);
    }

    public GurobiColoringBase(Graph graph, long timeLimit) {
        super(graph, timeLimit);
    }

    @Override
    protected void solve(int numColors) {
        solutions = new HashSet<>();
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

            if (createModel(numColors)) {
                model.optimize();
            }

            if (model.get(GRB.IntAttr.Status) == GRB.Status.OPTIMAL) {
                // Get the solution
                solutions.add(createColoring(numColors));
            } else {
                if (model.get(GRB.IntAttr.Status) == GRB.Status.TIME_LIMIT) {
                    throw new TimeLimitExceededException(timeLimit);
                }
            }
            model.dispose();
            env.dispose();

        } catch (GRBException ex) {
            System.out.println(ex);
        }
    }

    protected abstract boolean createModel(int numColors) throws GRBException;

    protected abstract Coloring createColoring(int numColors) throws GRBException;

}
