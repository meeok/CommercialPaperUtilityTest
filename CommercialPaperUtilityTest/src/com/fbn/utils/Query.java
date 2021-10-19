package com.fbn.utils;

public class Query {
    public static String setupTblName = "mm_setup_tbl";
    public static String bidTblName = "mm_bid_tbl";
    public static String extTblName = "moneymarket_ext";
    public static String investmentTblName = "mm_sminvestments_tbl";
    public static String stColId = "refid";
    public static String stColCloseFlag = "closeflag";
    

	public static String getCpOpenWindowQuery(String marketType) {
        return  "select closedate , winame,refid from mm_setup_tbl where process = 'Commercial Paper' and markettype = '"+marketType+"' and closeflag = 'N'";
    }

    public static String getCpPmBidsToProcessQuery (String id) {
        return "select custrefid, tenor, rate, ratetype from mm_bid_tbl where process = 'Commercial Paper' and markettype= 'primary' and processflag ='N' and groupindexflag = 'N' and winrefid = '"+id+"'";
    }

    public static String getCpAllocatedPrimaryBids(String flag) {
    	return "select custrefid, bidwiname, custsol,custacctno,custemail custprincipal, branchsol,allocationpercentage from mm_bid_tbl where failedflag = '"+flag+"' and process = 'Commercial Paper' and markettype = 'primary' and allocatedflag ='Y'";
    }

    public static String getCpProcessPostingFailureFailedBids(String flag) {
    	return "select custrefid from mm_bid_tbl where failedflag = '"+flag+"' and process = 'Commercial Paper' and markettype = 'primary' and allocatedflag ='Y' and postintegrationflag = 'Y' and failedpostflag = 'Y'";	
    }
    public static String getCpProcessPostingFailureSuccessBids(String flag) {
    	return "select custrefid from mm_bid_tbl where failedflag = '"+flag+"' and process = 'Commercial Paper' and markettype = 'primary' and allocatedflag ='Y' and postintegrationflag = 'Y' and (failedpostflag = 'C' or failedpostflag = 'D')";	  	
    }
    public static String getCpPostFailMaturityBids(String marketType) {
    	return "select custrefid from mm_bid_tbl where process = 'Commercial Paper' and markettype = '"+marketType+"' and postintegrationmatureflag = 'Y' and failedpostflag = 'Y' and maturedflag = 'Y'";
    }
    public static String getCpInvestmentCloseDateQuery(String id) {
    	return "select investmentid, closedate from mm_sminvestments_tbl where windowrefno = '"+id+"'";
    }

    public static String getCpProcessBidsOnAwaitingMaturity(String marketType){
    	return "select custrefid, bidwiname, maturitydate, custemail, branchsol, lienflag from mm_bid_tbl where markettype = '"+marketType+"' and awaitingmaturityflag = 'Y'  and failedpostflag = 'N'";
    }

    public static String getUsersInGroup(String groupName) {
        return "select username from pdbuser where userindex in (select userindex from pdbgroupmember where groupindex = (select groupindex from PDBGroup where GroupName='" + groupName + "'))";
    }
    public static  String getCpMaturedBids(String marketType){
	    return "select custacctno, custsol, custemail, custprincipal, principalatmaturity, rate, interest, investmenttype from mm_bid_tbl where markettype = '"+marketType+"' and  maturedflag = 'Y' and postintegrationmatureflag = 'N' and lienflag = 'N'";
    }
}
