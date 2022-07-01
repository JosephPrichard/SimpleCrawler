import java.io.*;
import java.util.List;

public class WikipediaGraph
{
    private final PrintWriter pw;

    public WikipediaGraph(OutputStream os) {
        pw = new PrintWriter(os);
    }

    private String extractTopic(String url) {
        return url.substring(url.lastIndexOf('/') + 1);
    }

    /**
     * Add a graph entry to the wikipedia graph, this function is synchronized
     * due to file access
     * @param vUrl url of the vertex
     * @param eUrls urls of each edge the vertex has
     */
    public synchronized void addEntry(String vUrl, List<String> eUrls) {
        pw.append(extractTopic(vUrl))
            .append(" - ")
            .append(vUrl)
            .append("\n");
        for (String eUrl : eUrls) {
            pw.append(" |-")
                .append(extractTopic(eUrl))
                .append(" - ")
                .append(eUrl)
                .append("\n");
        }
        pw.append("\n");
    }
}
