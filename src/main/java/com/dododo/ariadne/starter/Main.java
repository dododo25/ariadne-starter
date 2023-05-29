package com.dododo.ariadne.starter;

import com.dododo.ariadne.common.configuration.Configuration;
import com.dododo.ariadne.common.job.AbstractJob;
import com.dododo.ariadne.common.provider.CommonFlowchartJobsProvider;
import com.dododo.ariadne.common.provider.FlowchartJobsProvider;
import com.dododo.ariadne.core.model.State;
import com.dododo.ariadne.drawio.provider.DrawIoFlowchartJobsProvider;
import com.dododo.ariadne.renpy.provider.RenPyFlowchartJobsProvider;
import com.dododo.ariadne.starter.list.AddItemsOnlyList;
import com.dododo.ariadne.xml.provider.XmlFlowchartJobsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

public class Main {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    private static final Map<Class<? extends FlowchartJobsProvider>, Collection<String>> PROVIDER_PROFILES
            = new HashMap<>();

    static {
        PROVIDER_PROFILES.put(DrawIoFlowchartJobsProvider.class, Collections.singleton("drawio"));
        PROVIDER_PROFILES.put(RenPyFlowchartJobsProvider.class, Collections.singleton("renpy"));
        PROVIDER_PROFILES.put(XmlFlowchartJobsProvider.class, Collections.singleton("xml"));
    }

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
        Class<? extends FlowchartJobsProvider> providerType = PROVIDER_PROFILES.entrySet()
                .stream()
                .filter(entry -> entry.getValue().contains(value))
                .findAny()
                .map(Map.Entry::getKey)
                .orElseThrow(IllegalArgumentException::new);

        return providerType.getDeclaredConstructor().newInstance();
    }

    private static FlowchartJobsProvider prepareInnerProvider() {
        return new CommonFlowchartJobsProvider();
    }
}
