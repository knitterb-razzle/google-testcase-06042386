package images;

import java.io.IOException;
import java.io.RandomAccessFile;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.google.appengine.tools.development.testing.LocalBlobstoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalMailServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalMemcacheServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.appengine.tools.development.testing.LocalTaskQueueTestConfig;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.wolfapp.testcase.ImageProcess;

import static com.google.common.truth.Truth.assertThat;

public class ImageTest {

	protected LocalServiceTestHelper helper;

	public byte[] workingPngBytes;
	public byte[] failingPngBytes;
	
	@Rule
	public ExpectedException exception = ExpectedException.none();

	public ImageTest() throws IOException {
		
		LocalDatastoreServiceTestConfig helperDatastoreConfig = 
				new LocalDatastoreServiceTestConfig().setApplyAllHighRepJobPolicy();
		LocalTaskQueueTestConfig helperTaskQueueConfig = 
				new LocalTaskQueueTestConfig().setQueueXmlPath(System.getProperty("user.dir") + "/src/main/webapp/WEB-INF/queue.xml");
		LocalBlobstoreServiceTestConfig helperBlobstoreConfig = 
				new LocalBlobstoreServiceTestConfig();
		LocalMemcacheServiceTestConfig helperMemcacheConfig = 
				new LocalMemcacheServiceTestConfig();
		LocalMailServiceTestConfig helperMailConfig = 
				new LocalMailServiceTestConfig();

		this.helper = new LocalServiceTestHelper(
				helperDatastoreConfig, 
				helperTaskQueueConfig, 
				helperBlobstoreConfig, 
				helperMemcacheConfig,
				helperMailConfig
				);

		
		RandomAccessFile f;

		f = new RandomAccessFile("src/test/resources/testPattern.png", "r");
		workingPngBytes = new byte[(int) f.length()];
		f.read(workingPngBytes);
		f.close();
		
		f = new RandomAccessFile("src/test/resources/image.png", "r");
		failingPngBytes = new byte[(int) f.length()];
		f.read(failingPngBytes);
		f.close();
		

	}
	
	@Before
	public void setup() throws JsonIOException, JsonSyntaxException, IOException {
		this.helper.setUp();
	}

	@After
	public void tearDown() {
		try {
			this.helper.tearDown();
		} catch (NullPointerException ex) {
			// ignore this on tear down
			System.out.println("NullPointerException when shutting down helper");
		}
	}

	
	@Test
	public void testUpload() throws IOException {
		assertThat(workingPngBytes).isNotNull();
		
		ImageProcess ip=new ImageProcess();
		ip.processUpload(workingPngBytes, "image.png");
	}
	
	@Test
	public void testReprocess() throws IOException {
		assertThat(workingPngBytes).isNotNull();
		
		ImageProcess ip=new ImageProcess();
		ip.processUpload(workingPngBytes, "image.png");
		ip.processSmall("image.png", "image-small.png");
	}
	
	@Test
	public void testReprocessFailure() throws IOException {
		assertThat(failingPngBytes).isNotNull();
		
		ImageProcess ip=new ImageProcess();
		ip.processUpload(failingPngBytes, "image.png");
		
		// TODO(bk) This shouldn't fail -- but for test case reasons it makes sense that we expect it!!
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage("Failed to read image");
		ip.processSmall("image.png", "image-small.png");
	}
	
}
