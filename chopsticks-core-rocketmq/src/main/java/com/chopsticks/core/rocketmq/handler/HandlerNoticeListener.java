package com.chopsticks.core.rocketmq.handler;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
import com.chopsticks.core.rocketmq.Const;
import com.chopsticks.core.rocketmq.DefaultClient;
import com.chopsticks.core.rocketmq.caller.DelayNoticeRequest;
import com.chopsticks.core.rocketmq.exception.DefaultCoreException;
import com.chopsticks.core.rocketmq.handler.impl.DefaultNoticeContext;
import com.chopsticks.core.rocketmq.handler.impl.DefaultNoticeParams;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.Multimap;

public class HandlerNoticeListener extends BaseHandlerListener implements MessageListenerConcurrently {
	
	private static final Logger log = LoggerFactory.getLogger(HandlerNoticeListener.class);
	
	private Multimap<String, String> topicTags;
	
	private DefaultMQPushConsumer noticeConsumer;

	public HandlerNoticeListener(DefaultClient client, DefaultMQPushConsumer noticeConsumer, Multimap<String, String> topicTags, Map<String, BaseHandler> topicTagHandlers) {
		super(topicTagHandlers, client);
		this.noticeConsumer = noticeConsumer;
		this.topicTags = topicTags;
	}

	@Override
	public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
		for(MessageExt ext : msgs) {
			ext.setTags(Const.getOriginTag(getClient().getGroupName(), ext.getTags()));
			String topic = ext.getProperty(MessageConst.PROPERTY_RETRY_TOPIC);
			String msgId = ext.getProperty(MessageConst.PROPERTY_UNIQ_CLIENT_MESSAGE_ID_KEYIDX);
			if(Strings.isNullOrEmpty(topic)) {
				topic = ext.getTopic();
			}
			topic = topic.replace(Const.NOTICE_TOPIC_SUFFIX, "");
			if(!topicTags.keySet().contains(topic)) {
				log.warn("cancel consume topic : {}, tag : {}, msgId : {}", topic, ext.getTags(), msgId);
				return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
			}
			
			String noticeDealyReqStr = ext.getProperty(Const.DELAY_NOTICE_REQUEST_KEY);
			DelayNoticeRequest req = null;
			if(!Strings.isNullOrEmpty(noticeDealyReqStr)) {
				req = JSON.parseObject(noticeDealyReqStr, DelayNoticeRequest.class);
				// TODO 等待原有延迟消费完毕 1.0.9
				if(req.getExecuteGroupName() != null) {
					if(!noticeConsumer.getConsumerGroup().equals(req.getExecuteGroupName())) {
						log.trace("cur group is {}, msg group is {}, cancel consumer", noticeConsumer.getConsumerGroup(), req.getExecuteGroupName());
						return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
					}
				}else {
					req.setExecuteGroupName(noticeConsumer.getConsumerGroup());
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
						msg.setKeys(req.getRootId());
						try {
							SendResult ret = getClient().getProducer().send(msg);
							if(ret.getSendStatus() != SendStatus.SEND_OK) {
								throw new DefaultCoreException(ret.getSendStatus().name()).setCode(DefaultCoreException.NOTICE_EXECUTE_FORWORD_SEND_NOT_OK);
							}else {
								log.trace("rootId : {}, newId : {}, next delay(ms) : {}, diff(ms) : {}", msgId, ret.getMsgId(), delayLevel.get().getKey(), diff);
								return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
							}
						}catch (Throwable e) {
							log.error(e.getMessage(), e);
							return ConsumeConcurrentlyStatus.RECONSUME_LATER;
						}
					}else {
						log.trace("delayLevel be null, diff : {}", diff);
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
				DefaultNoticeContext ctx = new DefaultNoticeContext(msgId, ext.getMsgId(), ext.getReconsumeTimes(), noticeConsumer.getMaxReconsumeTimes() >= ext.getReconsumeTimes(), false, false);
				if(req != null) {
					ctx.setExtParams(req.getExtParams());
				}
				handler.notice(params, ctx);
			}catch (DefaultCoreException e) {
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
