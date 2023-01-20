/**
 * Licensed under the KARMA v.1 Law of Sharing. As others have shared freely to you, so shall you share freely back to us.
 * If you shall try to cheat and find a loophole in this license, then KARMA will exact your share.
 * and your worldly gain shall come to naught and those who share shall gain eventually above you.
 * In compliance with previous GPLv2.0 works of ComPiere USA, Redhuan D. Oon (www.red1.org) and iDempiere contributors 
*/
package org.crm.component;

import java.math.BigDecimal;

import org.adempiere.base.event.AbstractEventHandler;
import org.adempiere.base.event.IEventTopics;
import org.adempiere.base.event.LoginEventData;
import org.adempiere.exceptions.AdempiereException; 
import org.compiere.model.MBPartner;
import org.compiere.model.MBPartnerLocation;
import org.compiere.model.MLocation;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.model.MProduct;
import org.compiere.model.MRequest;
import org.compiere.model.MRequestType;
import org.compiere.model.MRequestUpdate;
import org.compiere.model.MUser;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.model.X_C_ContactActivity;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.TimeUtil;
import org.osgi.service.event.Event;
/**
 *  @author red1 
 */
public class PluginDocEvent extends AbstractEventHandler{
	private static CLogger log = CLogger.getCLogger(PluginDocEvent.class);
	private String trxName = "";
	private PO po = null; 
	private MUser newLead;
	private MLocation MLocation;
	private MOrderLine MOrderLine;
	private MBPartner BPartner;
	private static boolean requestChangeDone=false; 
	private static int SalesRep_ID=0;
	private static int emailRequestTypeID=0;
	
	@Override
	protected void initialize() { 
	//register EventTopics and TableNames   
		registerEvent(IEventTopics.AFTER_LOGIN); 
		registerTableEvent(IEventTopics.PO_AFTER_NEW, MRequest.Table_Name); 
		registerTableEvent(IEventTopics.PO_AFTER_NEW, X_C_ContactActivity.Table_Name); 
		registerTableEvent(IEventTopics.PO_AFTER_CHANGE, MRequest.Table_Name);
		log.info("<SalesLead CRM> .. IS NOW INITIALIZED");
		}

	@Override
	protected void doHandleEvent(Event event){
		String type = event.getTopic();
		//testing that it works at login
		if (type.equals(IEventTopics.AFTER_LOGIN)) {
			LoginEventData eventData = getEventData(event);
			log.fine(" topic="+event.getTopic()+" AD_Client_ID="+eventData.getAD_Client_ID()
					+" AD_Org_ID="+eventData.getAD_Org_ID()+" AD_Role_ID="+eventData.getAD_Role_ID()
					+" AD_User_ID="+eventData.getAD_User_ID());
			SalesRep_ID = eventData.getAD_User_ID();
			}
		else 
		{
			setPo(getPO(event));
			setTrxName(po.get_TrxName());
			log.info(" topic="+event.getTopic()+" po="+po);
			if (po instanceof MRequest && type.equals(IEventTopics.PO_AFTER_NEW)){
				BPartner = null;
				newLead = null;
				MRequest request = (MRequest)po ;
				if (request.getR_RequestType().getName().equals("Email") && type.equals(IEventTopics.PO_AFTER_NEW))
					processDirectFromEmail(request);
				else if (request.getSummary().startsWith("FROM:"))
						emailReplyHandling();
				else if (request.getR_RequestType().getName().equals("Sales Lead"))
					salesLeadRequestTypeHandling(request);				
				else if (request.getR_RequestType().getName().equals("Sales Order") && type.equals(IEventTopics.PO_AFTER_NEW))
					salesOrderRequestTypeHandling(request);
				
	
			} else if (po instanceof MRequest && type.equals(IEventTopics.PO_AFTER_CHANGE)) {
				if (!requestChangeDone)
					emailReplyHandling();
				requestChangeDone=false;
				
			} else if(po instanceof X_C_ContactActivity  && type.equals(IEventTopics.PO_AFTER_NEW)){
				contactActivityMeetingHandling();
			}
		}
	}

	private void processDirectFromEmail(MRequest request) {
		// TODO Auto-generated method stub
		String summary = request.getResult();
		String instruction[] = summary.split("#");
		if (instruction.length<1)
			return;
		String csvscript[] = instruction[1].split(",");
		for (String csvpart:csvscript){
			String tagscript[] = csvpart.split("=");
			String code = tagscript[0].trim().toUpperCase();
			if (code.equals("C")){ //bpartner
				getBPartner(tagscript[1], request);
				if (newLead!=null){
					request.setAD_User_ID(newLead.getAD_User_ID()); //now request under respective Lead tab
					request.saveEx(trxName);
				}
			}else if (code.equals("P")){//create Order based on Product
				createRequestSalesOrder(tagscript[1],request);
			}else if (code.equals("Q")){//Qty
				setOrderQty(tagscript[1]);
			}else if (code.equals("$")){//Price
				setOrderPrice(tagscript[1]);
			}				
		}
		request.setSummary("Sales Order created:" + request.getC_Order().getDocumentNo());
		request.setDateLastAlert(TimeUtil.addDays(request.getUpdated(),-2));
		request.setDateNextAction(request.getUpdated());
		request.saveEx(trxName);
		//send email back to sender with confirmation of Sales Order creation!
	}

