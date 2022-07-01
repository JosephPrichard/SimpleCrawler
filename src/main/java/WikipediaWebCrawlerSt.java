import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

public class WikipediaWebCrawlerSt
{
    private static final String WEBSITE = "https://en.wikipedia.org";
    private static final String BASE_PATH = "/wiki";

    private final WikipediaGraph wikipediaGraph;
    private final Deque<String> queue = new ArrayDeque<>();
    private final HashSet<String> visitedPages = new HashSet<>();

    public WikipediaWebCrawlerSt(String file) throws IOException {
        wikipediaGraph = new WikipediaGraph(new FileOutputStream(file));
    }

    /**
     * Performs single threaded breadth first search using a simple queue
     * @param startUrl the url to start crawling on
     */
    public void crawl(String startUrl) {
        // add base case to structures
        queue.add(startUrl);
        visitedPages.add(startUrl);
        // breadth first search
        while (!queue.isEmpty()) {
            String url = queue.poll();
            // fetch html page from the url
            String html = fetch(url);
            // extract all neighboring links from the page
            List<String> links = parse(html);
            // add the page to the graph
            wikipediaGraph.addEntry(url, links);
            // add neighboring links to the queue
            queue.addAll(links);
            visitedPages.addAll(links);
        }
    }

    /**
     * Parse the links from the html content
     * @param html content
     * @return the links from the page in a list
     */
    private List<String> parse(String html) {
        List<String> links = new ArrayList<>();
        Document doc = Jsoup.parse(html);
        // extract all a tags from the html page
        for (Element link : doc.select("a")) {
            // extract href link from a tag
            String linkHref = link.attr("href");
            // check if it matches the conditions
            if (matchesBase(linkHref)) {
                String url = WEBSITE + linkHref;
                // make sure we don't visit a page more than once
                if (!visitedPages.contains(url)) {
                    links.add(url);
                }
            }
        }
        return links;
    }

    private static boolean matchesBase(String fullPath) {
        return fullPath.length() >= BASE_PATH.length()
            && BASE_PATH.equals(fullPath.substring(0, BASE_PATH.length()));
    }

    /**
     * Fetch the html from a web page on the given link
     * @param link to fetch on
     * @return html
     */
    private String fetch(String link) {
        try {
            // create reader from the url to fetch html page
            URL url = new URL(link);
            URLConnection conn = url.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));

            // add the buffered input to a parsable string
            StringBuilder output = new StringBuilder();
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                output.append(inputLine);
            }
            in.close();

            return output.toString();
        } catch(IOException ex) {
            return "";
        }
    }

    public static void main(String[] args) throws IOException {
        new WikipediaWebCrawlerSt("crawl_log.txt")
            .crawl("https://en.wikipedia.org/wiki/Potato");
    }
}
