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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.graph4j.coloring.Coloring;

/**
 * Times are in seconds, with 2 decimals.
 *
 * @author Cristian Frăsinaru
 */
@JsonPropertyOrder({"numColors", "stableSetCount", "presolveTime", "solveTime", "feasible", "coloring", "colorClasses"})
@JsonIgnoreProperties(ignoreUnknown = true)
public class Result implements Comparable<Result> {

    @JsonIgnore
    private Instance instance;

    private int numColors;
    private long stableSetCount;
    private long presolveTime;
    private long solveTime;
    private Status status = Status.UNKNOWN;

    @JsonIgnore
    private Coloring coloring;

    public Result() {
    }
    
    public Result(Instance instance, int numColors) {
        this.instance = instance;
        this.numColors = numColors;
    }

    public int getNumColors() {
        return numColors;
    }

    public long getStableSetCount() {
        return stableSetCount;
    }

    public void setStableSetCount(long stableSetCount) {
        this.stableSetCount = stableSetCount;
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

    public long getSolveTime() {
        return solveTime;
    }

    public void setSolveTime(long solveTime) {
        this.solveTime = solveTime;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Coloring getColoring() {
        return coloring;
    }

    public void setColoring(Coloring coloring) {
        this.coloring = coloring;
    }

    @JsonProperty("coloring")
    public String getColoringAsString() {
        return coloring != null ? coloring.toString() : null;
    }

    public String getColorClasses() {
        return coloring != null ? coloring.getColorClasses().toString() : null;
    }

    @Override
    public int compareTo(Result o) {
        int ret = this.instance.getName().compareTo(o.instance.getName());
        if (ret != 0) {
            return ret;
        }
        return this.numColors - o.numColors;
    }

    @Override
    public String toString() {
        return instance.getName() + "[" + numColors + "]";
    }

}
