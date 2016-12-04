package ru.serdtsev.homemoney;

import net.sf.ehcache.CacheManager;
import org.apache.log4j.xml.DOMConfigurator;
import org.glassfish.jersey.server.ResourceConfig;

import javax.servlet.ServletContext;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Context;

@ApplicationPath("resources")
@SuppressWarnings("unused")
public class HmApplication extends ResourceConfig {
  public HmApplication(@Context ServletContext context) {
    DOMConfigurator.configureAndWatch(context.getRealPath("/WEB-INF/log4j.xml"));
    CacheManager.newInstance(context.getRealPath("/WEB-INF/ehcache.xml"));
  }
}
