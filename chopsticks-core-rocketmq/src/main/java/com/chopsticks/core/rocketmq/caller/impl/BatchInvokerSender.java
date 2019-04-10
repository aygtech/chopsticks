package com.chopsticks.core.rocketmq.caller.impl;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.UtilAll;
import org.apache.rocketmq.common.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.chopsticks.common.concurrent.impl.DefaultTimeoutPromise;
import com.chopsticks.common.utils.Reflect;
import com.chopsticks.core.rocketmq.Const;
import com.chopsticks.core.rocketmq.caller.BaseInvokeResult;
import com.chopsticks.core.rocketmq.caller.InvokeRequest;
import com.chopsticks.core.rocketmq.caller.BaseInvokeSender;
import com.google.common.base.MoreObjects;
import com.google.common.base.Stopwatch;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

public class BatchInvokerSender extends BaseInvokeSender{
	
	private static final Logger log = LoggerFactory.getLogger(BatchInvokerSender.class);
	
	private static final long MAX_BATCH_SIZE = 1000 * 1000;
	
	private static final long DEFAULT_COMPRESS_BODY_LENGTH = 1024 * 100;
	
	private LinkedBlockingQueue<BatchMessage> msgQueue = new LinkedBlockingQueue<BatchMessage>();
	
	private ScheduledThreadPoolExecutor executor;
	
	public BatchInvokerSender(final DefaultMQProducer producer, final long executeIntervalMillis){
		super(producer);
		executor = new ScheduledThreadPoolExecutor(1
						, new ThreadFactoryBuilder()
								.setNameFormat(producer.getProducerGroup() + "-invokeBatchSchedule-%d")
								.setDaemon(true)
								.build());
		executor.scheduleAtFixedRate(new Runnable() {
					@Override
					public void run() {
						Multimap<String, BatchMessage> batchMsgs = HashMultimap.create();
						Map<String, Long> size = Maps.newHashMap();
						Stopwatch watch = Stopwatch.createStarted();
						long pollTime;
						while(watch.elapsed(TimeUnit.MILLISECONDS) < executeIntervalMillis) {
							BatchMessage batchMsg = null;
							try {
								batchMsg = msgQueue.poll(executeIntervalMillis - watch.elapsed(TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS);
							}catch (Throwable e) {
								log.error(e.getMessage(), e);
								continue;
							}
							if(batchMsg != null) {
								Long oldLength = MoreObjects.firstNonNull(size.get(batchMsg.msg.getTopic()), 0L);
								if(oldLength + batchMsg.length < MAX_BATCH_SIZE) {
									size.put(batchMsg.msg.getTopic(), oldLength + batchMsg.length);
									batchMsgs.put(batchMsg.msg.getTopic(), batchMsg);
								}else {
									pollTime = watch.elapsed(TimeUnit.MILLISECONDS);
									Collection<BatchMessage> value = batchMsgs.removeAll(batchMsg.msg.getTopic());
									if(value != null && !value.isEmpty()) {
										batchMsgSend(value);
										log.trace("tmp poll : {}, send : {}, batch {} num : {}, size : {}"
												, pollTime
												, watch.elapsed(TimeUnit.MILLISECONDS) - pollTime
												, batchMsg.msg.getTopic()
												, value.size()
												, oldLength);
									}
									batchMsgs.put(batchMsg.msg.getTopic(), batchMsg);
									size.put(batchMsg.msg.getTopic(), batchMsg.length);
									watch.reset().start();
								}
							}
						}
						for(Entry<String, Collection<BatchMessage>> entry : batchMsgs.asMap().entrySet()) {
							pollTime = watch.elapsed(TimeUnit.MILLISECONDS);
							batchMsgSend(entry.getValue());
							log.trace("poll : {}, send : {}, batch {} num : {}, size : {}"
									, pollTime
									, watch.elapsed(TimeUnit.MILLISECONDS) - pollTime
									, entry.getKey()
									, entry.getValue().size()
									, size.get(entry.getKey()));
							watch.reset().start();
						}
					}
				}
				, 0L
				, executeIntervalMillis
				, TimeUnit.MILLISECONDS);
	}
	
	private void batchMsgSend(Collection<BatchMessage> collection) {
		if(collection != null && !collection.isEmpty()) {
			try {
				List<Message> msgs = Lists.newArrayList();
				for(BatchMessage batchMsg : collection) {
					msgs.add(batchMsg.msg);
				}
				super.producer.send(msgs);
			}catch (Throwable e) {
				for(BatchMessage batchMsg : collection) {
					batchMsg.promise.setException(e);
				}
			}
		}
	}
	@Override
	public void shutdown() {
		if(executor != null) {
			executor.shutdown();
		}
	}
	public void send(Message message, DefaultTimeoutPromise<BaseInvokeResult> promise) {
		Message compressMsg = message.getBody().length > DEFAULT_COMPRESS_BODY_LENGTH ? compressInvokeMsgBody(message) : message;
		BatchMessage batchMsg = new BatchMessage();
		batchMsg.msg = compressMsg;
		batchMsg.length = size(compressMsg);
		batchMsg.promise = promise;
		msgQueue.add(batchMsg);
	}
	
	/**
	 * 批量才需要手工压缩，其他情况rocketmq自带压缩机制
	 * @param msg
	 * @return
	 */
	private Message compressInvokeMsgBody(Message msg) {
		try {
			InvokeRequest req = JSON.parseObject(msg.getUserProperty(Const.INVOKE_REQUEST_KEY), InvokeRequest.class);
			req.setCompress(true);
			int level = Reflect.on(producer).field("defaultMQProducerImpl").field("zipCompressLevel").get();
			byte[] body = UtilAll.compress(msg.getBody(), level);
			msg.setBody(body);
			msg.putUserProperty(Const.INVOKE_REQUEST_KEY, JSON.toJSONString(req));
		}catch (Throwable e) {
			log.error(e.getMessage(), e);
		}
		return msg;
	}
	
	private long size(Message message) {
		long size = message.getTopic().length() + message.getBody().length;
		for(Entry<String, String> entry : message.getProperties().entrySet()) {
			size += entry.getKey().length() + entry.getValue().length();
		}
		size += 20; // log length
		return size;
	}
	
	class BatchMessage {
		Message msg;
		long length;
		DefaultTimeoutPromise<BaseInvokeResult> promise;
	}
}
