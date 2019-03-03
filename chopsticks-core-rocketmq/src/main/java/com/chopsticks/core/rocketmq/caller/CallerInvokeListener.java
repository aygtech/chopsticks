package com.chopsticks.core.rocketmq.caller;

import java.util.List;
import java.util.Map;

import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.UtilAll;
import org.apache.rocketmq.common.message.MessageExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.chopsticks.core.concurrent.impl.DefaultPromise;
import com.chopsticks.core.concurrent.impl.DefaultTimeoutPromise;
import com.chopsticks.core.exception.CoreException;
import com.chopsticks.core.rocketmq.caller.impl.DefaultInvokeResult;
import com.chopsticks.core.rocketmq.exception.DefaultCoreException;
import com.chopsticks.core.rocketmq.handler.InvokeResponse;
import com.chopsticks.core.utils.TimeUtils;

class CallerInvokeListener implements MessageListenerConcurrently{
	
	private static final Logger log = LoggerFactory.getLogger(CallerInvokeListener.class);
	
	private Map<String, DefaultTimeoutPromise<BaseInvokeResult>> callerInvokePromiseMap;
	
	CallerInvokeListener(Map<String, DefaultTimeoutPromise<BaseInvokeResult>> callerInvokePromiseMap) {
		this.callerInvokePromiseMap = callerInvokePromiseMap;
	}
	private static final long BEGIN_EXECUTABLE_TIME = com.chopsticks.core.rocketmq.Const.CLIENT_TIME.getNow();
	@Override
	public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
		for(MessageExt ext : msgs) {
			try {
				byte[] byteResp = ext.getBody();
				InvokeResponse resp = JSON.parseObject(byteResp, InvokeResponse.class);
				DefaultPromise<BaseInvokeResult> promise = callerInvokePromiseMap.remove(resp.getReqId());
				if(resp.getReqTime() < BEGIN_EXECUTABLE_TIME) {
					continue;
				}
				if(promise != null) {
					if(resp.getRespExceptionBody() != null) {
						CoreException e = new DefaultCoreException(String.format("%s-%s", resp.getReqId(), resp.getRespExceptionBody())).setCode(resp.getRespExceptionCode());
						promise.setException(e);
					}else {
						byte[] respBody = resp.getRespBody();
						if(respBody != null && respBody.length > 0 && resp.isCompressRespBody()) {
							respBody = UtilAll.uncompress(respBody);
						}
						DefaultInvokeResult ret = new DefaultInvokeResult(respBody);
						ret.setTraceNos(resp.getTraceNos());
						promise.set(ret);
					}
				}else {
					log.trace("promise not found, reqId : {}, respMsgId : {}, respTime : {}, reqTime : {}, diff : {}"
							, resp.getReqId()
							, ext.getMsgId()
							, TimeUtils.yyyyMMddHHmmssSSS(resp.getRespTime())
							, TimeUtils.yyyyMMddHHmmssSSS(resp.getReqTime())
							, resp.getRespTime() - resp.getReqTime());
				}
			}catch (Throwable e) {
				log.error("unknow exception", e);
			}
		}
		return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
	}

}
