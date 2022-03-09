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

package alluxio.master.job.common;

import alluxio.grpc.OperationType;
import alluxio.job.wire.JobSource;
import alluxio.master.job.tracker.CmdRunAttempt;

import com.google.common.base.MoreObjects;
import org.apache.commons.compress.utils.Lists;

import java.util.List;

/**
 * A class representation for Command Information.
 */
public class CmdInfo {
  private long mJobControlId;
  private List<CmdRunAttempt> mCmdRunAttempt;
  private OperationType mOperationType;
  private JobSource mJobSource;
  private long mJobSubmissionTime;
  private List<String> mFilePath;

  /**
   * Constructor for CmdInfo class.
   * @param jobControlId
   * @param operationType
   * @param jobSource
   * @param jobSubmissionTime
   * @param filePath
   */
  public CmdInfo(long jobControlId,
      OperationType operationType, JobSource jobSource,
      long jobSubmissionTime, List<String> filePath) {
    mJobControlId = jobControlId;
    mOperationType = operationType;
    mJobSource = jobSource;
    mJobSubmissionTime = jobSubmissionTime;
    mFilePath = filePath;
    mCmdRunAttempt = Lists.newArrayList();
  }

//  /**
//   * Set the CmdRunAttempt.
//   * @param attempts CmdRunAttempt
//   */
//  public void setCmdRunAttempt(List<CmdRunAttempt> attempts) {
//    mCmdRunAttempt = attempts;
//  }

  /**
   * Add the CmdRunAttempt.
   * @param attempt CmdRunAttempt
   */
  public void addCmdRunAttempt(CmdRunAttempt attempt) {
    mCmdRunAttempt.add(attempt);
  }

  /** Get CmdRunAttempt.
   * getCmdRunAttempt
   * @return list of attempt
   */
  public List<CmdRunAttempt> getCmdRunAttempt() {
    return mCmdRunAttempt;
  }

  /** Get job control Id.
   * getJobControlId
   * @return job control id
   */
  public long getJobControlId() {
    return mJobControlId;
  }

  /** Get operation type.
   * getOperationType
   * @return operation type
   */
  public OperationType getOperationType() {
    return mOperationType;
  }

  /** Get job source.
   * getJobSource
   * @return job source
   */
  public JobSource getJobSource() {
    return mJobSource;
  }

  /** Get submission time.
   * getJobSubmissionTime
   * @return timestamp
   */
  public long getJobSubmissionTime() {
    return mJobSubmissionTime;
  }

  /** Get file path.
   * getFilePath
   * @return list of paths
   */
  public List<String> getFilePath() {
    return mFilePath;
  }

  /**
   * tostring.
   * @return
   */
  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
            .add("jobControlId", mJobControlId)
            .add("operationType", mOperationType)
            .add("jobSource", mJobSource)
            .add("submission time", mJobSubmissionTime)
            .add("attempts", mCmdRunAttempt)
            .toString();
  }
}
