package com.fbn.cp;


import com.fbn.api.newgen.customservice.CompleteWorkItem;
import com.fbn.api.newgen.controller.Controller;
import com.fbn.api.newgen.customservice.CreateWorkItem;
import com.fbn.utils.Commons;
import com.fbn.utils.ConstantsI;
import com.fbn.utils.LoadProp;
import com.fbn.utils.Query;
import com.fbn.utils.MailSetup;
import java.util.Map;


public class PrimaryMarket extends Commons implements ConstantsI {

    private String postResp;

    public PrimaryMarket(String sessionId) {
        this.sessionId = sessionId;
    }

    private final String sessionId;


    public void main() {
    	closeCpMarketWindow();
//        processFailedBids();
//        processPostingFailureFailedBids();
//        processSuccessfulBids();
//        processPostingFailureSuccessBids();
//        processBidsOnAwaitingMaturity();
//        processMaturePmBids();
//        processPostFailedMaturePmBids();
    }
    

    private void closeCpMarketWindow(){
        resultSet = new Controller().getRecords(Query.getCpOpenWindowQuery(primaryMarket));

       if (!resultSet.isEmpty()) {
           for (Map<String, String> result : resultSet) {
               String closedDate = result.get("CLOSEDATE");
               String wiName = result.get("WINAME");
               String id = result.get("REFID");
               String value = "'" + flag + "'";
               String condition = "refid = '" + id + "'";
               String conditionBidTbl = "winrefid = '" + id + "'";

               if (Commons.checkClosedDate(closedDate)) {
                   new Controller().updateRecords(sessionId, Query.setupTblName, Query.stColCloseFlag, value, condition);
                   processPrimaryBids(id,wiName);
                   new Controller().updateRecords(sessionId, Query.bidTblName, Query.stColCloseFlag, value, conditionBidTbl);
                   break;
               }
           }
       }
    }

    private void  processPrimaryBids(String id,String wiName){
        resultSet = new Controller().getRecords(Query.getCpPmBidsToProcessQuery(id));

        if (!resultSet.isEmpty()) {
            String attribute = "<CP_UTILITYFLAG>Y</CP_UTILITYFLAG><G_SELECT_MARKET>cp_market</G_SELECT_MARKET><CP_SELECT_MARKET>" + primaryMarket + "</CP_SELECT_MARKET>";
           // String wiName = new CreateWorkItem(sessionId, attribute, initiateFlagNo).getCreatedWorkItem();
            String mailSubject = "MONEY MARKET NOTIFICATION - COMMERCIAL PAPER ";
            String mailMessage = "Commercial Paper Primary Bids with RefNumber: '" + wiName + "' are ready for processing.<br>Kindly login iBPS treat.";


            String columns = "utilitywiname,groupindex,groupindexflag,processflag";
            for (Map<String, String> result : resultSet) {
                String cusId = result.get("CUSTREFID");
                String tenor = result.get("TENOR");
                String rate = result.get("RATE");
                String rateType = result.get("RATETYPE");
                String groupIndex = getCpGroupIndex(wiName, tenor, rateType, rate);
                String values = "'" + wiName + "','" + groupIndex + "','Y','Y'";
                String condition = "CUSTREFID = '" + cusId + "'";

                new Controller().updateRecords(sessionId, Query.bidTblName, columns, values, condition);
            }
           // new CompleteWorkItem(sessionId, wiName);
            String condition ="WINAME = '"+wiName+"'";
            columns = "CP_UTILITYFLAG";
            String values = "'Y'";
            new Controller().updateRecords(sessionId, Query.extTblName, columns, values, condition);
            new MailSetup(sessionId, wiName, fbnMailer, Commons.getUsersMailsInGroup("TUSERS"), empty, mailSubject, mailMessage);
        }
    }
    private String getCpGroupIndex(String wiName,String tenor,String rateType, String rate){
        String strPattern = "^0+(?!$)";
        String groupLabel = "CPPMG";
        String pRateLabel = "P";
        String bRateLabel = "B";
        String [] wiNameArray = wiName.split("-");
        String wiNameTrim = wiNameArray[1];
        wiNameTrim = wiNameTrim.replaceAll(strPattern,"");

        return groupLabel + wiNameTrim + tenor + (isPRate(rateType) ? pRateLabel : bRateLabel) + (isPRate(rateType) ? rate : "");
    }
    private boolean isPRate(String rateType){
        String pRateType = "Personal";
        return rateType.equalsIgnoreCase(pRateType);
    }
    