	private void salesLeadRequestTypeHandling(MRequest request) {
		String summary = request.getSummary();
		String csvscript[] = summary.split(",");
		//get BPartner tag
		for (String csvpart:csvscript){
			String tagscript[] = csvpart.split("=");
			String code = tagscript[0].trim().toUpperCase();
			if (code.equals("C")){ //
				createLeadUser(tagscript[1],request);
				if (newLead==null) 
					return; //not a new lead, abort.
			}else if (code.equals("K")){//SearchValue
				setValue(tagscript[1]);
			}else if (code.equals("T")){//tel
				setTelNo(tagscript[1]);
			}else if (code.equals("A")){//address1
				setAddress1(tagscript[1]);
			}else if (code.equals("A2")){//address2
				setAddress2(tagscript[1]);
			}else if (code.equals("E")){//email
				setEmail(tagscript[1]);
			}
		}
	}

	private void contactActivityMeetingHandling() {
		requestChangeDone = true;
		X_C_ContactActivity act = (X_C_ContactActivity)po;
		//get ActivityType only for meetings
		if (!act.getContactActivityType().equals("ME"))
			return;
		//create new Request event for the Calendar to display
		MRequest request = new MRequest(Env.getCtx(),0,trxName); 
		request.setAD_User_ID(act.getAD_User_ID());
		request.setDateStartPlan(act.getStartDate());
		request.setStartTime(act.getStartDate());
		request.setDateCompletePlan(act.getEndDate());
		request.setEndTime(act.getEndDate());
		request.setSalesRep_ID(act.getSalesRep_ID());
		request.setConfidentialType(request.CONFIDENTIALTYPE_PartnerConfidential);
		request.setSummary("Meeting with "+act.getAD_User().getName()+" about "+act.getDescription());
		MRequestType rt = MRequestType.getDefault(Env.getCtx());
		request.setR_RequestType_ID(rt.get_ID());
		request.saveEx(trxName);
		log.info("Creating new Request "+request.get_ID());
	}

	private void salesOrderRequestTypeHandling(MRequest request) {
		requestChangeDone = true;
		String summary = request.getSummary();
		String csvscript[] = summary.split(",");
		for (String csvpart:csvscript){
			String tagscript[] = csvpart.split("=");
			String code = tagscript[0].trim().toUpperCase();
			if (code.equals("C")){ //bpartner
				getBPartner(tagscript[1], request);
				if (newLead!=null){
					request.setAD_User_ID(newLead.getAD_User_ID()); //now request under respective Lead tab
					request.saveEx(trxName);
				}
			}else if (code.equals("P")){//create Order based on Product
				createRequestSalesOrder(tagscript[1],request);
			}else if (code.equals("Q")){//Qty
				setOrderQty(tagscript[1]);
			}else if (code.equals("$")){//Price
				setOrderPrice(tagscript[1]);
			}				
		}
	}
	/**	
	 * https://idempiere.atlassian.net/browse/IDEMPIERE-3109 - Request sending out emails should get updates from replies
	 * After Save by RequestEmailProcessor, generate each reply as a RequestUpdate tab record
	 */
	private void emailReplyHandling() {
		MRequest emailRequest = (MRequest)po;
		requestChangeDone = true;
		if (emailRequestTypeID==0){
			emailRequestTypeID = setEmailRequestTypeID();
		}
		//handling for new Request - to derive and set BPartner ID from AD_User.Email	
		if (emailRequest.getSummary().startsWith("FROM: ") && emailRequest.getAD_User_ID()<1){
			String summary = emailRequest.getSummary();
			String[] summarySplit = summary.split("\\s+");
			for (String s:summarySplit){
				String emailuser = s;
				if (emailuser.contains("@") && emailuser.startsWith("<")&& emailuser.endsWith(">")){
					MUser user = new Query(Env.getCtx(),MUser.Table_Name,MUser.COLUMNNAME_EMail+"=?",trxName)
					.setParameters(emailuser.substring(1, emailuser.indexOf(">")))
					.first();
					if (user!=null){
						if (user.getC_BPartner_ID()>0){
							emailRequest.setC_BPartner_ID(user.getC_BPartner_ID());
						}
						if (user.getAD_User_ID()>0){
							emailRequest.setAD_User_ID(user.getAD_User_ID());
						} 
					}						
					if (emailRequestTypeID>0)
						emailRequest.setR_RequestType_ID(emailRequestTypeID);
					emailRequest.saveEx(trxName);
					break;
				}	
			}					
		}
		String result = emailRequest.getResult();
		if (result==null)
			result = emailRequest.getLastResult();
		if (result==null)
			return;
		if (result.startsWith("FROM: ")){ //Line 457:new StringBuilder("FROM: ").append(emailContent.fromAddress.get(0)).append("\n").append(emailContent.getTextContent());
			//check if similar result posted.
			MRequestUpdate requestupdate = new Query(Env.getCtx(),MRequestUpdate.Table_Name,MRequestUpdate.COLUMNNAME_Result+"=?",trxName)
			.setParameters(result)
			.first();
			if (requestupdate!=null)
				return;
			requestupdate = new MRequestUpdate(emailRequest);
			requestupdate.setResult(result);
			requestupdate.saveEx(trxName);
			return;
		}
	}
  
