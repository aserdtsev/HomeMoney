package ru.serdtsev.homemoney;

import net.sf.ehcache.CacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.Configuration;

@ServletComponentScan
@Configuration
@SpringBootApplication
public class HmApplication {

  @Autowired
  public HmApplication() {
    CacheManager.newInstance();
  }

  public static void main(String[] args) {
    SpringApplication.run(HmApplication.class, args);
  }
}
