/*
 * Copyright (C) 2017-2018 Dremio Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dremio.exec.store.jdbc.conf;

import static com.google.common.base.Preconditions.checkNotNull;

import org.hibernate.validator.constraints.NotBlank;
import com.dremio.exec.catalog.conf.Secret;
import com.dremio.exec.catalog.conf.DisplayMetadata;
import com.dremio.exec.catalog.conf.NotMetadataImpacting;
import com.dremio.exec.catalog.conf.SourceType;
import com.dremio.exec.server.SabotContext;
import com.dremio.exec.store.jdbc.CloseableDataSource;
import com.dremio.exec.store.jdbc.DataSources;
import com.dremio.exec.store.jdbc.JdbcStoragePlugin;
import com.dremio.exec.store.jdbc.JdbcStoragePlugin.Config;
import com.dremio.exec.store.jdbc.dialect.arp.ArpDialect;
import com.google.common.annotations.VisibleForTesting;

import io.protostuff.Tag;

/**
 * Configuration for SQLite sources.
 */
@SourceType(value = "AWSATHENA", label = "AWS Athena")
public class AwsAthenaConf extends AbstractArpConf<AwsAthenaConf> {
  private static final String ARP_FILENAME = "arp/implementation/awsathena-arp.yaml";
  private static final ArpDialect ARP_DIALECT =
      AbstractArpConf.loadArpFile(ARP_FILENAME, (ArpDialect::new));
 // private static final String DRIVER = "com.simba.athena.jdbc42.Driver";

  @Tag(1)
  @DisplayMetadata(label="JDBC URL  - for custom JDBC Driver - Override other settings")
  public String jdbcurl = "";
 
  @NotBlank 
  @Tag(2)
  @DisplayMetadata(label = "ARP YAML File path - Push down defintions")
  public String yamlPath = "arp/implementation/awsathena-arp.yaml";

  @Tag(7)
  @DisplayMetadata(label = "User Id / Name")
  public String username = "";

  @Tag(8)
  @DisplayMetadata(label = "Password")
  @Secret
  public String password = "";

  @NotBlank
  @Tag(3)
  @DisplayMetadata(label = "JDBC Driver Class - Default is AWS Athena")
  public String DRIVER = "com.simba.athena.jdbc42.Driver";

  @Tag(4)
  @DisplayMetadata(label = "AWSRegion - Used for AWS Services")
  public String awsregion = "ap-southeast-2";

  @Tag(5)
  @DisplayMetadata(label = "AWS Credential Provider - Default Athena Default Credential Provider")
  public String awscredentialProvider = "com.simba.athena.amazonaws.auth.DefaultAWSCredentialsProviderChain"; 

  @Tag(6)
  @DisplayMetadata(label = "S3 Ouputput Location - Required for Athena - Full S3 Path s3://...")
  public String s3OutputLocation = "s3://temp/athena-output/";
  
  
  @VisibleForTesting
  public String toJdbcConnectionString() {
	if ("".equals(awscredentialProvider)) {
		awscredentialProvider = "com.simba.athena.amazonaws.auth.DefaultAWSCredentialsProviderChain";
	}
	if ("".equals(s3OutputLocation)) {
		s3OutputLocation = "s3://temp/athena-output";
	}
    if (!("".equals(jdbcurl))) {
       return jdbcurl;
    }

    return String.format("jdbc:awsathena://AwsRegion=%s;S3OutputLocation=%s",
	  awsregion,awscredentialProvider,s3OutputLocation);
  }

  @Override
  @VisibleForTesting
  public Config toPluginConfig(SabotContext context) {
    return JdbcStoragePlugin.Config.newBuilder()
        .withDialect(getDialect())
  
        .withDatasourceFactory(this::newDataSource)
        .clearHiddenSchemas()
       
        .build();
  }

  private CloseableDataSource newDataSource() {
    return DataSources.newGenericConnectionPoolDataSource(DRIVER,
      toJdbcConnectionString(), ("".equals(username))?null:password, ("".equals(password))?null:password, null, DataSources.CommitMode.DRIVER_SPECIFIED_COMMIT_MODE);
  }

  @Override
  public ArpDialect getDialect() {
    return ARP_DIALECT;
  }

  @VisibleForTesting
  public static ArpDialect getDialectSingleton() {
    return ARP_DIALECT;
  }
}
