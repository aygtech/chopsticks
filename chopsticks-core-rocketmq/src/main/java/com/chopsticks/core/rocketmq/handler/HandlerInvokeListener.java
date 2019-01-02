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
					byte[] body = ext.getBody();
					if(req.isCompress()) {
						body = UtilAll.uncompress(body);
					}
					HandlerResult handlerResult = handler.invoke(new DefaultInvokeParams(topic, ext.getTags(), body), new DefaultInvokeContext());       
					if(handlerResult != null) {
						resp = new InvokeResponse(req.getReqId(), req.getReqTime(), Const.CLIENT_TIME.getNow(), handlerResult.getBody());
					}
				}catch (DefaultCoreException e) {
					log.error(e.getMessage(), e);
					resp = new InvokeResponse(req.getReqId(), req.getReqTime(), Const.CLIENT_TIME.getNow(), Throwables.getStackTraceAsString(e));
				}catch (Throwable e) {
					log.error(String.format("unknow exception, invoke process, msgid : %s, topic : %s, tag : %s", ext.getMsgId(), topic, ext.getTags()), e);
					resp = new InvokeResponse(req.getReqId(), req.getReqTime(), Const.CLIENT_TIME.getNow(), Throwables.getStackTraceAsString(e));
				}
				if(!Strings.isNullOrEmpty(req.getRespTopic()) && resp != null) {
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
						SendResult ret = getClient().getProducer().send(respMsg);
						if(ret.getSendStatus() == SendStatus.SEND_OK) {
							log.trace("invoke tag : {}, reqId : {}, msgId : {}, rec msgId : {}", ext.getTags(), req.getReqId(), ext.getMsgId(), ret.getMsgId());
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
