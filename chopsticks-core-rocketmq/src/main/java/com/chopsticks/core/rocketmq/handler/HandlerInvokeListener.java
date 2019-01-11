package com.chopsticks.core.rocketmq.handler;

import static com.google.common.base.Strings.isNullOrEmpty;

import java.util.List;
import java.util.Map;

import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.common.UtilAll;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.chopsticks.core.exception.CoreException;
import com.chopsticks.core.handler.HandlerResult;
import com.chopsticks.core.rocketmq.Const;
import com.chopsticks.core.rocketmq.DefaultClient;
import com.chopsticks.core.rocketmq.caller.InvokeRequest;
import com.chopsticks.core.rocketmq.exception.DefaultCoreException;
import com.chopsticks.core.rocketmq.handler.impl.DefaultInvokeContext;
import com.chopsticks.core.rocketmq.handler.impl.DefaultInvokeParams;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;

public class HandlerInvokeListener extends BaseHandlerListener implements MessageListenerConcurrently{
	
	private static final Logger log = LoggerFactory.getLogger(HandlerInvokeListener.class);
	
	
	public HandlerInvokeListener(DefaultClient client, Map<String, BaseHandler> topicTagHandlers) {
		super(topicTagHandlers, client);
	}

	@Override
	public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
		for(MessageExt ext : msgs) {
			try {
				return consumeMessage(ext, context);
			}catch (CoreException e) {
				log.error(e.getMessage(), e);
				return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
			}catch (Throwable e) {
				log.error(e.getMessage(), e);
				return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
			}
		}
		return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
	}
	
	public ConsumeConcurrentlyStatus consumeMessage(MessageExt ext, ConsumeConcurrentlyContext context) {
		InvokeResponse resp = null;
		String topic = ext.getTopic().replace(Const.INVOKE_TOPIC_SUFFIX, "");
		String invokeCmdExtStr = ext.getUserProperty(Const.INVOKE_REQUEST_KEY);
		if(!isNullOrEmpty(invokeCmdExtStr)) {
			InvokeRequest req = JSON.parseObject(invokeCmdExtStr, InvokeRequest.class);
			long now = Const.CLIENT_TIME.getNow();
			if(now > req.getDeadline()) {
				throw new DefaultCoreException(String.format("timeout, skip invoke process, reqTime : %s, deadline : %s, now : %s, topic : %s, tag : %s"
													, req.getReqTime()
													, req.getDeadline()
													, now
													, topic
													, ext.getTags())).setCode(DefaultCoreException.INVOKE_BEFORE_PROCESS_TIMEOUT);
			}
			BaseHandler handler = getHandler(topic, ext.getTags());
			if(handler == null) {
				throw new DefaultCoreException(String.format("cannot find handler by invoke, msgId: %s, topic : %s, tag : %s"
											, ext.getMsgId()
											, topic
											, ext.getTags())).setCode(DefaultCoreException.CANNOT_FIND_INVOKE_HANDLER);
			}
			Throwable tmp = null;
			try {
				byte[] body = ext.getBody();
				if(req.isCompress()) {
					body = UtilAll.uncompress(body);
				}
				DefaultInvokeContext ctx = new DefaultInvokeContext();
				ctx.setExtParams(req.getExtParams());
				ctx.setTraceNos(req.getTraceNos());
				HandlerResult handlerResult = handler.invoke(new DefaultInvokeParams(topic, ext.getTags(), body), ctx);       
				if(handlerResult != null) {
					resp = new InvokeResponse(req.getReqId(), req.getReqTime(), Const.CLIENT_TIME.getNow(), handlerResult.getBody());
					resp.setTraceNos(req.getTraceNos());
				}
			}catch (DefaultCoreException e) {
				tmp = e;
				resp = new InvokeResponse(req.getReqId(), req.getReqTime(), Const.CLIENT_TIME.getNow(), Throwables.getStackTraceAsString(tmp));
			}catch (Throwable e) {
				tmp = new DefaultCoreException(String.format("unknow exception, invoke process, msgid : %s, topic : %s, tag : %s", ext.getMsgId(), topic, ext.getTags()), e).setCode(CoreException.UNKNOW_EXCEPTION);
				resp = new InvokeResponse(req.getReqId(), req.getReqTime(), Const.CLIENT_TIME.getNow(), Throwables.getStackTraceAsString(tmp));
			}
			if(!Strings.isNullOrEmpty(req.getRespTopic()) && resp != null) {
				now = Const.CLIENT_TIME.getNow();
				if(now > req.getDeadline()) {
					throw new DefaultCoreException(String.format("timeout, skip invoke process response, reqTime : %s, deadline : %s, now : %s, topic : %s, tag : %s"
														, req.getReqTime()
														, req.getDeadline()
														, now
														, topic
														, ext.getTags())).setCode(DefaultCoreException.INVOKE_PROCESS_TIMEOUT);
				}
				Message respMsg = new Message(req.getRespTopic(), req.getRespTag(), JSON.toJSONBytes(resp));
				try {
					SendResult ret = getClient().getProducer().send(respMsg);
					if(ret.getSendStatus() == SendStatus.SEND_OK) {
						log.trace("invoke tag : {}, reqId : {}, msgId : {}, rec msgId : {}", ext.getTags(), req.getReqId(), ext.getMsgId(), ret.getMsgId());
						if(tmp == null) {
							return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
						}else {
							throw tmp;
						}
					}else {
						throw new DefaultCoreException(String.format("rec invoke error : %s", ret.getSendStatus().name())).setCode(DefaultCoreException.INVOKE_REC_ERROR);
					}
				}catch (CoreException e) {
					throw e;
				}catch (Throwable e) {
					throw new DefaultCoreException(String.format("unknow exception, invoke end, process send response, msgid : %s, topic : %s, tag : %s"
															, ext.getMsgId()
															, topic
															, ext.getTags())
												, e).setCode(CoreException.UNKNOW_EXCEPTION);
				}
			}else {
				return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
			}
		}else {
			throw new DefaultCoreException(String.format("InvokeRequest can not be null, msgid : %s", ext.getMsgId())).setCode(DefaultCoreException.INVOKE_REQUEST_NULL);
		}
	}

}
