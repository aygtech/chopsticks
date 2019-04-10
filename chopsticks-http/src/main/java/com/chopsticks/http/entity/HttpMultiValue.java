package com.chopsticks.http.entity;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HttpMultiValue {
	private String name;
	private InputStream value;
	private Map<String, List<String>> headers = new HashMap<String, List<String>>();
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public InputStream getValue() {
		return value;
	}
	public void setValue(InputStream value) {
		this.value = value;
	}
	public Map<String, List<String>> getHeaders() {
		return headers;
	}
	public void setHeaders(Map<String, List<String>> headers) {
		this.headers = headers;
	}
}
