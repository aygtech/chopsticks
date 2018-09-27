package com.chopsticks.core.rocketmq.handler;

import java.util.List;
import java.util.Map;

import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.message.MessageConst;
import org.apache.rocketmq.common.message.MessageExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.chopsticks.core.rocketmq.Const;
import com.chopsticks.core.rocketmq.exception.HandlerExecuteException;
import com.chopsticks.core.rocketmq.handler.impl.DefaultNoticeContext;
import com.chopsticks.core.rocketmq.handler.impl.DefaultNoticeParams;
import com.google.common.base.Strings;
import com.google.common.collect.Multimap;

public class HandlerNoticeListener extends BaseHandlerListener implements MessageListenerConcurrently {
	
	private static final Logger log = LoggerFactory.getLogger(HandlerNoticeListener.class);
	
	private Multimap<String, String> topicTags;

	public HandlerNoticeListener(Multimap<String, String> topicTags, Map<String, BaseHandler> topicTagHandlers) {
		super(topicTagHandlers);
		this.topicTags = topicTags;
	}

	@Override
	public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
		for(MessageExt ext : msgs) {
			String topic = ext.getProperty(MessageConst.PROPERTY_RETRY_TOPIC);
			String msgId = ext.getMsgId();
			if(Strings.isNullOrEmpty(topic)) {
				topic = ext.getTopic();
			}
			topic = topic.replace(Const.NOTICE_TOPIC_SUFFIX, "");
			if(!topicTags.keySet().contains(topic)) {
				log.warn("cancel consume topic : {}, tag : {}, msgId : {}", topic, ext.getTags(), msgId);
				return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
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
				handler.notice(new DefaultNoticeParams(topic, ext.getTags(), ext.getBody()), new DefaultNoticeContext(msgId, ext.getReconsumeTimes()));
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
