package com.dododo.ariadne.starter.reader;

import com.dododo.ariadne.core.configuration.Configuration;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.util.LinkedList;

public class XmlConfigurationReader extends ConfigurationReader {

    private final File file;

    private SAXParser parser;

    private ConfigurationHandler handler;

    public XmlConfigurationReader(String filepath) throws ParserConfigurationException, SAXException {
        SAXParserFactory factory = SAXParserFactory.newInstance();

        this.file = new File(filepath);
        this.parser = factory.newSAXParser();
        this.handler = new ConfigurationHandler();
    }

    @Override
    public Configuration read() throws Exception {
        parser.parse(file, handler);
        return handler.builder.build();
    }

    @Override
    public void close() throws Exception {
        parser = null;
        handler = null;
    }

    private static class ConfigurationHandler extends DefaultHandler {

        private LinkedList<String> tags;

        private Configuration.Builder builder;

        @Override
        public void startDocument() {
            tags = new LinkedList<>();
            builder = new Configuration.Builder();
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attrs) throws SAXException {
            switch (qName) {
                case "flags":
                    builder.setLoadReply(Boolean.parseBoolean(attrs.getValue("loadReply")));
                    break;
                case "file":
                    builder.addInputFile(attrs.getValue("value"));
                    break;
                case "profile":
                    String lastTag = tags.getLast();

                    if (lastTag.equals("input")) {
                        builder.setInputProfile(attrs.getValue("value"));
                    } else if (lastTag.equals("output")) {
                        builder.setOutputProfile(attrs.getValue("value"));
                    }
                    break;
                case "directory":
                    builder.setOutputDir(attrs.getValue("path"));
                    break;
                case "exclude":
                    builder.addExcluded(attrs.getValue("value"));
                    break;
                case "config":
                case "input":
                case "output":
                case "files":
                case "excludes":
                    break;
                default:
                    throw new SAXException(String.format("unknown tag %s", qName));
            }

            tags.add(qName);
        }

        @Override
        public void endElement(String uri, String localName, String qName) {
            tags.removeLast();
        }
    }
}
