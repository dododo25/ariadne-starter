package com.dododo.ariadne.starter.reader;

import com.dododo.ariadne.core.configuration.Configuration;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class JsonConfigurationReader extends ConfigurationReader {

    private final BufferedReader reader;

    private final JSONParser parser;

    public JsonConfigurationReader(String filepath) throws FileNotFoundException {
        this.reader = new BufferedReader(new FileReader(filepath));
        this.parser = new JSONParser();
    }

    @Override
    public Configuration read() throws Exception {
        StringBuilder composed = new StringBuilder();

        String line;

        while ((line = reader.readLine()) != null) {
            composed.append(line);
        }

        return createConfiguration((JSONObject) parser.parse(composed.toString()));
    }

    @Override
    public void close() throws Exception {
        reader.close();
    }

    private Configuration createConfiguration(JSONObject obj) {
        Configuration.Builder builder = new Configuration.Builder();

        builder.setInputProfile((String) getValue(obj, "input.profile"));
        builder.setOutputProfile((String) getValue(obj, "output.profile"));
        builder.setOutputDir(prepareOutputProfile(obj));
        builder.setLoadReply((boolean) getValue(obj, "flags.loadReply"));

        getValuesList(obj, "input.files").stream()
                .map(o -> (String) o)
                .forEach(builder::addInputFile);

        getValuesList(obj, "excludes").stream()
                .map(o -> (String) o)
                .forEach(builder::addExcluded);

        return builder.build();
    }

    private String prepareOutputProfile(JSONObject obj) {
        String path = (String) getValue(obj, "output.directory.path");

        if ((boolean) getValue(obj, "output.directory.relative")) {
            return Paths.get(path).toAbsolutePath().toString();
        }

        return path;
    }

    private static Object getValue(JSONObject obj, String path) {
        Object res = obj;

        for (String part : path.split("\\.")) {
            res = ((JSONObject) res).get(part);
        }

        return res;
    }

    private static List<Object> getValuesList(JSONObject obj, String path) {
        Object res = obj;

        for (String part : path.split("\\.")) {
            res = ((JSONObject) res).get(part);
        }

        return Arrays.stream(((JSONArray) res).toArray())
                .collect(Collectors.toList());
    }
}
