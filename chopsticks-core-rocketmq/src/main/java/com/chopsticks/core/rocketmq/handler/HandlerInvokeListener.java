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
import com.chopsticks.common.concurrent.Promise;
import com.chopsticks.common.concurrent.PromiseListener;
import com.chopsticks.common.utils.TimeUtils;
import com.chopsticks.core.exception.CoreException;
import com.chopsticks.core.handler.HandlerResult;
import com.chopsticks.core.rocketmq.Const;
import com.chopsticks.core.rocketmq.DefaultClient;
import com.chopsticks.core.rocketmq.caller.InvokeRequest;
import com.chopsticks.core.rocketmq.exception.DefaultCoreException;
import com.chopsticks.core.rocketmq.handler.impl.DefaultInvokeContext;
import com.chopsticks.core.rocketmq.handler.impl.DefaultInvokeParams;
import com.google.common.base.Strings;

public class HandlerInvokeListener extends BaseHandlerListener implements MessageListenerConcurrently{
	
	private static final Logger log = LoggerFactory.getLogger(HandlerInvokeListener.class);
//	private static final long DEFAULT_INVOKE_RESP_COMPRESS_BODY_LENGTH = 1024 * 100;
	
	
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
	
	public ConsumeConcurrentlyStatus consumeMessage(final MessageExt ext, ConsumeConcurrentlyContext context) {
		InvokeResponse resp = null;
		final String topic = ext.getTopic().replace(Const.INVOKE_TOPIC_SUFFIX, "");
		String invokeCmdExtStr = ext.getUserProperty(Const.INVOKE_REQUEST_KEY);
		if(!isNullOrEmpty(invokeCmdExtStr)) {
			final InvokeRequest req = JSON.parseObject(invokeCmdExtStr, InvokeRequest.class);
			if(req.getReqTime() < getBeginExecutableTime()) {
				log.trace("reqTime < beginExecutableTime, reqTime : {}, beginExecutableTime : {}, reqId : {}"
						, TimeUtils.yyyyMMddHHmmssSSS(req.getReqTime())
						, TimeUtils.yyyyMMddHHmmssSSS(getBeginExecutableTime())
						, req.getReqId());
				return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
			}
			final long now = Const.CLIENT_TIME.getNow();
			if(now > req.getDeadline()) {
//				throw new DefaultCoreException(String.format("timeout, %s-%s skip invoke process, reqTime : %s, deadline : %s, now : %s, queue : %s"
//													, topic
//													, ext.getTags()
//													, TimeUtils.yyyyMMddHHmmssSSS(req.getReqTime())
//													, TimeUtils.yyyyMMddHHmmssSSS(req.getDeadline())
//													, TimeUtils.yyyyMMddHHmmssSSS(now)
//													, ext.getQueueId())).setCode(DefaultCoreException.INVOKE_BEFORE_PROCESS_TIMEOUT);
				log.warn("timeout, {}-{} skip invoke process, reqTime : {}, deadline : {}, now : {}, queue : {}"
													, topic
													, ext.getTags()
													, TimeUtils.yyyyMMddHHmmssSSS(req.getReqTime())
													, TimeUtils.yyyyMMddHHmmssSSS(req.getDeadline())
													, TimeUtils.yyyyMMddHHmmssSSS(now)
													, ext.getQueueId());
				return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
			}
			BaseHandler handler = getHandler(topic, ext.getTags());
			if(handler == null) {
				throw new DefaultCoreException(String.format("cannot find handler by invoke, msgId: %s, %s-%s"
											, ext.getMsgId()
											, topic
											, ext.getTags())).setCode(DefaultCoreException.CANNOT_FIND_INVOKE_HANDLER);
			}
			CoreException tmp = null;
			try {
				byte[] body = ext.getBody();
				if(req.isCompress()) {
					body = UtilAll.uncompress(body);
				}
				DefaultInvokeContext ctx = new DefaultInvokeContext();
				ctx.setReqTime(req.getReqTime());
				ctx.setExtParams(req.getExtParams());
				ctx.setTraceNos(req.getTraceNos());
				HandlerResult handlerResult = handler.invoke(new DefaultInvokeParams(topic, ext.getTags(), body), ctx);  
				if(handlerResult != null) {
					Promise<HandlerResult> primise = handlerResult.getPromise();
					if(primise != null) {
						//once listener
						addListener(ext, topic, req, now, primise);
						return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
					}else {
						resp = new InvokeResponse(req.getReqId(), req.getReqTime(), Const.CLIENT_TIME.getNow(), handlerResult.getBody());
						resp.setTraceNos(req.getTraceNos());
					}
				}
			}catch (CoreException e) {
				tmp = e;
				resp = new InvokeResponse(req.getReqId(), req.getReqTime(), Const.CLIENT_TIME.getNow(), tmp.getOriMessage());
				resp.setRespExceptionCode(tmp.getCode());
			}catch (Throwable e) {
				tmp = new DefaultCoreException(String.format("unknow exception, invoke process, msgid : %s, %s-%s", ext.getMsgId(), topic, ext.getTags()), e).setCode(CoreException.UNKNOW_EXCEPTION);
				resp = new InvokeResponse(req.getReqId(), req.getReqTime(), Const.CLIENT_TIME.getNow(), tmp.getMessage());
				resp.setRespExceptionCode(tmp.getCode());
			}
			if(!Strings.isNullOrEmpty(req.getRespTopic()) && resp != null) {
				return sendRespMsg(ext, resp, topic, req, now, tmp);
			}else {
				if(tmp == null) {
					return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
				}else {
					throw tmp;
				}
			}
		}else {
			throw new DefaultCoreException(String.format("InvokeRequest can not be null, msgid : %s", ext.getMsgId())).setCode(DefaultCoreException.INVOKE_REQUEST_NULL);
		}
	}

