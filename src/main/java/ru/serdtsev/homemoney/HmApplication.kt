package ru.serdtsev.homemoney

import net.sf.ehcache.CacheManager
import org.apache.log4j.xml.DOMConfigurator
import org.glassfish.jersey.server.ResourceConfig
import javax.servlet.ServletContext
import javax.ws.rs.ApplicationPath
import javax.ws.rs.core.Context

@ApplicationPath("resources")
class HmApplication(@Context context: ServletContext) : ResourceConfig() {
  init {
    DOMConfigurator.configureAndWatch(context.getRealPath("/WEB-INF/log4j.xml"))
    CacheManager.newInstance(context.getRealPath("/WEB-INF/ehcache.xml"))
  }
}
