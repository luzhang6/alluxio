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

package alluxio.job.cmd.load;

import alluxio.grpc.OperationType;
import alluxio.job.cmd.CliConfig;
import alluxio.job.wire.JobSource;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.concurrent.ThreadSafe;

/**
 * A config for a LoadCli job.
 */
@ThreadSafe
public class LoadCliConfig implements CliConfig {
  public static final String NAME = "LoadCli";
  private static final long serialVersionUID = -5268683490243619278L;

  private final String mFilePath;
  private final int mReplication;
  private final Set<String> mWorkerSet;
  private final Set<String> mExcludedWorkerSet;
  private final Set<String> mLocalityIds;
  private final Set<String> mExcludedLocalityIds;
  private final int mBatchSize;
  private final boolean mDirectCache;

  /**
   * @param filePath the file path
   * @param batchSize the batch size
   * @param replication the number of workers to store each block on, defaults to 1
   * @param workerSet the worker set
   * @param excludedWorkerSet the excluded worker set
   * @param localityIds the locality identify set
   * @param excludedLocalityIds the excluded locality identify set
   * @param directCache direct cache
   **/
  public LoadCliConfig(@JsonProperty("filePath") String filePath,
                   @JsonProperty("batchSize") Integer batchSize,
                   @JsonProperty("replication") Integer replication,
                   @JsonProperty("workerSet") Set<String> workerSet,
                   @JsonProperty("excludedWorkerSet") Set<String> excludedWorkerSet,
                   @JsonProperty("localityIds") Set<String> localityIds,
                   @JsonProperty("excludedLocalityIds") Set<String> excludedLocalityIds,
                   @JsonProperty("directCache") boolean directCache) {
    mFilePath = Preconditions.checkNotNull(filePath, "The file path cannot be null");
    mBatchSize = batchSize;
    mReplication = replication == null ? 1 : replication;
    mWorkerSet = workerSet == null ? Collections.EMPTY_SET : new HashSet(workerSet);
    mExcludedWorkerSet = excludedWorkerSet == null ? Collections.EMPTY_SET
        : new HashSet(excludedWorkerSet);
    mLocalityIds = localityIds == null ? Collections.EMPTY_SET : new HashSet(localityIds);
    mExcludedLocalityIds = excludedLocalityIds == null ? Collections.EMPTY_SET
        : new HashSet(excludedLocalityIds);
    mDirectCache = directCache;
  }

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public JobSource getJobSource() {
    return JobSource.CLI;
  }

  @Override
  public OperationType getOperationType() {
    return OperationType.DIST_LOAD;
  }

  @Override
  public Collection<String> affectedPaths() {
    return ImmutableList.of(mFilePath);
  }

  /**
   * @return file path
   */
  public String getFilePath() {
    return mFilePath;
  }

  /**
   * @return replication
   */
  public int getReplication() {
    return mReplication;
  }

  /**
   * @return worker set
   */
  public Set<String> getWorkerSet() {
    return mWorkerSet;
  }

  /**
   * @return excluded worker set
   */
  public Set<String> getExcludedWorkerSet() {
    return mExcludedWorkerSet;
  }

  /**
   * @return locality identify set
   */
  public Set<String> getLocalityIds() {
    return mLocalityIds;
  }

  /**
   * @return excluded locality identify set
   */
  public Set<String> getExcludedLocalityIds() {
    return mExcludedLocalityIds;
  }

  /**
   * Get batch size.
   * @return batch size
   */
  public int getBatchSize() {
    return mBatchSize;
  }

  /**
   * Use direct cache or not.
   * @return boolean flag
   */
  public boolean getDirectCache() {
    return mDirectCache;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof LoadCliConfig)) {
      return false;
    }
    LoadCliConfig that = (LoadCliConfig) obj;
    return mFilePath.equals(that.mFilePath) && mReplication == that.mReplication;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(mFilePath, mReplication);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
            .add("FilePath", mFilePath)
            .add("Replication", mReplication)
            .toString();
  }
}
