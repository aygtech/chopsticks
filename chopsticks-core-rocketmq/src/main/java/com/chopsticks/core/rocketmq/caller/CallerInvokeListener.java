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
import com.chopsticks.core.concurrent.impl.GuavaPromise;
import com.chopsticks.core.concurrent.impl.GuavaTimeoutPromise;
import com.chopsticks.core.rocketmq.caller.impl.DefaultInvokeResult;
import com.chopsticks.core.rocketmq.exception.DefaultCoreException;
import com.chopsticks.core.rocketmq.handler.InvokeResponse;
import com.chopsticks.core.utils.TimeUtils;

class CallerInvokeListener implements MessageListenerConcurrently{
	
	private static final Logger log = LoggerFactory.getLogger(CallerInvokeListener.class);
	
	private Map<String, GuavaTimeoutPromise<BaseInvokeResult>> callerInvokePromiseMap;
	
	CallerInvokeListener(Map<String, GuavaTimeoutPromise<BaseInvokeResult>> callerInvokePromiseMap) {
		this.callerInvokePromiseMap = callerInvokePromiseMap;
	}

	@Override
	public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
		for(MessageExt ext : msgs) {
			try {
				byte[] byteResp = ext.getBody();
				InvokeResponse resp = JSON.parseObject(byteResp, InvokeResponse.class);
				GuavaPromise<BaseInvokeResult> promise = callerInvokePromiseMap.remove(resp.getReqId());
				if(promise != null) {
					if(resp.getRespExceptionBody() != null) {
						promise.setException(new DefaultCoreException(resp.getRespExceptionBody()).setCode(resp.getRespExceptionCode() == 0 ? DefaultCoreException.INVOKE_EXECUTE_ERROR : resp.getRespExceptionCode()));
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
