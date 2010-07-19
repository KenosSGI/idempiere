/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 1999-2006 Adempiere, Inc. All Rights Reserved.                *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 *
 * Copyright (C) 2005 Robert Klein. robeklein@hotmail.com
 * Contributor(s): Low Heng Sin hengsin@avantz.com
 *****************************************************************************/
package org.adempiere.pipo2.handler;

import java.util.List;
import java.util.Properties;

import javax.xml.transform.sax.TransformerHandler;

import org.adempiere.pipo2.AbstractElementHandler;
import org.adempiere.pipo2.PoExporter;
import org.adempiere.pipo2.Element;
import org.adempiere.pipo2.PackOut;
import org.adempiere.pipo2.PoFiller;
import org.adempiere.pipo2.exception.POSaveFailedException;
import org.compiere.model.X_AD_Package_Imp_Detail;
import org.compiere.model.X_AD_WF_NodeNext;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.wf.MWFNodeNext;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

public class WorkflowNodeNextElementHandler extends AbstractElementHandler {

	public void startElement(Properties ctx, Element element) throws SAXException {
		List<String> excludes = defaultExcludeList(X_AD_WF_NodeNext.Table_Name);

		String entitytype = getStringValue(element, "EntityType");
		if (isProcessElement(ctx, entitytype)) {
			if (isParentSkip(element, null)) {
				element.skip = true;
				return;
			}

			String workflowValue = getStringValue(element, "AD_Workflow.Value", excludes);
			int workflowId = 0;
			if (getParentId(element, "workflow") > 0) {
				workflowId = getParentId(element, "workflow");
			} else {
				workflowId = findIdByColumn(ctx, "AD_Workflow", "Value", workflowValue);
			}
			if (workflowId <= 0) {
				element.defer = true;
				element.unresolved = "AD_Workflow: " + workflowValue;
				return;
			}

			String workflowNodeValue = getStringValue(element, "AD_WF_Node.ValuTe", excludes);
			String workflowNodeNextValue = getStringValue(element, "AD_WF_Next_ID.AD_WF_Node.Value", excludes);

			int wfNodeId = DB.getSQLValue(getTrxName(ctx), "SELECT AD_WF_Node_Id FROM AD_WF_Node WHERE AD_Workflow_ID=? AND Value=? AND AD_Client_ID=?", workflowId, workflowNodeValue, Env.getAD_Client_ID(ctx));
			if (wfNodeId <= 0) {
				element.defer = true;
				element.unresolved = "AD_WF_Node: " + workflowNodeValue;
				return;
			}

			int AD_WF_Next_ID = DB.getSQLValue(getTrxName(ctx), "SELECT AD_WF_Node_Id FROM AD_WF_Node WHERE AD_Workflow_ID=? AND Value=? AND AD_Client_ID=?", workflowId, workflowNodeNextValue, Env.getAD_Client_ID(ctx));
			if (AD_WF_Next_ID <= 0) {
				element.defer = true;
				element.unresolved = "AD_WF_Node: " + workflowNodeNextValue;
				return;
			}

			X_AD_Package_Imp_Detail impDetail = createImportDetail(ctx, element.qName, X_AD_WF_NodeNext.Table_Name,
					X_AD_WF_NodeNext.Table_ID);

			int AD_WF_NodeNext_ID = DB.getSQLValue(getTrxName(ctx), "SELECT AD_WF_NodeNext_ID FROM AD_WF_NodeNext WHERE AD_WF_Node_ID=? and AD_WF_NEXT_ID =?", wfNodeId, AD_WF_Next_ID);

			MWFNodeNext mWFNodeNext = new MWFNodeNext(ctx, AD_WF_NodeNext_ID, getTrxName(ctx));
			PoFiller filler = new PoFiller(ctx, mWFNodeNext, element, this);
			String action = null;
			if (AD_WF_NodeNext_ID <= 0 && isOfficialId(element, "AD_WF_NodeNext_ID"))
				mWFNodeNext.setAD_WF_NodeNext_ID(getIntValue(element, "AD_WF_NodeNext_ID"));

			if (AD_WF_NodeNext_ID > 0){
				backupRecord(ctx, impDetail.getAD_Package_Imp_Detail_ID(), X_AD_WF_NodeNext.Table_Name,mWFNodeNext);
				action = "Update";
			} else{
				action = "New";
			}

			mWFNodeNext.setAD_WF_Node_ID(wfNodeId);
			mWFNodeNext.setAD_WF_Next_ID(AD_WF_Next_ID);
			List<String> notfounds = filler.autoFill(excludes);
			if (notfounds.size() > 0) {
				element.defer = true;
				return;
			}
			if (mWFNodeNext.save(getTrxName(ctx)) == true){
				log.info("m_WFNodeNext save success");
				logImportDetail (ctx, impDetail, 1, String.valueOf(mWFNodeNext.get_ID()),mWFNodeNext.get_ID(), action);
			} else{
				log.info("m_WFNodeNext save failure");
				logImportDetail (ctx, impDetail, 0, String.valueOf(mWFNodeNext.get_ID()), mWFNodeNext.get_ID(), action);
				throw new POSaveFailedException("WorkflowNodeNext");
			}
		} else {
			element.skip = true;
		}
	}

	public void endElement(Properties ctx, Element element) throws SAXException {
	}

	public void create(Properties ctx, TransformerHandler document)
			throws SAXException {
		int ad_wf_nodenext_id = Env.getContextAsInt(ctx, X_AD_WF_NodeNext.COLUMNNAME_AD_WF_NodeNext_ID);
		X_AD_WF_NodeNext m_WF_NodeNext = new X_AD_WF_NodeNext(
				ctx, ad_wf_nodenext_id, null);
		AttributesImpl atts = new AttributesImpl();
		atts.addAttribute("", "", "type", "CDATA", "object");
		atts.addAttribute("", "", "type-name", "CDATA", "ad.workflow.node.next-element");
		document.startElement("", "", "workflowNodeNext",
				atts);
		createWorkflowNodeNextBinding(ctx, document, m_WF_NodeNext);
		document.endElement("", "", "workflowNodeNext");

	}

	private void createWorkflowNodeNextBinding(Properties ctx, TransformerHandler document,
			X_AD_WF_NodeNext m_WF_NodeNext)
	{

		PoExporter filler = new PoExporter(ctx, document, m_WF_NodeNext);
		List<String> excludes = defaultExcludeList(X_AD_WF_NodeNext.Table_Name);

		if (m_WF_NodeNext.getAD_WF_NodeNext_ID() <= PackOut.MAX_OFFICIAL_ID)
	        filler.add("AD_WF_NodeNext_ID", new AttributesImpl());

		filler.export(excludes);
	}
}
