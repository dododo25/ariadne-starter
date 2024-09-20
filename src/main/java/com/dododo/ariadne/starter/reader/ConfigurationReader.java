package com.dododo.ariadne.starter.reader;

import com.dododo.ariadne.core.configuration.Configuration;

public abstract class ConfigurationReader implements AutoCloseable {

    public abstract Configuration read() throws Exception;
}
