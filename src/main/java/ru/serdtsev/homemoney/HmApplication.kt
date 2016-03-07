package ru.serdtsev.homemoney

import net.sf.ehcache.CacheManager
import org.glassfish.jersey.server.ResourceConfig

import javax.servlet.ServletContext
import javax.ws.rs.ApplicationPath
import javax.ws.rs.core.Context

@ApplicationPath("resources")
class HmApplication(@Context context: ServletContext) : ResourceConfig() {
  init {
    CacheManager.newInstance(context.getRealPath("/WEB-INF/ehcache.xml"))
  }
}
