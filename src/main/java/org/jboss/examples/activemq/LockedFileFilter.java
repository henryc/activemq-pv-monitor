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
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

import org.apache.camel.component.file.GenericFile;
import org.apache.camel.component.file.GenericFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LockedFileFilter<T> implements GenericFileFilter<T> {

  private static final Logger log = LoggerFactory.getLogger(LockedFileFilter.class);
  
  @Override
  public boolean accept(GenericFile<T> file) {
    log.debug(String.format("Trying to acquire lock on [%s].", file.getAbsoluteFilePath()));
    boolean matches;
    try {
      FileLock flock = null;
      try (RandomAccessFile f = new RandomAccessFile(new File(file.getAbsoluteFilePath()), "rw");
        FileChannel fin = f.getChannel();) {
        try {
          flock = fin.tryLock();
          matches = (flock != null);
        } finally {
          if (flock != null) {
            flock.release();
          }
        }
      } 
    } catch (Exception e) {
      log.debug(String.format("Caught an exception trying to acquire lock on file [%s].", file.getAbsoluteFilePath()), e);
      matches = false;
    }
    log.debug(String.format("Was %s to acquire lock on file [%s].", (matches) ? "able" : "unable", file.getAbsoluteFilePath()));
    return matches;
  }
}
