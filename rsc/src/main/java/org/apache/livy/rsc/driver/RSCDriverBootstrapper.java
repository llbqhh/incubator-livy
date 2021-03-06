/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.livy.rsc.driver;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Properties;
import static java.nio.charset.StandardCharsets.UTF_8;

import org.apache.spark.SparkConf;

import org.apache.livy.rsc.RSCConf;
import static org.apache.livy.rsc.RSCConf.Entry.*;

/**
 * The entry point for the RSC. Parses command line arguments and instantiates the correct
 * driver based on the configuration.
 *
 * The driver is expected to have a public constructor that takes a two parameters:
 * a SparkConf and a RSCConf.
 */
public final class RSCDriverBootstrapper {

  public static void main(String[] args) throws Exception {
    Properties props;

    switch (args.length) {
    case 0:
      props = System.getProperties();
      break;

    case 1:
      // 测试时，直接从临时文件中读取配置参数
      props = new Properties();
      File propertyFile = new File(args[0]);
      String fileName = propertyFile.getName();
      if (!fileName.startsWith("livyConf") && fileName.endsWith("properties")) {
        throw new IllegalArgumentException("File name " + fileName + "is not a legal file name.");
      }

      Reader r = new InputStreamReader(new FileInputStream(propertyFile), UTF_8);
      try {
        props.load(r);
      } finally {
        r.close();
      }
      break;

    default:
      throw new IllegalArgumentException("Too many arguments.");
    }

    SparkConf conf = new SparkConf(false);
    RSCConf livyConf = new RSCConf(null);

    for (String key : props.stringPropertyNames()) {
      String value = props.getProperty(key);
      if (key.startsWith(RSCConf.LIVY_SPARK_PREFIX)) {
        livyConf.set(key.substring(RSCConf.LIVY_SPARK_PREFIX.length()), value);
        props.remove(key);
      } else if (key.startsWith(RSCConf.SPARK_CONF_PREFIX)) {
        conf.set(key, value);
      }
    }

    String driverClass = livyConf.get(DRIVER_CLASS);
    if (driverClass == null) {
      driverClass = RSCDriver.class.getName();
    }

    RSCDriver driver = (RSCDriver) Thread.currentThread()
      .getContextClassLoader()
      .loadClass(driverClass)
      .getConstructor(SparkConf.class, RSCConf.class)
      .newInstance(conf, livyConf);

    driver.run();
  }

}
