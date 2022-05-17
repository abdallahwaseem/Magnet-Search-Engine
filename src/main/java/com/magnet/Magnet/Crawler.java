package com.magnet.Magnet;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

public class Crawler implements Runnable {

    public static int numThreads;
    public static int numUrls;
    public static String[] urls;
    public static ConcurrentHashMap<String, PageContent> crawledPages;
    public static ConcurrentHashMap<String, Boolean> visitedUrls;
    public static ConcurrentHashMap<String, Boolean> urlsToBeCrawled;
    // store compact version of the crawled pages to avoid storing the whole page content again
    public static ConcurrentHashMap<String, String> compactPages;
    // dataaccess object to access the database
    public static DataAccess dataAccess;

    public void run() {
        // convert thread name to int
        int threadNum = Character.getNumericValue(Thread.currentThread().getName().charAt(0));
        // print out the thread name
        System.out.println("Thread " + threadNum + " is running.");
        // divide the urls into chunks and run the threads
        int chunkSize = numUrls / numThreads;
        int start = chunkSize * threadNum;
        //shift start by modulus for other threads
        if (threadNum != 0)
            start += numUrls % numThreads;
            int end = start + chunkSize;
            // shift end by modulus for first thread
            if (threadNum == 0)
                end += numUrls % numThreads;
            // print start and end of each thread
            System.out.println("Thread " + threadNum + ": " + start + " to " + end);
            for (int i = 0; i < numThreads; i++) {
                try {
                    if(start < end)
                     crawlWebPage(Arrays.copyOfRange(urls, start, end), crawledPages, visitedUrls, urlsToBeCrawled, compactPages);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
            }
        }

    //function to crawl a webpage pass visited urls and urls to be crawled
    public void crawlWebPage(String[] urls, ConcurrentHashMap<String, PageContent> crawledPages,
                                    ConcurrentHashMap<String, Boolean> visitedUrls,
                                    ConcurrentHashMap<String, Boolean> urlsToBeCrawled,
                                    ConcurrentHashMap<String,String> compactPages) throws IOException, URISyntaxException {
        //foreach url in urls to be crawled
        for (String url : urls) {
            //if url visited, this may happen if the program interrupted and started the crawling list again
            if (!visitedUrls.containsKey(url)) {
                System.out.println(Thread.currentThread().getName() + ":Crawling... " + url);
                // check if url is allowed to be crawled by robots.txt
                if (UrlUtils.isDisallowedByRobots(url)) {
                    System.out.println(Thread.currentThread().getName() + ":Disallowed by robots.txt: " + url);
                    //mark url as visited
                    visitedUrls.put(url, true);
                    // add url to database
                    dataAccess.addVisitedUrl(url);
                    continue;
                }
                //create a document object
                Document doc = null;
                try {
                    doc = Jsoup.connect(url).get();
                } catch (IOException e) {
                    System.out.println(Thread.currentThread().getName() + ":Can't Crawl... " + url);
                    //mark url as visited
                    visitedUrls.put(url, true);
                    // add url to database
                    dataAccess.addVisitedUrl(url);
                    continue;
                }// catch malformed url
                catch (Exception e) {
                    System.out.println(Thread.currentThread().getName() + ":Can't Crawl Malformed URL... " + url);
                    //mark url as visited
                    visitedUrls.put(url, true);
                    // add url to database
                    dataAccess.addVisitedUrl(url);
                    continue;
                }
                String compactPage = UrlUtils.compactPage(doc.body().text());
                // print compact page
                System.out.println(Thread.currentThread().getName() + ":Compact Page: " + compactPage);
                // check if compact version of body text is already in the compactPages
                if (compactPages != null && compactPages.containsKey(compactPage)) {
                    //mark url as visited
                    visitedUrls.put(url, true);
                    // add url to database
                    dataAccess.addVisitedUrl(url);
                    continue;
                }
                //get working directory
                String workingDir = System.getProperty("user.dir");
                String fileName = visitedUrls.size() + Thread.currentThread().getName();
                //create file if not exists to store the html
                File file = new File(workingDir+"\\html_files\\" + fileName + ".html");
                file.createNewFile();
                //create file writer
                FileWriter fw = new FileWriter(file);
                //write the html to the file
                fw.write(doc.html());
                //close the file writer
                fw.close();
                //add compact version of body text to compactPages
                compactPages.put(compactPage, url);
                //mark url as visited
                visitedUrls.put(url, true);
                // add url to database
                dataAccess.addVisitedUrlandCompactPagesFilename(url, compactPage, fileName);
                //create page content object
                PageContent pageContent = new PageContent();
                //add webpage title
                pageContent.title = doc.title();
                //add description from document object.
                Elements meta = doc.select("meta[name=description]");
                //check if description is not null
                if (meta.attr("content") != null) {
                    //add description
                    pageContent.description = meta.attr("content");
                }
                //Get keywords from document object.
                Elements metaKeywords = doc.select("meta[name=keywords]");
                //check if keywords is not null
                if (metaKeywords.attr("content") != null) {
                    //add keywords
                    pageContent.keywords = metaKeywords.attr("content");
                }
                //add body text
                pageContent.body = doc.body().text();
                //add page content to crawled pages
                crawledPages.put(url, pageContent);
                //get all hyperlinks from Document
                Elements hyperlinks = doc.select("a[href]");
                System.out.println( Thread.currentThread().getName() + ": Found " + hyperlinks.size() + " hyperlinks");
                ConcurrentHashMap<String, Boolean> urlsTobeSentToDB = new ConcurrentHashMap<>();
                //iterate through all hyperlinks
                for (Element link : hyperlinks) {
                    //normalize link
                    String linkUrl = link.attr("abs:href");
                    try {
                        linkUrl = UrlUtils.normalizeUrl(linkUrl);
                    } catch (URISyntaxException e) { }
                    //check if link is not visited
                    if (!visitedUrls.containsKey(linkUrl)) {
                            //add link to urls to be crawled
                            urlsToBeCrawled.put(linkUrl, true);
                            urlsTobeSentToDB.put(linkUrl, true);
                    }
                }
                //add urls to database
                dataAccess.addUrlsToBeCrawled(urlsTobeSentToDB);
            }
        }
    }
}
