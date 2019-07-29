package com.chopsticks.http;

import java.security.cert.X509Certificate;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.nio.conn.NoopIOSessionStrategy;
import org.apache.http.nio.conn.SchemeIOSessionStrategy;
import org.apache.http.nio.conn.ssl.SSLIOSessionStrategy;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.chopsticks.common.concurrent.Promise;
import com.chopsticks.common.concurrent.PromiseListener;
import com.chopsticks.common.concurrent.impl.DefaultPromise;
import com.chopsticks.http.entity.HttpMultiValue;
import com.chopsticks.http.entity.HttpRequest;
import com.chopsticks.http.entity.HttpResponse;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;

public class HttpClient {
	
	private static final Logger log = LoggerFactory.getLogger(HttpClient.class);
	
	private static final Registry<SchemeIOSessionStrategy> registry;
	static {
		try {
			TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() {
                            return null;
                        }
                        public void checkClientTrusted(X509Certificate[] certs, String authType) {
                            // don't check
                        }
                        public void checkServerTrusted(X509Certificate[] certs, String authType) {
                            // don't check
                        }
                    }
			};
			SSLContext sslContext = SSLContext.getInstance("TLS");
			sslContext.init(null, trustAllCerts, null);
			// (HostnameVerifier)SSLIOSessionStrategy.ALLOW_ALL_HOSTNAME_VERIFIER
            SSLIOSessionStrategy sslioSessionStrategy = new SSLIOSessionStrategy(sslContext, NoopHostnameVerifier.INSTANCE);
            registry = RegistryBuilder.<SchemeIOSessionStrategy>create()
                    .register("http", NoopIOSessionStrategy.INSTANCE)
                    .register("https", sslioSessionStrategy)
                    .build();  
			
		}catch (Throwable e) {
			Throwables.throwIfUnchecked(e);
			throw new RuntimeException(e);
		}
		
	}
	
	private CloseableHttpAsyncClient client;
	
	private boolean started;
	
	public HttpClient(){
		
	}
	
	public synchronized void start() {
		if(!started) {
			try {
				IOReactorConfig ioReactorConfig = IOReactorConfig.custom().setIoThreadCount(Runtime.getRuntime().availableProcessors()).build();
				ConnectingIOReactor ioReactor = new DefaultConnectingIOReactor(ioReactorConfig);
				PoolingNHttpClientConnectionManager connManager = new PoolingNHttpClientConnectionManager(ioReactor, registry);
				connManager.setMaxTotal(10000);
				connManager.setDefaultMaxPerRoute(10000);
				client = HttpAsyncClients.custom()
										 .setConnectionManager(connManager)
										 .build();
				client.start();
				started = true;
			}catch (Throwable e) {
				Throwables.throwIfUnchecked(e);
				throw new RuntimeException(e);
			}
		}
	}
	public synchronized void shutdown() {
		if(started) {
			try {
				client.close();
				started = false;
			}catch (Throwable e) {
				Throwables.throwIfUnchecked(e);
				throw new RuntimeException(e);
			}

		}
	}
	
	public HttpResponse invoke(HttpRequest req) {
		try {
			return asyncInvoke(req).get();
		}catch (Throwable e) {
			Throwables.throwIfUnchecked(e);
			throw new RuntimeException(e);
		}
	}
	
	public Promise<HttpResponse> asyncInvoke(HttpRequest req){
		Preconditions.checkArgument(!Strings.isNullOrEmpty(req.getUrl()), "url cannot be null or empty");
		Preconditions.checkArgument(started, "must call start method");
		final DefaultPromise<HttpResponse> ret = new DefaultPromise<HttpResponse>();
		HttpClientContext ctx = new HttpClientContext();
		HttpHost proxy = null;
		if(!Strings.isNullOrEmpty(req.getProxyAddr())) {
			proxy = new HttpHost(req.getProxyAddr(), req.getProxyPort(), req.getProxyScheme());
		}
		RequestConfig cfg = RequestConfig.custom()
										 .setConnectionRequestTimeout(req.getConnTimeoutMillis())
										 .setSocketTimeout(req.getReadTimeoutMillis())
										 .setProxy(proxy)
										 .build();
		ctx.setRequestConfig(cfg);
		HttpUriRequest realReq = null;
		if(HttpPost.METHOD_NAME.equalsIgnoreCase(req.getMethod())) {
			HttpPost post = new HttpPost(req.getUrl());
			if(req.getBody() != null) {
				post.setEntity(new InputStreamEntity(req.getBody()));
			}else if(req.getFormBody() != null && !req.getFormBody().isEmpty()) {
				List<NameValuePair> nvps = Lists.newArrayList();
				for(Iterator<Entry<String, List<String>>> iter = req.getFormBody().entrySet().iterator(); iter.hasNext();) {
					Entry<String, List<String>> entry = iter.next();
					if(entry.getValue() != null) {
						for(String value : entry.getValue()) {
							nvps.add(new BasicNameValuePair(entry.getKey(), value));
						}
					}
				}
				if(!nvps.isEmpty()) {
					try {
						post.setEntity(new UrlEncodedFormEntity(nvps, Charsets.UTF_8));
					}catch (Throwable e) {
						Throwables.throwIfUnchecked(e);
						throw new RuntimeException(e);
					}
				}
			}else if(req.getMultiFormBody() != null && !req.getMultiFormBody().isEmpty()){
				MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
				boolean hasBody = false;
				for(Iterator<Entry<String, List<HttpMultiValue>>> iter = req.getMultiFormBody().entrySet().iterator(); iter.hasNext();) {
					Entry<String, List<HttpMultiValue>> entry = iter.next();
					if(entry.getValue() != null) {
						for(HttpMultiValue value : entry.getValue()) {
							if(value.getValue() != null) {
								entityBuilder.addPart(entry.getKey(), new InputStreamBody(value.getValue(), value.getName()));
								hasBody = true;
							}
						}
					}
				}
				if(hasBody) {
					post.setEntity(entityBuilder.build());
				}
			}
			realReq = post; 
		}else if(HttpGet.METHOD_NAME.equalsIgnoreCase(req.getMethod())) {
			realReq = new HttpGet(req.getUrl());
		}else {
			throw new RuntimeException("unsupport method" + req.getMethod());
		}
		try {
			if(req.getHeaders() != null) {
				for(Entry<String, List<String>> entry : req.getHeaders().entrySet()) {
					if(entry.getValue() != null) {
						for(String value : entry.getValue()) {
							realReq.addHeader(entry.getKey(), value);
						}
					}
				}
			}
			client.execute(realReq, ctx, new FutureCallback<org.apache.http.HttpResponse>() {
				@Override
				public void failed(Exception ex) {
					ret.setException(ex);
				}
				@Override
				public void completed(org.apache.http.HttpResponse result) {
					HttpResponse resp = new HttpResponse();
					resp.setStatus(result.getStatusLine().getStatusCode());
					if(result.getAllHeaders() != null) {
						for(Header header : result.getAllHeaders()) {
							resp.addHeaderValue(header.getName(), header.getValue());
						}
					}
					if(result.getEntity() != null) {
						try {
							resp.setBody(result.getEntity().getContent());
						}catch (Throwable e) {
							ret.setException(e);
						}
					}
					ret.set(resp);
				}
				@Override
				public void cancelled() {
					ret.setException(new CancellationException());
				}
			});
		}catch (Throwable e) {
			Throwables.throwIfUnchecked(e);
			throw new RuntimeException(e);
		}
		
		return ret;
	}
	
	public static void main(String[] args) throws Throwable{
		HttpClient testClient = new HttpClient();
		testClient.start();
		HttpRequest req = new HttpRequest();
		req.setUrl("http://testehub.56linked.cn/ss/server");
		final AtomicInteger ii = new AtomicInteger();
		for(int i = 0; i < 1; i++) {
			testClient.asyncInvoke(req).addListener(new PromiseListener<HttpResponse>() {
				@Override
				public void onFailure(Throwable t) {
					System.out.println(ii.decrementAndGet());
					t.printStackTrace();
				}
				@Override
				public void onSuccess(HttpResponse result) {
					if(result.getBody() != null) {
						try {
							result.getBody().close();
						}catch (Throwable e) {
							e.printStackTrace();
						}
					}
					System.out.println(ii.decrementAndGet() + " " + result.getStatus());
					
				}
			});
		}
	}
}
