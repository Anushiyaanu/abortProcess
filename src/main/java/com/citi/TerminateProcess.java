package com.citi;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManagerFactory;

import org.jbpm.runtime.manager.impl.jpa.EntityManagerFactoryManager;
import org.kie.api.executor.Command;
import org.kie.api.executor.CommandContext;
import org.kie.api.executor.ExecutionResults;
import org.kie.api.executor.ExecutorService;
import org.kie.api.executor.Reoccurring;
import org.kie.server.api.model.instance.ProcessInstance;
import org.kie.server.client.KieServicesClient;
import org.kie.server.client.ProcessServicesClient;

public class TerminateProcess implements Command, Reoccurring {

    @Override
    public Date getScheduleTime() {
        return null;
    }
    
    @Override
    public ExecutionResults execute(CommandContext ctx) throws Exception {

        KieUtility kieUtility = new KieUtility();
        KieServicesClient kieServicesClient = kieUtility.getKieServicesClient();

        // String emfName = (String) ctx.getData("EmfName");
        
        // EntityManagerFactory emf = EntityManagerFac+


        ProcessServicesClient processServicesClient = kieServicesClient.getServicesClient(ProcessServicesClient.class);
        
        List<Long> activeProcesses = new ArrayList<Long>();
        Integer page_count = 0;
        Integer page_size = 100;

        List<ProcessInstance> process = processServicesClient.findProcessInstances("HumanTask_1.0.0-SNAPSHOT",page_count,page_size);
        if (process.size() > 0){
            for(int i=0; i<process.size(); i++){
                ProcessInstance processObj = process.get(i);
                activeProcesses.add(processObj.getId());
            }
            page_count += 1;
        }
        System.out.println("List of process instance"+activeProcesses);
        processServicesClient.abortProcessInstances("HumanTask_1.0.0-SNAPSHOT", activeProcesses);
        System.out.println("Process Aborted");
        ExecutionResults executionResults = new ExecutionResults();
        executionResults.setData("sucess",activeProcesses);
        return executionResults;
        
    }

}
