package com.chopsticks.core.rocketmq.caller.impl;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.chopsticks.core.concurrent.impl.GuavaTimeoutPromise;
import com.chopsticks.core.rocketmq.caller.BaseInvokeResult;
import com.chopsticks.core.rocketmq.caller.InvokeSender;
import com.google.common.base.MoreObjects;
import com.google.common.base.Stopwatch;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

public class BatchInvokerSender extends InvokeSender{
	
	private static final Logger log = LoggerFactory.getLogger(BatchInvokerSender.class);
	
	private static final long MAX_BATCH_SIZE = 1000 * 1000;
	
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
										log.trace("poll : {}, send : {}, batch {} num : {}, size : {}"
												, pollTime
												, watch.elapsed(TimeUnit.MILLISECONDS) - pollTime
												, batchMsg.msg.getTopic()
												, value.size()
												, oldLength);
									}
									batchMsgs.put(batchMsg.msg.getTopic(), batchMsg);
									size.put(batchMsg.msg.getTopic(), batchMsg.length);
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
						}
					}
				}
				, executeIntervalMillis
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
	public void send(Message message, GuavaTimeoutPromise<BaseInvokeResult> promise) {
		BatchMessage batchMsg = new BatchMessage();
		batchMsg.msg = message;
		batchMsg.length = size(message);
		batchMsg.promise = promise;
		msgQueue.add(batchMsg);
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
		GuavaTimeoutPromise<BaseInvokeResult> promise;
	}
}
