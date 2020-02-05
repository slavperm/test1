package com.lt.cameraloader.model;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Исходные данные и агрегация результата
 *
 */
public class SequenceItem {
    
    private Long id;
    
    private String sourceDataUrl;
    
    private String tokenDataUrl;
    
    private SourceData source;
    
    private TokenData token;
    
    private AtomicInteger succesCount=new AtomicInteger(0);
    
    private AtomicInteger errorCount=new AtomicInteger(0);
    
    
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getSourceDataUrl() {
		return sourceDataUrl;
	}

	public void setSourceDataUrl(String sourceDataUrl) {
		this.sourceDataUrl = sourceDataUrl;
	}

	public String getTokenDataUrl() {
		return tokenDataUrl;
	}

	public void setTokenDataUrl(String tokenDataUrl) {
		this.tokenDataUrl = tokenDataUrl;
	}

	public SourceData getSource() {
		return source;
	}

	public void setSource(SourceData source) {
		this.source = source;
	}

	public TokenData getToken() {
		return token;
	}

	public void setToken(TokenData token) {
		this.token = token;
	}

	public int getSuccesCount() {
		return succesCount.get();
	}

	synchronized public void incSuccesCount() {
		this.succesCount.getAndIncrement();
	}

	public int getErrorCount() {
		return errorCount.get();
	}

	synchronized public void incErrorCount() {
		this.errorCount.getAndIncrement();
	}


    
}
