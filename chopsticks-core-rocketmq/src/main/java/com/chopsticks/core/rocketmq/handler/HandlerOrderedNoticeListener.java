package com.chopsticks.core.rocketmq.handler;

import java.util.List;
import java.util.Map;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeOrderlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeOrderlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerOrderly;
import org.apache.rocketmq.common.message.MessageConst;
import org.apache.rocketmq.common.message.MessageExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.chopsticks.common.utils.TimeUtils;
import com.chopsticks.core.exception.CoreException;
import com.chopsticks.core.rocketmq.Const;
import com.chopsticks.core.rocketmq.DefaultClient;
import com.chopsticks.core.rocketmq.caller.OrderedNoticeRequest;
import com.chopsticks.core.rocketmq.exception.DefaultCoreException;
import com.chopsticks.core.rocketmq.handler.impl.DefaultNoticeContext;
import com.chopsticks.core.rocketmq.handler.impl.DefaultNoticeParams;
import com.google.common.base.Strings;
import com.google.common.collect.Multimap;

public class HandlerOrderedNoticeListener extends BaseHandlerListener implements MessageListenerOrderly {
	
	private static final Logger log = LoggerFactory.getLogger(HandlerOrderedNoticeListener.class);

	private Multimap<String, String> topicTags;
	private DefaultMQPushConsumer orderedNoticeConsumer;
	
	public HandlerOrderedNoticeListener(DefaultClient client, DefaultMQPushConsumer orderedNoticeConsumer, Multimap<String, String> topicTags, Map<String, BaseHandler> topicTagHandlers) {
		super(topicTagHandlers, client);
		this.orderedNoticeConsumer = orderedNoticeConsumer;
		this.topicTags = topicTags;
	}

	@Override
	public ConsumeOrderlyStatus consumeMessage(List<MessageExt> msgs, ConsumeOrderlyContext context) {
		for(MessageExt ext : msgs) {
			try {
				if(orderedNoticeConsumer.getMaxReconsumeTimes() < ext.getReconsumeTimes()) {
					log.warn("retryCount {} < realRetryCount {}"
							, orderedNoticeConsumer.getMaxReconsumeTimes()
							, ext.getReconsumeTimes());
					return ConsumeOrderlyStatus.SUCCESS;
				}
				return consumeMessage(ext, context);
			}catch (Throwable e) {
				log.error(e.getMessage(), e);
				if(orderedNoticeConsumer.getMaxReconsumeTimes() <= ext.getReconsumeTimes()) {
					return ConsumeOrderlyStatus.SUCCESS;
				}else {
					return ConsumeOrderlyStatus.SUSPEND_CURRENT_QUEUE_A_MOMENT;
				}
			}
		}
		return ConsumeOrderlyStatus.SUCCESS;
	}
	
	private ConsumeOrderlyStatus consumeMessage(MessageExt ext, ConsumeOrderlyContext context) {
		String topic = ext.getProperty(MessageConst.PROPERTY_RETRY_TOPIC);
		String msgId = ext.getProperty(MessageConst.PROPERTY_UNIQ_CLIENT_MESSAGE_ID_KEYIDX);
		if(Strings.isNullOrEmpty(topic)) {
			topic = ext.getTopic();
		}else {
			msgId = ext.getProperty(MessageConst.PROPERTY_ORIGIN_MESSAGE_ID);
		}
		topic = topic.replace(Const.ORDERED_NOTICE_TOPIC_SUFFIX, "");
		String orderedNoticeReqStr = ext.getProperty(Const.ORDERED_NOTICE_REQUEST_KEY);
		OrderedNoticeRequest req = null;
		if(!Strings.isNullOrEmpty(orderedNoticeReqStr)) {
			req = JSON.parseObject(orderedNoticeReqStr, OrderedNoticeRequest.class);
			if(req.getReqTime() < getBeginExecutableTime()) {
				log.trace("reqTime < beginExecutableTime, reqTime : {}, beginExecutableTime : {}, msgId : {}"
						, TimeUtils.yyyyMMddHHmmssSSS(req.getReqTime())
						, TimeUtils.yyyyMMddHHmmssSSS(getBeginExecutableTime())
						, msgId);
				return ConsumeOrderlyStatus.SUCCESS;
			}
		}
		if(!topicTags.keySet().contains(topic)) {
			log.warn("cancel consume topic : {}, tag : {}, msgId : {}", topic, ext.getTags(), msgId);
			return ConsumeOrderlyStatus.SUCCESS;
		}
		BaseHandler handler = getHandler(topic, ext.getTags());
		if(handler == null) {
			throw new DefaultCoreException(String.format("%s-%s cannot find handler by orderedNotice, reconsumeTimes : %s, msgId: %s"
					, topic
					, ext.getTags()
					, ext.getReconsumeTimes()
					, msgId)).setCode(DefaultCoreException.CANNOT_FIND_ORDERED_NOTICE_HANDLER);
		}
		try {
			DefaultNoticeParams params = new DefaultNoticeParams(topic, ext.getTags(), ext.getBody());
			DefaultNoticeContext ctx = new DefaultNoticeContext(msgId
															, ext.getMsgId()
															, ext.getReconsumeTimes()
															, orderedNoticeConsumer.getMaxReconsumeTimes() <= ext.getReconsumeTimes()
															, true
															, req != null ? req.getOrderKey() : null
															, false);
			if(req != null) {
				ctx.setReqTime(req.getReqTime());
				ctx.setExtParams(req.getExtParams());
				ctx.setTraceNos(req.getTraceNos());
			}
			handler.notice(params, ctx);
			return ConsumeOrderlyStatus.SUCCESS;
		}catch (DefaultCoreException e) {
			throw e;
		}catch (Throwable e) {
			throw new DefaultCoreException(String.format("%s-%s unknow orderedNotice execute exception, reconsumeTimes : %s, msgid : %s"
											, topic
											, ext.getTags()
											, ext.getReconsumeTimes()
											, msgId)
					, e).setCode(CoreException.UNKNOW_EXCEPTION);
		} 
	}
}
