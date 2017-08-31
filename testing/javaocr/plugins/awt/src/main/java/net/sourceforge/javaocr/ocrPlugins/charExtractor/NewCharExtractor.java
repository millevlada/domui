package net.sourceforge.javaocr.ocrPlugins.charExtractor;

import net.sourceforge.javaocr.filter.SauvolaBinarisationFilter;
import net.sourceforge.javaocr.ocr.PixelImage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
 * Created on 31-8-17.
 */
public class NewCharExtractor {
	public static final int SAUVOLA_WINDOW = 1;
	public static final int MEDIAN_WINDOW = 3;
	private static final double SAUVOLA_WEIGHT = 0.20;




	public static void main(String[] args) throws Exception {
		NewCharExtractor ce = new NewCharExtractor();

		BufferedImage srcBi = ImageIO.read(new File("/tmp/input-2.png"));
		int[] buffer = new int[srcBi.getHeight() * srcBi.getWidth()];

		int offset = 0;
		for(int y = 0; y < srcBi.getHeight(); y++) {
			for(int x = 0; x < srcBi.getWidth(); x++) {
				buffer[offset++] = srcBi.getRGB(x, y);
			}
		}

		PixelImage destinationImage = new PixelImage(srcBi.getWidth() + SAUVOLA_WINDOW, srcBi.getHeight() + SAUVOLA_WINDOW);

		SauvolaBinarisationFilter sauvolaBinarisationFilter = new SauvolaBinarisationFilter(0, 1, destinationImage, 256, SAUVOLA_WEIGHT, SAUVOLA_WINDOW);

		//srcBi.getRaster().getPixels(0, 0, srcBi.getWidth(), srcBi.getHeight(), buffer);

		PixelImage pi = new PixelImage(buffer, srcBi.getWidth(), srcBi.getHeight());

		//SauvolaImageProcessor imageProcessor = new SauvolaImageProcessor(srcBi.getWidth(), srcBi.getHeight(), srcBi.getWidth(), srcBi.getHeight(), 0, 1);

		IntegralImageSlicer slicer = new IntegralImageSlicer(new PixelImage(srcBi.getWidth(), srcBi.getHeight()));

		sauvolaBinarisationFilter.process(pi);

		offset = 0;
		buffer = destinationImage.pixels;
		for(int y = 0; y < srcBi.getHeight(); y++) {
			for(int x = 0; x < srcBi.getWidth(); x++) {
				srcBi.setRGB(x, y, buffer[offset++]);
			}
		}
		ImageIO.write(srcBi, "png", new File("/tmp/o1.png"));

		//List<List<Image>> lists = slicer.sliceUp(pi);



	}
}
