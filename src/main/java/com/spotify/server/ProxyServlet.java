package com.spotify.server;

import com.google.common.io.ByteStreams;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.client.apache4.ApacheHttpClient4;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ProxyServlet extends HttpServlet {

  private WebResource resource;
  private static final Logger log = LoggerFactory.getLogger(ProxyServlet.class);

  @Override
  public void init() throws ServletException {
    ApacheHttpClient4 client = new ApacheHttpClient4();
    resource = client.resource(getInitParameter("url"));
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    WebResource r = resource;
    if (req.getPathInfo() != null) {
      r = r.path(req.getPathInfo());
    }
    for (Map.Entry<String, String[]> p : req.getParameterMap().entrySet()) {
      r = r.queryParam(p.getKey(), p.getValue()[0]);
    }
    ClientResponse response = r.get(ClientResponse.class);
    log.info("{} {}", r.toString(), response.getStatus());
    resp.setStatus(response.getStatus());
    for (Map.Entry<String, List<String>> h : response.getHeaders().entrySet()) {
      resp.setHeader(h.getKey(), h.getValue().get(0));
    }
    ByteStreams.copy(response.getEntityInputStream(), resp.getOutputStream());
  }
}
