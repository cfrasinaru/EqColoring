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

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.graph4j.io.DimacsIO;

/**
 *
 * @author Cristian Frăsinaru
 */
public class Dataset {

    public static final String DATASET_PATH = "d:/datasets/coloring/instances/";
    public static final String RESULTS_PATH = "results/benchmark/";
    public static final String INFO_FILENAME = "instances.json";
    private final Map<String, Instance> instanceMap = new HashMap<>();

    public Dataset() {
        loadInstances();
    }

    public static void main(String args[]) throws IOException {
        var app = new Dataset();
        //app.runTests();
        System.out.println(app.loadInstanceResults("C2000.9"));
    }

    private void runTests() {
        System.out.println(findAll());
    }

    private void loadInstances() {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            instanceMap.clear();
            List<String> lines = Files.readAllLines(Paths.get(INFO_FILENAME));
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("--")) {
                    continue;
                }
                Instance i = objectMapper.readValue(line, Instance.class);
                instanceMap.put(i.getName(), i);
            }
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    public List<Instance> findAll() {
        return instanceMap.values().stream().sorted().toList();
    }

    public Instance getInstance(String name) {
        var instance = instanceMap.get(name);
        if (instance == null) {
            System.err.println("No information found for instance: " + name);
            instance = new Instance(name);
        }
        var graph = new DimacsIO().read(DATASET_PATH + name + ".col");
        instance.setGraph(graph);
        return instance;
    }

    public void writeResults(InstanceResults r) {
        String filename = RESULTS_PATH + r.getInstance().getName() + ".json";
        try (var out = new PrintWriter(new FileWriter(filename, true))) {
            out.println();
            ObjectMapper mapper = new ObjectMapper();
            out.println();
            mapper.writerWithDefaultPrettyPrinter().writeValue(out, r);
        } catch (IOException ex) {
            System.err.println(ex);
        }
    }

    public List<InstanceResults> loadInstanceResults(String name) {
        String filename = RESULTS_PATH + name + ".json";
        try {
            ObjectMapper mapper = new ObjectMapper();
            MappingIterator<InstanceResults> it
                    = mapper.readerFor(InstanceResults.class)
                            .readValues(new File(filename));

            List<InstanceResults> list = new ArrayList<>();
            while (it.hasNext()) {
                var ir = it.next();
                ir.setInstance(getInstance(name));
                list.add(ir);
            }
            return list;
        } catch (IOException ex) {
            System.err.println(ex);
            return null;
        }
    }
    
}
