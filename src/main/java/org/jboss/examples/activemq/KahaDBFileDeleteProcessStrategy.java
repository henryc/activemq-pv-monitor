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

import org.apache.camel.Exchange;
import org.apache.camel.component.file.GenericFile;
import org.apache.camel.component.file.GenericFileEndpoint;
import org.apache.camel.component.file.GenericFileOperations;
import org.apache.camel.component.file.strategy.GenericFileDeleteProcessStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.FileSystemUtils;

public class KahaDBFileDeleteProcessStrategy<T> extends GenericFileDeleteProcessStrategy<T> {

  private static final Logger log = LoggerFactory.getLogger(KahaDBFileDeleteProcessStrategy.class);
  
  @Override
  public void commit(GenericFileOperations<T> operations, GenericFileEndpoint<T> endpoint, Exchange exchange, GenericFile<T> file) throws Exception {
    String kahaDb = exchange.getIn().getHeader("ActiveMQKahaDB", String.class);
    boolean drained = exchange.getIn().getHeader("ActiveMQKahaDBDrained", false, boolean.class);
    if (drained && kahaDb != null && !kahaDb.trim().equals("")) {
      log.info(String.format("Removing KahaDB [%s].", kahaDb));
      FileSystemUtils.deleteRecursively(new File(kahaDb));
      super.commit(operations, endpoint, exchange, file);
    }
  }
}
