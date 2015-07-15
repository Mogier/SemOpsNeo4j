package semopsneo4j;

import java.io.IOException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class WikipediaCrawler {
	protected String baseURL;

	public WikipediaCrawler(String baseURL) {
		super();
		this.baseURL = baseURL;
	}
	
	public Elements getWikilinks(String tag) {
		try {
			Document doc = Jsoup.connect(baseURL+tag).get();
			Element para = doc.select("div#mw-content-text > p").first();
			Elements links = para.select("a[href^=/wiki/]");
//			System.err.println(tag);
//			for(Element link : links)
//				System.out.println(link.attr("title"));
			return links;
		} catch (IOException e) {
			System.err.println("Page " + tag + " not found");
			return null;
		}
	}
}
