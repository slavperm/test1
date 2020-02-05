package com.lt.cameraloader.engine;

import java.io.Reader;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lt.cameraloader.model.SequenceItem;
import com.lt.cameraloader.model.SourceData;
import com.lt.cameraloader.model.SourceUrlType;
import com.lt.cameraloader.model.TokenData;
/**
 *
 * Поток с асинхронными http запросами
 *
 * 
 *
 */
public class AsyncExecutor extends Thread {
	private final Logger LOGGER = LoggerFactory.getLogger(AsyncExecutor.class);
    
    private final Client client;

    private final List<SequenceItem> sequence;
    
    private AtomicInteger successCount=new AtomicInteger(0);
    
    private AtomicInteger errorCount=new AtomicInteger(0);

    private static JsonObject getJson(Response rawResponse) throws Exception {
        JsonObject vObj=null;
        JsonReader jsonReader = Json.createReader(rawResponse.readEntity(Reader.class));
        //try {
        	vObj =  jsonReader.readObject();
		//} catch (Exception err) {
		//	LOGGER.error("ERROR:" +  "(" + rawResponse.readEntity(String.class) + "):" + err.getMessage());
		//}
        return vObj;
    }

    public AsyncExecutor( List<SequenceItem> sequence)
             {
        this.sequence = sequence;
        this.client = ClientBuilder.newClient();

    }


    @Override
    public void run() {
    	AsyncExecutor parent = this;
            for (int i = 0; i < getSequence().size(); i++) {
            	if (Thread.currentThread().isInterrupted()) {
            		this.incErrorCount();
            		break;
            	}
            	if (getErrorCount()>0) {
            		break;
            	}
                SequenceItem item = getSequence().get(i);
                
                try {
                    final String reqtUrl = item.getSourceDataUrl();
                    LOGGER.debug("Target URL 1 {}", reqtUrl);

                    WebTarget target = client.target(reqtUrl);
                    Invocation.Builder builder = target.request().accept(MediaType.APPLICATION_JSON);

                    builder.async().get( new InvocationCallback<Response>() {
                    	SequenceItem itemCur = item;
                    	AsyncExecutor parentCur = parent;
                    	String urlCur=reqtUrl;
    					@Override
    					public void completed(Response response) {
    						try {
    							if (response.getStatusInfo().getStatusCode()==Response.Status.OK.getStatusCode()) {
		    						JsonObject jsonObj=getJson(response);
		    						SourceData respSD=new SourceData();
		    						respSD.setUrlType(SourceUrlType.valueOf( jsonObj.getJsonString("urlType").getString() ));
		    						respSD.setVideoUrl(jsonObj.getJsonString("videoUrl").getString());
		    						itemCur.setSource(respSD);
		    						itemCur.incSuccesCount();
		    						if (itemCur.getSuccesCount()>=2) {
		    							parentCur.incSuccessCount();
		    						}
		    						LOGGER.debug("SourceData http_code=" + response.getStatus()+" url:"+urlCur);
    							}else {
        							itemCur.incErrorCount();
            						parentCur.incErrorCount(); 
            						LOGGER.error("ERROR:read SourceData http_code=" + response.getStatus()+" url:"+urlCur);
    							}
    						}
    						catch (Exception err) {
    							itemCur.incErrorCount();
        						parentCur.incErrorCount();
        						LOGGER.error("ERROR:read SourceData " + err.getMessage()+" url:"+urlCur);
    						}

    					}

    					@Override
    					public void failed(Throwable errInfo) {
    						itemCur.incErrorCount();
    						parentCur.incErrorCount();
    						LOGGER.error("ERROR:http SourceData " + errInfo.getMessage()+" url:"+urlCur);
    					}
    				});
                    
                    
                    final String reqtUrl2 = item.getTokenDataUrl();
                    LOGGER.debug("Target URL 2 {}", reqtUrl2);

                     target = client.target(reqtUrl2);
                     builder = target.request().accept(MediaType.APPLICATION_JSON);

                    builder.async().get( new InvocationCallback<Response>() {
                    	SequenceItem itemCur = item;
                    	AsyncExecutor parentCur = parent;
                    	String urlCur=reqtUrl2;
    					@Override
    					public void completed(Response response) {
    						try {
    							if (response.getStatusInfo().getStatusCode()==Response.Status.OK.getStatusCode()) {
		    						JsonObject jsonObj=getJson(response);
		    						TokenData respTD=new TokenData();
		    						respTD.setValue(jsonObj.getJsonString("value").getString());
		    						respTD.setTtl( jsonObj.getInt("ttl") );
		
		    						itemCur.setToken(respTD);
		    						itemCur.incSuccesCount();
		    						if (itemCur.getSuccesCount()>=2) {
		    							parentCur.incSuccessCount();
		    							
		    						}
		    						LOGGER.debug("TokenData http_code=" + response.getStatus()+" url:"+urlCur);
    							}else {
        							itemCur.incErrorCount();
            						parentCur.incErrorCount(); 
            						LOGGER.error("ERROR:read TokenData http_code=" + response.getStatus()+" url:"+urlCur);
    							}
    						}
    						catch (Exception err) {
    							itemCur.incErrorCount();
        						parentCur.incErrorCount();
        						LOGGER.error("ERROR:read TokenData " + err.getMessage()+" url:"+urlCur);
    						}
    					}

    					@Override
    					public void failed(Throwable errInfo) {
    						itemCur.incErrorCount();
    						parentCur.incErrorCount();
    						LOGGER.error("ERROR:http TokenData " + errInfo.getMessage()+" url:"+urlCur);
    					}
    				});
                                     

                } catch (Throwable th) {
                	item.incErrorCount();
                	this.incErrorCount();
                	LOGGER.error(ExceptionUtils.getStackTrace(th));
                }

            }
         // waiting
         //while (this.getErrorCount()==0 && this.getSuccessCount()<getSequence().size()) {
        //	 Thread.yield();
        // }
            

    }


	public int getSuccessCount() {
		return successCount.get();
	}

	public synchronized void incSuccessCount() {
		this.successCount.getAndIncrement();
	}

	public int getErrorCount() {
		return errorCount.get();
	}

	public synchronized void incErrorCount() {
		this.errorCount.getAndIncrement();
	}

	public List<SequenceItem> getSequence() {
		return sequence;
	}



}
