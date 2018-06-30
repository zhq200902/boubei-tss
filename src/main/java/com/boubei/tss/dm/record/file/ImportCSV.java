/* ==================================================================   
 * Created [2016-3-5] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.dm.record.file;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

import com.boubei.tss.EX;
import com.boubei.tss.dm.DataExport;
import com.boubei.tss.dm.ddl._Database;
import com.boubei.tss.dm.ddl._Field;
import com.boubei.tss.dm.record.Record;
import com.boubei.tss.dm.record.RecordService;
import com.boubei.tss.framework.Global;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.framework.web.servlet.AfterUpload;
import com.boubei.tss.modules.sn.SerialNOer;
import com.boubei.tss.util.BeanUtil;
import com.boubei.tss.util.EasyUtils;
import com.boubei.tss.util.ExcelUtils;
import com.boubei.tss.util.FileHelper;

/**
 * var url = URL_UPLOAD_FILE + "?afterUploadClass=com.boubei.tss.dm.record.file.ImportCSV";
   url += "&record=" + rcTable;
   url += "&uniqueCodes=oto,phone";
   url += "&together=false";
   url += "&ignoreExist=true";
   url += "&headerTL=订单号:订单编码,货品:sku,数量:qty,类型:{良品}; // 模板表头字段映射，适用于第三方系统导出的数据导入至TSS数据表，映射表以外的字段则被忽略不进行导入
    
 * 根据数据表提供的导入模板，填写后导入实现批量录入数据。
 * CSV文件需满足条件：
 * 1、模板从 xdata/import/tl/ 接口导出
 * 2、表头列的字段名需要 和 录入表定义的字段Label严格一致，且不能重复
 * 3、每一行数据的列数及顺序 == 表头的列数及顺序，不能多也不能少
 * 4、每个字段值不允许存在 换行符、英文逗号
 * 5、覆盖式导入，需定义判断规则，支持多个字段（ 定义在数据表页面【全局脚本】里：var uniqueCodes="oto,sjphone";）
 * 
 * TODO 批量插入，如果某一批（目前10000一批）出错，如何回滚所有已经插入的数据
 */
public class ImportCSV implements AfterUpload {

	Logger log = Logger.getLogger(this.getClass());
	
	RecordService recordService = (RecordService) Global.getBean("RecordService");
	
	private List<List<String>> readData(File targetFile, String charSet, String headerTL) {
		
		String dataStr = FileHelper.readFile(targetFile, charSet); 
		dataStr = dataStr.replaceAll(";", ","); // mac os 下excel另存为csv是用分号;分隔的
		String[] rows = EasyUtils.split(dataStr, "\n");
		
		// 先移除空行
		List<List<String>> rowList = new ArrayList<List<String>>();
		for(String row : rows) {
			if( row.replaceAll(",", "").trim().length() > 0 ) {
				rowList.add( EasyUtils.toList(row) );
			}
		}
		
		List<String> headers = new ArrayList<String>( rowList.get(0) );
		
		// 检查是否有表头转换模板（比如将一个三方系统导出数据导入到数据表），格式headerTL=订单号:订单编码,货品:sku,数量:qty,类型:{良品}
		if( headerTL != null ) {
			Map<String, String> columnMap = new HashMap<String, String>();
			Map<String, String> adds = new LinkedHashMap<String, String>();
			
			String[] pairs = headerTL.split(",");
			for( String _pair : pairs ) {
				String[] pair = _pair.split(":");
				String tssColumn = pair[0].trim();
				String xxxColumn = pair[1].trim();
				
				// 默认值
				if( xxxColumn.startsWith("{") && xxxColumn.endsWith("}") ) {
					adds.put(tssColumn, xxxColumn.substring(1, xxxColumn.length() -1));
				} else { // 映射列
					columnMap.put(xxxColumn, tssColumn);
				}
			}
			
			int index = 0;
			List<Integer> surplus = new ArrayList<Integer>();
			for(String column : headers) {
				String tssColumn = columnMap.get(column);
				if( tssColumn == null || EasyUtils.isNullOrEmpty(column) ) {
					surplus.add(0, index);
				} else {
					rowList.get(0).set(index, tssColumn);
				}
				index ++;
			}
			
			// 对数据进行处理，如果表头为空，则值也全部置为空；加上表头模板里设置的新增列
			index = 0;
			for(List<String> row : rowList) {
				// 删除列
				for(int idx : surplus) { 
					if(idx < row.size())
						row.remove(idx);
				}
				
				int columnSize = rowList.get(0).size();
				if( row.size() > columnSize ) {
					rowList.set(index, row.subList(0, columnSize));
				}
				
				// 如果行的数据列大于表头的数据量，则销掉多的列
				
				// 增加列
				for( String key : adds.keySet() ) {
					row.add( index == 0 ? key : adds.get(key) );
				}
				
				index ++;
			}
		}
		
		return rowList;
	}

