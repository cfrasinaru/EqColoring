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

/**
 *
 * @author Cristian Frăsinaru
 */
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.graph4j.Graph;

@JsonPropertyOrder({"name", "lb", "ub", "solveTime", "chi"})
public class Instance {

    private String name;

    @JsonProperty("lb")
    private Integer lowerBound;

    @JsonProperty("ub")
    private Integer upperBound;

    @JsonProperty("chi")
    private Integer coloringNumber; // GCP, not ECP

    @JsonIgnore
    private Graph graph;

    public Instance() {
    }
    
    public Instance(Graph graph) {
        this("unknown graph");
        this.graph = graph;
    }

    public Instance(String name) {
        this(name, null, null, null);
    }

    public Instance(String name, Integer lowerBound, Integer upperBound, Integer coloringNumber) {
        this.name = name;
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
        this.coloringNumber = coloringNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getLowerBound() {
        return lowerBound;
    }

    public void setLowerBound(Integer lowerBound) {
        this.lowerBound = lowerBound;
    }

    public Integer getUpperBound() {
        return upperBound;
    }

    public void setUpperBound(Integer upperBound) {
        this.upperBound = upperBound;
    }

    public Integer getColoringNumber() {
        return coloringNumber;
    }

    public void setColoringNumber(Integer coloringNumber) {
        this.coloringNumber = coloringNumber;
    }

    public Graph getGraph() {
        return graph;
    }

    public void setGraph(Graph graph) {
        this.graph = graph;
    }

    @Override
    public String toString() {
        return name;
    }

}