	private int setEmailRequestTypeID() {
		MRequestType type = new Query(Env.getCtx(),MRequestType.Table_Name,MRequestType.COLUMNNAME_Name+"=?",trxName)
		.setParameters("Email").first();
		if (type!=null)
			return type.get_ID();
		return 0;
	}

	private void setOrderPrice(String price) {
		BigDecimal Discount = Env.ZERO;
		BigDecimal PriceList = this.MOrderLine.getPriceList();
		Double amount = Double.valueOf(price);
		BigDecimal PriceActual = BigDecimal.valueOf(amount);
		this.MOrderLine.setPriceEntered(PriceActual);
		this.MOrderLine.setPriceActual(PriceActual);
		if (PriceList.compareTo(Env.ZERO) == 0)
			Discount = Env.ZERO;
		else
			Discount = BigDecimal.valueOf((PriceList.doubleValue() - PriceActual.doubleValue()) / PriceList.doubleValue() * 100.0);
		if (Discount.scale() > 2)
			Discount = Discount.setScale(2, BigDecimal.ROUND_HALF_UP);
		this.MOrderLine.setDiscount(Discount);
		this.MOrderLine.saveEx(trxName);
		MOrderLine.getParent().saveEx(trxName);
	}

	private void setOrderQty(String qty) {
		this.MOrderLine.setQty(new BigDecimal(Integer.valueOf(qty)));
		this.MOrderLine.saveEx(trxName);		
	}

	private void createRequestSalesOrder(String productname, MRequest request) {
		requestChangeDone = true;
		//find Product
		String s = productname.replaceAll("\\n", "");
		productname = s.replaceAll("\\r", "");
		MProduct product = new Query(Env.getCtx(),MProduct.Table_Name,MProduct.COLUMNNAME_Value+" Like '"+productname+"%'",trxName)
		.first();
		if (product==null)
			throw new AdempiereException("No such product. Aborted!");
		if (BPartner==null)
			throw new AdempiereException("BPartner not set");
		MOrder order = new MOrder(Env.getCtx(),0,trxName);
		order.setC_BPartner_ID(BPartner.getC_BPartner_ID());
		if (BPartner.getAD_Org_ID()>0)
			order.setAD_Org_ID(BPartner.getAD_Org_ID());
		else 
			order.setAD_Org_ID(Env.getAD_Org_ID(Env.getCtx()));
		MBPartnerLocation[] locations = BPartner.getLocations(true);
		order.setC_BPartner_Location_ID(locations.length>0?locations[0].get_ID():0);
		order.setAD_Org_ID(Env.getContextAsInt(Env.getCtx(), "#AD_Org_ID"));
		if (request.getDateStartPlan()!=null)
			order.setDateOrdered(request.getDateStartPlan());
		else
			order.setDateOrdered(request.getUpdated());
		if (request.getDateCompletePlan()!=null)
			order.setDatePromised(request.getDateCompletePlan());
		else 
			order.setDatePromised(request.getUpdated());
		order.saveEx(trxName);
		MOrderLine orderline = new MOrderLine(order);
		orderline.setM_Product_ID(product.get_ID());
		orderline.setQty(Env.ONE);
		orderline.saveEx(trxName);
		order.saveEx(trxName);
		this.MOrderLine = orderline; 
		request.setC_Order_ID(order.getC_Order_ID());
		request.setC_BPartner_ID(BPartner.get_ID());
		request.saveEx(trxName);
	}

