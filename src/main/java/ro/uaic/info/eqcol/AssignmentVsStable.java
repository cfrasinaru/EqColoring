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

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import org.graph4j.GraphUtils;
import org.graph4j.exceptions.TimeLimitExceededException;
import org.graph4j.generators.RandomMultipartiteGenerator;
import static org.graph4j.util.Tools.*;

/**
 * Tests on randomly generated dense graphs comparing comparing the assignment
 * and the set covering models.
 *
 * @author Cristian Frăsinaru
 */
public class AssignmentVsStable {

    private final int timeLimit = 5 * 60 * 1000; // 5min
    private static final String SEP = ", ";
    private boolean assignmentTimeExpired, setCoveringTimeExpired;

    public static void main(String args[]) throws IOException {
        var app = new AssignmentVsStable();
        app.runTests(0.4, 8, 2, 10, 1);
        //app.runTests(0.7, 3, 10, 100, 10);
    }

    private void runTests(double probability, int colorClassSize,
            int numColorsMin, int numColorsMax, int step) throws IOException {
        try (var out = new PrintWriter(new FileWriter("results/random/random_"
                + "_d=" + probability
                + "_k=" + colorClassSize
                + "_p=" + numColorsMin + "-" + numColorsMax
                + ".csv"))) {
            out.println("Density" + SEP
                    + "ColorClassSize" + SEP
                    + "NumColors" + SEP
                    + "NumVertices" + SEP
                    + "NumEdges" + SEP
                    + "NumStableSets" + SEP
                    + "StableCreationTime" + SEP
                    + "SetCoveringTime" + SEP
                    + "AssignmentTime");
            out.flush();

            for (int numColors = numColorsMin; numColors <= numColorsMax; numColors += step) {
                test(out, numColors, colorClassSize, probability);
                if (setCoveringTimeExpired && assignmentTimeExpired) {
                    break;
                }
            }
        }
    }

    private void test(PrintWriter out, int numColors, int colorClassSize, double probability) {
        var graph = new RandomMultipartiteGenerator(numColors, colorClassSize, probability).create();
        graph = GraphUtils.shuffle(graph);
        long t0, t1;

        //run set covering model
        long stableCount = -1;
        long stableCreationTime = -1;
        long setCoveringTime = -1;
        if (!setCoveringTimeExpired) {
            try {
                System.out.println("Set covering model");
                t0 = System.currentTimeMillis();
                var alg1 = new GurobiSetCoveringSolver(graph, timeLimit);
                alg1.createStableSets(colorClassSize, colorClassSize);
                var stableSets = alg1.getStableSets();
                stableCount = stableSets == null ? -1 : stableSets.size();
                t1 = System.currentTimeMillis();
                stableCreationTime = t1 - t0;

                alg1.findColoring(numColors);
                setCoveringTime = System.currentTimeMillis() - t0;
            } catch (TimeLimitExceededException e) {
                setCoveringTimeExpired = true;
            }
            System.out.println("\tSet covering time: " + setCoveringTime);
        }

        //run assignment model
        long assignmentTime = -1;
        if (!assignmentTimeExpired) {
            try {
                System.out.println("Assignment model");
                t0 = System.currentTimeMillis();
                var alg2 = new GurobiAssignmentSolver(graph, timeLimit);
                alg2.findColoring(numColors);
                assignmentTime = System.currentTimeMillis() - t0;
            } catch (TimeLimitExceededException e) {
                assignmentTimeExpired = true;
            }
            System.out.println("\tassignmentTime: " + assignmentTime);
        }

        out.println(round(probability, 2) + SEP
                + colorClassSize + SEP
                + numColors + SEP
                + graph.numVertices() + SEP
                + graph.numEdges() + SEP
                + stableCount + SEP
                + msToSeconds(stableCreationTime) + SEP
                + msToSeconds(setCoveringTime) + SEP
                + msToSeconds(assignmentTime));
        out.flush();
    }

    public static double msToSeconds(long milliseconds) {
        if (milliseconds == -1) {
            return -1;
        }
        return Math.round((milliseconds / 1000.0) * 100.0) / 100.0;
    }

}
