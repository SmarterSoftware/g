/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.giraph.comm.requests;

import org.apache.giraph.comm.ServerData;
import org.apache.giraph.comm.aggregators.AggregatorUtils;
import org.apache.giraph.comm.aggregators.OwnerAggregatorServerData;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Writable;

import java.io.DataInput;
import java.io.IOException;

/**
 * Request to send partial aggregated values for current superstep (values
 * which were computed by one worker's vertices)
 */
public class SendWorkerAggregatorsRequest extends
    ByteArrayWithSenderTaskIdRequest implements WorkerRequest {

  /**
   * Constructor
   *
   * @param data Serialized aggregator data
   * @param senderTaskId Sender task id
   */
  public SendWorkerAggregatorsRequest(byte[] data, int senderTaskId) {
    super(data, senderTaskId);
  }

  /**
   * Constructor used for reflection only
   */
  public SendWorkerAggregatorsRequest() {
  }

  @Override
  public void doRequest(ServerData serverData) {
    DataInput input = getDataInput();
    OwnerAggregatorServerData aggregatorData =
        serverData.getOwnerAggregatorData();
    try {
      int numAggregators = input.readInt();
      for (int i = 0; i < numAggregators; i++) {
        String aggregatorName = input.readUTF();
        if (aggregatorName.equals(
            AggregatorUtils.SPECIAL_COUNT_AGGREGATOR)) {
          LongWritable count = new LongWritable(0);
          count.readFields(input);
          aggregatorData.receivedRequestCountFromWorker(count.get(),
              getSenderTaskId());
        } else {
          Writable aggregatedValue =
              aggregatorData.createAggregatorInitialValue(aggregatorName);
          aggregatedValue.readFields(input);
          aggregatorData.aggregate(aggregatorName, aggregatedValue);
        }
      }
    } catch (IOException e) {
      throw new IllegalStateException("doRequest: " +
          "IOException occurred while processing request", e);
    }
    aggregatorData.receivedRequestFromWorker();
  }


  @Override
  public void doLocalRequest(ServerData serverData) {
    doRequest(serverData);  // YH: dummy wrapper
  }

  @Override
  public RequestType getType() {
    return RequestType.SEND_WORKER_AGGREGATORS_REQUEST;
  }
}
