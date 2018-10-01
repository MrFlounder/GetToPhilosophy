import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.LinkedHashSet;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class Server {

    static Connection conn = null;

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
        server.createContext("/test", new FindWikiPhilosophy());
        server.setExecutor(null); // creates a default executor
        conn = connectToDB();
        System.out.println(conn.toString());
        System.out.println("Starting server, listening to port: 8000");
        server.start();
    }
    // Simple http handler to register for router
    static class FindWikiPhilosophy implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            StringBuilder resText = new StringBuilder();
            LinkedHashSet<String> path = new LinkedHashSet<>();
            resText.append("Begining:\n");
            String url = convertStreamToString(t.getRequestBody());
            url = java.net.URLDecoder.decode(url, "UTF-8").replace("url=", "");
            findPhilosophy(url, resText, path);
            insertPathToDB(path);
            System.out.println(resText.toString()); // debug purpose only
            t.sendResponseHeaders(200, resText.toString().length());
            OutputStream os = t.getResponseBody();
            os.write(resText.toString().getBytes());
            os.close();
        }
    }

    static String convertStreamToString(java.io.InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    static void findPhilosophy(String url, StringBuilder resText, LinkedHashSet<String> path) throws IOException {
        if (!url.startsWith("http://en.wikipedia.org/wiki/") && !url.startsWith("https://en.wikipedia.org/wiki/")) {
            resText.append("Input url not valid. Please check input box.\n");
            return;
        }
        gettingToPhilosophy(url, resText, path);
        return;
    }

    // GettingToPhilosophy
    static void gettingToPhilosophy(String url, StringBuilder resText, LinkedHashSet<String> path) throws IOException {
        System.out.println("Checking: " + url);
        if (url.equalsIgnoreCase("http://en.wikipedia.org/wiki/philosophy") || url.equalsIgnoreCase("http://en.wikipedia.org/wiki/philosophy")) {
            resText.append(url + "\n");
            resText.append("Found philosophy!\n");
            resText.append("Total jumps taken: " + path.size() + "\n");
            return;
        } else if (path.contains(url)) {
            resText = new StringBuilder();
            resText.append("Entered a infinite loop, terminated.\n");
            return;
        } else {
            String nextUrl = grabNextValidUrl(url);
            path.add(url);
            resText.append(url + "\n");
            gettingToPhilosophy(nextUrl, resText, path);
            return;
        }
    }

    public static String grabNextValidUrl(String url) throws IOException {
        String nextLink = "";
        Document doc = Jsoup.connect(url).get();          // builds URL from string
        Elements links = doc.select("p > a");           // selects only links within <p> tags

        for (int i = 0; i < links.size(); i++) {        // chooses the first suitable link
            if (isLinkValid(links.get(i))) {
                nextLink = links.get(i).toString();
                break;
            }
        }
        if (nextLink == "") {
            return "";
        }
        return "http://en.wikipedia.org" + nextLink.substring(9, nextLink.indexOf("\"", 10));
    }

    static boolean isLinkValid(Element link) {
        String url = link.toString();
        boolean passFiltering =
            !url.contains("Help:") &&
            !url.contains("File:") &&
            !url.contains("Wikipedia:") &&
            !url.contains("wiktionary.org/") &&
            url.contains("/wiki/") &&
            !url.contains("Latin") &&
            !url.contains("Greek") &&
            !url.contains("wiktionary");
        // TODO: Check not in parenthesis
        // Check not italic
        boolean validParent = link.parent().tagName() != "i";
        return passFiltering && validParent;
    }
    // Get db connection
    public static Connection connectToDB() {
        System.out.println("-------- Connecting To DB started ------------");
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            System.out.println("Couldn't find jdbc driver!");
            e.printStackTrace();
            return null;
        }
        System.out.println("PostgreSQL JDBC Driver Registered!");
        Connection connection = null;
        try {
            connection = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:5432/bento_db", "postgres","000000");
        } catch (SQLException e) {
            System.out.println("Connection Failed! Check output console");
            e.printStackTrace();
            return null;
        }
        if (connection != null) {
            System.out.println("Connection established.");
        } else {
            System.out.println("Failed to make connection!");
        }
        return connection;
    }


    public static void insertPathToDB(LinkedHashSet<String> path) {
        String SQL = "INSERT INTO path_track(path) "
                + "VALUES(?)";
        try {
            PreparedStatement pstmt = conn.prepareStatement(SQL);
            pstmt.setString(1, path.toString());
            pstmt.executeUpdate();
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
        return;
    }
}
