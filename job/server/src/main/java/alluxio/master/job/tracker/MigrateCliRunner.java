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

package alluxio.master.job.tracker;

import alluxio.AlluxioURI;
import alluxio.client.file.FileSystem;
import alluxio.client.file.FileSystemContext;
import alluxio.client.file.URIStatus;
import alluxio.collections.Pair;
import alluxio.conf.AlluxioConfiguration;
import alluxio.conf.PropertyKey;
import alluxio.exception.AlluxioException;
import alluxio.exception.FileAlreadyExistsException;
import alluxio.exception.InvalidPathException;
import alluxio.grpc.OperationType;
import alluxio.job.JobConfig;
import alluxio.job.plan.BatchedJobConfig;
import alluxio.job.plan.migrate.MigrateConfig;
import alluxio.job.wire.JobInfo;
import alluxio.job.wire.JobSource;
import alluxio.master.job.JobMaster;
import alluxio.master.job.common.CmdInfo;
import alluxio.master.job.metrics.DistributedCmdMetrics;
import alluxio.retry.CountingRetry;
import alluxio.retry.RetryPolicy;
import alluxio.util.io.PathUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * A config runner for a MigrateCli job.
 */
public final class MigrateCliRunner extends AbstractCmdRunner {
  private static final Logger LOG = LoggerFactory.getLogger(MigrateCliRunner.class);

  MigrateCliRunner(FileSystemContext fsContext, JobMaster jobMaster) {
    super(fsContext, jobMaster);
  }

  /**
   * Run a DistCp command.
   * @param srcPath source path
   * @param dstPath destination path
   * @param overwrite overwrite file or not
   * @param batchSize batch size to run at one time
   * @param jobControlId the parent id or jobControlId
   * @return CmdInfo
   */
  public CmdInfo runDistCp(AlluxioURI srcPath, AlluxioURI dstPath,
                           boolean overwrite, int batchSize, long jobControlId) throws IOException {
    long submissionTime = System.currentTimeMillis();
    AlluxioConfiguration conf = mFsContext.getPathConf(dstPath);
    String writeType = conf.get(PropertyKey.USER_FILE_WRITE_TYPE_DEFAULT);
    List<Pair<String, String>> filePool = new ArrayList<>(batchSize);

    //only use the source path as the file parameter.
    List<String> filePath = Lists.newArrayList(srcPath.getPath());

    CmdInfo cmdInfo = new CmdInfo(jobControlId, OperationType.DIST_CP, JobSource.CLI,
            submissionTime, filePath);

    try {
      if (mFileSystem.getStatus(srcPath).isFolder()) {
        createFolders(srcPath, dstPath, mFileSystem);
      }
      copy(srcPath, dstPath, overwrite, batchSize, filePool, writeType, cmdInfo);
    } catch (IOException | AlluxioException e) {
      LOG.warn("failing in distcp!");
      LOG.error(e.getMessage());
      throw new IOException(e.getMessage());
    }

    // add all the jobs left in the pool
    if (filePool.size() > 0) {
      submitDistCp(filePool, overwrite, writeType, cmdInfo);
      filePool.clear();
    }
    return cmdInfo;
  }

  // call this method after finishing distCp job submission.
  // user can only see the child jobs after submisssion is completed.
  private List<CmdRunAttempt> consolidateDistCpJobs(long jobControlId) {
    if (mJobMap.containsKey(jobControlId)) {
      List<CmdRunAttempt> attempts = mJobMap.get(jobControlId);
      return attempts;
    } else {
      return null;
    }
  }

  private void copy(AlluxioURI srcPath, AlluxioURI dstPath, boolean overwrite, int batchSize,
      List<Pair<String, String>> pool, String writeType, CmdInfo cmdInfo)
          throws IOException, AlluxioException {
    for (URIStatus srcInnerStatus : mFileSystem.listStatus(srcPath)) {
      String dstInnerPath =
              computeTargetPath(srcInnerStatus.getPath(), srcPath.getPath(), dstPath.getPath());
      if (srcInnerStatus.isFolder()) {
        copy(new AlluxioURI(srcInnerStatus.getPath()), new AlluxioURI(dstInnerPath), overwrite,
                batchSize, pool, writeType, cmdInfo);
      } else {
        pool.add(new Pair<>(srcInnerStatus.getPath(), dstInnerPath));
        if (pool.size() == batchSize) {
          submitDistCp(pool, overwrite, writeType, cmdInfo);
          pool.clear();
        }
      }
    }
  }