	/**
	 * check if BPartner existed before. If not, and its a new Lead then create.
	 */
	private void getBPartner(String value, MRequest request) {
		MBPartner bp = null;
		if (newLead==null)
			getLeadUser(value); 
		bp = new Query(Env.getCtx(),MBPartner.Table_Name,MBPartner.COLUMNNAME_Name+" Like '"+value+"%'",trxName)
			.first();
		if (bp!=null){
			BPartner = bp;
			return;
		} 
		//creating BP record for Lead. This is converted into a sales.
		bp = new MBPartner(Env.getCtx(),0,trxName);
		bp.setValue(newLead.getValue());
		bp.setName(newLead.getName());
		bp.setFirstSale(bp.getCreated());
		bp.setIsCustomer(true);
		bp.setSalesRep_ID(newLead.getSalesRep_ID());
		bp.saveEx(trxName);
		newLead.setC_BPartner_ID(bp.getC_BPartner_ID());
		this.BPartner = bp;
		if (newLead.getC_Location_ID() != 0)
		{
			MLocation leadAddress = (MLocation) newLead.getC_Location();
			MBPartnerLocation loc = new MBPartnerLocation(bp);
			MLocation address = new MLocation(Env.getCtx(), 0, trxName);
			PO.copyValues(leadAddress, address);
			address.saveEx(trxName);
			
			loc.setC_Location_ID(address.getC_Location_ID());
			loc.setPhone(newLead.getPhone());
			loc.setPhone2(newLead.getPhone2());
			loc.setFax(newLead.getFax());
			loc.saveEx(trxName);
			
			newLead.setC_BPartner_Location_ID(loc.getC_BPartner_Location_ID());
			newLead.setLeadStatus(MUser.LEADSTATUS_Converted); 
		}
		
		// company address
		if (newLead.getBP_Location_ID() != 0)
		{
			MLocation leadAddress = (MLocation) newLead.getBP_Location();
			MBPartnerLocation loc = new MBPartnerLocation(bp);
			MLocation address = new MLocation(Env.getCtx(), 0, trxName);
			PO.copyValues(leadAddress, address);
			address.saveEx(trxName);
			
			loc.setC_Location_ID(address.getC_Location_ID());
			loc.saveEx(trxName); 
		}
		newLead.saveEx(trxName);
	}

	private void getLeadUser(String value) { 
		MUser user = new Query(Env.getCtx(),MUser.Table_Name,MUser.COLUMNNAME_Name+" Like '"+value+"%'",trxName)
		.first();
		if (user!=null)
			newLead = user;
	}

	private void setAddress2(String address2) {
		if (MLocation==null) return;
		MLocation.setAddress2(address2);
		MLocation.saveEx(trxName);		
	}

	private void setValue(String value) {
		newLead.setValue(value);
		newLead.saveEx(trxName);
	}

	private void setEmail(String EMail) {
		newLead.setEMail(EMail);
		newLead.setPassword(EMail);
		newLead.saveEx(trxName);
	}

	private void setAddress1(String address1) {
		MLocation location = new Query(Env.getCtx(),MLocation.Table_Name,MLocation.COLUMNNAME_Address1+"=?",trxName)
		.setParameters(address1)
		.first();
		if  (location==null){
			location = new MLocation(Env.getCtx(),0,trxName);
			location.setAddress1(address1);
			location.saveEx(trxName);
		} else {
			location.setAddress1(address1);
			location.saveEx(trxName);
		}
		this.MLocation = location;
		newLead.setC_Location_ID(this.MLocation.getC_Location_ID());
		newLead.saveEx(trxName);
		this.MLocation.saveEx(trxName);
	}

	private void setTelNo(String phone) {
	newLead.setPhone(phone);
	newLead.saveEx(trxName);
		
	}

	/**
	 * create LeadUser unless it has a BPartner before
	 * @param prospect
	 * @param request
	 */
	private void createLeadUser(String prospect,MRequest request) {
		requestChangeDone = true;
		MUser user = new Query(Env.getCtx(),MUser.Table_Name,MUser.COLUMNNAME_Name+"=?",trxName)
		.setParameters(prospect)
		.first();
		if (user==null){
			MUser newlead = new MUser(Env.getCtx(),0,trxName); 
			newlead.setSalesRep_ID(SalesRep_ID);
			newlead.setName(prospect);
			newlead.setBPName(prospect);
			newlead.setIsSalesLead(true);
			newlead.setLeadStatus(MUser.LEADSTATUS_New);
			newlead.saveEx(trxName);
			this.newLead = newlead;				
			request.setAD_User_ID(newLead.getAD_User_ID()); //now all such requests under respective Lead tab
			request.saveEx(trxName);
		} else
			throw new AdempiereException("Lead user existed before");	
	}

	private void setPo(PO eventPO) {
		 po = eventPO;
	}

	private void setTrxName(String get_TrxName) {
		trxName = get_TrxName;		
	}
  
}
