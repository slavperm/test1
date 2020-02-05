package com.lt.cameraloader;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.script.ScriptException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lt.cameraloader.engine.AsyncExecutor;
import com.lt.cameraloader.model.SequenceItem;

/**
 * Многопоточное чтение и агрегация данных по камерам maxTherad - можно
 * ограничить количество нитей, по умолчанию = количеству доступных логических
 * процессоров
 * 
 *
 */
public class CameraLoader {

	private static final Logger LOGGER = LoggerFactory.getLogger("");

	public static String loadUrlList(String url, int limitThread) {
		String result = null;

		Client client = ClientBuilder.newClient();
		LOGGER.debug("Target Main url {}", url);
		Response rawResponse;
		try {
		WebTarget target = client.target(url);
		Invocation.Builder builder = target.request().accept(MediaType.APPLICATION_JSON);
		rawResponse = builder.get();
		}catch (Exception err) {
			LOGGER.error("ERROR:http " + err.getMessage());
			return null;
		}
		if (rawResponse.getStatusInfo().getStatusCode() != Response.Status.OK.getStatusCode()) {
			LOGGER.error("ERROR:http error " + rawResponse.getStatus() + " url=" + url);
			return null;
		}
		JsonArray vObj = null;
		JsonReader jsonReader = Json.createReader(rawResponse.readEntity(Reader.class));
		try {
			vObj = jsonReader.readArray();
		} catch (Exception err) {
			LOGGER.error("ERROR:readArray " + err.getMessage());
			return null;
		}
		if (vObj != null) {
			final int maxTherad = (limitThread == -1 ? Runtime.getRuntime().availableProcessors() : limitThread);
			List<JsonObject> jsonArr = vObj.getValuesAs(JsonObject.class);
			final int maxItemPerThread = jsonArr.size() / maxTherad;
			List<AsyncExecutor> itemsArr = new ArrayList<AsyncExecutor>();
			{
				List<SequenceItem> items = new ArrayList<SequenceItem>();
				for (JsonObject jsonObj : jsonArr) {
					SequenceItem item = new SequenceItem();
					item.setId(jsonObj.getJsonNumber("id").longValue());
					item.setSourceDataUrl(jsonObj.getJsonString("sourceDataUrl").getString());
					item.setTokenDataUrl(jsonObj.getJsonString("tokenDataUrl").getString());
					items.add(item);
					if (items.size() > maxItemPerThread) {
						itemsArr.add(new AsyncExecutor(items));
						items = new ArrayList<SequenceItem>();
					}
				}
				if (items.size() > 0) {
					itemsArr.add(new AsyncExecutor(items));
				}
			}
			List<ExecutorService> mainExArr = new ArrayList<ExecutorService>();
			ExecutorService mainEx = Executors.newFixedThreadPool(itemsArr.size());//newSingleThreadExecutor();			
			boolean hasError = false;
			for (AsyncExecutor se : itemsArr) {

				try {
					se.setDaemon(false);
					mainEx.submit(se);
				} catch (Exception err) { // UnknownHostException
					LOGGER.error("Error create thread", err);
					hasError = true;
				}

			}
			mainExArr.add(mainEx);
			try {
				// Ожидание окончания опроса или ошибки
				
				int complitedCount = 0;
				while (!hasError && complitedCount < itemsArr.size()) {
					complitedCount = 0;
					for (AsyncExecutor se : itemsArr) {
						if (se.getErrorCount() > 0) {
							hasError = true;
							break;
						}
						if (se.getSuccessCount() == se.getSequence().size()) {
							complitedCount++;
						}
						Thread.yield();
					}
				}

				if (!hasError) {
					JsonArrayBuilder vRes = Json.createArrayBuilder();
					for (AsyncExecutor se : itemsArr) {
						for (SequenceItem item : se.getSequence()) {
							JsonObjectBuilder vItem = Json.createObjectBuilder();
							vItem.add("id", item.getId());
							vItem.add("urlType", item.getSource().getUrlType().name());
							vItem.add("videoUrl", item.getSource().getVideoUrl());
							vItem.add("value", item.getToken().getValue());
							vItem.add("ttl", item.getToken().getTtl());
							vRes.add(vItem);
						}
					}
					result = vRes.build().toString();

				}
			} catch (Exception err) {
				LOGGER.error("Error execute thread", err);
			}
			for (ExecutorService mainExI : mainExArr) {
				if (!mainExI.isShutdown()) {
					mainExI.shutdownNow();
				}
			}
		}
		return result;
	}

	public static void main(String[] args) throws ScriptException, InterruptedException {
		if (args.length >= 1) {
			boolean hitest = false;
			int hicount=10;
			for (String arg : args) {
				if (arg.startsWith("-")) {
					hitest = true;
					if (arg.length()>1) {
						hicount=Integer.parseInt(arg.substring(1));
					}
				} else {
					// режим бенчмарка
					if (hitest) {
						double d1=hiloadtest(arg, hicount, 1);
						double d2=hiloadtest(arg, hicount, -1);
						System.out.println("delay_avg1:" +d1);
						System.out.println("delay_avg2:" +d2);
					} else {
						System.out.println(loadUrlList(arg, -1));
					}
				}
			}
		}
	}

	public static double hiloadtest(String url, int maxCount, int limitThread) {
		//int maxCount=60;
		long sumCpu = 0L;
		String res = null;
		for (int i = 0; i < maxCount; i++) {
			long startCpu = System.nanoTime();
			res = loadUrlList(url, limitThread);
			long endCpu = System.nanoTime();
			sumCpu += (endCpu - startCpu);
			System.out.println("delay_" + i + ":" + (endCpu - startCpu));
		}
		System.out.println(res);
		System.out.println("delay_avg:" + ((sumCpu / 1000000.0) / maxCount));
		return ((sumCpu / 1000000.0) / maxCount);
	}

}
