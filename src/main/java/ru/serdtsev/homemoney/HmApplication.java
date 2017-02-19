package ru.serdtsev.homemoney;

import net.sf.ehcache.CacheManager;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import ru.serdtsev.homemoney.dao.DbPatches;

@ComponentScan
@ServletComponentScan
@EnableAutoConfiguration
@Configuration
public class HmApplication {

  public HmApplication() {
    //DOMConfigurator.configureAndWatch();
    CacheManager.newInstance();
  }

  public static void main(String[] args) throws Exception {
    SpringApplication.run(HmApplication.class, args);
    DbPatches.doPatch2();
  }
}
