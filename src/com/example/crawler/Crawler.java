package src.com.example.crawler;

import java.io.*;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;


/**
 * Created by amandaholl on 4/11/16.
 */
public class Crawler {
    private static File repository = new File("repository");
    private static FileWriter report = null;
    private static BufferedWriter writer_report;
    private static Set<String> pages_crawled = new HashSet<String>();
    private static List<String> pages_to_crawl = new LinkedList<String>();
    private static int max_pages;

    public static ConcurrentSkipListSet<String> hosts_visited = new ConcurrentSkipListSet<String>();
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/535.1 (KHTML, like Gecko) Chrome/13.0.782.112 Safari/535.1";

    public static void main(String[] args) throws IOException {

        /*  If the "repository" directory already exists delete it
         *  by removing all of its files and then the directory itself.
         *  Ensures only pages from this run are added to the folder.
         */
        if(repository.exists()){
            String[] entries = repository.list();
            for(String s: entries){     // Delete all pages in repository
                //System.out.println("here");
                File current = new File(repository.getPath(),s);
                current.delete();
            }
            repository.delete();    // Then delete repository
        }

        /*  Create a new "repository" directory to hold all of the
         *  pages crawled.
         */
        if(repository.mkdir())
            System.out.println("Repository created.");
        else
            System.out.println("Failed to create repository."); // Error message

        /*  Create a new report.html file for the results */
        try{
            report = new FileWriter("report.html");     // Overwrite contents of current file
        }catch(Exception e){
            System.out.println("Error initially opening report.");
        }

        writer_report = new BufferedWriter(report);
        writer_report.write("<html><head><title>Report</title></head><body>");
        writer_report.newLine();

        String[] seed = readCSV();

        /*  Check the seed value returned */
        if(seed != null){
            String domain_restriction = "";
            max_pages = Integer.parseInt(seed[1]);

            /*  Check if a domain restriction is supplied in specifications.csv */
            if(seed.length > 2) // If it is, capture it
                domain_restriction = seed[2];

            System.out.println(domain_restriction);

            /*  Start crawling with the specifications pulled from specifications.csv */
            crawl(seed[0], domain_restriction);
        }
        else{
            System.out.println("Error getting seed.");
        }

    }

    public static String[] readCSV(){
        String csv = "specifications.csv";
        String[] seed = new String[3];

        BufferedReader reader = null;

        String buffer = "";
        String separator = ",";

        try{
            FileReader input = new FileReader(csv);
            reader = new BufferedReader(input);

            while((buffer = reader.readLine()) != null){
                seed = buffer.split(separator);
            }
        }catch (FileNotFoundException e) {
            System.out.println("specifications.csv not found.");
            return null;
        }
        catch (IOException e) {
            System.out.println("Error reading specifications.csv.");
            return null;
        }

        return seed;    // Return the seed
    }

