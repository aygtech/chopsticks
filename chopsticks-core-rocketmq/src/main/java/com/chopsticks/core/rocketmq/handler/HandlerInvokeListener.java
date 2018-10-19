package com.chopsticks.core.rocketmq.handler;

import static com.google.common.base.Strings.isNullOrEmpty;

import java.util.List;
import java.util.Map;

import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.chopsticks.core.exception.HandlerExecuteException;
import com.chopsticks.core.handler.HandlerResult;
import com.chopsticks.core.rocketmq.Const;
import com.chopsticks.core.rocketmq.caller.InvokeRequest;
import com.chopsticks.core.rocketmq.handler.impl.DefaultInvokeContext;
import com.chopsticks.core.rocketmq.handler.impl.DefaultInvokeParams;
import com.google.common.base.Throwables;

public class HandlerInvokeListener extends BaseHandlerListener implements MessageListenerConcurrently{
	
	private static final Logger log = LoggerFactory.getLogger(HandlerInvokeListener.class);
	
	private DefaultMQProducer producer;
	
	public HandlerInvokeListener(DefaultMQProducer producer, Map<String, BaseHandler> topicTagHandlers) {
		super(topicTagHandlers);
		this.producer = producer;
	}

	@Override
	public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
		for(MessageExt ext : msgs) {
			InvokeResponse resp = null;
			String topic = ext.getTopic().replace(Const.INVOKE_TOPIC_SUFFIX, "");
			String invokeCmdExtStr = ext.getUserProperty(Const.INVOKE_REQUEST_KEY);
			if(!isNullOrEmpty(invokeCmdExtStr)) {
				InvokeRequest req = JSON.parseObject(invokeCmdExtStr, InvokeRequest.class);
				long now = Const.CLIENT_TIME.getNow();
				if(now > req.getDeadline()) {
					log.warn("timeout, skip invoke process, reqTime : {}, deadline : {}, now : {}, topic : {}, tag : {}"
														, req.getReqTime()
														, req.getDeadline()
														, now
														, topic
														, ext.getTags());
					continue;
				}
				BaseHandler handler = getHandler(topic, ext.getTags());
				if(handler == null) {
					log.error("cannot find handler by invoke, msgId: {}, topic : {}, tag : {}", ext.getMsgId(), topic, ext.getTags());
					continue;
				}
				try {
					HandlerResult handlerResult = handler.invoke(new DefaultInvokeParams(topic, ext.getTags(), ext.getBody()), new DefaultInvokeContext());       
					if(handlerResult != null) {
						resp = new InvokeResponse(req.getReqId(), req.getReqTime(), Const.CLIENT_TIME.getNow(), handlerResult.getBody());
					}
				}catch (Throwable e) {
					if(e instanceof HandlerExecuteException) {
						log.error(e.getMessage(), e);
					}else {
						log.error(String.format("unknow exception, invoke process, msgid : %s, topic : %s, tag : %s", ext.getMsgId(), topic, ext.getTags()), e);
					}
					resp = new InvokeResponse(req.getReqId(), req.getReqTime(), Const.CLIENT_TIME.getNow(), Throwables.getStackTraceAsString(e));
				}
				if(resp != null) {
					now = Const.CLIENT_TIME.getNow();
					if(now > req.getDeadline()) {
						log.warn("timeout, skip invoke process response, reqTime : {}, deadline : {}, now : {}, topic : {}, tag : {}"
															, req.getReqTime()
															, req.getDeadline()
															, now
															, topic
															, ext.getTags());
						continue;
					}
					Message respMsg = new Message(req.getRespTopic(), req.getRespTag(), JSON.toJSONBytes(resp));
					try {
						SendResult ret = producer.send(respMsg);
						if(ret.getSendStatus() == SendStatus.SEND_OK) {
							log.trace("invoke reqId : {}, req msgId : {}, rec msgId : {}", req.getReqId(), ext.getMsgId(), ret.getMsgId());
						}else {
							log.error("rec invoke error : {}", ret.getSendStatus().name());
						}
					}catch (Throwable e) {
						log.error(String.format("unknow exception, invoke end, process send response, msgid : %s, topic : %s, tag : %s", ext.getMsgId(), topic, ext.getTags()), e);
					}
				}
			}
		}
		return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
	}

}