	public String processUploadFile(HttpServletRequest request,
			String filepath, String oldfileName) throws Exception {

		String _record = request.getParameter("recordId");
		_record = (String) EasyUtils.checkNull( _record, request.getParameter("record") );
		
		Long recordId = recordService.getRecordID(_record, false);
		Record record = recordService.getRecord(recordId);
		_Database _db = _Database.getDB(record);
		
		String charSet  = (String) EasyUtils.checkNull(request.getParameter("charSet"), DataExport.CSV_GBK); // 默认GBK
		String headerTL = request.getParameter("headerTL");

		// 解析附件数据   
		// 如果上传的是一个 XLS，先将XLS转换为CSV文件
		if( ExcelUtils.isXLS(filepath) ) { 
			filepath = ExcelUtils.excel2CSV( filepath, charSet );
		}
		
		File targetFile = new File(filepath);
		List<List<String>> rowList = readData(targetFile, charSet, headerTL);
		if( rowList.size() < 2) {
			return "parent.alert('导入文件没有数据');";
		}
		 
		List<String> headers = rowList.get(0);
		int messyCount = 0;
		for(String fieldName : headers) {
			if( !_db.ncm.containsKey(fieldName) ) messyCount++; // 表头名 在数据表字段定义里不存在
		}
		// header一半以上都找不着，可能是CSV文件为UTF-8编码，以UTF-8再次尝试读取
		if( messyCount*1.0 / headers.size() > 0.5 && !DataExport.CSV_UTF8.equals(charSet) ) {
			rowList = readData(targetFile, DataExport.CSV_UTF8, headerTL);
			headers = rowList.get(0);
		}
		
		// 校验数据
		List<String> errLines = new ArrayList<String>(); // errorLine = lineIndex + errorMsg + row
		List<Integer> errLineIndexs = new ArrayList<Integer>();
		
		String vailderClass = request.getParameter("vailderClass");
		vailderClass = (String) EasyUtils.checkNull(vailderClass, DefaultDataVaild.class.getName());
		IDataVaild vailder = (IDataVaild) BeanUtil.newInstanceByName(vailderClass);

		List<String> valSQLFields = new ArrayList<String>();
		for(String code : _db.csql.keySet()) {
			if( _db.csql.get(code) != null) {
				String name = _db.cnm.get(code);
				valSQLFields.add(name);
			}
		}
		vailder.vaild(_db, rowList, headers, valSQLFields, errLines, errLineIndexs);
		
		String fileName = null;
		if(errLineIndexs.size() > 0) {
			// 将 errorLines 输出到一个单独文件
			StringBuffer sb = new StringBuffer("行号,导入失败原因," + EasyUtils.list2Str(rowList.get(0))).append("\n");
			for(String err : errLines) {
				sb.append(err).append("\n");
			}
			log.info( sb );
			
			fileName = "err-" + recordId + Environment.getUserId();
	        String exportPath = DataExport.getExportPath() + "/" + fileName + ".csv";
	        DataExport.exportCSV(exportPath, sb.toString()); // 先输出内容到服务端的导出文件中
			
			// 根据配置，是够终止导入。默认要求一次性导入，不允许分批
			if( !"false".equals( request.getParameter("together") ) ) {
				return "parent.alert('导入失败，" +EX.parse(EX.DM_29, errLineIndexs.size(), fileName)+ "'); ";
			}
		}
		
		// 执行导入到数据库
		headers = rowList.get(0);
		return import2db(_db, request, rowList, headers, errLineIndexs, fileName);
	}

