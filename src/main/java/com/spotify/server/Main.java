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

import java.io.FileInputStream;
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

    ServletContextHandler servlets = new ServletContextHandler();
    Yaml yaml = new Yaml();
    Map<?, ?> config = (Map<?, ?>) yaml.load(new FileInputStream("server.conf"));
    for (Map.Entry<?, ?> proxy : config.entrySet()) {
      if (((String) proxy.getKey()).startsWith("proxy_")) {
        String name = ((String) proxy.getKey()).substring("proxy_".length());
        ServletHolder proxyHandler = new ServletHolder(name, new ProxyServlet());
        proxyHandler.setInitParameter("url", (String) proxy.getValue());
        servlets.addServlet(proxyHandler, "/" + name);  
      }
    }
    handlers.addHandler(servlets);

    Server server = new Server(8080);
    server.setHandler(handlers);
    server.start();
    server.join();
  }
}