    public static void crawl(String URL, String domain_restriction){
        while(pages_crawled.size() < max_pages){
            String current_page;

            if(!pages_to_crawl.isEmpty()){  // If there are pages to crawl
                do{
                    current_page = pages_to_crawl.remove(0);
                }while(pages_crawled.contains(current_page));
            }
            else
                current_page = URL;

            //System.out.println(pages_crawled.size());

            /*  If the domain has been visited in the last second, don't crawl this page */
            if(!checkDomain(current_page)) {
                System.out.println("be polite");
                pages_to_crawl.add(current_page);   // Add the page back in the list of pages to crawl b/c politeness protocol says we can't right now
                pages_crawled.remove(current_page);
                continue;
            }

            pages_crawled.add(current_page);

            /*  Crawling this page is disallowed by site's robots.txt
             *  Return and do not crawl.
             */
            if(!checkRobots(current_page))
                continue;

            /*  There was a problem with crawling the page. Ignore and keep crawling */
            if(!getPage(current_page, domain_restriction))
                continue;
        }
        System.out.println("\n\nDone-----------------");

        try {
            writer_report.write("</body></html>");
            writer_report.newLine();
            writer_report.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*  Check robots will open up the robots.txt file for the URL's host
     *  and return true if robots.txt allows the page to be crawled.
     *  Otherwise, the function returns false.
     */
    public static boolean checkRobots(String URL){

        /*  Convert URL string into URL element */
        URL url;
        try{
            url = new URL(URL);
        }
        catch(MalformedURLException e){
            System.out.println("Could not create URL for page");
            return false;
        }

        /*  Get the name of the host from the URL element */
        String host = url.getHost();

        /*  Create the URL for the robots.txt file. Should be of the type http://host/robots.txt */
        String robots_address = "http://" + host + "/robots.txt";
        URL robots_url;

        try{
            robots_url = new URL(robots_address);
        }
        catch(MalformedURLException e){
            System.out.println("Could not create URL for robots.txt");
            return false;
        }
        //System.out.println(robots_url.toString());


        try{
            BufferedReader robots_stream = new BufferedReader(new InputStreamReader(robots_url.openStream()));
            String line = "";
            String disallow = "Disallow:";
            String page = url.getFile();

            while((line = robots_stream.readLine()) != null){
                //System.out.println(line);
                int offset = 0;

                /*   Check if the line is a disallow statement we need to follow */
                if(line.indexOf(disallow, offset) != -1){

                    offset += disallow.length();

                    /*  Tokenize the line read, starting after the phrase "Disallow:"
                     *  to get the directory banned
                     */
                    String path = line.substring(offset);
                    StringTokenizer tokens = new StringTokenizer(path);

                    if(tokens.hasMoreTokens()){
                        String forbidden = tokens.nextToken();

                        //System.out.println("Forbidden " + forbidden);

                        /*  Check that the URL we want to crawl is not in the banned
                         *  directory
                         */
                        if(page.indexOf(forbidden) == 0){
                            System.out.println("CRAWL FORBIDDEN on this page");
                            return false;
                        }
                    }


                }
            }
            //close stream ?
        }
        catch(IOException e){
            System.out.println("Error reading from robots.txt");
        }

        return true;
    }

    /*  Check if the page has been crawled recently */
    public static boolean checkDomain(String URL){
        String domain = "";
        try {
            domain = new URI(URL).getHost();
           // System.out.println(domain);
        }
        catch(URISyntaxException e){
            System.out.println("Error getting host");
            return false;
        }

        /*  Check if crawling a page from a domain visited within the last second.
         *  If so, do not crawl. Otherwise, go ahead and crawl.
         */

        if(domain != null && !hosts_visited.contains(domain)) { // If domain not in list of domains visited
            hosts_visited.add(domain);  // Add it
            System.out.println("\n"+ hosts_visited+ "\n");
            new DomainTimer(domain);
            return true;
        }else
            return false;   // Don't crawl this page

    }

    public static boolean getPage(String URL, String domain_restriction){
        List<String> links = new LinkedList<String>();
        String report_content = "";

        try {
            /* Connect to page */
            Connection connection = Jsoup.connect(URL).userAgent("USER_AGENT");
            Document doc = connection.get();


            if(!connection.response().contentType().contains("text/html")){
                System.out.println("Error: Page doesn't contain HTML content.");
                return false;
            }

            Elements img = doc.getElementsByTag("img");
            Elements outlinks = doc.select("a[href~=\\bhttps?://(?!\\S+(?:jpe?g|png|bmp|gif))\\S+]");   // Make sure no images fetched

            /* Write requirements to report.html */
            FileWriter page = null;

            BufferedWriter writer_page = null;


            /*  Generate the filename for the local copy of file. This will be the title of the page followed by a unqiue
             *  number to prevent conflicts between pages that have the same title since titles aren't unique.
             */
            String filename = "repository/"+doc.title().replaceAll("[^A-Za-z0-9()\\[\\]]", "")+pages_crawled.size()+".html";


            /* Download file */
            try{
                page = new FileWriter(filename);
                writer_page = new BufferedWriter(page);

                writer_page.write(doc.outerHtml());
                writer_page.newLine();
                writer_page.close();
            }
            catch(Exception e){
                System.out.println("Error downloading local copy.");
            }

            String title;
            if(doc.title().isEmpty())
                title = "N/A";
            else
                title = doc.title();

            /*  Req. 1: Write clickable title, links to live site */
            report_content += "<h3><a href=\""+URL+"\"> "+title+" </a></h3>";
            /*  Req. 2: Write link to local copy of the page */
            report_content += "<p><a href='"+filename+"'>[Local copy]</a>";
            /*  Req. 3: Write HTTP status code received (ex. 200, 404, etc.) */
            report_content += "<p><strong>Status Code:</strong> "+connection.response().statusCode()+"</p>";
            /*  Req. 4: Write the number of images on the page */
            report_content += "<p><strong>Number of images:</strong> "+img.size()+"</p>";
            /*  Req. 5: Write the number of outlinks contained on the page */
            report_content += "<p><strong>Number of outlinks:</strong> "+outlinks.size()+"</p>";


            for(Element link: outlinks){
                /*  If there is no domain restriction, add the link to the list of pages to crawl */
                if(domain_restriction == ""){
                    //System.out.println("no restrict");
                    links.add(link.absUrl("href"));
                }
                /*  If there is domain restriction (from specifications.csv, add the link to the list
                 *  pages to crawl only if it conforms to the domain restriction to the set of pages
                 *  to crawl.
                 */
                else{
                    if(link.attr("href").contains(domain_restriction))  //Check conformity to domain restriction
                        links.add(link.absUrl("href"));
                }
            }

            //System.out.println(URL); // Test Statement

            /*  Add the valid outlinks to the queue of pages to crawl */
            pages_to_crawl.addAll(links);

        }
        catch(HttpStatusException e){
            /*  Req. 1: Write clickable title, links to live site */
            report_content += "<h3><a href=\""+URL+"\"> "+e.getUrl()+" </a></h3>";
            /*  Req. 3: Write HTTP status code received (ex. 200, 404, etc.) */
            report_content += "<p><strong>Status Code:</strong> "+e.getStatusCode()+"</p>";
        }
        catch(IOException ioe) {
            System.out.println("IO Exception.");
            System.out.println(URL);
            return false;
        }

        writeToReport(report_content);

        return true;
    }

    public static void writeToReport(String report_content){
        System.out.println("content" + report_content);
        try {
            writer_report.write(report_content);
            writer_report.newLine();
        } catch (IOException e) {
            System.out.println("Error writing to report.html");
        }
        return;
    }
}