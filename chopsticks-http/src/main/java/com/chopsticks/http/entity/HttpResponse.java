package com.chopsticks.http.entity;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.alibaba.fastjson.JSON;
import com.google.common.base.Charsets;
import com.google.common.base.MoreObjects;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.io.CharStreams;

public class HttpResponse {
	
	private Map<String, List<String>> headers;
	private InputStream body;
	private int status;
	
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
	public Map<String, List<String>> getHeaders() {
		return headers;
	}
	public void setHeaders(Map<String, List<String>> headers) {
		this.headers = headers;
	}
	public InputStream getBody() {
		return body;
	}
	public void setBody(InputStream body) {
		this.body = body;
	}
	public int getStatus() {
		return status;
	}
	public void setStatus(int status) {
		this.status = status;
	}
	public Object parseJson(){
		if(getBody() != null) {
			return JSON.parse(parseString());
		}
		return null;
	}
	
	public <T> List<T> parseJsonArray(Class<T> clazz) {
		if(getBody() != null) {
			return JSON.parseArray(parseString(), clazz);
		}
		return null;
	}
	
	public <T> T parseJsonObject(Class<T> clazz) {
		if(getBody() != null) {
			try {
				return JSON.parseObject(getBody(), clazz);
			}catch (Throwable e) {
				Throwables.throwIfUnchecked(e);
				throw new RuntimeException(e);
			}
		}
		return null;
	}
	
	public String parseString() {
		return parseString(Charsets.UTF_8);
	}
	
	public String parseString(Charset charset) {
		if(getBody() != null) {
			try {
				return CharStreams.toString(new InputStreamReader(getBody(), charset));
			}catch (Throwable e) {
				Throwables.throwIfUnchecked(e);
				throw new RuntimeException(e);
			}
		}
		return null;
	}
}
