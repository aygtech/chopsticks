package com.chopsticks.core.rocketmq;

import java.util.concurrent.TimeUnit;

import com.chopsticks.core.handler.HandlerResult;
import com.chopsticks.core.handler.InvokeContext;
import com.chopsticks.core.handler.InvokeParams;
import com.chopsticks.core.handler.NoticeContext;
import com.chopsticks.core.handler.NoticeParams;
import com.chopsticks.core.rocketmq.caller.BaseInvokeResult;
import com.chopsticks.core.rocketmq.caller.impl.DefaultInvokeCommand;
import com.chopsticks.core.rocketmq.handler.BaseHandler;
import com.chopsticks.core.rocketmq.handler.impl.DefaultHandlerResult;
import com.google.common.collect.Sets;

public class DefaultClientInvokeTest {
	
	private DefaultClient client;
	private String groupName = "testClientGroupName";
	private final String topic = "testTopic";
	private final String tag = "testTag";
	
	public static void main(String[] args) throws Throwable{
		DefaultClientInvokeTest test = new DefaultClientInvokeTest();
		test.before();
		test.testInvoke();
		test.after();
	}
	
	public void before() {
		client = new DefaultClient(groupName);
		client.setNamesrvAddr("localhost:9876");
		BaseHandler handler = new BaseHandler(topic, tag) {
			@Override
			public HandlerResult invoke(InvokeParams params, InvokeContext ctx) {
				System.out.println("body : " + new String(params.getBody()));
				return new DefaultHandlerResult(params.getBody());
			}
			@Override
			public void notice(NoticeParams params, NoticeContext ctx) {
			}
		};
		client.register(Sets.newHashSet(handler));
		client.start();
	}
	
	public void after() {
		client.shutdown();
	}
	
	public void testInvoke() throws Throwable{
		for(int i = 0; i < 10; i++) {
			BaseInvokeResult result = client.invoke(new DefaultInvokeCommand(topic
																		, tag
																		, (i + "").getBytes())
												, 15
												, TimeUnit.SECONDS);
			System.out.println("invoke result : " + new String(result.getBody()));
		}
		System.in.read();
	}
	
}
