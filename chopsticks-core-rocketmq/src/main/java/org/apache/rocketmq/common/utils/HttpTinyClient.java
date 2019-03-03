/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.rocketmq.common.utils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.rocketmq.common.MQVersion;
import org.apache.rocketmq.common.MixAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.chopsticks.core.rocketmq.Const;
import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;

public class HttpTinyClient {
	private static final Logger log = LoggerFactory.getLogger(HttpTinyClient.class);
	
	private static final AtomicLong UPDATE_COUNT = new AtomicLong(0L);
	private static final int MAX_UPDATE_COUNT = 10;
	
	private static final String MESSAGE_DELAY_LEVEL = "messageDelayLevel";
	
    static public HttpResult httpGet(String url, List<String> headers, List<String> paramValues,
		String encoding, long readTimeoutMs) throws IOException {
    	String encodedContent = encodingParams(paramValues, encoding);
        url += (null == encodedContent) ? "" : ("?" + encodedContent);

        HttpURLConnection conn = null;
        try {
        	log.trace(url);
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout((int) readTimeoutMs);
            conn.setReadTimeout((int) readTimeoutMs);
            setHeaders(conn, headers, encoding);
            Stopwatch watch = Stopwatch.createStarted();
            conn.connect();
            long diff = watch.elapsed(TimeUnit.MILLISECONDS);
            log.trace("diff millis : {}", diff);
            long date = conn.getHeaderFieldLong("date", 0);
            if(date > 0) {
            	if(UPDATE_COUNT.getAndIncrement() % MAX_UPDATE_COUNT == 0) {
            		Const.CLIENT_TIME.setNow(date - diff);
            	}
            }
            
            int respCode = conn.getResponseCode();
            String delayLevel = conn.getHeaderField(MESSAGE_DELAY_LEVEL);
            if(!Strings.isNullOrEmpty(delayLevel)) {
            	log.trace("delayLevel : {}", delayLevel);
            	String[] delayLevels = delayLevel.split(" ");
                TreeMap<Long, Integer> curDelayLevel = new TreeMap<Long, Integer>();
                long mills;
                String delay;
                for(int i = 0; i < delayLevels.length; i++) {
                	delay = delayLevels[i];
                	if(delay.contains("s")) {
                		mills = TimeUnit.SECONDS.toMillis(Long.parseLong(delay.replace("s", "")));
                	}else if(delay.contains("m")){
                		mills = TimeUnit.MINUTES.toMillis(Long.parseLong(delay.replace("m", "")));
                	}else if(delay.contains("h")){
                		mills = TimeUnit.HOURS.toMillis(Long.parseLong(delay.replace("h", "")));
                	}else {
                		mills = 10000L;
                	}
                	curDelayLevel.put(mills, i + 1);
                }
                log.trace("delayLevel : {}", curDelayLevel);
                Const.setDelayLevel(curDelayLevel);
            }
            
            String resp = null;

            if (HttpURLConnection.HTTP_OK == respCode) {
                resp = IOTinyUtils.toString(conn.getInputStream(), encoding);
            } else {
                resp = IOTinyUtils.toString(conn.getErrorStream(), encoding);
                log.warn("connection server error : {}, code : {}", resp, respCode);
            }
            return new HttpResult(respCode, resp);
        }catch (Throwable e) {
        	log.warn("connection server error, msg : {}", e.getMessage());
        	Throwables.throwIfUnchecked(e);
        	throw new RuntimeException(e);
        }finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    static private String encodingParams(List<String> paramValues, String encoding)
        throws UnsupportedEncodingException {
        StringBuilder sb = new StringBuilder();
        if (null == paramValues) {
            return null;
        }

        for (Iterator<String> iter = paramValues.iterator(); iter.hasNext(); ) {
            sb.append(iter.next()).append("=");
            sb.append(URLEncoder.encode(iter.next(), encoding));
            if (iter.hasNext()) {
                sb.append("&");
            }
        }
        return sb.toString();
    }

    static private void setHeaders(HttpURLConnection conn, List<String> headers, String encoding) {
        if (null != headers) {
            for (Iterator<String> iter = headers.iterator(); iter.hasNext(); ) {
                conn.addRequestProperty(iter.next(), iter.next());
            }
        }
        conn.addRequestProperty("Client-Version", MQVersion.getVersionDesc(MQVersion.CURRENT_VERSION));
        conn.addRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=" + encoding);

        String ts = String.valueOf(System.currentTimeMillis());
        conn.addRequestProperty("Metaq-Client-RequestTS", ts);
    }

    static public HttpResult httpPost(String url, List<String> headers, List<String> paramValues,
        String encoding, long readTimeoutMs) throws IOException {
        String encodedContent = encodingParams(paramValues, encoding);

        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout((int) readTimeoutMs);
            conn.setDoOutput(true);
            conn.setDoInput(true);
            setHeaders(conn, headers, encoding);

            conn.getOutputStream().write(encodedContent.getBytes(MixAll.DEFAULT_CHARSET));

            int respCode = conn.getResponseCode();
            String resp = null;

            if (HttpURLConnection.HTTP_OK == respCode) {
                resp = IOTinyUtils.toString(conn.getInputStream(), encoding);
            } else {
                resp = IOTinyUtils.toString(conn.getErrorStream(), encoding);
            }
            return new HttpResult(respCode, resp);
        } finally {
            if (null != conn) {
                conn.disconnect();
            }
        }
    }

    static public class HttpResult {
        final public int code;
        final public String content;

        public HttpResult(int code, String content) {
            this.code = code;
            this.content = content;
        }
    }
}
