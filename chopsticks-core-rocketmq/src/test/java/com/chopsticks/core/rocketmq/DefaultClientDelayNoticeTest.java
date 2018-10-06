package com.chopsticks.core.rocketmq;

import java.util.concurrent.TimeUnit;

import com.chopsticks.core.Client;
import com.chopsticks.core.caller.NoticeResult;
import com.chopsticks.core.handler.HandlerResult;
import com.chopsticks.core.handler.InvokeContext;
import com.chopsticks.core.handler.InvokeParams;
import com.chopsticks.core.handler.NoticeContext;
import com.chopsticks.core.handler.NoticeParams;
import com.chopsticks.core.rocketmq.caller.impl.DefaultNoticeCommand;
import com.chopsticks.core.rocketmq.handler.BaseHandler;
import com.google.common.collect.Sets;

public class DefaultClientDelayNoticeTest {
	
	static {
		System.setProperty("rocketmq.namesrv.domain", "ehub.server.com");
	}
	
	private Client client;
	private String groupName = "testClientGroupName";
	private final String topic = "testTopic";
	private final String tag = "testTag";
	
	public static void main(String[] args) throws Throwable{
		DefaultClientDelayNoticeTest test = new DefaultClientDelayNoticeTest();
		test.before();
		test.testOrderedNotice();
		test.after();
	}
	
	public void before() {
		client = new DefaultClient(groupName);
		//((DefaultClient)client).setNamesrvAddr("localhost:9876");
		BaseHandler handler = new BaseHandler(topic, tag) {
			@Override
			public void notice(NoticeParams params, NoticeContext ctx) {
				System.out.println("body : " + new String(params.getBody()));
			}
			@Override
			public HandlerResult invoke(InvokeParams params, InvokeContext ctx) {
				return null;
			}
		}; 
		client.register(Sets.newHashSet(handler));
		client.start();
	}
	
	public void after() {
		client.shutdown();
	}
	
	public void testOrderedNotice() throws Throwable{
		NoticeResult result = client.notice(new DefaultNoticeCommand(topic, tag, "0".getBytes()), 25L, TimeUnit.SECONDS);
		System.out.println(result.getId());
		System.in.read();
	}
}
