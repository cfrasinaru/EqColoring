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

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.graph4j.Graph;
import org.graph4j.measures.GraphMeasures;

/**
 *
 * @author Cristian Frăsinaru
 */
@JsonPropertyOrder({"instanceName", "numVertices", "numEdges", "density",
    "knownLowerBound", "knownUpperBound", "coloringNumber",
    "timeLimit", "startTime", "endTime", "totalTime",
    "initialLowerBound", "initialUpperBound", "maxCliqueSize", "presolveTime",
    "results",
    "successResults", "totalResults", "avgSolveTime", "maxSolveTime",
    "lowerBound", "upperBound"
})
@JsonIgnoreProperties(ignoreUnknown = true)
public class InstanceResults {

    @JsonIgnore
    private Instance instance;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;
    //
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;
    //
    private int initialLowerBound;
    private int initialUpperBound;
    private long timeLimit;
    private int maxCliqueSize;
    private long presolveTime;
    private final List<Result> results = new ArrayList<>();

    @JsonIgnore
    private Graph graph;

    public InstanceResults() {
    }

    public InstanceResults(Instance instance, long timeLimit) {
        this.instance = instance;
        this.timeLimit = timeLimit;
        this.graph = instance.getGraph();
        this.startTime = new Date();
    }

    public void setInstance(Instance instance) {
        this.instance = instance;
    }

    public Instance getInstance() {
        return instance;
    }

    public String getInstanceName() {
        return instance.getName();
    }

    public void setTimeLimit(long timeLimit) {
        this.timeLimit = timeLimit;
    }

    public void setGraph(Graph graph) {
        this.graph = graph;
    }

    public Graph getGraph() {
        return graph;
    }

    public Integer getKnownLowerBound() {
        return instance.getLowerBound();
    }

    public Integer getKnownUpperBound() {
        return instance.getUpperBound();
    }

    public Integer getColoringNumber() {
        return instance.getColoringNumber();
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public long getTotalTime() {
        if (startTime == null || endTime == null) {
            return 0;
        }
        return endTime.getTime() - startTime.getTime();
    }

    public List<Result> getResults() {
        return results;
    }

    public void add(Result result) {
        results.add(result);
    }

    public int getInitialLowerBound() {
        return initialLowerBound;
    }

    public void setInitialLowerBound(int initialLowerBound) {
        this.initialLowerBound = initialLowerBound;
    }

    public int getInitialUpperBound() {
        return initialUpperBound;
    }

    public void setInitialUpperBound(int initialUpperBound) {
        this.initialUpperBound = initialUpperBound;
    }

    public long getTimeLimit() {
        return timeLimit;
    }

    public int getNumVertices() {
        return graph.numVertices();
    }

    public int getNumEdges() {
        return (int) graph.numEdges();
    }

    public double getDensity() {
        double d = GraphMeasures.density(graph);
        return Math.round(d * 100.0) / 100.0;
    }

    public int getMaxCliqueSize() {
        return maxCliqueSize;
    }

    public void setMaxCliqueSize(int maxCliqueSize) {
        this.maxCliqueSize = maxCliqueSize;
    }

    public long getPresolveTime() {
        return presolveTime;
    }

    public void setPresolveTime(long presolveTime) {
        this.presolveTime = presolveTime;
    }

    public void incPresolveTime(long time) {
        this.presolveTime += time;
    }

    public int getLowerBound() {
        return results.stream()
                .filter(r -> r.getStatus() == Status.UNFEASIBLE)
                .mapToInt(r -> 1 + r.getNumColors())
                .max().orElse(0);
    }

    public int getUpperBound() {
        return results.stream()
                .filter(r -> r.getStatus() == Status.FEASIBLE)
                .mapToInt(r -> r.getNumColors())
                .min().orElse(0);
    }

    public long getAvgSolveTime() {
        return (long) results.stream()
                .filter(r -> r.getStatus() == Status.FEASIBLE || r.getStatus() == Status.UNFEASIBLE)
                .mapToLong(r -> r.getPresolveTime() + r.getSolveTime())
                .average().orElse(0);
    }

    public long getMaxSolveTime() {
        return (long) results.stream()
                .filter(r -> r.getStatus() == Status.FEASIBLE || r.getStatus() == Status.UNFEASIBLE)
                .mapToLong(r -> r.getPresolveTime() + r.getSolveTime())
                .max().orElse(0);
    }

    public int getSuccessResults() {
        return (int) results.stream()
                .filter(r -> r.getStatus() == Status.FEASIBLE || r.getStatus() == Status.UNFEASIBLE)
                .count();
    }

    public int getTotalResults() {
        return results.size();
    }

    @Override
    public String toString() {
        return instance.getName() + "(" + initialLowerBound + "," + initialUpperBound + ") at " + startTime;
    }

}
