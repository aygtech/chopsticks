package com.chopsticks.core.rocketmq.handler;

import java.util.List;
import java.util.Map;

import org.apache.rocketmq.client.consumer.listener.ConsumeOrderlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeOrderlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerOrderly;
import org.apache.rocketmq.common.message.MessageConst;
import org.apache.rocketmq.common.message.MessageExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.chopsticks.core.exception.HandlerExecuteException;
import com.chopsticks.core.rocketmq.Const;
import com.chopsticks.core.rocketmq.handler.impl.DefaultNoticeContext;
import com.chopsticks.core.rocketmq.handler.impl.DefaultNoticeParams;
import com.google.common.base.Strings;
import com.google.common.collect.Multimap;

public class HandlerOrderedNoticeListener extends BaseHandlerListener implements MessageListenerOrderly {
	
	private static final Logger log = LoggerFactory.getLogger(HandlerOrderedNoticeListener.class);

	private Multimap<String, String> topicTags;
	
	public HandlerOrderedNoticeListener(String groupName, Multimap<String, String> topicTags, Map<String, BaseHandler> topicTagHandlers) {
		super(topicTagHandlers, groupName);
		this.topicTags = topicTags;
	}

	@Override
	public ConsumeOrderlyStatus consumeMessage(List<MessageExt> msgs, ConsumeOrderlyContext context) {
		for(MessageExt ext : msgs) {
			String topic = ext.getProperty(MessageConst.PROPERTY_RETRY_TOPIC);
			String msgId = ext.getProperty(MessageConst.PROPERTY_UNIQ_CLIENT_MESSAGE_ID_KEYIDX);
			if(Strings.isNullOrEmpty(topic)) {
				topic = ext.getTopic();
			}else {
				msgId = ext.getProperty(MessageConst.PROPERTY_ORIGIN_MESSAGE_ID);
			}
			topic = topic.replace(Const.ORDERED_NOTICE_TOPIC_SUFFIX, "");
			if(!topicTags.keySet().contains(topic)) {
				log.warn("cancel consume topic : {}, tag : {}, msgId : {}", topic, ext.getTags(), msgId);
				return ConsumeOrderlyStatus.SUCCESS;
			}
			BaseHandler handler = getHandler(topic, ext.getTags());
			if(handler == null) {
				log.error("cannot find handler by orderedNotice, reconsumeTimes : {}, msgId: {}, topic : {}, tag : {}"
						, ext.getReconsumeTimes()
						, msgId
						, topic
						, ext.getTags());
				return ConsumeOrderlyStatus.SUSPEND_CURRENT_QUEUE_A_MOMENT;
			}
			try {
				DefaultNoticeParams params = new DefaultNoticeParams(topic, ext.getTags(), ext.getBody());
				DefaultNoticeContext ctx = new DefaultNoticeContext(msgId, ext.getMsgId(), ext.getReconsumeTimes(), true);
				handler.notice(params, ctx);
			}catch (HandlerExecuteException e) {
				log.error(e.getMessage(), e);
				return ConsumeOrderlyStatus.SUSPEND_CURRENT_QUEUE_A_MOMENT;
			}catch (Throwable e) {
				log.error(String.format("orderedNotice process exception, reconsumeTimes : %s, msgid : %s, topic : %s, tag : %s"
						, ext.getReconsumeTimes()
						, msgId
						, topic
						, ext.getTags()), e);
				return ConsumeOrderlyStatus.SUSPEND_CURRENT_QUEUE_A_MOMENT;
			}
		}
		return ConsumeOrderlyStatus.SUCCESS;
	}

}