  // Submit a child job within a distributed command job.
  private void submitDistCp(List<Pair<String, String>> pool, boolean overwrite,
         String writeType, CmdInfo cmdInfo) {
    if (mSubmitted.size() >= DEFAULT_ACTIVE_JOBS) {
      LOG.info("waiting for submitted job number to decrease...");
      waitForCmdJob();
    }

    MigrateRunAttempt attempt = new MigrateRunAttempt(new CountingRetry(3), mJobMaster);
    setJobConfigAndFileMetrics(pool, overwrite, writeType, attempt);
    mSubmitted.add(attempt);
    cmdInfo.addCmdRunAttempt(attempt);

    LOG.info("submitDistCp, attempt = " + attempt.getJobConfig().toString());
    attempt.run();
  }

  private class MigrateRunAttempt extends CmdRunAttempt {
    private JobConfig mJobConfig;
    private long mFileCount;
    private long mFileSize;

    MigrateRunAttempt(RetryPolicy retryPolicy, JobMaster jobMaster) {
      super(retryPolicy, jobMaster);
    }

    /**
     * Set job config.
     * @param config
     */
    public void setConfig(JobConfig config) {
      mJobConfig = config;
    }

    /**
     * Set file count.
     * @param fileCount
     */
    public void setFileCount(long fileCount) {
      mFileCount = fileCount;
    }

    /**
     * Set file size.
     * @param fileSize
     */
    public void setFileSize(long fileSize) {
      mFileSize = fileSize;
    }

    @Override
    public JobConfig getJobConfig() {
      return mJobConfig;
    }

    @Override
    public long getFileCount() {
      return mFileCount;
    }

    @Override
    public long getFileSize() {
      return mFileSize;
    }

    @Override
    protected void logFailedAttempt(JobInfo jobInfo) {
    }

    @Override
    protected void logFailed() {
    }

    @Override
    protected void logCompleted() {
    }
  }

  // Create a JobConfig and set file count and size for the Migrate job.
  private void setJobConfigAndFileMetrics(List<Pair<String, String>> filePath,
        boolean overwrite, String writeType, MigrateRunAttempt attempt) {
    int poolSize = filePath.size();
    JobConfig jobConfig;
    long fileCount = 0;
    long fileSize = 0;
    if (poolSize == 1) {
      Pair<String, String> pair = filePath.iterator().next();
      String source = pair.getFirst();
      //LOG.info("pool size = 1, Copying " + source + " to " + pair.getSecond()); //print
      jobConfig = new MigrateConfig(source, pair.getSecond(), writeType, overwrite);
      fileCount = DEFAULT_FILE_COUNT;
      fileSize = DistributedCmdMetrics.getFileSize(source, mFileSystem, new CountingRetry(1));
    } else {
      HashSet<Map<String, String>> configs = Sets.newHashSet();
      ObjectMapper oMapper = new ObjectMapper();
      for (Pair<String, String> pair : filePath) {
        String source = pair.getFirst();
        MigrateConfig config =
                new MigrateConfig(source, pair.getSecond(), writeType, overwrite);
        //LOG.info("pool size = " + poolSize + ", Copying " + source + " to " + pair.getSecond());
        Map<String, String> map = oMapper.convertValue(config, Map.class);
        configs.add(map);
        fileSize += DistributedCmdMetrics.getFileSize(source, mFileSystem, new CountingRetry(1));
      }
      fileCount = poolSize; // file count equals poolSize.
      jobConfig = new BatchedJobConfig(MigrateConfig.NAME, configs);
    }
    attempt.setFileCount(fileCount);
    attempt.setFileSize(fileSize);
    attempt.setConfig(jobConfig);
    LOG.info("file  count = " + fileCount + ", file size = " + fileSize);
  }

  private void createFolders(AlluxioURI srcPath, AlluxioURI dstPath, FileSystem fileSystem)
          throws IOException, AlluxioException {
    try {
      fileSystem.createDirectory(dstPath);
      System.out.println("Created directory at " + dstPath.getPath());
    } catch (FileAlreadyExistsException e) {
      if (!fileSystem.getStatus(dstPath).isFolder()) {
        throw e;
      }
    }

    for (URIStatus srcInnerStatus : fileSystem.listStatus(srcPath)) {
      if (srcInnerStatus.isFolder()) {
        String dstInnerPath =
                computeTargetPath(srcInnerStatus.getPath(), srcPath.getPath(), dstPath.getPath());
        createFolders(new AlluxioURI(srcInnerStatus.getPath()),
                new AlluxioURI(dstInnerPath), fileSystem);
      }
    }
  }

  private String computeTargetPath(String path, String source, String destination)
          throws InvalidPathException {
    String relativePath = PathUtils.subtractPaths(path, source);

    return PathUtils.concatPath(destination, relativePath);
  }
}
