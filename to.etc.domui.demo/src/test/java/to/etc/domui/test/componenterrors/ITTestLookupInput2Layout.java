package to.etc.domui.test.componenterrors;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import to.etc.domui.webdriver.core.AbstractWebDriverTest;
import to.etc.domui.webdriver.core.ScreenInspector;
import to.etc.domuidemo.pages.test.componenterrors.LookupInput2TestPage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
 * Created on 25-8-17.
 */
public class ITTestLookupInput2Layout extends AbstractWebDriverTest {
	@Before
	public void initializeScreen() throws Exception {
		wd().openScreenIf(this, LookupInput2TestPage.class);
	}

	@Test
	public void testOneLineForOne() throws Exception {
		//-- Both one and two must use only one line
		WebElement one = wd().getElement("one");
		Assert.assertTrue("Control one must span one line", one.getSize().height < 25);
	}

	@Test
	public void testOneLineForTwo() throws Exception {
		WebElement two = wd().getElement("two");
		Assert.assertTrue("Control two must span one line", two.getSize().height < 25);
	}

	@Ignore("Fails currently")
	@Test
	public void testOneCannotHaveInput() throws Exception {
		//-- One must not contain an input
		WebElement one = wd().getElement("one");
		Assert.assertTrue("One cannot have 'input' because it has no QuickSearch", one.findElements(By.tagName("input")).size() == 0);
	}

	@Test
	public void testTwoMustHaveInput() throws Exception {
		//-- Two MUST have an input
		WebElement two = wd().getElement("two");
		Assert.assertTrue("Two must have 'input' because it HAS QuickSearch", two.findElements(By.tagName("input")).size() == 1);
	}

	@Test
	public void labelMustBeAlignedOne() throws Exception {
		WebElement one = wd().getElement("one");
		String id = one.getAttribute("id");
		WebElement label = wd().getElement(By.cssSelector("label[for='" + id + "']"));

		Assert.assertEquals("Label and control for ONE must be on same Y", label.getLocation().getY(), one.getLocation().getY());
	}

	@Test
	public void labelMustBeAlignedTwo() throws Exception {
		WebElement two = wd().getElement("two");
		String id = two.getAttribute("id");
		WebElement label = wd().getElement(By.cssSelector("label[for='" + id + "']"));

		Assert.assertEquals("Label and control for TWO must be on same Y", label.getLocation().getY(), two.getLocation().getY());
	}

	@Test
	public void labelMustBeAlignedFontTwo() throws Exception {
		WebElement two = wd().getElement("two");
		String id = two.getAttribute("id");
		WebElement label = wd().getElement(By.cssSelector("label[for='" + id + "']"));


		Assert.assertEquals("Label and control for TWO must be on same Y", label.getLocation().getY(), two.getLocation().getY());
	}


	@Test
	public void labelOCRTest() throws Exception {
		wd().cmd().type("hello world").on(By.cssSelector("*[testid='two'] input"));
		Thread.sleep(1000);

		WebElement two = wd().getElement("two");
		ScreenInspector in = wd().screenInspector();
		if(in == null)
			return;

		BufferedImage bufferedImage = in.elementScreenshot(two);
		ImageIO.write(bufferedImage, "png", new File("/tmp/input-1.png"));
	}

}
