/**
 *  Copyright 2005-2016 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package org.jboss.examples.activemq;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportResource;

@SpringBootApplication
@ImportResource({"classpath:spring/camel-context.xml"})
public class Application extends RouteBuilder {

  @Value("${broker.username}")
  String remoteBrokerUsername;
  
  @Value("${broker.password}")
  String remoteBrokerPassword;
  
  @Value("${${broker.serviceName}_SERVICE_HOST}")
  String remoteBrokerHost;
  
  @Value("${${broker.serviceName}_SERVICE_PORT}")
  String remoteBrokerPort;
  
  @Bean
  LockedFileFilter<?> lockedFileFilter() {
    return new LockedFileFilter<>();
  }
  
  @Bean
  MessageDrainerProcessor messageDrainerProcessor() {
    return new MessageDrainerProcessor(remoteConnectionFactory());
  }
  
  @Bean
  ActiveMQConnectionFactory remoteConnectionFactory() {
    return new ActiveMQConnectionFactory(remoteBrokerUsername, remoteBrokerPassword, String.format("tcp://%s:%s", remoteBrokerHost, remoteBrokerPort));
  }
  
  @Bean
  KahaDBFileDeleteProcessStrategy<?> kahaDbFileDeleteProcessStrategy() {
    return new KahaDBFileDeleteProcessStrategy<>();
  }
  
  public static void main(String[] args) {
      SpringApplication.run(Application.class, args);
  }

  @Override
  public void configure() throws Exception {
    from("file:/mnt?initialDelay={{kahaDb.pollingPeriod}}&delay={{kahaDb.pollingPeriod}}&noop=true&idempotent=false&recursive=true&antInclude=**/split-*/serverData/kahadb/lock&readLock=none&processStrategy=#kahaDbFileDeleteProcessStrategy")
      .onException(Exception.class).handled(true).log("Caught an exception processing file [${headers[CamelFileName]}]. Exception [${exception}]").end()
      .log("Checking file: [${headers[CamelFileName]}]")
      .filter(method("lockedFileFilter", "accept"))
        .log("Able to lock file but waiting a bit to make sure it's not just a pod restart: [${headers[CamelFileName]}]")
        .delay(constant("{{kahaDb.lockWaitPeriod}}"))
        .filter(method("lockedFileFilter", "accept"))
          .log("Processing file: [${headers[CamelFileName]}]")
          .process("messageDrainerProcessor")
        .end()
      .end()
    ;
  }
}
