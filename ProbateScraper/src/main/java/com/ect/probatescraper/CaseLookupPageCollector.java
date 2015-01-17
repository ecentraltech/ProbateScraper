package com.ect.probatescraper;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.log4j.Logger;

/**
 * Traverses the open cases present on apps.ctprobate.gov by district to pull the individual 
 * case details and put locally.
 *
 */
public class CaseLookupPageCollector {
    private static final Logger LOG = Logger.getLogger(CaseLookupPageCollector.class);
    public static final String NEWLINE = System.getProperty("line.separator");
    public static final String FILESEP = File.separator;
    public static final String HTML_FILENAME_SUFFIX = PageParserProbateList.HTML_FILENAME_SUFFIX;
    public static final String CSV_FILENAME_SUFFIX = PageParserProbateList.CSV_FILENAME_SUFFIX;
    public static final String DATA_DIR = PageParserProbateList.DATA_DIR;

    static PrintWriter output;
    static Random random;
	static int sleepMin = 2;
	static int sleepMax = 20;
	static boolean useLocalListFiles = false;

    public static void main( String[] args ) {
        LOG.info("CaseLookupPageCollector...");
    	random = new Random();

        //Obtain main or first page of the lookup, it provides a portion of the list and 
        //links to all of the other pages in the list
        String pageLink = "http://apps.ctprobate.gov/caselookup?CaseType=0&District=PD05&CaseStatusOption=Open";
        LOG.info("Going after page: " + pageLink);
        String fileNameSuffix = PageParserProbateList.getJulianWithMillis();  
        
        //Remember the first page, since it contains links to all other pages 
        File mainCaseListPageFile;
        if (useLocalListFiles) {
            //read file from the local
            mainCaseListPageFile = new File(DATA_DIR, PageParserProbateList.FILENAME_PREFIX_CASE_LIST + "01.html");
        }
        else {
            String mainCaseFilename = PageParserProbateList.createCaseListFile(1, fileNameSuffix);
            mainCaseListPageFile = dumpPage(pageLink, new File(DATA_DIR, mainCaseFilename));
        }
        
        //Extract case details by case type
        if (!retrieveDetails(mainCaseListPageFile, fileNameSuffix)) {
			LOG.info("CaseLookupPageCollector...Failed to retrieveDetails for page 1");
			return;
        }
        
        //Extract all remaining pages from the list
        Map<Integer, String> pageLinks = PageParserProbateList.extractPageLinks (mainCaseListPageFile);
        //During development, I'm trying no to go after pages more than once. The server is painfully slow
        File currentCaseListFile;
        int startAtPage = 2;
        int endAtPage = 27;
        for (int pageNumber = startAtPage; pageNumber < 1000; pageNumber++) {
        	pageLink = pageLinks.get(new Integer(pageNumber));
        	if (pageLink == null) {
        		break;
        	}

        	LOG.info("Going after page " + pageNumber + ": " + pageLink);
        	String currentCaseListFilename;
            if (useLocalListFiles) {
                //read file from the local
            	currentCaseListFilename = PageParserProbateList.createCaseListFile(pageNumber, null);
            	currentCaseListFile = new File(DATA_DIR, currentCaseListFilename);
            }
            else {
            	//retrieve file and write it to local
            	currentCaseListFilename = PageParserProbateList.createCaseListFile(pageNumber, fileNameSuffix);
            	currentCaseListFile = dumpPage(pageLink, new File(DATA_DIR, currentCaseListFilename));
            }

            //Extract case details by case type
            if (!retrieveDetails(currentCaseListFile, fileNameSuffix)) {
    			LOG.info("CaseLookupPageCollector...Failed to retrieveDetails for page " + pageNumber);
    			return;
            }
        	
        	//should we stop now?
        	if (pageNumber >= endAtPage) {
        		break;
        	}
        	
        	//Avoid hammering the server, it is painfully slow already
        	//Use random sleep to see if special sleep time makes a difference
            int sleepTime = random.nextInt((sleepMax - sleepMin) + 1) + sleepMin;
            LOG.info("Next request in " + sleepTime + " seconds...");
        	try {
				Thread.sleep(sleepTime * 1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
				LOG.info("CaseLookupPageCollector...Failed trying to sleep inbetween calls");
				return;
			}
		}

        LOG.info("CaseLookupPageCollector...Complete");
    }

	private static boolean retrieveDetails(File mainCaseListPageFile, String fileNameSuffix) {
		List<String[]> detailsPageLinks = PageParserProbateList.extractDetailsLinks(mainCaseListPageFile, PageParserProbateList.CASE_TYPE_DECEDENT_ESTATE_REGULAR);
        for (String[] detailsPageLink : detailsPageLinks) {
        	LOG.info("Going after case number " + detailsPageLink[0] + ": " +  detailsPageLink[1]);
        	String fileName = PageParserProbateList.createCaseDetailFile(detailsPageLink[0], fileNameSuffix);
            dumpPage(detailsPageLink[1], new File(DATA_DIR, fileName));
        	
            //Avoid hammering the server, it is painfully slow already
        	//Use random sleep to see if special sleep time makes a difference
            int sleepTime = random.nextInt((sleepMax - sleepMin) + 1) + sleepMin;
            LOG.info("Next request in " + sleepTime + " seconds...");
        	try {
				Thread.sleep(sleepTime * 1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
				LOG.info("CaseLookupPageCollector...Failed trying to sleep inbetween calls");
				return false;
			}
        }
        
        return true;
	}
    
    static final File dumpPage(String link, File outputFile) {
        LOG.info("Page Dump...");
        LOG.info("Going after URL: " + link);
        output = openOutput(outputFile);
        if (output == null){
        	return null;
        }
        
        URL url;
		InputStream urlIn = null;
		BufferedReader urlBuffReader = null;

		try {
			url = new URL(link);
			long sendRequest = System.nanoTime();
			urlIn = url.openStream();
			long receiveResponse = System.nanoTime();
			LOG.info(String.format("Request time  : %3.6f Seconds", ((receiveResponse - sendRequest) / 1000000000.0)));
		} catch (IOException e) {
        	//Avoid hammering the server, it is painfully slow already
        	//Use random sleep to see if special sleep time makes a difference
            int sleepTime = random.nextInt((sleepMax - sleepMin) + 1) + sleepMin;
			LOG.error("Problem communicating with site, trying again in " + sleepTime + " seconds...");        	
			try {
				Thread.sleep(sleepTime * 1000);
			} catch (InterruptedException e1) {
				e.printStackTrace();
			}
			//trying again
			try {
				LOG.info("Trying again...");
				url = new URL(link);
				long sendRequest = System.nanoTime();
				urlIn = url.openStream();
				long receiveResponse = System.nanoTime();
				LOG.info(String.format("Second request time  : %3.6f Seconds", ((receiveResponse - sendRequest) / 1000000000.0)));
			} catch (IOException e2) {
				LOG.error("Problem communicating with site, skipping..." );        	
				e.printStackTrace();
			} 
		} 
		
		// chain the InputStream to a Reader
		StringBuffer result = null;
		try {
			urlBuffReader = new BufferedReader(new InputStreamReader(
					new BufferedInputStream(urlIn)));
			int c = 0;
			result = new StringBuffer();
			while ((c = urlBuffReader.read()) != -1) {
				result.append("" + (char) c);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		LOG.info("Response written to " + outputFile.getAbsolutePath());
		output.print(result);

		closeOutput(output);
		LOG.info("Page Dump...Complete");
		
        return outputFile;
    }
    
    
    static final PrintWriter openOutput (File outputFile) {
        FileOutputStream fos;
		try {
			fos = new FileOutputStream(outputFile);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
        	LOG.info("CaseLookupPageCollector...Failed to open " + outputFile.getAbsolutePath());
			return null;
		}
        BufferedOutputStream outputBuffer = new BufferedOutputStream(fos);
        return new PrintWriter(outputBuffer);
    }

    static final void closeOutput (PrintWriter output) {
        if (output != null) {
        	output.close();
        }
    }

}
