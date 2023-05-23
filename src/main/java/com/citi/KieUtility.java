package com.citi;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.jbpm.services.api.ProcessService;
import org.kie.api.KieServices;
import org.kie.server.api.marshalling.MarshallingFormat;
import org.kie.server.client.KieServicesClient;
import org.kie.server.client.KieServicesConfiguration;
import org.kie.server.client.KieServicesFactory;
import org.kie.server.client.ProcessServicesClient;
import org.kie.server.client.impl.ProcessServicesClientImpl;
// import org.apache.logging.log4j.LogManager;
// import org.apache.logging.log4j.Logger;
import org.kie.server.api.model.KieContainerResource;
import org.kie.server.api.model.instance.ProcessInstance;

public class KieUtility {

    // private static final Logger logger = LogManager.getLogger(KieUtility.class);
    private static final String URL = "http://localhost:8080/kie-server/services/rest/server";
    private static final String USER = "rhpamAdmin";
    private static final String PASSWORD = "@Anushiya22";

    private static final MarshallingFormat FORMAT = MarshallingFormat.JSON;

    public KieServicesClient getKieServicesClient() {
        KieServicesConfiguration conf = KieServicesFactory.newRestConfiguration(URL, USER, PASSWORD);
        conf.setMarshallingFormat(FORMAT);
        return KieServicesFactory.newKieServicesClient(conf);
    }

    public static void main(String[] args) {
        TerminateProcess terminateProcess =  new TerminateProcess();
        try {
            terminateProcess.getScheduleTime();
            terminateProcess.execute(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
