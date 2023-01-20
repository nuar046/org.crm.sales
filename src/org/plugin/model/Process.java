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

import java.math.BigDecimal;
import java.util.logging.Level;

import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;

public class Process extends SvrProcess {

	public Process() {
		// TODO Auto-generated constructor stub
	}
    private int p_AD_Client_ID = 0;
    
    protected void prepare() {
    	//TODO implement parameters capture
        ProcessInfoParameter[] para = getParameter();
        for (int i = 0; i < para.length; i++) {
            String name = para[i].getParameterName();
            if (para[i].getParameter() == null);
            else if (name.equals("AD_Client_ID")) {
                p_AD_Client_ID = ((BigDecimal) para[i].getParameter()).intValue();
            }  else {
                log.log(Level.SEVERE, "Unknown Parameter: " + name);
            }
        }

    }

    protected String doIt() {
    	String message = "";
    	
    	//TODO implement process handling - set in Application Dictionary as Process Menu Item
    	
		return message;   	
    }

}
