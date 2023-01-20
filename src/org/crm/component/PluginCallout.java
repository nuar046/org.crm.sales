/**
 * Licensed under the KARMA v.1 Law of Sharing. As others have shared freely to you, so shall you share freely back to us.
 * If you shall try to cheat and find a loophole in this license, then KARMA will exact your share.
 * and your worldly gain shall come to naught and those who share shall gain eventually above you.
 * In compliance with previous GPLv2.0 works of ComPiere USA, Redhuan D. Oon (www.red1.org) and iDempiere contributors 
*/

package org.crm.component;
import java.util.Properties;

import org.adempiere.base.IColumnCallout;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.MColumn;
import org.compiere.model.MElement;
import org.compiere.model.M_Element;
import org.compiere.util.Env;
import org.jfree.util.Log;


public class PluginCallout implements IColumnCallout {

	public PluginCallout() {
		 
	}

	@Override
	public String start(Properties ctx, int WindowNo, GridTab mTab,
			GridField mField, Object value, Object oldValue) { 
	
		return null;
	}

}
