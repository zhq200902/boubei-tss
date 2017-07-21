package com.boubei.tss.dm;

import com.boubei.tss.PX;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.modules.param.ParamConfig;
import com.boubei.tss.modules.param.ParamManager;

public final class DMConstants {
	
	public final static String LOCAL_CONN_POOL = "connectionpool";
    
	public final static String USER_ID      = "userId";
	public final static String USER_CODE    = "userCode";
	public final static String FROM_USER_ID = "fromUserId";
	
	// 常用宏
	public final static String FILTER_BY_DOMAIN = "filterByDomain";
	
	// XForm 模板
	public static final String XFORM_GROUP  = "template/dm/group_xform.xml";
	public static final String XFORM_REPORT = "template/dm/report_xform.xml";
	public static final String XFORM_RECORD = "template/dm/record_xform.xml";
    
    // Grid 模板
	public static final String GRID_RECORD_ATTACH = "template/dm/record_attach_grid.xml";
    
    //报表模板资源文件目录
	public static final String REPORT_TL_DIR_DEFAULT = "more/bi_template";
	public static final String REPORT_TL_TYPE = "reportTL";
	
	/**
	 * 如果是一个开发者角色的用户，则需要为其隔离出一个专有目录
	 */
	public static String getReportTLDir() {
		String tlDir = ParamConfig.getAttribute(PX.REPORT_TL_DIR, REPORT_TL_DIR_DEFAULT);
		if( Environment.isDeveloper() ) {
			tlDir += "/" + Environment.getUserCode();
		}
		return tlDir;
	}
	
	public static String getDS(String ds) {
		if( ds == null ) {
            try {
                return ParamManager.getValue(PX.DEFAULT_CONN_POOL).trim(); // 默认数据源
            } catch (Exception e) {
            }
        }
		return ds;
	}
}