    private void processFailedBids() {
        resultSet = new Controller().getRecords(Query.getCpAllocatedPrimaryBids("Y"));
        if (!resultSet.isEmpty()) {
            String columnsS = "POSTINTEGRATIONFLAG,REVERSALFLAG";
            String columnsF = "POSTINTEGRATIONFLAG,FAILEDPOSTFLAG";
            String wiName;
            String attribute = "FAILEDBID";
            String mailSubject = "MONEY MARKET NOTIFICATION - COMMERCIAL PAPER ";

            for (Map<String, String> result : resultSet) {
                String id = result.get(bidCustIdCol.toUpperCase());
                wiName = result.get(bidWinameCol.toUpperCase());
                String cusSol = result.get(bidCustSolCol.toUpperCase());
                String cusPrincipal = result.get(bidCustPrincipalCol.toUpperCase());
                String branchSol = result.get(bidBranchSolCol.toUpperCase());
                String cusAcctNo = result.get(bidCustAcctNoCol.toUpperCase());
                String cusEmail = result.get(bidCustEmail.toUpperCase());
                String bidWiname = result.get(bidWinameCol.toUpperCase());
                String tranPart = "CP/" + id.toUpperCase() + "/FAILEDBID";
                String values = "'Y', 'Y'";
                String condition = "CUSTREFID = '" + id + "'";
                postResp = IntegrationCall.postTransaction(LoadProp.headOfficeCpAcctNo, LoadProp.headOfficeCpSol, cusPrincipal, tranPart, wiName, cusAcctNo, cusSol);
                if (postingIsSuccessful(postResp)) {
                    new Controller().updateRecords(sessionId, Query.bidTblName, columnsS, values, condition);
                    new CompleteWorkItem(sessionId, wiName, attribute, flag);
                    String mailMessageS = "A successful reversal request for failed Primary Market Commercial Paper with number '" + wiName + "' now being reversed successfully.";
                    new MailSetup(sessionId, bidWiname, fbnMailer, Commons.getUsersMailsInGroup("TUSERS"), empty, mailSubject, mailMessageS);
                } else if (postingNotSuccessful(postResp)) {
                    new Controller().updateRecords(sessionId, Query.bidTblName, columnsF, values, condition);
                    String mailMessageF = "A reversal request for failed Primary Market Commercial Paper with number '" + wiName + "' has failed posting.<br>Please log into the iBPS workflow to execute action ";
                    new MailSetup(sessionId, bidWiname, fbnMailer, Commons.getUsersMailsInGroup("TUSERS"), empty, mailSubject, mailMessageF);
                }
            }
        }
    }
    
    
    private void processPostingFailureFailedBids() {
        resultSet = new Controller().getRecords(Query.getCpProcessPostingFailureFailedBids(flag));
        if (!resultSet.isEmpty()) {
            String mailSubject = "MONEY MARKET NOTIFICATION - COMMERCIAL PAPER ";
            String attribute = "<CP_UTILITYFLAG>F</CP_UTILITYFLAG><G_SELECT_MARKET>cp_market</G_SELECT_MARKET><CP_SELECT_MARKET>" + primaryMarket + "</CP_SELECT_MARKET>";
            String wiName = new CreateWorkItem(sessionId, attribute, initiateFlagNo).getCreatedWorkItem();
            String column = "FAILEDTRANUTILITYWINAME,FAILEDPOSTFLAG";
            String value = "'" + wiName + "','T'";


            for (Map<String, String> result : resultSet) {
                String id = result.get(bidCustIdCol.toUpperCase());
                String condition = "CUSTREFID = '" + id + "'";
                new Controller().updateRecords(sessionId, Query.bidTblName, column, value, condition);
            }

            new CompleteWorkItem(sessionId, wiName);
            String mailMessage = "Kindly login to post transactions Utility failed to post for failed Primary Market Commercial Paper with Workitem number '" + wiName + "'";
            new MailSetup(sessionId, wiName, fbnMailer, Commons.getUsersMailsInGroup("TUSERS"), empty, mailSubject, mailMessage);
        }
    }
    
