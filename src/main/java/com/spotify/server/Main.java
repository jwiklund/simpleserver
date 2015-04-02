package com.spotify.server;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.Resource;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

public class Main {
  public static void main(String[] args) throws Exception {
    Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    rootLogger.setLevel(Level.INFO);

    HandlerList handlers = new HandlerList();

    handlers.addHandler(configureResource(args));
    handlers.addHandler(configureProxy());

    Server server = new Server(8080);
    server.setHandler(handlers);
    server.start();
    server.join();
  }

  private static ResourceHandler configureResource(String[] args) throws ZipException, IOException {
    ResourceHandler resources = new ResourceHandler();
    resources.setDirectoriesListed(true);
    resources.setWelcomeFiles(new String[]{ "index.html" });
    if (args.length == 1) {
      if (new File(args[0]).isDirectory()) {
        resources.setResourceBase(args[0]);
      } else {
        resources.setBaseResource(configureZipResource(new File(args[0]).getAbsoluteFile()));
      }
    } else {
      resources.setResourceBase(".");
    }
    return resources;
  }

  private static Resource configureZipResource(File zipFile) throws ZipException, IOException {
    String resourceURL = "jar:" + zipFile.toURI().toURL() + "!/";

    try (ZipFile zip = new ZipFile(zipFile)) {      
      Set<String> topEntries = new HashSet<>();
      for (Enumeration<? extends ZipEntry> entries = zip.entries(); entries.hasMoreElements() ; ) {
        ZipEntry entry = entries.nextElement();
        int index = entry.getName().indexOf('/');
        if (index == -1) {
          topEntries.add(entry.getName());
        } else {
          topEntries.add(entry.getName().substring(0, index));
        }
      }
      if (topEntries.size() == 1) {
        resourceURL = resourceURL + topEntries.iterator().next() + "/";
      }
    }
    return Resource.newResource(resourceURL);
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
