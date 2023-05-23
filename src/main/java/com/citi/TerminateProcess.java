package com.citi;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManagerFactory;

import org.jbpm.process.audit.ProcessInstanceLog;
import org.jbpm.process.core.timer.DateTimeUtils;
import org.jbpm.runtime.manager.impl.jpa.EntityManagerFactoryManager;
import org.jbpm.shared.services.impl.TransactionalCommandService;
import org.jbpm.shared.services.impl.commands.QueryStringCommand;
import org.kie.api.executor.Command;
import org.kie.api.executor.CommandContext;
import org.kie.api.executor.ExecutionResults;
import org.kie.api.executor.Reoccurring;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.manager.RuntimeManager;
import org.kie.internal.runtime.manager.RuntimeManagerRegistry;
import org.kie.internal.runtime.manager.context.ProcessInstanceIdContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TerminateProcess implements Command, Reoccurring {

    private static final Logger logger = LoggerFactory.getLogger(TerminateProcess.class);

    private long nextScheduleTimeAdd = 1 * 60 * 60 * 1000;

    @Override
    public Date getScheduleTime() {
        if (nextScheduleTimeAdd < 0) {
            return null;
        }

        long current = System.currentTimeMillis();

        Date nextSchedule = new Date(current + nextScheduleTimeAdd);
        logger.info("Next schedule for job {} is set to {}", this.getClass().getSimpleName(), nextSchedule);

        return nextSchedule;
    }

    @Override
    public ExecutionResults execute(CommandContext ctx) throws Exception {
        ExecutionResults executionResults = new ExecutionResults();

        String emfName = (String) ctx.getData("EmfName");
        if (emfName == null) {
            emfName = "org.jbpm.domain";
        }

        String singleRun = (String) ctx.getData("SingleRun");
        if ("true".equalsIgnoreCase(singleRun)) {
            // disable rescheduling
            this.nextScheduleTimeAdd = -1;
        }

        String nextRun = (String) ctx.getData("NextRun");
        if (nextRun != null) {
            nextScheduleTimeAdd = DateTimeUtils.parseDateAsDuration(nextRun);
        }

        // get hold of persistence and create instance of audit service
        EntityManagerFactory emf = EntityManagerFactoryManager.get().getOrCreate(emfName);

        // collect parameters
        String forDeployment = (String) ctx.getData("ForDeployment");

        Map<String, Object> parameters = new HashMap<>();
        // parameters.put("now", new Date());
        StringBuilder lookupQuery = new StringBuilder();

        lookupQuery.append("select log from ProcessInstanceLog log where log.status=1");

        if (forDeployment != null && !forDeployment.isEmpty()) {
            lookupQuery.append(" and log.externalId = :forDeployment");
            parameters.put("forDeployment", forDeployment);
        } else {
            logger.info("Please specify ContainerId to remove a specific container process instances");
        }

        TransactionalCommandService commandService = new TransactionalCommandService(emf);
        List<ProcessInstanceLog> processInstancesViolations = commandService
                .execute(new QueryStringCommand<List<ProcessInstanceLog>>(lookupQuery.toString(), parameters));
        logger.info("Number of process instances with violated SLA {}", processInstancesViolations.size());
        logger.info("Data of process instances with violated SLA {}", processInstancesViolations.toString());

        logger.info("----------------Process utility ended ----------------");

        if (!processInstancesViolations.isEmpty()) {
            logger.debug("Signaling process instances that have SLA violations");
            int processSignals = 0;
            for (ProcessInstanceLog piLog : processInstancesViolations) {
                RuntimeManager runtimeManager = RuntimeManagerRegistry.get().getManager(piLog.getExternalId());
                if (runtimeManager == null) {
                    logger.info("No runtime manager found for {}, not able to send SLA violation signal",
                            piLog.getExternalId());
                    continue;
                }

                RuntimeEngine engine = runtimeManager
                        .getRuntimeEngine(ProcessInstanceIdContext.get(piLog.getProcessInstanceId()));

                try {

                    engine.getKieSession().abortProcessInstance(piLog.getProcessInstanceId());
                    processSignals++;
                } catch (Exception e) {
                    logger.warn("Unexpected error when signalig process instance {} about SLA violation {}",
                            piLog.getProcessInstanceId(), e.getMessage(), e);
                } finally {
                    runtimeManager.disposeRuntimeEngine(engine);
                }
            }
            logger.info("SLA Violations JOB :: Number of process instances successfully Aborted is {}", processSignals);
            executionResults.setData("ProcessSLA_Aborts", processSignals);
        }
        return executionResults;

    }

}