    private void processSuccessfulBids() {
        resultSet = new Controller().getRecords(Query.getCpAllocatedPrimaryBids("N"));
        if (!resultSet.isEmpty()) {
            String columnS = "POSTINTEGRATIONFLAG,AWAITINGMATURITYFLAG";
            String columnsF = "POSTINTEGRATIONFLAG,FAILEDPOSTFLAG";
            String wiName = "";
            String attribute = "SUCCESSBID";

            for (Map<String, String> result : resultSet) {
                String id = result.get(bidCustIdCol.toUpperCase());
                wiName = result.get(bidWinameCol.toUpperCase());
                String cusSol = result.get(bidCustSolCol.toUpperCase());
                String cusPrincipal = result.get(bidCustPrincipalCol.toUpperCase());
                String branchSol = result.get(bidBranchSolCol.toUpperCase());
                String allocationPercentage = result.get(bidAllocationPercentageCol.toUpperCase());
                String cusAcctNo = result.get(bidCustAcctNoCol.toUpperCase());
                String condition = "CUSTREFID = '" + id + "'";
                String tranPart1 = "CP/" + id.toUpperCase() + "/REVERSAL";
                String tranPart2 = "CP/" + id.toUpperCase() + "/SUCCESSBID";


                postResp = IntegrationCall.postSuccessBids(LoadProp.headOfficeCpAcctNo, LoadProp.headOfficeCpSol, cusPrincipal, tranPart1, tranPart2, wiName, cusAcctNo, cusSol, allocationPercentage);
                if (postingIsSuccessful(postResp)) {
                    String value = "'Y','Y'";
                    new Controller().updateRecords(sessionId, Query.bidTblName, columnS, value, condition);
                    new CompleteWorkItem(sessionId, wiName, attribute, flag);
                } else if (postResp.equalsIgnoreCase(apiFailed)) {
                    String value = "'Y','C'";
                    new Controller().updateRecords(sessionId, Query.bidTblName, columnsF, value, condition);
                } else if (postResp.equalsIgnoreCase(apiFailure)) {
                    String value = "'Y','D'";
                    new Controller().updateRecords(sessionId, Query.bidTblName, columnsF, value, condition);
                }
            }
        }
    }
     

    private void processPostingFailureSuccessBids() {
        resultSet = new Controller().getRecords(Query.getCpProcessPostingFailureSuccessBids("N"));
        if (!resultSet.isEmpty()) {
            String mailSubject = "MONEY MARKET NOTIFICATION - COMMERCIAL PAPER";
            String attribute = "<CP_UTILITYFLAG>S</CP_UTILITYFLAG><G_SELECT_MARKET>cp_market</G_SELECT_MARKET><CP_SELECT_MARKET>" + primaryMarket + "</CP_SELECT_MARKET>";
            String wiName = new CreateWorkItem(sessionId, attribute, initiateFlagNo).getCreatedWorkItem();
            String column = "FAILEDTRANUTILITYWINAME,FAILEDPOSTFLAG";
            String value = "'" + wiName + "','T'";


            for (Map<String, String> result : resultSet) {
                String id = result.get(bidCustIdCol.toUpperCase());
                String condition = "CUSTREFID = '" + id + "'";
                new Controller().updateRecords(sessionId, Query.bidTblName, column, value, condition);
            }
            new CompleteWorkItem(sessionId, wiName);
            //send mail to TUSer
            String mailMessage = "Kindly login to post transactions Utility failed to post for Success bids Primary Market Commercial Paper with Workitem number '" + wiName + "'";
            new MailSetup(sessionId, wiName, fbnMailer, Commons.getUsersMailsInGroup("TUSERS"), empty, mailSubject, mailMessage);
        }
    }
    

    
    private void processBidsOnAwaitingMaturity() {
        resultSet = new Controller().getRecords(Query.getCpProcessBidsOnAwaitingMaturity(primaryMarket));
        if (!resultSet.isEmpty()) {
            String mailMessageB = "Kindly be informed that your Commercial Paper request initiated from your branch on lien has matured, liaise with the customer to perfect your obligations to enable you access your funds.<br>Thanks for choosing First bank.";
            String mailMessageC = "Kindly be informed that your Commercial Paper on Lien is 7 days to maturity, liaise with your branch to perfect your obligations to enable you access your funds.";
            String attribute = "MATURED";
            String mailSubject = "MONEY MARKET NOTIFICATION - COMMERCIAL PAPER ";


            for (Map<String, String> result : resultSet) {
                String id = result.get(bidCustIdCol);
                String bidWiName = result.get(bidWinameCol.toUpperCase());
                String cusEmail = result.get(bidCustEmail.toUpperCase());
                String branchSol = result.get(bidBranchSolCol.toUpperCase());
                String matureDate = result.get(bidmaturityDate);
                String lienFlag = result.get(bidlienflag);
                if (Commons.isDaysToMaturity(matureDate, 7) && lienFlag.equalsIgnoreCase(flag)) {
                    //send mail to Money_Market_Branch Initiator and Money_Market_Branch_Verifier
                    new MailSetup(sessionId, bidWiName, fbnMailer, Commons.getUsersMailsInGroup("TUSERS_'" + branchSol + "'"), Commons.getUsersMailsInGroup("TUSERS_'" + branchSol + "'"), mailSubject, mailMessageB);
                    //send mail to Customer
                    new MailSetup(sessionId, bidWiName, fbnMailer, cusEmail, empty, mailSubject, mailMessageC);
                } else {
                    if (Commons.isMatured(matureDate) && lienFlag.equalsIgnoreCase("N")) {
                        String column = "STATUS,MATUREDFLAG";
                        String value = "'Matured', 'Y'";
                        String condition = "CUSTREFID = '" + id + "'";
                        new Controller().updateRecords(sessionId, Query.bidTblName, column, value, condition);
                        new CompleteWorkItem(sessionId, bidWiName, attribute, flag);
                    }
                }
            }
        }
    }