	private void addListener(final MessageExt ext, final String topic, final InvokeRequest req, final long now,
			Promise<HandlerResult> primise) {
		primise.addListener(new PromiseListener<HandlerResult>() {
			@Override
			public void onFailure(Throwable t) {
				CoreException tmp = null;
				InvokeResponse resp = null;
				if(t instanceof CoreException) {
					tmp = (CoreException)t;
					resp = new InvokeResponse(req.getReqId(), req.getReqTime(), Const.CLIENT_TIME.getNow(), tmp.getOriMessage());
					resp.setRespExceptionCode(tmp.getCode());
				}else {
					tmp = new DefaultCoreException(String.format("unknow exception, invoke process, msgid : %s, %s-%s", ext.getMsgId(), topic, ext.getTags()), t).setCode(CoreException.UNKNOW_EXCEPTION);
					resp = new InvokeResponse(req.getReqId(), req.getReqTime(), Const.CLIENT_TIME.getNow(), tmp.getMessage());
					resp.setRespExceptionCode(tmp.getCode());
				}
				if(!Strings.isNullOrEmpty(req.getRespTopic())) {
					try {
						sendRespMsg(ext, resp, topic, req, now, tmp);
					}catch (Throwable e) {
						log.error(e.getMessage(), e);
					}
				}
			}
			@Override
			public void onSuccess(HandlerResult result) {
				CoreException tmp = null;
				InvokeResponse resp = new InvokeResponse(req.getReqId(), req.getReqTime(), Const.CLIENT_TIME.getNow(), result.getBody());
				resp.setTraceNos(req.getTraceNos());
				if(!Strings.isNullOrEmpty(req.getRespTopic())) {
					try {
						sendRespMsg(ext, resp, topic, req, now, tmp);
					}catch (Throwable e) {
						log.error(e.getMessage(), e);
					}
				}
			}
		});
	}

	private ConsumeConcurrentlyStatus sendRespMsg(MessageExt ext, InvokeResponse resp, String topic, InvokeRequest req,
			long now, CoreException tmp) {
		long processEnd = Const.CLIENT_TIME.getNow();
		if(processEnd > req.getDeadline()) {
			throw new DefaultCoreException(String.format("timeout, slow Invocation, %s-%s skip invoke process response, reqId : %s, reqTime : %s, deadline : %s, begin : %s, processEnd : %s"
												, topic
												, ext.getTags()
												, req.getReqId()
												, TimeUtils.yyyyMMddHHmmssSSS(req.getReqTime())
												, TimeUtils.yyyyMMddHHmmssSSS(req.getDeadline())
												, TimeUtils.yyyyMMddHHmmssSSS(now)
												, TimeUtils.yyyyMMddHHmmssSSS(processEnd))).setCode(DefaultCoreException.INVOKE_PROCESS_TIMEOUT);
		}
		if(req.isRespCompress() && resp.getRespBody() != null && resp.getRespBody().length > getClient().getProducer().getMaxMessageSize()) {
			try {
				int level = getClient().getProducer().getDefaultMQProducerImpl().getZipCompressLevel();
				resp.setRespBody(UtilAll.compress(resp.getRespBody(), level));
				resp.setCompressRespBody(true);
				log.warn("resp body size is max : {}, reqId : {}", resp.getRespBody().length, req.getReqId());
			}catch (Throwable e) {
				//ig no compress
			}
		}
		Message respMsg = new Message(req.getRespTopic(), req.getRespTag(), JSON.toJSONBytes(resp));
		respMsg.setKeys(Const.buildTraceInvokeReqId(req.getReqId()));
		try {
			SendResult ret = null;
			if(req.getRespQueue() != null) {
				ret = getClient().getProducer().send(respMsg, req.getRespQueue());
			}else {
				ret = getClient().getProducer().send(respMsg);
			}
			if(ret.getSendStatus() == SendStatus.SEND_OK) {
				log.trace("invoke {}-{}, reqId : {}, msgId : {}, rec msgId : {}, recId : {}, reqTime : {}, deadline : {}, begin : {}, processEnd : {}"
						, topic
						, ext.getTags()
						, req.getReqId()
						, ext.getMsgId()
						, ret.getMsgId()
						, ret.getOffsetMsgId()
						, TimeUtils.yyyyMMddHHmmssSSS(req.getReqTime())
						, TimeUtils.yyyyMMddHHmmssSSS(req.getDeadline())
						, TimeUtils.yyyyMMddHHmmssSSS(now)
						, TimeUtils.yyyyMMddHHmmssSSS(processEnd));
				if(tmp == null) {
					return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
				}else {
					throw tmp;
				}
			}else {
				throw new DefaultCoreException(String.format("%s-%s rec invoke error : %s"
															, topic
															, ext.getTags()
															, ret.getSendStatus().name())).setCode(DefaultCoreException.INVOKE_REC_ERROR);
			}
		}catch (CoreException e) {
			throw e;
		}catch (Throwable e) {
			throw new DefaultCoreException(String.format("unknow exception, %s-%s invoke end, process send response, reqId : %s, msgid : %s"
													, topic
													, ext.getTags()
													, req.getReqId()
													, ext.getMsgId())
										, e).setCode(CoreException.UNKNOW_EXCEPTION);
		}
	}

}
