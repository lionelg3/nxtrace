package com.gmail.lionelg3.nxtraces;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.gmail.lionelg3.nxtraces.db.Repository;
import com.gmail.lionelg3.nxtraces.engine.NXTraces;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.plist.PropertyListConfiguration;
import org.jboss.logging.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URISyntaxException;

/**
 * Project "nstraces"
 * Created by lionel on 07/03/2016.
 */
public class Main {

    private static final Logger LOGGER = Logger.getLogger(Main.class);

    @Parameter(names={"--server"}, description = "Run as server mode")
    private boolean server;

    @Parameter(names={"--dev"}, description = "Run as server dev mode")
    private boolean dev;

    @Parameter(names={"--help"}, description = "Show usage command")
    private boolean help;

    @Parameter(names={"--conf"}, description = "Application configuration file")
    private String configFile;

    public static void main(String ... args) {
        Main main = new Main();
        JCommander cmd = new JCommander(main, args);
        // Server mode
        if (main.server && main.configFile != null && main.configFile.length() > 0) {
            main.runInServerMode(main.configFile);
            return;
        }
        // Dev mode
        if (main.dev) {
            main.runInDevMode();
            return;
        }
        // Usage
        cmd.usage();
    }

    private void run() {
        try {
            // Load WebServer
            new NXTraces().startup();
            LOGGER.info("WebServer ready");
            // Load Repository
            Repository.singleton();
            LOGGER.info("Repository ready");
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    private void runInDevMode() {
        System.out.println("=========================================================================================");
        System.out.println("  Dev mode");
        System.out.println("  Current path: " + new File(".").getAbsolutePath());
        NXTraces.CONFIGURATION = new PropertyListConfiguration();
        NXTraces.CONFIGURATION.setProperty("undertow.host", "0.0.0.0");
        NXTraces.CONFIGURATION.setProperty("undertow.context", "nxtraces");
        NXTraces.CONFIGURATION.setProperty("undertow.ajp_port", "8009");
        NXTraces.CONFIGURATION.setProperty("undertow.http_port", "8080");
        NXTraces.CONFIGURATION.setProperty("repository.path", "db/files");
        NXTraces.CONFIGURATION.setProperty("logging.configuration", "./conf/logback-dev.xml");
        NXTraces.CONFIGURATION.setProperty("database.configuration", "./conf/database.xml");
        loadLoggerFile(NXTraces.CONFIGURATION.getString("logging.configuration"));
        System.out.println("  - logging configuration = " + NXTraces.CONFIGURATION.getString("logging.configuration"));
        System.out.println("  - database configuration = " + NXTraces.CONFIGURATION.getString("database.configuration"));
        System.out.println("=========================================================================================");
        run();
    }

    private void runInServerMode(String configFile) {
        System.out.println("=========================================================================================");
        System.out.println("  Server mode");
        System.out.println("  - configuration file = " + configFile);
        loadConfgurationFile(configFile);
        System.out.println("  - logging configuration = " + NXTraces.CONFIGURATION.getString("logging.configuration"));
        loadLoggerFile(NXTraces.CONFIGURATION.getString("logging.configuration"));
        System.out.println("  - database configuration = " + NXTraces.CONFIGURATION.getString("database.configuration"));
        System.out.println("=========================================================================================");
        run();
    }

    private void loadConfgurationFile(String configFile) {
        try {
            NXTraces.CONFIGURATION = new PropertiesConfiguration(configFile);
        } catch (ConfigurationException e) {
            System.err.println("Configuration file " + configFile +" not found.");
            System.exit(-1);
        }
    }

    private void loadLoggerFile(String loggerFile) {
        try {
            LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
            context.reset();
            JoranConfigurator configurator = new JoranConfigurator();
            configurator.setContext(context);
            context.putProperty("application-name", NXTraces.CONFIGURATION.getString("undertow.context"));
            configurator.doConfigure(loggerFile);
        } catch (Exception e) {
            System.err.println("logback configuration file not found " + loggerFile);
            e.printStackTrace();
            System.exit(-1);
        }
    }
}
