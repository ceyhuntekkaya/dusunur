package tr.okul.iletisim.print;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
public class PrintPageCounter {

	public Integer count(Resource pdf) {
		try (PDDocument doc = Loader.loadPDF(pdf.getFile())) {
			return doc.getNumberOfPages();
		}
		catch (Exception e) {
			return null;
		}
	}

}