    private void processMaturePmBids(){
        resultSet = new Controller().getRecords(Query.getCpMaturedBids(primaryMarket));
        if (!resultSet.isEmpty()) {
            String mailSubject = "MONEY MARKET NOTIFICATION - COMMERCIAL PAPER ";
            String columnsS = "POSTINTEGRATIONMATUREFLAG,PAIDFLAG";
            String columnsF = "POSTINTEGRATIONMATUREFLAG,FAILEDPOSTFLAG";
            String attribute = "MATURED";

            for (Map<String, String> result : resultSet) {
                String id = result.get(bidCustIdCol);
                String wiName = result.get(bidWinameCol);
                String cusAcc = result.get(bidCustAcctNoCol);
                String cusSol = result.get(bidCustSolCol);
                String cusMail = result.get(bidCustEmail);
                String principal = result.get(bidCustPrincipalCol);
                String rate = result.get(bidRate);
                String principalAtMaturity = result.get(bidPrincipalMaturityCol.toUpperCase());
                String interest = result.get(bidInterestCol.toUpperCase());
                String investmentType = result.get(bidInvestmentTypeCol.toUpperCase());
                String tranPart = "CP/" + id.toUpperCase() + "/MATUREDBID";

                String values = "'Y', 'Y'";
                String condition = "CUSTREFID = '" + id + "'";

                if (investmentType.equalsIgnoreCase(investmentTypePrincipalInterest))
                    principalAtMaturity = String.valueOf(Double.parseDouble(principalAtMaturity) + Double.parseDouble(interest));
                postResp = IntegrationCall.postTransaction(LoadProp.headOfficeCpAcctNo, LoadProp.headOfficeCpSol, principalAtMaturity, tranPart, wiName, cusAcc, cusSol);

                if (postingIsSuccessful(postResp)) {
                    new Controller().updateRecords(sessionId, Query.bidTblName, columnsS, values, condition);
                    new CompleteWorkItem(sessionId, wiName, attribute, flag);
                    String mailMessageS = "We wish to inform you that your Commercial Paper of N'" + principalAtMaturity + "' with reference number '" + id + "' at '" + rate + "'% has matured. Depending on your instruction, your funds should be credited to your account shortly.<br>Thank you for choosing First Bank.";
                    new MailSetup(sessionId, wiName, fbnMailer, cusMail, empty, mailSubject, mailMessageS);
                } else if (postingNotSuccessful(postResp)) {
                    new Controller().updateRecords(sessionId, Query.bidTblName, columnsF, values, condition);
                    String mailMessage = "Kindly login to post transactions Utility failed to post for Matured Primary Market Commercial Paper with Workitem number '" + wiName + "'";
                    new MailSetup(sessionId, wiName, fbnMailer, Commons.getUsersMailsInGroup("TUSERS"), empty, mailSubject, mailMessage);
                }
            }
        }
    }

    private void processPostFailedMaturePmBids(){
        resultSet = new Controller().getRecords(Query.getCpPostFailMaturityBids(primaryMarket));
        if (!resultSet.isEmpty()) {
            String attribute = "<CP_UTILITYFLAG>M</CP_UTILITYFLAG><G_SELECT_MARKET>cp_market</G_SELECT_MARKET><CP_SELECT_MARKET>" + primaryMarket + "</CP_SELECT_MARKET>";
            String wiName = new CreateWorkItem(sessionId, attribute, initiateFlagNo).getCreatedWorkItem();
            String column = "FAILEDTRANUTILITYWINAME,FAILEDPOSTFLAG";
            String value = "'" + wiName + "','T'";


            for (Map<String, String> result : resultSet) {
                String id = result.get(bidCustIdCol.toUpperCase());
                String condition = "CUSTREFID = '" + id + "'";
                new Controller().updateRecords(sessionId, Query.bidTblName, column, value, condition);
            }
            new CompleteWorkItem(sessionId, wiName);
        }
    }

    private boolean postingIsSuccessful (String data){
        return data.equalsIgnoreCase(apiSuccess);
    }
    private boolean postingNotSuccessful (String data){
        return data.equalsIgnoreCase(apiFailed);
    }

}
