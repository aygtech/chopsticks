package com.chopsticks.core.rocketmq.handler;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageConst;
import org.apache.rocketmq.common.message.MessageExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.chopsticks.core.exception.HandlerExecuteException;
import com.chopsticks.core.rocketmq.Const;
import com.chopsticks.core.rocketmq.caller.DelayNoticeRequest;
import com.chopsticks.core.rocketmq.handler.impl.DefaultNoticeContext;
import com.chopsticks.core.rocketmq.handler.impl.DefaultNoticeParams;
import com.chopsticks.core.utils.TimeUtils;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.Multimap;

public class HandlerDelayNoticeListener extends BaseHandlerListener implements MessageListenerConcurrently {
	
	private static final Logger log = LoggerFactory.getLogger(HandlerDelayNoticeListener.class);
	
	private Multimap<String, String> topicTags;
	
	private DefaultMQProducer producer;
	private DefaultMQPushConsumer consumer;
	
	public HandlerDelayNoticeListener(String groupName, DefaultMQPushConsumer consumer, DefaultMQProducer producer, Multimap<String, String> topicTags, Map<String, BaseHandler> topicTagHandlers) {
		super(topicTagHandlers, groupName);
		this.consumer = consumer;
		this.producer = producer;
		this.topicTags = topicTags;
	}

	@Override
	public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
		for(MessageExt ext : msgs) {
			ext.setTags(Const.getOriginTag(getGroupName(), ext.getTags()));
			String topic = ext.getProperty(MessageConst.PROPERTY_RETRY_TOPIC);
			String msgId = ext.getProperty(MessageConst.PROPERTY_UNIQ_CLIENT_MESSAGE_ID_KEYIDX);
			if(Strings.isNullOrEmpty(topic)) {
				topic = ext.getTopic();
			}
			topic = topic.replace(Const.DELAY_NOTICE_TOPIC_SUFFIX, "");
			if(!topicTags.keySet().contains(topic)) {
				log.warn("cancel consume topic : {}, tag : {}, msgId : {}", topic, ext.getTags(), msgId);
				return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
			}
			
			String noticeDealyReqStr = ext.getProperty(Const.DELAY_NOTICE_REQUEST_KEY);
			if(!Strings.isNullOrEmpty(noticeDealyReqStr)) {
				DelayNoticeRequest req = JSON.parseObject(noticeDealyReqStr, DelayNoticeRequest.class);
				// TODO 等待原有延迟消费完毕 1.0.9
				if(req.getExecuteGroupName() != null) {
					if(!consumer.getConsumerGroup().equals(req.getExecuteGroupName())) {
						log.trace("cur group is {}, msg group is {}, cancel consumer", consumer.getConsumerGroup(), req.getExecuteGroupName());
						return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
					}
				}else {
					req.setExecuteGroupName(consumer.getConsumerGroup());
				}
				if(!Strings.isNullOrEmpty(req.getRootId())) {
					msgId = req.getRootId();
				}
				req.setRootId(msgId);
				long diff = req.getExecuteTime() - Const.CLIENT_TIME.getNow();
				if(diff > 0) {
					Optional<Entry<Long, Integer>> delayLevel = Const.getDelayLevel(diff);
					if(delayLevel.isPresent()) {
						Message msg = new Message(ext.getTopic(), Const.buildCustomTag(getGroupName(), ext.getTags()), ext.getBody());
						msg.setDelayTimeLevel(delayLevel.get().getValue());
						msg.putUserProperty(Const.DELAY_NOTICE_REQUEST_KEY, JSON.toJSONString(req));
						msg.setKeys(req.getRootId());
						try {
							SendResult ret = producer.send(msg);
							if(ret.getSendStatus() != SendStatus.SEND_OK) {
								throw new HandlerExecuteException(ret.getSendStatus().name());
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
							log.error(e.getMessage(), e);
							return ConsumeConcurrentlyStatus.RECONSUME_LATER;
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
				log.error("cannot find handler by notice, reconsumeTimes : {}, msgId: {}, topic : {}, tag : {}"
						, ext.getReconsumeTimes()
						, msgId
						, topic
						, ext.getTags());
				return ConsumeConcurrentlyStatus.RECONSUME_LATER;
			}
			try {
				DefaultNoticeParams params = new DefaultNoticeParams(topic, ext.getTags(), ext.getBody());
				DefaultNoticeContext ctx = new DefaultNoticeContext(msgId, ext.getMsgId(), ext.getReconsumeTimes());
				handler.notice(params, ctx);
			}catch (HandlerExecuteException e) {
				log.error(e.getMessage(), e);
				return ConsumeConcurrentlyStatus.RECONSUME_LATER;
			}catch (Throwable e) {
				log.error(String.format("handler notice execute exception, reconsumeTimes : %s, msgid : %s, topic : %s, tag : %s"
										, ext.getReconsumeTimes()
										, msgId
										, topic
										, ext.getTags())
						, e);
				return ConsumeConcurrentlyStatus.RECONSUME_LATER;
			}
		}
		return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
	}

}
