/**
 * Licensed under the KARMA v.1 Law of Sharing. As others have shared freely to you, so shall you share freely back to us.
 * If you shall try to cheat and find a loophole in this license, then KARMA will exact your share.
 * and your worldly gain shall come to naught and those who share shall gain eventually above you.
 * In compliance with previous GPLv2.0 works of ComPiere USA, Redhuan D. Oon (www.red1.org) and iDempiere contributors 
*/

package org.crm.component;

import java.sql.ResultSet;

import org.adempiere.base.IModelFactory; 
import org.compiere.model.PO;
import org.compiere.util.Env; 
import org.plugin.model.MRequest;

public class PluginModelFactory implements IModelFactory {

	@Override
	public Class<?> getClass(String tableName) {
		 if (tableName.equals(MRequest.Table_Name))
			 return MRequest.class; 
		return null;
	}

	@Override
	public PO getPO(String tableName, int Record_ID, String trxName) {
		 if (tableName.equals(MRequest.Table_Name)) 
		     return new MRequest(Env.getCtx(), Record_ID, trxName); 
		return null;
	}

	@Override
	public PO getPO(String tableName, ResultSet rs, String trxName) {
		 if (tableName.equals(MRequest.Table_Name)) 
		     return new MRequest(Env.getCtx(), rs, trxName); 
		 return null;
	}
}
