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

import java.io.File;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.store.PersistenceAdapter;
import org.apache.activemq.store.kahadb.KahaDBPersistenceAdapter;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.file.GenericFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageDrainerProcessor implements Processor {

  private static final Logger log = LoggerFactory.getLogger(MessageDrainerProcessor.class);

  private final ActiveMQConnectionFactory remoteConnectionFactory;
  private final String remoteConnectionFactoryUsername;
  private final String remoteConnectionFactoryPassword;

  public MessageDrainerProcessor(ActiveMQConnectionFactory remoteConnectionFactory) {
    this(remoteConnectionFactory, null, null);
  }

  public MessageDrainerProcessor(ActiveMQConnectionFactory remoteConnectionFactory, String remoteConnectionFactoryUsername, String remoteConnectionFactoryPassword) {
    this.remoteConnectionFactory = remoteConnectionFactory;
    this.remoteConnectionFactoryUsername = remoteConnectionFactoryUsername;
    this.remoteConnectionFactoryPassword = remoteConnectionFactoryPassword;
  }

  public ActiveMQConnectionFactory getRemoteConnectionFactory() {
    return remoteConnectionFactory;
  }

  public String getRemoteConnectionFactoryUsername() {
    return remoteConnectionFactoryUsername;
  }

  public String getRemoteConnectionFactoryPassword() {
    return remoteConnectionFactoryPassword;
  }

  @Override
  public void process(Exchange exchange) throws Exception {
    GenericFile<?> file = exchange.getIn().getBody(GenericFile.class);
    File kahaDb = new File(file.getAbsoluteFilePath()).getParentFile();
    exchange.getIn().setHeader("ActiveMQKahaDB", kahaDb.getAbsolutePath());

    BrokerService broker = new BrokerService();
    try {
      broker.setUseJmx(false);
      PersistenceAdapter persistenceAdapter = new KahaDBPersistenceAdapter();
      persistenceAdapter.setDirectory(kahaDb);
      broker.setPersistenceAdapter(persistenceAdapter);
      log.info(String.format("Starting a broker pointed at [%s]...", kahaDb.getAbsolutePath()));
      broker.start();
  
      ConnectionFactory localConnectionFactory = new ActiveMQConnectionFactory(broker.getVmConnectorURI());
      Connection localConnection = localConnectionFactory.createConnection();
      Session localSession = localConnection.createSession(false, Session.CLIENT_ACKNOWLEDGE);
  
      Connection remoteConnection = null;
      if (remoteConnectionFactoryUsername != null && !remoteConnectionFactoryUsername.trim().isEmpty()) {
        remoteConnection = remoteConnectionFactory.createConnection(remoteConnectionFactoryUsername, remoteConnectionFactoryPassword);
      } else {
        remoteConnection = remoteConnectionFactory.createConnection();
      }
      Session remoteSession = remoteConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
  
      localConnection.start();
      for (Destination destination : broker.getBroker().getDestinations()) {
        log.info(String.format("Checking destination [%s]...", destination.toString()));
        if (destination instanceof Queue && !broker.checkQueueSize(((Queue) destination).getQueueName())) {
          log.info(String.format("Migrating messages for [%s] to [%s]...", destination.toString(), remoteConnectionFactory.getBrokerURL()));
          long migratedMessageCount = 0;
          MessageConsumer localConsumer = localSession.createConsumer(destination);
          MessageProducer remoteProducer = remoteSession.createProducer(destination);
          Message message = null;
          do {
            message = localConsumer.receive(1000L);
            if (message != null) {
              remoteProducer.send(message);
              message.acknowledge();
              ++migratedMessageCount;
            }
          } while (message != null || !broker.checkQueueSize(((Queue) destination).getQueueName()));
          remoteProducer.close();
          localConsumer.close();
          log.info(String.format("Finished migrating [%s] messages for [%s] to [%s].", migratedMessageCount, destination.toString(), remoteConnectionFactory.getBrokerURL()));
        }
      }
  
      remoteSession.close();
      remoteConnection.close();
  
      localSession.close();
      localConnection.close();
      
      exchange.getIn().setHeader("ActiveMQKahaDBDrained", true);
    } catch (Exception e) {
      exchange.getIn().setHeader("ActiveMQKahaDBDrained", false);
      throw e;
    } finally {
      log.info(String.format("Stopping the broker pointed at [%s].", kahaDb.getAbsolutePath()));
      broker.stop();
    }
  }
}
