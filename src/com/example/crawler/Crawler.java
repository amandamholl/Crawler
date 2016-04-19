package com.example.crawler;

import java.io.*;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.Timer;
import java.util.TimerTask;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.jsoup.Connection;
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
    private static Set<String> pages_crawled = new HashSet<String>();
    private static List<String> pages_to_crawl = new LinkedList<String>();
    private static int max_pages;
    private static String prev_page = "";
    private static ConcurrentSkipListSet<String> hosts_visited = new ConcurrentSkipListSet<String>();
    private static Timer remove;

    public static void main(String[] args){

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
        if (repository.mkdir()) {
            System.out.println("Repository created.");
        } else {
            System.out.println("Failed to create repository."); // Error message
        }

        /*  Create a new report.html file for the results */
        try {
            report = new FileWriter("report.html");     // Overwrite contents of current file
        }
        catch(Exception e){
            System.out.println("Error initially opening report.");
        }

        String[] seed = readCSV();

        /*  Check the seed value returned */
        if(seed != null){
            String domain_restriction = "";
            max_pages = Integer.parseInt(seed[1]);
            /*  Check if a domain restriction is supplied in specifications.csv */
            if(seed[2] != null) // If it is, capture it
                domain_restriction = seed[2];
            System.out.println(domain_restriction);

            TimerTask removeDomains = new TimerTask(){
                @Override
                public void run() {
                    System.out.println("\n HERE \n");
                    if(!hosts_visited.isEmpty())
                        hosts_visited.remove(hosts_visited.pollLast());
                }
            };

            remove = new Timer("MyTimer");
            remove.scheduleAtFixedRate(removeDomains, 0, 1000);


            /*  Start crawling with the specifications pulled from specifications.csv */
            crawl(seed[0], seed[2]);
            //crawl("http://www.usc.edu", "www.usc.edu"); //test
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

            /*  If the domain has been visited in the last second, don't crawl this page */
            if(!checkDomain(current_page)) {
                System.out.println("BE POLITE");
                pages_to_crawl.add(current_page);   // Add the page back in the list of pages to crawl b/c politeness protocol says we can't right now
                continue;
            }

            pages_crawled.add(current_page);

            System.out.println(current_page + " "+ prev_page);

            /*  Crawling this page is disallowed by site's robots.txt
             *  Return and do not crawl.
             */
            if(!checkRobots(current_page)){
                continue;
            }

            getPage(current_page, domain_restriction);
        }
        System.out.println("\n\nDone-----------------");
        remove.cancel();
    }

    /*  Check robots will open up the robots.txt file for the URL's host
     *  and return true if robots.txt allows the page to be crawled.
     *  Otherwise, the function returns false.
     */
    public static boolean checkRobots(String URL){
        String host = "";
        try {
            host = new URI(URL).getHost();
            //System.out.println("Host is: "+ host);
        }
        catch(URISyntaxException e){
            System.out.println("Error getting host");
        }
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

        URL url;
        try{
            url = new URL(URL);
        }
        catch(MalformedURLException e){
            System.out.println("Could not create URL for page");
            return false;
        }

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
                // Get the folder disallowed
                // if url is in there, don't crawl, return false
            }
            //close stream ?
        }
        catch(IOException e){
            System.out.println("Error reading from robots.txt");
        }

        return true;
    }

    public static boolean checkDomain(String URL){
        String domain = "";
        try {
            domain = new URI(URL).getHost();
            System.out.println(domain);
        }
        catch(URISyntaxException e){
            System.out.println("Error getting host");
            return false;
        }

        /*  Check if crawling a page from a domain visited within the last second.
         *  If so, do not crawl. Otherwise, go ahead and crawl.
         */
        if(hosts_visited.isEmpty() || !hosts_visited.contains(domain)) { // If domain not in list of domains visited
            hosts_visited.add(domain);  // Add it
            System.out.println("\n"+ hosts_visited+ "\n");
        }else
            return false;   // Don't crawl this page

        return true;

    }

    public static void getPage(String URL, String domain_restriction){
        List<String> links = new LinkedList<String>();

        try {
            /* Connect to page */
            Connection connection = Jsoup.connect(URL);
            Document doc = connection.get();

            Elements img = doc.getElementsByTag("img");
            Elements outlinks = doc.select("a[href]");

            /* Write requirements to report.html */
            FileWriter page = null;

            BufferedWriter writer_report = null;
            BufferedWriter writer_page = null;

            /* Generate report */
            try{
                System.out.println(doc.title());
                String filename = "repository/"+doc.title()+".html";

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
                report = new FileWriter("report.html", true);   // Append new entry to report.html
                writer_report = new BufferedWriter(report);

                /*  Req. 1: Write clickable title, links to live site */
                writer_report.write("<h3><a href='"+URL+"'> "+doc.title()+" ("+URL+")</a></h3>");
                /*  Req. 2: Write link to local copy of the page */
                writer_report.write("<p><a href='"+filename+"'>[Local copy]</a>");
                /*  Req. 3: Write HTTP status code received (ex. 200, 404, etc.) */
                writer_report.write("<p><strong>Status Code:</strong> "+connection.response().statusCode()+"</p>");
                /*  Req. 4: Write the number of images on the page */
                writer_report.write("<p><strong>Number of images:</strong> "+img.size()+"</p>");
                /*  Req. 5: Write the number of outlinks contained on the page */
                writer_report.write("<p><strong>Number of outlinks:</strong> "+outlinks.size()+"</p>");
                writer_report.newLine();
                writer_report.close();

            }
            catch(Exception e){
                System.out.println("Error writing to report.");
            }

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
        catch(IOException ioe)
        {
            System.out.println("Error fetching page.");
            return;
        }
    }


}
