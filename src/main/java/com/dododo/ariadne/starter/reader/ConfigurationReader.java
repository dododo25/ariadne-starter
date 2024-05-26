package com.dododo.ariadne.starter.reader;

import com.dododo.ariadne.common.configuration.Configuration;

public abstract class ConfigurationReader implements AutoCloseable {

    public abstract Configuration read() throws Exception;
}
