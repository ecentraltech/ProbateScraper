# ProbateScraper

This is a small sample project to traverse a public probate case Internet site and
extract the individual details from each case. This was nothing more than a test project
and thus is not a finished product. 

To run as is:
* Check out of GitHub as a Maven project
* Create a new directory \Projects\ProbateScraperData
* Run CaseLookupPageCollector as java program
  - Collect individual html pages in the directory above
  - There are controls in the logic to avoid pulling the entire site at once, this can take a long time
* Run PageParserProbateList as a java program
  - This will extract all the probate information into a csv file

This project is/was a work in progress. If continued the following are my tasks:
* Introduce a database for final case data
* Add the ability to track new and/or closed cases or case status in general
* Page maintenance for persisted page. 
  - Keep them
  - Purge them
  - Zip them up
