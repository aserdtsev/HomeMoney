package ru.serdtsev.homemoney;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import javax.ws.rs.core.Application;

public class MyResourceTest extends JerseyTest {
  @Override
  protected Application configure() {
    return new ResourceConfig(UserResource.class);
  }

//  @Test
//  public void testGetIt() throws Exception {
//    final String getIt = target("myresource").request().get(String.class);
//    assertEquals("Got it!", getIt);
//  }

}