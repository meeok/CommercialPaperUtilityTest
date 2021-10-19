package com.fbn.start;

import com.fbn.api.newgen.controller.Controller;
import com.fbn.cp.CpMain;
import com.fbn.utils.Shared;
import com.fbn.utils.ConstantsI;
import com.fbn.utils.LoadProp;


public class Main implements ConstantsI {


    public void run() throws Exception {
//       executeUtility();
       executeUtility2();
    }
    private void disconnectSession (String sessionId){
        new Controller().disconnectSession(sessionId);
    }
    private void executeUtility () throws Exception {
        try {
            while (true) {
                String sessionId = new Controller().getSessionId();
                CpMain cpMain = new CpMain();
                Thread cp = new Thread(cpMain);
                cp.start();
                cp.join();

                disconnectSession(sessionId);

                Shared.logger.info("Current thread name-- "+ Thread.currentThread().getName());
                Thread.sleep(Long.parseLong(LoadProp.sleepTime));
            }
        }
        catch (Exception e){
            Shared.logger.info("Exception occurred in Main class-- "+e.getMessage());
            throw new Exception("Exception occurred in Main Class-- "+ e.getMessage());
        }
    }
    private void executeUtility2(){
        try{
            //String sessionId = new Controller().getSessionId();

            CpMain cpMain = new CpMain();
            Thread cp = new Thread(cpMain);
            cp.start();
            //cp.join();
           // disconnectSession(sessionId);
        }
        catch(Exception e){
            Shared.logger.info("Exception occurred in Main class-- "+e.getMessage());
            System.out.println("Exception occurred in Main Class-- "+ e.getMessage());
        }
    }
}
