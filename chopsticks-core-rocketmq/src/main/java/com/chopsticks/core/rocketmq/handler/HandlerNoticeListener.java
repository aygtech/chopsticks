package com.chopsticks.core.rocketmq.handler;

import java.util.List;
import java.util.Map;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.message.MessageConst;
import org.apache.rocketmq.common.message.MessageExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.chopsticks.common.utils.TimeUtils;
import com.chopsticks.core.exception.CoreException;
import com.chopsticks.core.rocketmq.Const;
import com.chopsticks.core.rocketmq.DefaultClient;
import com.chopsticks.core.rocketmq.caller.NoticeRequest;
import com.chopsticks.core.rocketmq.exception.DefaultCoreException;
import com.chopsticks.core.rocketmq.handler.impl.DefaultNoticeContext;
import com.chopsticks.core.rocketmq.handler.impl.DefaultNoticeParams;
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
			try {
				return consumeMessage(ext, context);
			}catch (CoreException e) {
				log.error(e.getMessage(), e);
				return ConsumeConcurrentlyStatus.RECONSUME_LATER;
			}catch (Throwable e) {
				log.error(e.getMessage(), e);
				return ConsumeConcurrentlyStatus.RECONSUME_LATER;
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
		topic = topic.replace(Const.NOTICE_TOPIC_SUFFIX, "");
		if(!topicTags.keySet().contains(topic)) {
			log.warn("cancel consume topic : {}, tag : {}, msgId : {}", topic, ext.getTags(), msgId);
			return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
		}
		
		String noticeReqStr = ext.getProperty(Const.NOTICE_REQUEST_KEY);
		NoticeRequest req = null;
		if(!Strings.isNullOrEmpty(noticeReqStr)) {
			req = JSON.parseObject(noticeReqStr, NoticeRequest.class);
			if(req.getReqTime() < getBeginExecutableTime()) {
				log.trace("reqTime < beginExecutableTime, reqTime : {}, beginExecutableTime : {}, msgId : {}"
						, TimeUtils.yyyyMMddHHmmssSSS(req.getReqTime())
						, TimeUtils.yyyyMMddHHmmssSSS(getBeginExecutableTime())
						, msgId);
				return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
			}
		}
		
		BaseHandler handler = getHandler(topic, ext.getTags());
		if(handler == null) {
			throw new DefaultCoreException(String.format("%s-%s cannot find handler by notice, reconsumeTimes : %s, msgId: %s"
					, topic
					, ext.getTags()
					, ext.getReconsumeTimes()
					, msgId)).setCode(DefaultCoreException.CANNOT_FIND_NOTICE_HANDLER);
		}
		try {
			DefaultNoticeParams params = new DefaultNoticeParams(topic, ext.getTags(), ext.getBody());
			DefaultNoticeContext ctx = new DefaultNoticeContext(msgId
															, ext.getMsgId()
															, ext.getReconsumeTimes()
															, noticeConsumer.getMaxReconsumeTimes() <= ext.getReconsumeTimes()
															, false
															, null
															, false);
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
			throw new DefaultCoreException(String.format("%s-%s unknow handler notice execute exception, reconsumeTimes : %s, msgid : %s"
									, topic
									, ext.getTags()
									, ext.getReconsumeTimes()
									, msgId)
					, e).setCode(CoreException.UNKNOW_EXCEPTION);
		}
	
	}

}
