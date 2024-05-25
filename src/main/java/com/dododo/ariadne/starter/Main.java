package com.dododo.ariadne.starter;

import com.dododo.ariadne.common.configuration.Configuration;
import com.dododo.ariadne.common.job.AbstractJob;
import com.dododo.ariadne.common.provider.CommonFlowchartJobsProvider;
import com.dododo.ariadne.common.provider.FlowchartJobsProvider;
import com.dododo.ariadne.core.model.State;
import com.dododo.ariadne.starter.list.AddItemsOnlyList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

public class Main {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    public static void main(String... args) {
        try {
            Configuration configuration = createConfiguration();

            FlowchartJobsProvider inputProvider = prepareProvider(configuration.getInputProfile());
            FlowchartJobsProvider innerProvider = prepareInnerProvider();
            FlowchartJobsProvider outputProvider = prepareProvider(configuration.getOutputProfile());

            inputProvider.setConfiguration(configuration);
            outputProvider.setConfiguration(configuration);

            run(configuration, inputProvider, innerProvider, outputProvider);
        } catch (ReflectiveOperationException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    private static void run(Configuration configuration, FlowchartJobsProvider... distributors) {
        AtomicReference<State> ref = new AtomicReference<>();

        List<AbstractJob> jobs = new AddItemsOnlyList<>();

        Arrays.stream(distributors)
                .forEach(provider -> provider.populateJobs(jobs));

        for (AbstractJob job : jobs) {
            job.setConfiguration(configuration);
            job.setFlowchart(ref.get());

            LOG.info("Running {}", job.getName());

            job.run();
            ref.set(job.getFlowchart());

            LOG.info("{} ends successfully with code 0", job.getName());
        }
    }

    private static Configuration createConfiguration() {
        Properties properties = new Properties();

        System.getProperties().stringPropertyNames()
                .stream()
                .filter(key -> key.startsWith("flowchart"))
                .forEach(key -> properties.setProperty(key, System.getProperty(key)));

        validateProperties(properties);
        prepareProperties(properties);

        return Configuration.create(properties);
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

    private static FlowchartJobsProvider prepareProvider(String value)
            throws ReflectiveOperationException {
        Class<?> providerType;
        ClassLoader loader = ClassLoader.getSystemClassLoader();

        try {
            switch (value) {
                case "ariadne":
                    providerType = loader.loadClass("com.dododo.ariadne.thread.provider.ThreadFlowchartJobsProvider");
                    break;
                case "drawio":
                    providerType = loader.loadClass("com.dododo.ariadne.drawio.provider.DrawIoFlowchartJobsProvider");
                    break;
                case "renpy":
                    providerType = loader.loadClass("com.dododo.ariadne.renpy.rpy.provider.RenPyFlowchartJobsProvider");
                    break;
                case "xml":
                    providerType = loader.loadClass("com.dododo.ariadne.xml.provider.XmlFlowchartJobsProvider");
                    break;
                case "unity":
                    providerType = loader.loadClass("com.dododo.ariadne.renpy.unity.provider.UnityFlowchartJobsProvider;");
                    break;
                default:
                    throw new IllegalArgumentException(String.format("Can`t find provider class for %s", value));
            }
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(String.format("Can`t find provider class for %s", value), e);
        }

        return (FlowchartJobsProvider) providerType.getDeclaredConstructor().newInstance();
    }

    private static FlowchartJobsProvider prepareInnerProvider() {
        return new CommonFlowchartJobsProvider();
    }
}
