/*
 * The Alluxio Open Foundation licenses this work under the Apache License, version 2.0
 * (the "License"). You may not use this work except in compliance with the License, which is
 * available at www.apache.org/licenses/LICENSE-2.0
 *
 * This software is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied, as more fully set forth in the License.
 *
 * See the NOTICE file distributed with this work for information regarding copyright ownership.
 */

package alluxio.client.cli.fs.command;

import alluxio.cli.fs.FileSystemShell;
import alluxio.client.cli.fs.AbstractFileSystemShellTest;
import alluxio.client.file.FileSystem;
import alluxio.client.file.FileSystemTestUtils;
import alluxio.conf.PropertyKey;
import alluxio.conf.ServerConfiguration;
import alluxio.exception.AlluxioException;
import alluxio.grpc.WritePType;
import alluxio.master.LocalAlluxioJobCluster;
import alluxio.testutils.LocalAlluxioClusterResource;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;

/**
 * Tests for getting cmd status {@link alluxio.cli.fs.command.GetCmdStatusCommand}.
 */
public class GetCmdStatusCommandTest extends AbstractFileSystemShellTest  {
  @Rule
  public TemporaryFolder mTempFolder = new TemporaryFolder();

  @ClassRule
  public static LocalAlluxioClusterResource sResource =
          new LocalAlluxioClusterResource.Builder()
                  .setNumWorkers(4)
                  .setProperty(PropertyKey.MASTER_PERSISTENCE_CHECKER_INTERVAL_MS, "10ms")
                  .setProperty(PropertyKey.MASTER_PERSISTENCE_SCHEDULER_INTERVAL_MS, "10ms")
                  .setProperty(PropertyKey.JOB_MASTER_WORKER_HEARTBEAT_INTERVAL, "200ms")
                  .setProperty(PropertyKey.WORKER_RAMDISK_SIZE, SIZE_BYTES)
                  .setProperty(PropertyKey.USER_BLOCK_SIZE_BYTES_DEFAULT, SIZE_BYTES)
                  .setProperty(PropertyKey.MASTER_TTL_CHECKER_INTERVAL_MS, Long.MAX_VALUE)
                  .setProperty(PropertyKey.USER_FILE_RESERVED_BYTES, SIZE_BYTES / 2)
                  .setProperty(PropertyKey.CONF_DYNAMIC_UPDATE_ENABLED, true)
                  .build();

  @BeforeClass
  public static void beforeClass() throws Exception {
    sLocalAlluxioCluster = sResource.get();
    sLocalAlluxioJobCluster = new LocalAlluxioJobCluster();
    sLocalAlluxioJobCluster.start();
    sFileSystem = sLocalAlluxioCluster.getClient();
    sJobMaster = sLocalAlluxioJobCluster.getMaster().getJobMaster();
    sJobShell = new alluxio.cli.job.JobShell(ServerConfiguration.global());
    sFsShell = new FileSystemShell(ServerConfiguration.global());
  }

  @Test
  public void testGetCmdStatusForLoad() throws IOException, AlluxioException {
    FileSystem fs = sResource.get().getClient();
    FileSystemTestUtils.createByteFile(fs, "/testRoot/testFileA", WritePType.THROUGH,
            10);
    FileSystemTestUtils
            .createByteFile(fs, "/testRoot/testFileB", WritePType.MUST_CACHE, 10);
    sFsShell.run("distributedLoad", "/testRoot");

    String[] output = mOutput.toString().split("\n");
    String jobControlId = output[3].split("=\\s+")[1];
    sFsShell.run("getCmdStatus", jobControlId);
    Assert.assertTrue(mOutput.toString().contains("Get command status information below: "));
    Assert.assertTrue(mOutput.toString().contains("Status = COMPLETED"));
  }

  @Test
  public void testGetCmdStatusForBatchLoad() throws IOException {
    int batch = 2;
    FileSystem fs = sResource.get().getClient();
    FileSystemTestUtils.createByteFile(fs, "/testBatchRoot/testBatchFileA", WritePType.THROUGH, 10);
    FileSystemTestUtils.createByteFile(fs, "/testBatchRoot/testBatchFileB", WritePType.THROUGH, 10);
    FileSystemTestUtils.createByteFile(fs, "/testBatchRoot/testBatchFileC", WritePType.THROUGH, 10);
    sFsShell.run("distributedLoad", "/testBatchRoot", "--batch-size", String.valueOf(batch));

    String[] output = mOutput.toString().split("\n");
    String jobControlId = output[2].split("=\\s+")[1];
    sFsShell.run("getCmdStatus", jobControlId);
    output = mOutput.toString().split("\n");
    Assert.assertTrue(output[output.length - batch].contains("Status = COMPLETED"));
    Assert.assertTrue(output[output.length - batch + 1].contains("Status = COMPLETED"));
  }
}
