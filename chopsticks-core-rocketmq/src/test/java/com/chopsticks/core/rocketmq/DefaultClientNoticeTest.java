package com.chopsticks.core.rocketmq;

import com.chopsticks.core.Client;
import com.chopsticks.core.caller.NoticeResult;
import com.chopsticks.core.concurrent.Promise;
import com.chopsticks.core.concurrent.PromiseListener;
import com.chopsticks.core.handler.HandlerResult;
import com.chopsticks.core.handler.InvokeContext;
import com.chopsticks.core.handler.InvokeParams;
import com.chopsticks.core.handler.NoticeContext;
import com.chopsticks.core.handler.NoticeParams;
import com.chopsticks.core.rocketmq.caller.impl.DefaultNoticeCommand;
import com.chopsticks.core.rocketmq.handler.BaseHandler;
import com.google.common.collect.Sets;

public class DefaultClientNoticeTest {

	private Client client;
	private String groupName = "testClientGroupName";
	private final String topic = "testTopic";
	private final String tag = "testTag";

	public static void main(String[] args) throws Throwable {
		DefaultClientNoticeTest test = new DefaultClientNoticeTest();
		test.before();
		test.testNotice();
		test.after();
	}

	public void before() {
		client = new DefaultClient(groupName);
		((DefaultClient)client).setNamesrvAddr("localhost:9876");
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

	public void testNotice() throws Throwable {
		for (int i = 0; i < 10; i++) {
			Promise<? extends NoticeResult> result = client.asyncNotice(new DefaultNoticeCommand(topic, tag, (i + "").getBytes()));
			final int body = i;
			result.addListener(new PromiseListener<NoticeResult>() {
				@Override
				public void onSuccess(NoticeResult result) {
					System.out.println("notice result id : " + result.getId() + ", body : " + body);
				}
				@Override
				public void onFailure(Throwable t) {
					t.printStackTrace();
				}
			});
		}
		System.in.read();
	}
}
