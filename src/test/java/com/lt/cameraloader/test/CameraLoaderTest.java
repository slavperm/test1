package com.lt.cameraloader.test;


import org.junit.Test;

import com.lt.cameraloader.CameraLoader;


import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

public class CameraLoaderTest {

	@Test
	public void mockTest() {
		JSONAssert.assertEquals(
				CameraLoader.loadUrlList("http://www.mocky.io/v2/5c51b9dd3400003252129fb5",-1),
				"[{\"id\":1,\"urlType\":\"LIVE\",\"videoUrl\":\"rtsp://127.0.0.1/1\",\"value\":\"fa4b588e-249b-11e9-ab14-d663bd873d93\",\"ttl\":120},{\"id\":20,\"urlType\":\"ARCHIVE\",\"videoUrl\":\"rtsp://127.0.0.1/2\",\"value\":\"fa4b5b22-249b-11e9-ab14-d663bd873d93\",\"ttl\":60},{\"id\":3,\"urlType\":\"ARCHIVE\",\"videoUrl\":\"rtsp://127.0.0.1/3\",\"value\":\"fa4b5d52-249b-11e9-ab14-d663bd873d93\",\"ttl\":120},{\"id\":2,\"urlType\":\"LIVE\",\"videoUrl\":\"rtsp://127.0.0.1/20\",\"value\":\"fa4b5f64-249b-11e9-ab14-d663bd873d93\",\"ttl\":180}]",
				JSONCompareMode.NON_EXTENSIBLE
				);
	}

}
