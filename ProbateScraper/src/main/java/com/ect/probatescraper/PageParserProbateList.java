package com.ect.probatescraper;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * Turns the local copies of all probate html pages into a csv format for ingestion. 
 *
 */
public class PageParserProbateList {
    private static final Logger LOG = Logger.getLogger(PageParserProbateList.class);
    public static final String NEWLINE = System.getProperty("line.separator");
    public static final String FILESEP = File.separator;
    public static final String DATA_DIR = FILESEP + "Projects" + FILESEP + "ProbateScraperData";
    public static final String HTML_FILENAME_SUFFIX = ".html";
    public static final String CSV_FILENAME_SUFFIX = ".csv";
    public static final String FILENAME_PREFIX_CASE_LIST = "ProbateCaseList_";
    public static final String FILENAME_PREFIX_CASE_DETAILS = "ProbateCaseDetails_";
    public static final String FILENAME_PREFIX_CASE_FEDUCIARY = "ProbateCaseFiduciary_";

    public static final String CASE_TYPE_DECEDENT_ESTATE_REGULAR = "DR";

    static PrintWriter caseListOutput;
    static PrintWriter caseDetailsOutput;
    static PrintWriter caseFiduciaryOutput;

    public static void main( String[] args ) {
        LOG.info("PageParserProbateList...");
        
        File dataDirectory = new File(DATA_DIR);
        if (!dataDirectory.exists() || !dataDirectory.isDirectory()) {
	        LOG.info("PageParserProbateList...Invalid data directory " + dataDirectory.getAbsolutePath());
        	boolean success = dataDirectory.mkdir();
        	if (success) {
    	        LOG.info("PageParserProbateList...directory created");
        	}
        	else {
    	        LOG.error("PageParserProbateList...failed to create directory!");
        	}
			return;
        }

        if (!openOutputFiles(dataDirectory)) {
        	return;
        }
        
        //get a list of case list files
        File[] htmlcaseListPages = dataDirectory.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
	            return name.matches(FILENAME_PREFIX_CASE_LIST + "(.*)" + HTML_FILENAME_SUFFIX);
			}
        });

        //parse case list into csv file
        Arrays.sort(htmlcaseListPages);
        boolean printedHeaders = false; 
        for(File htmlCaseListPage : htmlcaseListPages) {
	        LOG.info("PageParserProbateList...Parsing case list page " + htmlCaseListPage);
            Document doc;
            try {
    			doc = Jsoup.parse(htmlCaseListPage, "UTF-8", "http://apps.ctprobate.gov/");
    		} catch (IOException e1) {
    			e1.printStackTrace();
    	        LOG.error("PageParserProbateList...Failed to parse page " + htmlCaseListPage);
    			return;
    		}        

            //is there a results section?
            Elements resultsSection = doc.select("section#search-results-container");
            if (resultsSection.isEmpty()) {
    	        LOG.info("PageParserProbateList...No results");
            	return; 
            }

            if (!printedHeaders) {
            	extractCaseListHeader(resultsSection, caseListOutput);
            	printedHeaders = true;
            }
        	int count = extractCaseListing(resultsSection, caseListOutput);
	        LOG.info("PageParserProbateList..." + count + " cases extracted");
        }

        //get a list of case details files
        File[] caseDetailsPages = dataDirectory.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
	            return name.matches(FILENAME_PREFIX_CASE_DETAILS + "(.*)" + HTML_FILENAME_SUFFIX);
			}
        });

        printedHeaders = false; 
        for(File caseDetailsPage : caseDetailsPages) {
	        LOG.info("PageParserProbateList...Parsing case detail page " + caseDetailsPage);
            Document doc;
            try {
    			doc = Jsoup.parse(caseDetailsPage, "UTF-8", "http://apps.ctprobate.gov/");
    		} catch (IOException e1) {
    			e1.printStackTrace();
    	        LOG.error("PageParserProbateList...Failed to parse page " + caseDetailsPage);
    			return;
    		}        

            //find the table of data, this is free form so far, so this will get complicated
            Elements caseTable = doc.select("body div table");
            if (!printedHeaders) {
            	extractCaseDetailsHeader(caseTable, caseDetailsOutput);
            	printedHeaders = true;
            }

        	extractCaseDetails(caseTable, caseDetailsOutput);
	        LOG.info("PageParserProbateList...Cases details extracted");
        }
        
        closeOutputFiles();
        LOG.info("PageParserProbateList...Complete");
    }

	private static void extractCaseDetailsHeader(Elements table, PrintWriter output) {
        //extract all the headers fields
    	output.print("Decedent");
    	output.print(",");
    	output.print("Case Number");
    	output.print(",");
    	output.print("Case Type");
    	output.print(",");
    	output.print("Date Filed");
    	output.print(",");
    	output.print("Fiduciary1 Name");
    	output.print(",");
    	output.print("Fiduciary1 Represented By");
    	output.print(",");
    	output.print("Fiduciary1 Addr Ln 1");
    	output.print(",");
    	output.print("Fiduciary1 Addr Ln 2");
    	output.print(",");
    	output.print("Fiduciary1 City State Zip");
    	output.print(",");
    	output.print("Fiduciary1 Phone");
    	output.print(",");
    	output.print("Fiduciary1 Fax");
    	output.print(",");
    	output.print("Fiduciary2 Name");
    	output.print(",");
    	output.print("Fiduciary2 Represented By");
    	output.print(",");
    	output.print("Fiduciary2 Addr Ln 1");
    	output.print(",");
    	output.print("Fiduciary2 Addr Ln 2");
    	output.print(",");
    	output.print("Fiduciary2 City State Zip");
    	output.print(",");
    	output.print("Fiduciary2 Phone");
    	output.print(",");
    	output.print("Fiduciary2 Fax");
    	output.print(",");
    	
    	output.println("");
	}
	
	private static void extractCaseDetails(Elements caseTable, PrintWriter output) {
        int rowCount = 0;
        String fullName = null; 
        String caseNumber = null; 
        String caseType = null; 
        String dateFiled = null;
        boolean hasFiduciaries = false;
        List<String> fiduciaryRows = new ArrayList<String>();
        
        //extract results
        for (Element row : caseTable.select("tr")) {
        	rowCount++;
        	switch (rowCount) {
        		case 1:
        			Elements tds1 = row.select("td");
                    fullName = cleanText(tds1.get(0).text()); 
                    caseNumber = tds1.get(1).select("span").get(1).text(); 
        			break;
        		case 2:
        			Elements tds2 = row.select("td");
        			caseType = tds2.get(0).text(); 
        			dateFiled = tds2.get(1).select("span").get(1).text();
        			break;
        		case 3:
        			break;
        		case 4:
        			Elements tds4 = row.select("td");
        			String fiduciaries = tds4.get(0).text().trim();
        			if (fiduciaries.startsWith("Fiduciary:") || fiduciaries.startsWith("Fiduciaries:")) {
        				hasFiduciaries = true;
        			}
        			break;
       			default:
       				Elements tdsRest = row.select("td");
       				String tester = tdsRest.get(0).text();
       				//hack to get rid a bogus character
       				if (tester.startsWith("Phone:")) {
       					fiduciaryRows.add("Phone:" + tdsRest.get(0).select("span").get(1).text());
       				}
       				else if (tester.startsWith("Fax:")) {
       					fiduciaryRows.add("Fax:" + tdsRest.get(0).select("span").get(1).text());
       				}
       				else {
           				fiduciaryRows.add(cleanText(tdsRest.get(0).text().trim()));
       				}
        			break;
        	}
        }
    	
        List<Fiduciary> fiduciaries = new ArrayList<Fiduciary>(25);
        if (hasFiduciaries && !fiduciaryRows.isEmpty()){
        	//create group of info for processing
        	Map<Integer, List<String>> fidGroups = new Hashtable<Integer, List<String>>(20);
        	int groupCount = 0;
        	for (String row : fiduciaryRows) {
        		//is this a spacing row?
        		if (row.length() == 0) {
        			groupCount++;
        			continue;
        		}
        		
        		//get current group (make if necessary)
        		List<String> group = fidGroups.get(new Integer(groupCount));
        		if (group == null){
        			group = new ArrayList<String>(25);
        			fidGroups.put(new Integer(groupCount), group);
        		}
        		
        		group.add(row);				
			}

        	//build feduciaries by group
        	Fiduciary fiduciary = null;
        	for (groupCount = 0; groupCount < fidGroups.size(); groupCount++){
        		List<String> groupedInfo = fidGroups.get(new Integer(groupCount));
        		if (groupedInfo.isEmpty()){
        			continue;
        		}
        		
        		if (Fiduciary.isPhoneFaxGroup(groupedInfo)) {
        			if (fiduciary != null) {
            			fiduciary.setPhoneFaxGroupInfo(groupedInfo);
        			}
        		}
        		else {
        			fiduciary = new Fiduciary(groupedInfo);
        			fiduciaries.add(fiduciary);
        		}
        	}
        }
        
        //dump results
        output.print(fullName);
    	output.print(",");
    	output.print(caseNumber);
    	output.print(",");
    	output.print(caseType);
    	output.print(",");
    	output.print(dateFiled);
    	output.print(",");
    	
    	for (Fiduciary fiduciary : fiduciaries) {
            output.print(fiduciary.getName());
        	output.print(",");
        	output.print(fiduciary.getRepresentedBy());
        	output.print(",");
        	output.print(fiduciary.getAddrLn1());
        	output.print(",");
        	output.print(fiduciary.getAddrLn2());
        	output.print(",");
        	output.print(fiduciary.getCityStateZip());
        	output.print(",");
        	output.print(fiduciary.getPhone());
        	output.print(",");
        	output.print(fiduciary.getFax());
        	output.print(",");
		}
    	
    	output.println("");
	}
	
	private static void extractCaseListHeader(Elements resultsSection, PrintWriter output) {
        //extract all the headers fields
        Elements headers = resultsSection.select("header > span.column-header");
        for (Element header : headers) {
        	output.print(header.text());
        	output.print(",");
        }        
    	output.println("Case Link");
	}
	
	private static int extractCaseListing(Elements resultsSection, PrintWriter output) {
		int count = 0;
		//extract all data results
        Elements articles = resultsSection.select(".data-row");
        for (Element artcle : articles) {
        	count++;
        	output.print(artcle.select(".column-case-number").text());
        	output.print(",");
        	output.print(cleanText(artcle.select(".column-last-name").text()));
        	output.print(",");
        	output.print(cleanText(artcle.select(".column-first-name").text()));
        	output.print(",");
        	output.print(cleanText(artcle.select(".column-middle-initial").text()));
        	output.print(",");
        	output.print(artcle.select(".column-casetype").text());
        	output.print(",");
        	output.print(artcle.select(".column-district").text());
        	output.print(",");
        	output.print(artcle.select(".column-case-number a").attr("abs:href"));
        	output.println("");
        }
        
        return count;
	}

    public static List<String[]> extractDetailsLinks (File input, String caseTypeTarget) {
    	Document doc = null;
        try {
        	doc = Jsoup.parse(input, "UTF-8", "http://apps.ctprobate.gov/");
		} catch (IOException e1) {
			e1.printStackTrace();
	        LOG.info("PageParserProbateList...Failed to parse html " + input.getAbsolutePath());
	        return new ArrayList<String[]>(0);
		}        

        Elements resultsSection = doc.select("section#search-results-container");
        if (resultsSection.isEmpty()) {
	        LOG.info("PageParserProbateList...No results");
        	return new ArrayList<String[]>(0); 
        }
        
        return extractDetailsLinks (resultsSection, caseTypeTarget);
    }
        
    private static List<String[]> extractDetailsLinks (Elements resultsSection, String caseTypeTarget) {
    	List<String[]> pageLinks = new ArrayList<String[]>();
    	if (caseTypeTarget != null){
            Elements articles = resultsSection.select(".data-row");
            for (Element artcle : articles) {
            	String caseType = artcle.select(".column-casetype").text();
            	if (caseTypeTarget.equalsIgnoreCase(caseType)){
                	String[] caseDetail = new String[2];
            		caseDetail[0] = artcle.select(".column-case-number").text();
            		caseDetail[1] = artcle.select(".column-case-number a").attr("abs:href");
                	pageLinks.add(caseDetail);
            	}
            }
    	}
    	
        return pageLinks;
    }
	
    public static Map<Integer, String> extractPageLinks (File input) {
    	Document doc = null;
        try {
        	doc = Jsoup.parse(input, "UTF-8", "http://apps.ctprobate.gov/");
		} catch (IOException e1) {
			e1.printStackTrace();
	        LOG.info("PageParserProbateList...Failed to parse html " + input.getAbsolutePath());
	        return new Hashtable<Integer, String>(0);
		}        

        Elements resultsSection = doc.select("section#search-results-container");
        if (resultsSection.isEmpty()) {
	        LOG.info("PageParserProbateList...No results");
        	return new Hashtable<Integer, String>(0); 
        }
        
        return extractPageLinks (resultsSection);
    }
    
    
    private static Map<Integer, String> extractPageLinks (Elements resultsSection) {
    	Map<Integer, String> pageLinks = new Hashtable<Integer, String>();
        Elements pages = resultsSection.select("footer > span a.paging-link");
        for (Element pageLink : pages) {
        	Set<String> classNames = pageLink.classNames();
        	boolean includeLink = true;
        	for (String className : classNames) {
        		if (className.equals("current-page")){
        			includeLink = false;
        			break;
        		}
        	}
        	
        	if (includeLink) {
            	Integer pageNo = Integer.parseInt(pageLink.text());
            	String link = pageLink.attr("abs:href");
            	LOG.info("Page " + pageNo + ": " + link);
            	pageLinks.put(pageNo, link);
        	}
        }
        
        return pageLinks;
    }

    private static boolean openOutputFiles(File dataDirectory) {
        String fileJulian = getJulianWithMillis();
        File caseListFilename = new File(dataDirectory, FILENAME_PREFIX_CASE_LIST + fileJulian + CSV_FILENAME_SUFFIX);
        caseListOutput = openOutput(caseListFilename);
        if (caseListOutput == null){
	        LOG.error("PageParserProbateList...Failed to open output file " + caseListFilename.getAbsolutePath());
			return false;
        }

        File caseDetailsFilename = new File(dataDirectory, FILENAME_PREFIX_CASE_DETAILS + fileJulian + CSV_FILENAME_SUFFIX);
        caseDetailsOutput = openOutput(caseDetailsFilename);
        if (caseDetailsOutput == null){
	        LOG.error("PageParserProbateList...Failed to open output file " + caseDetailsFilename.getAbsolutePath());
			return false;
        }

        File caseFiduciaryFilename = new File(dataDirectory, FILENAME_PREFIX_CASE_FEDUCIARY + fileJulian + CSV_FILENAME_SUFFIX);
        caseFiduciaryOutput = openOutput(caseFiduciaryFilename);
        if (caseFiduciaryOutput == null){
	        LOG.info("PageParserProbateList...Failed to open output file " + caseFiduciaryFilename.getAbsolutePath());
			return false;
        }

        return true;
    }

    static final PrintWriter openOutput (File outputFile) {
        FileOutputStream fos;
		try {
			fos = new FileOutputStream(outputFile);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		}
        BufferedOutputStream outputBuffer = new BufferedOutputStream(fos);
        return new PrintWriter(outputBuffer);

    }

    private static void closeOutputFiles() {
    	closeOutput (caseFiduciaryOutput);
    	closeOutput (caseDetailsOutput);
    	closeOutput (caseListOutput);
    }

    static final void closeOutput (PrintWriter output) {
        if (output != null) {
        	output.close();
        }
    }
    
    private static String cleanText (String text) {
    	if (text == null){
    		return "";
    	}
    	
    	//remove commas 
    	text = text.replace(',', ' ');
    	//remove quotes 
    	text = text.replace('"', ' ');
    	
    	return text;
    }

    public static String createCaseListFile(int pageNumber, String makeCopySuffix) {
    	String pageNo = (pageNumber < 10 ? ("0" + pageNumber) : ("" + pageNumber));
    	String fileName = FILENAME_PREFIX_CASE_LIST + pageNo + HTML_FILENAME_SUFFIX;
    	if ((new File(DATA_DIR, fileName)).exists() && makeCopySuffix != null) {
    		fileName = FILENAME_PREFIX_CASE_LIST + pageNo  + "_" + makeCopySuffix + HTML_FILENAME_SUFFIX;
    	}
    	
    	return fileName;
    }

    public static String createCaseDetailFile(String caseNumber, String makeCopySuffix) {
    	String fileName = FILENAME_PREFIX_CASE_DETAILS + caseNumber + HTML_FILENAME_SUFFIX;
    	if ((new File(DATA_DIR, fileName)).exists()) {
    		fileName = FILENAME_PREFIX_CASE_DETAILS + caseNumber  + "_" + makeCopySuffix + HTML_FILENAME_SUFFIX;
    	}
    	
    	return fileName;
    }

    /**
     * returns a formatted date time stamp Julian (YYDDDmmmmmmmm)
     * 
     * @param String
     *            to be formatted
     */
    public static final String getJulianWithMillis() {
        StringBuilder sb = new StringBuilder().append(getYYDDDFromDate(Calendar.getInstance().getTime()));

        NumberFormat format = NumberFormat.getIntegerInstance();
        format.setMinimumIntegerDigits(8);
        format.setParseIntegerOnly(true);
        format.setGroupingUsed(false);
        sb.append(format.format(getNoMillisecondsInToday()));

        return sb.toString();
    }

    /**
     * returns the supplied date in the format of YYDDD (Julian)
     * 
     * @return java.lang.String date in YYDDD
     */
    public static final String getYYDDDFromDate(Date date) {
        StringBuffer returnDate = new StringBuffer();
        DateFormat df = new SimpleDateFormat("yy");
        returnDate.append(df.format(date));

        df = new SimpleDateFormat("D");
        String dayOfYear = df.format(date);

        // leading zeros.
        if (dayOfYear.length() < 3) {
            returnDate.append("0");
        }
        if (dayOfYear.length() < 2) {
            returnDate.append("0");
        }
        returnDate.append(dayOfYear);

        return returnDate.toString();
    }

    /**
     * returns the nubmer of milliseconds for today
     * 
     * @param String
     *            to be formatted
     */
    public static final long getNoMillisecondsInToday() {
        Calendar today = Calendar.getInstance();
        Calendar thisMorning = Calendar.getInstance();
        thisMorning.set(Calendar.HOUR, 0);
        thisMorning.set(Calendar.MINUTE, 0);
        thisMorning.set(Calendar.MILLISECOND, 0);

        return today.getTimeInMillis() - thisMorning.getTimeInMillis();
    }
}
