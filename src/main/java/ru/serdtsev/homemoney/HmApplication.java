package ru.serdtsev.homemoney;

import net.sf.ehcache.CacheManager;
import org.apache.log4j.xml.DOMConfigurator;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Context;

@ApplicationPath("resources")
@SuppressWarnings("unused")
public class HmApplication extends ResourceConfig {
  private Logger log = LoggerFactory.getLogger(getClass());

  public HmApplication(@Context ServletContext context, @Context ServletConfig config) {
    DOMConfigurator.configureAndWatch(context.getRealPath("/WEB-INF/log4j.xml"));
    CacheManager.newInstance(context.getRealPath("/WEB-INF/ehcache.xml"));
    HmExecutorService.initInstance(config);
  }
}
