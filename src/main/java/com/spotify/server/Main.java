package com.spotify.server;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Map;

public class Main {
  public static void main(String[] args) throws Exception {
    Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    rootLogger.setLevel(Level.INFO);

    HandlerList handlers = new HandlerList();

    ResourceHandler resources = new ResourceHandler();
    resources.setDirectoriesListed(true);
    resources.setWelcomeFiles(new String[]{ "index.html" });
    resources.setResourceBase(".");
    handlers.addHandler(resources);

    handlers.addHandler(configureProxy());

    Server server = new Server(8080);
    server.setHandler(handlers);
    server.start();
    server.join();
  }

  private static ServletContextHandler configureProxy() throws FileNotFoundException {
    ServletContextHandler servlets = new ServletContextHandler();
    if (!new File("server.conf").exists()) {
      return servlets;
    }
    Yaml yaml = new Yaml();
    Object loaded = yaml.load(new FileInputStream("server.conf"));
    if (!(loaded instanceof Map)) {
      throw new Error("Invalid YAML, got " + loaded.getClass() + ", expected map (see README.md for valid yaml example)");
    }
    Map<?, ?> config = (Map<?, ?>) loaded;
    if (config.entrySet() != null) {
      for (Map.Entry<?, ?> proxy : config.entrySet()) {
        if (((String) proxy.getKey()).startsWith("proxy_")) {
          String name = ((String) proxy.getKey()).substring("proxy_".length());
          ServletHolder proxyHandler = new ServletHolder(name, new ProxyServlet());
          proxyHandler.setInitParameter("url", (String) proxy.getValue());
          servlets.addServlet(proxyHandler, "/" + name);
        }
      }
    }
    return servlets;
  }
}
