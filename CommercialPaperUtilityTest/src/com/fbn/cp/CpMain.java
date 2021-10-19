package com.fbn.cp;

import com.fbn.api.newgen.controller.Controller;

public class CpMain implements Runnable {

    @Override
    public void run() {
        execute();
    }

    private void execute (){

    	//test push
        System.out.println("sessionId: "+sessionId());
        new PrimaryMarket(sessionId()).main();
       // new SecondaryMarket(sessionId).main();
    }

    private void disconnectSession (String sessionId){
        new Controller().disconnectSession(sessionId);
    }
    private String sessionId(){
        return new Controller().getSessionId();
    }
}
