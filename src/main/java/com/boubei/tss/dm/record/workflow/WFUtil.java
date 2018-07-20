package com.boubei.tss.dm.record.workflow;

import java.util.List;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;

import com.boubei.tss.framework.exception.BusinessException;
import com.boubei.tss.util.BeanUtil;
import com.boubei.tss.util.EasyUtils;

public class WFUtil {

	/**
	 * 检查流程定义是否合法
	 */
	public static boolean checkWorkFlow(String wfDefine) {
		return  !EasyUtils.isNullOrEmpty(wfDefine) && wfDefine.indexOf("to") > 0;
	}
	
	@SuppressWarnings("unchecked")
	public static Map<String, List<Map<String, String>>> parseWorkflow(String wfDefine, Map<String, Object> context, String rcName) {
		Map<String, List<Map<String, String>>> rules;
		try {
			String workflow = EasyUtils.fmParse(wfDefine, context);
			rules = new ObjectMapper().readValue(workflow, Map.class);
		} 
		catch (Exception e) {
			throw new BusinessException(rcName + " workflow parse error: " + e.getMessage());
		}
		
		return rules;
	}
	
	public static String toString(Object bean) {
		Map<String, Object> m = BeanUtil.getProperties(bean);
		m.remove("id");
		m.remove("tableId");
		m.remove("currStepIndex");
		m.remove("PK");
		m.remove("class");
		
		return m.toString();
	}
	
}
