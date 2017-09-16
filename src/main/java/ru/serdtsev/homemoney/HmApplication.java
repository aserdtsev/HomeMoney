package ru.serdtsev.homemoney;

import net.sf.ehcache.CacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import ru.serdtsev.homemoney.patch.DbPatch005;

@ComponentScan
@ServletComponentScan
@EnableAutoConfiguration
@Configuration
@SpringBootApplication
public class HmApplication {

  @Autowired
  public HmApplication(DbPatch005 patch005) {
    //DOMConfigurator.configureAndWatch();
    CacheManager.newInstance();
//    patch005.invoke();
  }

  public static void main(String[] args) throws Exception {
    SpringApplication.run(HmApplication.class, args);
  }
}
