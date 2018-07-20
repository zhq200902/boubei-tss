package com.boubei.tss.dm.record.workflow;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.boubei.tss.framework.persistence.IEntity;

@Entity
@Table(name = "dm_wf_status")
@SequenceGenerator(name = "wf_status_sequence", sequenceName = "wf_status_sequence", initialValue = 1, allocationSize = 10)
public class WFStatus implements IEntity {
	
	public final static String NEW       = "待审核";
	public final static String APPROVING = "审批中";
	public final static String TRANS     = "已转审";
	public final static String PASSED    = "已通过";
	public final static String REJECTED  = "已驳回";
	public final static String CANCELED  = "已撤销";
	
	@Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "wf_status_sequence")
	private Long id;
	
	@Column(nullable = false)
	private Long tableId;  // 流程对应数据表
	
	@Column(nullable = false)
	private Long itemId;  // 对应的数据记录
	
	private String applier; // 申请人
	
	private String to;
	private String cc;
	
	private Integer stepCount; // 审批步数
	
	/* 流程处理人汇总 */
	private String processors;
	
	/* 当前流程状态: 审批中、已通过、已撤销 */
	private String currentStatus;
	
	/* 流程最后处理人 */
	private String lastProcessor;
	
	/* 流程下一处理人 */
	private String nextProcessor;
	
	@Transient
	public int currStepIndex;
	
	public List<String> toUsers() {
		return Arrays.asList( to.split(",") );
	}
	
	public String toString() {
		return WFUtil.toString(this);
	}

	public Serializable getPK() {
		return this.getId();
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getTableId() {
		return tableId;
	}

	public void setTableId(Long tableId) {
		this.tableId = tableId;
	}

	public Long getItemId() {
		return itemId;
	}

	public void setItemId(Long itemId) {
		this.itemId = itemId;
	}

	public String getTo() {
		return to;
	}

	public void setTo(String to) {
		this.to = to;
	}

	public String getCc() {
		return cc;
	}

	public void setCc(String cc) {
		this.cc = cc;
	}

	public String getCurrentStatus() {
		return currentStatus;
	}

	public void setCurrentStatus(String currentStatus) {
		this.currentStatus = currentStatus;
	}

	public String getNextProcessor() {
		return nextProcessor;
	}

	public void setNextProcessor(String nextProcessor) {
		this.nextProcessor = nextProcessor;
	}

	public Integer getStepCount() {
		return stepCount;
	}

	public void setStepCount(Integer stepCount) {
		this.stepCount = stepCount;
	}

	public String getProcessors() {
		return processors;
	}

	public void setProcessors(String processors) {
		this.processors = processors;
	}

	public String getLastProcessor() {
		return lastProcessor;
	}

	public void setLastProcessor(String lastProcessor) {
		this.lastProcessor = lastProcessor;
	}

	public String getApplier() {
		return applier;
	}

	public void setApplier(String applier) {
		this.applier = applier;
	} 
}