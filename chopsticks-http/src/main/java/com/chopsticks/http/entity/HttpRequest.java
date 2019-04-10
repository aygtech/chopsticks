package com.chopsticks.http.entity;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;

public class HttpRequest {
	
	public HttpRequest() {}
	public HttpRequest(String url) {
		this.url = url;
	}
	public HttpRequest(String url, String body) {
		setUrl(url);
		setBody(body);
	}
	
	private String url;
	private String method = "get";
	private int connTimeoutMillis = (int)TimeUnit.SECONDS.toMillis(5L);
	private int readTimeoutMillis = (int)TimeUnit.SECONDS.toMillis(30L);
	private int getConnTimeoutMillis = (int)TimeUnit.SECONDS.toMillis(5L);
	private Map<String, List<String>> headers;
	private Map<String, List<String>> formBody;
	private InputStream body;
	private Map<String, List<HttpMultiValue>> multiFormBody;
	public void addHeaderValue(String key, String value) {
		setHeaders(MoreObjects.firstNonNull(getHeaders(), new HashMap<String, List<String>>()));
		List<String> values = getHeaders().get(key);
		if(values == null) {
			values = Lists.newArrayList();
			getHeaders().put(key, values);
		}
		values.add(value);
	}
	public String getSingleValueByHeader(String key) {
		if(headers != null) {
			for (Entry<String, List<String>> entry : headers.entrySet()) {
				if (entry.getKey().equalsIgnoreCase(key)) {
					List<String> headerValue = entry.getValue();
					if (headerValue == null || headerValue.isEmpty()) {
						return null;
					} else {
						return headerValue.get(0);
					}
				}
			}
		}
		return null;
	}
	
	public void setBody(String body){
		this.body = new ByteArrayInputStream(body.getBytes());
	}
	public Map<String, List<String>> getHeaders() {
		return headers;
	}
	public void setHeaders(Map<String, List<String>> headers) {
		this.headers = headers;
	}
	public Map<String, List<String>> getFormBody() {
		return formBody;
	}
	public void addFormBodyValue(String key, String value) {
		setFormBody(MoreObjects.firstNonNull(getFormBody(), new HashMap<String, List<String>>()));
		List<String> values = getFormBody().get(key);
		if(values == null) {
			values = Lists.newArrayList();
			getFormBody().put(key, values);
		}
		values.add(value);
	}
	public String getSingleValueByFormBody(String key) {
		if(getFormBody() != null) {
			List<String> values = getFormBody().get(key);
			if(values == null || values.isEmpty()) {
				return null;
			}else {
				return values.get(0);
			}
		}else {
			return null;
		}
	}
	public void setFormBody(Map<String, List<String>> formBody) {
		this.formBody = formBody;
	}
	public InputStream getBody() {
		return body;
	}
	public void setBody(InputStream body) {
		this.body = body;
	}
	public Map<String, List<HttpMultiValue>> getMultiFormBody() {
		return multiFormBody;
	}
	public void setMultiFormBody(Map<String, List<HttpMultiValue>> multiFormBody) {
		this.multiFormBody = multiFormBody;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public String getMethod() {
		return method;
	}
	public void setMethod(String method) {
		this.method = method;
	}
	public int getConnTimeoutMillis() {
		return connTimeoutMillis;
	}
	public void setConnTimeoutMillis(int connTimeoutMillis) {
		this.connTimeoutMillis = connTimeoutMillis;
	}
	public int getReadTimeoutMillis() {
		return readTimeoutMillis;
	}
	public void setReadTimeoutMillis(int readTimeoutMillis) {
		this.readTimeoutMillis = readTimeoutMillis;
	}
	public int getGetConnTimeoutMillis() {
		return getConnTimeoutMillis;
	}
	public void setGetConnTimeoutMillis(int getConnTimeoutMillis) {
		this.getConnTimeoutMillis = getConnTimeoutMillis;
	}
}
