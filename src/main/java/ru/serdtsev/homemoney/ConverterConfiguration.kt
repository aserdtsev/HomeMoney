package ru.serdtsev.homemoney;

import lombok.val;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ConversionServiceFactoryBean;
import ru.serdtsev.homemoney.moneyoper.model.MoneyOperItemToDtoConverter;

import java.util.HashSet;

@Configuration
public class ConverterConfiguration implements ApplicationContextAware {
  private ApplicationContext ctx;

  @Bean(name="conversionService")
  public ConversionServiceFactoryBean getConversionService() {
    val bean = new ConversionServiceFactoryBean();
    val converters = new HashSet<Object>();
    converters.add(new MoneyOperItemToDtoConverter(ctx));
    bean.setConverters(converters);
    return bean;
  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    ctx = applicationContext;
  }
}
