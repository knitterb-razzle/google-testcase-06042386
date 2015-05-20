package com.wolfapp.testcase;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.logging.Logger;

import com.google.appengine.api.images.Image;
import com.google.appengine.api.images.ImagesService;
import com.google.appengine.api.images.ImagesServiceFactory;
import com.google.appengine.api.images.Transform;
import com.google.appengine.api.images.ImagesService.OutputEncoding;
import com.google.appengine.tools.cloudstorage.GcsFileMetadata;
import com.google.appengine.tools.cloudstorage.GcsFileOptions;
import com.google.appengine.tools.cloudstorage.GcsFilename;
import com.google.appengine.tools.cloudstorage.GcsInputChannel;
import com.google.appengine.tools.cloudstorage.GcsOutputChannel;
import com.google.appengine.tools.cloudstorage.GcsService;
import com.google.appengine.tools.cloudstorage.GcsServiceFactory;
import com.google.appengine.tools.cloudstorage.RetryParams;

public class ImageProcess {
	private static final Logger log = Logger.getLogger(ImageProcess.class.getName());

	private static final GcsService gcsService = GcsServiceFactory.createGcsService(RetryParams.getDefaultInstance());

	public void processUpload(byte[] data, String filename) throws IOException {
		ImagesService imagesService = ImagesServiceFactory.getImagesService();
		Image img = ImagesServiceFactory.makeImage(data);
		
		if (img.getWidth()>2000 || img.getHeight()>2000) {
			log.warning("Size "+img.getHeight()+"x"+img.getWidth()+" -> 2000x2000");
			Transform conversion = ImagesServiceFactory.makeResize(2000, 2000, false);
			Image newImg = imagesService.applyTransform(conversion, img, OutputEncoding.PNG);
			img = newImg;
		}
		log.info("Size "+img.getHeight()+"x"+img.getWidth());

		GcsFilename file = new GcsFilename("testcase_06042386", filename);
		GcsFileOptions.Builder options = new GcsFileOptions.Builder().mimeType("image/png");
		GcsOutputChannel outputChannel = gcsService.createOrReplace(file, options.build());
		outputChannel.write(ByteBuffer.wrap(data));
		outputChannel.close();
		
	}
	
	public void processSmall(String inFilename, String outFilename) throws IOException {
		GcsFilename file = new GcsFilename("testcase_06042386", inFilename);
		
		GcsInputChannel inputChannel = gcsService.openPrefetchingReadChannel(file, 0, 1024*1024);
		GcsFileMetadata gfm=gcsService.getMetadata(file);
		if (gfm==null) // file isn't found
			return;
		int fileSize = (int) gfm.getLength();
		ByteBuffer buf=ByteBuffer.allocate(fileSize);
		// TODO(bk) this is a blocking call that reads everything into memory, may want to convert this to a stream (for large objects)
		// https://cloud.google.com/appengine/docs/java/googlecloudstorageclient/getstarted#reading_gcs_data_into_an_object
		inputChannel.read(buf);
		inputChannel.close();
		byte[] data = buf.array();

		ImagesService imagesService = ImagesServiceFactory.getImagesService();
		Image img = ImagesServiceFactory.makeImage(data);
		log.info("Size (before) "+img.getHeight()+"x"+img.getWidth());

		Transform conversion = ImagesServiceFactory.makeResize(100, 100, false);
		Image newImg = imagesService.applyTransform(conversion, img, OutputEncoding.PNG);
		img = newImg;
		log.info("Size (after) "+img.getHeight()+"x"+img.getWidth());

		GcsFilename fileout = new GcsFilename("testcase_06042386", outFilename);
		GcsFileOptions.Builder options = new GcsFileOptions.Builder().mimeType("image/png");
		GcsOutputChannel outputChannel = gcsService.createOrReplace(fileout, options.build());
		outputChannel.write(ByteBuffer.wrap(data));
		outputChannel.close();

		
	}
	
}
