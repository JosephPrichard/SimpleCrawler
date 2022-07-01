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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WikipediaWebCrawlerMt
{
    private static final String WEBSITE = "https://en.wikipedia.org";
    private static final String BASE_PATH = "/wiki";

    private final WikipediaGraph wikipediaGraph;
    private final ExecutorService executorService;
    private final Map<String, Boolean> visitedPages = new ConcurrentHashMap<>();

    public WikipediaWebCrawlerMt(int threads, String file) throws IOException {
        wikipediaGraph = new WikipediaGraph(new FileOutputStream(file));
        executorService = Executors.newFixedThreadPool(threads);
    }

    /**
     * Start the crawling by adding the first crawl task on the executor
     * @param url to start the crawl
     */
    public void crawl(String url) {
        executorService.execute(runCrawl(url));
    }

    /**
     * Returns a lambda expression which fetches the html from the url,
     * then recursively executes on each link in the html
     * @param url base case
     * @return runnable expression for our executor thread pool
     */
    private Runnable runCrawl(String url) {
        return () -> {
            // fetch html page from the url
            String html = fetch(url);
            // extract all neighboring links from the page
            List<String> links = parse(html);
            // add the page to the graph
            wikipediaGraph.addEntry(url, links);
            // recursively schedule this task on each neighboring link
            for (String link : links) {
                executorService.execute(runCrawl(link));
                visitedPages.put(link, true);
            }
        };
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
                if (visitedPages.get(url) == null) {
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
        new WikipediaWebCrawlerMt(30, "crawl_log.txt")
            .crawl("https://en.wikipedia.org/wiki/United_States");
    }
}