	/**
	 * 对于有特殊需求的数据导入，可继承本Class，然后在录入表的全局JS里重新定义afterUploadClass的值为自定义的Class
	 * @param _db
	 * @param ignoreExist  忽略or覆盖已存在的记录
	 * @param uniqueCodes  唯一性字段（一个或多个）
	 * @param rows
	 * @param headers
	 * @param errLineIndexs 校验错误的记录行
	 * @return
	 */
	protected String import2db(_Database _db, HttpServletRequest request, List<List<String>> rows, List<String> headers, 
			List<Integer> errLineIndexs, String fileName) {
		
		boolean ignoreExist = "true".equals( request.getParameter("ignoreExist") );
		String uniqueCodes = request.getParameter("uniqueCodes");
		
		int errLineSize = errLineIndexs.size();
		int insertCount = 0, updateCount = 0;
		List<Integer> ignoreLines = new ArrayList<Integer>();
		List<Map<String, String>> valuesMaps = new ArrayList<Map<String, String>>();
		
		List<String> snList = null; // 自动取号
		
		for(int index = 1; index < rows.size(); index++) { // 第一行为表头，不要
			
			if( errLineIndexs.contains(index) ) continue;
			
			List<String> fieldVals = rows.get(index);
			Map<String, String> valuesMap = new HashMap<String, String>();
			for(int j = 0; j < fieldVals.size(); j++) {
    			String value = fieldVals.get(j).trim();
    			value = value.replaceAll("，", ","); // 导出时英文逗号替换成了中文逗号，导入时替换回来
    			
    			String filedLabel = headers.get(j);
    			String fieldCode = _db.ncm.get(filedLabel);
    			
    			String defaultVal = _db.cval.get(fieldCode);
    			if( EasyUtils.isNullOrEmpty(value) && !EasyUtils.isNullOrEmpty(defaultVal) ) {    				
    				// 检查值为空的字段，是否配置自动取号规则，是的话先批量取出一串连号
        			if( _Field.isAutoSN(defaultVal) ) {
        				String preCode = defaultVal.replaceAll(_Field.SNO_yyMMddxxxx, "");
        				if(snList == null) {
        					int snNum = rows.size() - 1 - errLineSize;
							snList = new SerialNOer().create(preCode, snNum);
        				}
        				value = snList.get(insertCount);
        			}
    			}
    			
				valuesMap.put(fieldCode, value);
        	}
			
			// 支持覆盖式导入，覆盖规则为参数指定的某个（或几个）字段
			if( !EasyUtils.isNullOrEmpty(uniqueCodes) ) {
				// 检测记录是否已经存在
				Map<String, String> params = new HashMap<String, String>();
				
				String[] codes = uniqueCodes.trim().split(",");
				boolean hasNullParam = false;
				for(String code : codes) {
					code = code.trim();
					String value = valuesMap.get(code); // 值不能为空，为空会查出无辜的数据覆盖【查询条件为空致使查出无关的数据】
					if( EasyUtils.isNullOrEmpty(value) ) {
						hasNullParam = true;
					}
					params.put(code, value);
				}
				if( !hasNullParam ) {
					List<Map<String, Object>> result = _db.select(1, 1, params).result;
					if( result.size() > 0 ) {
						// 是否覆盖已存在数据
						if( ignoreExist ) {
							ignoreLines.add(index);
						} 
						else {
							Map<String, Object> old = result.get(0);
							Long itemId = EasyUtils.obj2Long(old.get("id"));
							_db.update(itemId, valuesMap);
							
							updateCount ++;
						}
						
						continue;
					}
				}
			}
			
			// 批量新增
			valuesMaps.add(valuesMap);
			insertCount ++;
			
			if(valuesMaps.size() == 10000) { // 按每一万批量插入一次
				_db.insertBatch(valuesMaps);
				valuesMaps.clear();
			}
		}
    	_db.insertBatch(valuesMaps);
		
		// 向前台返回成功信息
    	String noInserts = ignoreExist ? ("忽略了第【" +EasyUtils.list2Str(ignoreLines)+ "】行，") : ("覆盖" +updateCount+ "行，");
    	String errMsg = errLineSize == 0 ? "请刷新查看。" : EX.parse(EX.DM_29, errLineSize, fileName);
		return "parent.alert('导入完成：共新增" +insertCount+ "行，" + noInserts + errMsg + "');";
	}
	
}