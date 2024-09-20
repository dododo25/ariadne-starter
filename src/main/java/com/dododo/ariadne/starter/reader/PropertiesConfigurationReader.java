package com.dododo.ariadne.starter.reader;

import com.dododo.ariadne.core.configuration.Configuration;

import java.util.Comparator;
import java.util.Properties;

public class PropertiesConfigurationReader extends ConfigurationReader {

    private static final Comparator<String> KEYS_COMPARATOR = Comparator
            .comparingInt(o -> Integer.parseInt(o.substring(o.lastIndexOf('.') + 1)));

    private static final Properties SYSTEM_PROPERTIES = System.getProperties();

    @Override
    public Configuration read() {
        Properties properties = new Properties();

        SYSTEM_PROPERTIES
                .stringPropertyNames()
                .stream()
                .filter(key -> key.startsWith("flowchart"))
                .forEach(key -> properties.setProperty(key, SYSTEM_PROPERTIES.getProperty(key)));

        validateProperties(properties);
        prepareProperties(properties);

        return create(properties);
    }

    @Override
    public void close() throws Exception {
        // not used for this class
    }

    private static void validateProperties(Properties properties) {
        validateProperty(properties, "flowchart.input.profile");
        validateProperty(properties, "flowchart.output.profile");

        boolean b = properties.stringPropertyNames().stream().anyMatch(k -> k.startsWith("flowchart.input.file"));

        if (!b) {
            throw new IllegalArgumentException("Config file must contain at least one 'flowchart.input.file' property");
        }
    }

    private static void validateProperty(Properties properties, String key) {
        if (!properties.containsKey(key)) {
            throw new IllegalArgumentException(String.format("Config file must contain '%s' property", key));
        }
    }

    private static void prepareProperties(Properties properties) {
        if (!properties.containsKey("flowchart.output.directory")) {
            properties.setProperty("flowchart.output.directory", ".");
        }
    }

    private static Configuration create(Properties properties) {
        Configuration.Builder builder = new Configuration.Builder();

        builder.setInputProfile(properties.getProperty("flowchart.input.profile"));
        builder.setOutputProfile(properties.getProperty("flowchart.output.profile"));
        builder.setOutputDir(properties.getProperty("flowchart.output.directory"));
        builder.setLoadReply(Boolean.parseBoolean(properties.getProperty("flowchart.loadReply", "false")));

        prepareSortedInputFilesList(builder, properties);
        prepareExcluded(builder, properties);

        return builder.build();
    }

    private static void prepareSortedInputFilesList(Configuration.Builder builder, Properties properties) {
        properties.stringPropertyNames()
                .stream()
                .filter(key -> key.startsWith("flowchart.input.file"))
                .sorted(KEYS_COMPARATOR)
                .map(properties::getProperty)
                .forEach(builder::addInputFile);
    }

    private static void prepareExcluded(Configuration.Builder builder, Properties properties) {
        properties.stringPropertyNames()
                .stream()
                .filter(key -> key.startsWith("flowchart.excluded"))
                .map(properties::getProperty)
                .forEach(builder::addExcluded);
    }
}
