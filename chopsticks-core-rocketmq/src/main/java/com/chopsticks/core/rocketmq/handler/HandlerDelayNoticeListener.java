package com.chopsticks.core.rocketmq.handler;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageConst;
import org.apache.rocketmq.common.message.MessageExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.chopsticks.common.utils.TimeUtils;
import com.chopsticks.core.exception.CoreException;
import com.chopsticks.core.rocketmq.Const;
import com.chopsticks.core.rocketmq.DefaultClient;
import com.chopsticks.core.rocketmq.caller.DelayNoticeRequest;
import com.chopsticks.core.rocketmq.exception.DefaultCoreException;
import com.chopsticks.core.rocketmq.handler.impl.DefaultNoticeContext;
import com.chopsticks.core.rocketmq.handler.impl.DefaultNoticeParams;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.Multimap;

public class HandlerDelayNoticeListener extends BaseHandlerListener implements MessageListenerConcurrently {
	
	private static final Logger log = LoggerFactory.getLogger(HandlerDelayNoticeListener.class);
	
	private Multimap<String, String> topicTags;
	
	private DefaultMQPushConsumer delayNoticeConsumer;
	
	public HandlerDelayNoticeListener(DefaultClient client, DefaultMQPushConsumer delayNoticeConsumer, Multimap<String, String> topicTags, Map<String, BaseHandler> topicTagHandlers) {
		super(topicTagHandlers, client);
		this.delayNoticeConsumer = delayNoticeConsumer;
		this.topicTags = topicTags;
	}

	@Override
	public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
		for(MessageExt ext : msgs) {
			try {
				return consumeMessage(ext, context);
			}catch (Throwable e) {
				log.error(e.getMessage(), e);
				if(delayNoticeConsumer.getMaxReconsumeTimes() <= ext.getReconsumeTimes()) {
					return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
				}else {
					return ConsumeConcurrentlyStatus.RECONSUME_LATER;
				}
			}
		}
		return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
	}
	
	public ConsumeConcurrentlyStatus consumeMessage(MessageExt ext, ConsumeConcurrentlyContext context) {

		ext.setTags(Const.getOriginTag(getClient().getGroupName(), ext.getTags()));
		String topic = ext.getProperty(MessageConst.PROPERTY_RETRY_TOPIC);
		String msgId = ext.getProperty(MessageConst.PROPERTY_UNIQ_CLIENT_MESSAGE_ID_KEYIDX);
		if(Strings.isNullOrEmpty(topic)) {
			topic = ext.getTopic();
		}
		topic = topic.replace(Const.DELAY_NOTICE_TOPIC_SUFFIX, "");
		if(!topicTags.keySet().contains(topic)) {
			log.warn("cancel consume {}-{}, msgId : {}", topic, ext.getTags(), msgId);
			return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
		}
		
		String dealyNoticeReqStr = ext.getProperty(Const.DELAY_NOTICE_REQUEST_KEY);
		DelayNoticeRequest req = null;
		if(!Strings.isNullOrEmpty(dealyNoticeReqStr)) {
			req = JSON.parseObject(dealyNoticeReqStr, DelayNoticeRequest.class);
			if(req.getReqTime() < getBeginExecutableTime()) {
				log.trace("reqTime < beginExecutableTime, reqTime : {}, beginExecutableTime : {}, msgId : {}"
						, TimeUtils.yyyyMMddHHmmssSSS(req.getReqTime())
						, TimeUtils.yyyyMMddHHmmssSSS(getBeginExecutableTime())
						, msgId);
				return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
			}
			// TODO 等待原有延迟消费完毕 1.0.9
			if(req.getExecuteGroupName() != null) {
				if(!delayNoticeConsumer.getConsumerGroup().equals(req.getExecuteGroupName())) {
					log.trace("cur group is {}, msg group is {}, cancel consumer", delayNoticeConsumer.getConsumerGroup(), req.getExecuteGroupName());
					return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
				}
			}else {
				req.setExecuteGroupName(delayNoticeConsumer.getConsumerGroup());
			}
			if(!Strings.isNullOrEmpty(req.getRootId())) {
				msgId = req.getRootId();
			}
			req.setRootId(msgId);
			long diff = req.getExecuteTime() - Const.CLIENT_TIME.getNow();
			if(diff > 0) {
				Optional<Entry<Long, Integer>> delayLevel = Const.getDelayLevel(diff);
				if(delayLevel.isPresent()) {
					Message msg = new Message(ext.getTopic(), Const.buildCustomTag(getClient().getGroupName(), ext.getTags()), ext.getBody());
					msg.setDelayTimeLevel(delayLevel.get().getValue());
					msg.putUserProperty(Const.DELAY_NOTICE_REQUEST_KEY, JSON.toJSONString(req));
					msg.setKeys(getClient().getGroupName() + req.getRootId());
					try {
						SendResult ret = getClient().getProducer().send(msg);
						if(ret.getSendStatus() != SendStatus.SEND_OK) {
							throw new DefaultCoreException(ret.getSendStatus().name()).setCode(DefaultCoreException.DELAY_NOTICE_EXECUTE_FORWORD_SEND_NOT_OK);
						}else {
							log.trace("rootId : {}, newId : {}, invokeTime : {}, executeTime : {}, next delay(ms) : {}, diff(ms) : {}"
									, msgId
									, ret.getMsgId()
									, TimeUtils.yyyyMMddHHmmssSSS(req.getInvokeTime())
									, TimeUtils.yyyyMMddHHmmssSSS(req.getExecuteTime())
									, String.format("%s-%s", delayLevel.get().getKey(), delayLevel.get().getValue())
									, diff);
							return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
						}
					}catch (Throwable e) {
						throw new DefaultCoreException(e).setCode(CoreException.UNKNOW_EXCEPTION);
					}
				}else {
					log.trace("delayLevel be null, diff : {}", diff);
					if(diff < TimeUnit.SECONDS.toMillis(1)) {
						try {
							TimeUnit.MILLISECONDS.sleep(diff);	
						}catch (Throwable e) {
							log.error(e.getMessage(), e);
						}
					}
				}
			}
		}
		
		BaseHandler handler = getHandler(topic, ext.getTags());
		if(handler == null) {
			throw new DefaultCoreException(String.format("%s-%s cannot find handler by notice, reconsumeTimes : %s, msgId: %s"
					, topic
					, ext.getTags()
					, ext.getReconsumeTimes()
					, msgId)).setCode(DefaultCoreException.CANNOT_FIND_DELAY_NOTICE_HANDLER);
		}
		try {
			DefaultNoticeParams params = new DefaultNoticeParams(topic, ext.getTags(), ext.getBody());
			DefaultNoticeContext ctx = new DefaultNoticeContext(msgId
															, ext.getMsgId()
															, ext.getReconsumeTimes()
															, delayNoticeConsumer.getMaxReconsumeTimes() <= ext.getReconsumeTimes()
															, false
															, null
															, true);
			if(req != null) {
				ctx.setReqTime(req.getReqTime());
				ctx.setExtParams(req.getExtParams());
				ctx.setTraceNos(req.getTraceNos());
			}
			handler.notice(params, ctx);
			return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
		}catch (DefaultCoreException e) {
			throw e;
		}catch (Throwable e) {
			throw new DefaultCoreException(String.format("%s-%s handler notice execute exception, reconsumeTimes : %s, msgid : %s"
									, topic
									, ext.getTags()				
									, ext.getReconsumeTimes()
									, msgId)
					, e).setCode(CoreException.UNKNOW_EXCEPTION);
		}
	
	}

}
