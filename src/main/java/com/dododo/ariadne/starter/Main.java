package com.dododo.ariadne.starter;

import com.dododo.ariadne.core.configuration.Configuration;
import com.dododo.ariadne.core.job.AbstractJob;
import com.dododo.ariadne.core.model.State;
import com.dododo.ariadne.core.provider.FlowchartJobsProvider;
import com.dododo.ariadne.starter.list.AddItemsOnlyList;
import com.dododo.ariadne.starter.reader.ConfigurationReader;
import com.dododo.ariadne.starter.reader.JsonConfigurationReader;
import com.dododo.ariadne.starter.reader.PropertiesConfigurationReader;
import com.dododo.ariadne.starter.reader.XmlConfigurationReader;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class Main {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    public static void main(String... args) {
        HelpFormatter formatter = new HelpFormatter();

        OptionGroup group = new OptionGroup()
                .addOption(new Option(null, "json", true, "path to *.json configuration file"))
                .addOption(new Option(null, "xml", true, "path to *.xml configuration file"));

        Options options = new Options()
                .addOption("h", "help", false, "print help")
                .addOptionGroup(group);

        try {
            CommandLine line = new DefaultParser()
                    .parse(options, args);

            if (line.hasOption("help")) {
                formatter.printHelp("java -jar [-VM options] ariadne", options, true);
            } else {
                start(line.getOptionValue("xml"), line.getOptionValue("json"));
            }
        } catch (ParseException e) {
            formatter.printHelp("java -jar [-VM options] ariadne", options, true);
        }
    }

    private static void start(String xmlValue, String jsonValue) {
        try(ConfigurationReader reader = prepareReader(xmlValue, jsonValue)) {
            Configuration configuration = reader.read();

            FlowchartJobsProvider inputProvider  = prepareProvider(configuration.getInputProfile());
            FlowchartJobsProvider outputProvider = prepareProvider(configuration.getOutputProfile());

            inputProvider.setConfiguration(configuration);
            outputProvider.setConfiguration(configuration);

            run(configuration, inputProvider, outputProvider);
        } catch (Exception e) {
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

    private static FlowchartJobsProvider prepareProvider(String value)
            throws ReflectiveOperationException {
        Class<?> providerType;
        ClassLoader loader = ClassLoader.getSystemClassLoader();

        try {
            switch (value) {
                case "renpy":
                    providerType = loader.loadClass("com.dododo.ariadne.renpy.provider.RenPyFlowchartJobsProvider");
                    break;
                case "xml":
                    providerType = loader.loadClass("com.dododo.ariadne.xml.provider.XmlFlowchartJobsProvider");
                    break;
                default:
                    throw new IllegalArgumentException(String.format("Can`t find provider class for %s", value));
            }
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(String.format("Can`t find provider class for %s", value), e);
        }

        return (FlowchartJobsProvider) providerType.getDeclaredConstructor().newInstance();
    }

    private static ConfigurationReader prepareReader(String xmlValue, String jsonValue) throws Exception {
        if (xmlValue != null) {
            return new XmlConfigurationReader(xmlValue);
        }

        if (jsonValue != null) {
            return new JsonConfigurationReader(jsonValue);
        }

        return new PropertiesConfigurationReader();
    }
}
