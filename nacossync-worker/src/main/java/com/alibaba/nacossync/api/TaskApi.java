/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the NOTICE
 * file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file
 * to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.alibaba.nacossync.api;

import com.alibaba.nacossync.pojo.request.*;
import com.alibaba.nacossync.pojo.result.*;
import com.alibaba.nacossync.template.SkyWalkerTemplate;
import com.alibaba.nacossync.template.processor.*;
import com.alibaba.nacossync.timer.AutoGenerateBatchSyncTaskTimer;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * @author NacosSync
 * @version $Id: Task.java, v 0.1 2018-09-24 PM3:43 NacosSync Exp $$
 */
@Slf4j
@RestController
@Api
public class TaskApi {

    private final TaskUpdateProcessor taskUpdateProcessor;

    private final TaskAddProcessor taskAddProcessor;

    private final TaskDeleteProcessor taskDeleteProcessor;

    private final TaskDeleteInBatchProcessor taskDeleteInBatchProcessor;

    private final TaskListQueryProcessor taskListQueryProcessor;

    private final TaskDetailProcessor taskDetailProcessor;

    private final TaskBatchAddProcessor taskBatchAddProcessor;

    public TaskApi(TaskUpdateProcessor taskUpdateProcessor, TaskAddProcessor taskAddProcessor,
        TaskDeleteProcessor taskDeleteProcessor, TaskDeleteInBatchProcessor taskDeleteInBatchProcessor,
        TaskListQueryProcessor taskListQueryProcessor, TaskDetailProcessor taskDetailProcessor,
                   TaskBatchAddProcessor taskBatchAddProcessor) {
        this.taskUpdateProcessor = taskUpdateProcessor;
        this.taskAddProcessor = taskAddProcessor;
        this.taskDeleteProcessor = taskDeleteProcessor;
        this.taskDeleteInBatchProcessor = taskDeleteInBatchProcessor;
        this.taskListQueryProcessor = taskListQueryProcessor;
        this.taskDetailProcessor = taskDetailProcessor;
        this.taskBatchAddProcessor = taskBatchAddProcessor;
    }

    @RequestMapping(path = "/v1/task/list", method = RequestMethod.GET)
    public TaskListQueryResult tasks(TaskListQueryRequest taskListQueryRequest) {

        return SkyWalkerTemplate.run(taskListQueryProcessor, taskListQueryRequest, new TaskListQueryResult());
    }

    @RequestMapping(path = "/v1/task/detail", method = RequestMethod.GET)
    public TaskDetailQueryResult getByTaskId(TaskDetailQueryRequest taskDetailQueryRequest) {

        return SkyWalkerTemplate.run(taskDetailProcessor, taskDetailQueryRequest, new TaskDetailQueryResult());
    }

    @RequestMapping(path = "/v1/task/delete", method = RequestMethod.DELETE)
    public BaseResult deleteTask(TaskDeleteRequest taskDeleteRequest) {

        return SkyWalkerTemplate.run(taskDeleteProcessor, taskDeleteRequest, new BaseResult());
    }

    /**
     * @author yongchao9
     * @param taskBatchDeleteRequest
     * @return
     */
    @RequestMapping(path = "/v1/task/deleteInBatch", method = RequestMethod.DELETE)
    public BaseResult batchDeleteTask(TaskDeleteInBatchRequest taskBatchDeleteRequest) {
        return SkyWalkerTemplate.run(taskDeleteInBatchProcessor, taskBatchDeleteRequest, new BaseResult());
    }

    @RequestMapping(path = "/v1/task/add", method = RequestMethod.POST)
    public BaseResult taskAdd(@RequestBody TaskAddRequest addTaskRequest) {

        return SkyWalkerTemplate.run(taskAddProcessor, addTaskRequest, new TaskAddResult());
    }

    @RequestMapping(path = "/v1/task/update", method = RequestMethod.POST)
    public BaseResult updateTask(@RequestBody TaskUpdateRequest taskUpdateRequest) {

        return SkyWalkerTemplate.run(taskUpdateProcessor, taskUpdateRequest, new BaseResult());
    }

    @RequestMapping(path = "/v1/task/batchAdd", method = RequestMethod.POST)
    public BaseResult taskBatchAdd(@RequestBody TaskBatchAddRequest batchAddTaskRequest) {

        return SkyWalkerTemplate.run(taskBatchAddProcessor, batchAddTaskRequest, new TaskBatchAddResult());
    }

    @RequestMapping(path = "/v1/task/stopAenerateBatchTask", method = RequestMethod.GET)
    public BaseResult getByTaskId(@RequestParam("enable") boolean enabled) {
        AutoGenerateBatchSyncTaskTimer.autoGenerateTask = enabled;
        return new BaseResult();
    }
}
