/******************************************************************************
 * Product: iDempiere Free ERP Project based on Compiere (2006)               *
 * Copyright (C) 2014 Redhuan D. Oon All Rights Reserved.                     *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 *  FOR NON-COMMERCIAL DEVELOPER USE ONLY                                     *
 *  @author Redhuan D. Oon  - red1@red1.org  www.red1.org                     *
 *****************************************************************************/

package org.plugin.model;

import java.sql.ResultSet;
import java.util.Properties;

import org.adempiere.model.POWrapper;

public class MRequest extends org.compiere.model.MRequest {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public MRequest(Properties ctx, int R_Request_ID, String trxName) {
		super(ctx, R_Request_ID, trxName); 
	}

	public MRequest(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}
	
 
}
