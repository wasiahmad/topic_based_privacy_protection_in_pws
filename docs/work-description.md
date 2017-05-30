
## Crawling and Generating Lucene Index

#### Step. 1 - Crawl all web Urls from AOL dataset
There are **1,632,797** URLs in AOL dataset and we have crawled **1,051,483** wchich is **64.4%** of the total number of URLs. Remaining URLs are either dead, broken or moved permanently to another URL. List of all the URLs can be found [here](https://drive.google.com/a/virginia.edu/file/d/0B8ZGlkqDw7hFNkc0c0p1OVF2YTA/view). We have stored the crawled data in one xml file and the format of the file looks like below.

```xml
<?xml version="1.0" encoding="utf-8"?>
<crawledData>
    <page id="45" url="http://www.google.com/">
        <anchor>Null</anchor>
        <content>Search Images Maps Play YouTube News Gmail Drive More »Web History | Settings | Sign in × Try a fast, secure browser with updates built in. Yes, get Chrome now  Advanced searchLanguage toolsAdvertising ProgramsBusiness Solutions+GoogleAbout Google© 2016 - Privacy - Terms</content>
    </page>
    <page id="455" url="http://www.apple.com/">
        <anchor>Null</anchor>
        <content>Open Menu Close Menu Apple Shopping Bag Apple Mac iPad iPhone Watch TV Music Support Search apple.com Shopping Bag iPad Pro Super. Computer. NowÂ inÂ twoÂ sizes. Learn more Watch the film Watch the keynote iPhone SE A big step for small. Learn more Watch the keynote Apple Watch You. At a glance. Learn more Watch the keynote March Event 2016 Watch the keynote Apple and Education. Create more a-ha moments. iPhone 6s. 3D Touch. 12MP photos. 4K video. One powerful phone. Apple tv. The future of television is here. Macbook. Light. Years ahead. Better together. Shop our collection of curated accessories. AC Wall Plug Adapter Recall Program Apple Footer Shop and Learn Open Menu Close Menu MaciPadiPhoneWatchTVMusiciTunesiPodAccessoriesGift Cards Apple Store Open Menu Close Menu Find a StoreGenius BarWorkshops and LearningYouth ProgramsApple Store AppRefurbishedFinancingReuse and RecyclingOrder StatusShopping Help For Education Open Menu Close Menu Apple and EducationShop for College For Business Open Menu Close Menu iPhone in BusinessiPad in BusinessMac in BusinessShop for Your Business Account Open Menu Close Menu Manage Your Apple IDApple Store AccountiCloud.com Apple Values Open Menu Close Menu EnvironmentSupplier ResponsibilityAccessibilityPrivacyInclusion and DiversityEducation About Apple Open Menu Close Menu Apple InfoJob OpportunitiesPress InfoInvestorsEventsHot NewsContact Apple More ways to shop: Visit an Apple Store, call 1-800-MY-APPLE, or find a reseller. United States Copyright Â© 2016 Apple Inc. All rights reserved. Privacy Policy Terms of Use Sales and Refunds Legal Site Map</content>
    </page>
</crawledData>
```

#### Step. 2 - Creating Lucene Index
  * Create the lucene index from all crawled data by running [Indexer.java](https://github.com/wasiuva/Privacy-Preserving-IR/blob/master/src/edu/virginia/cs/index/Indexer.java).
  * For AOL dataset, our lucene index can be found [here](https://drive.google.com/a/virginia.edu/file/d/0B8ZGlkqDw7hFMGZkVF9FSUtqMW8/view?usp=sharing).

## Generating Topic Model

Requirement: [BBC dataset](http://mlg.ucd.ie/datasets/bbc.html), [Binary for LDA-C](https://github.com/magsilva/lda-c/tree/master/bin), [Settings file](https://github.com/wasiuva/Privacy-Preserving-IR/blob/master/settings.txt) to set parameters for LDA

#### Step. 1 - Constructing dictionary and document record
  * Run [BuildTopicModel.java](https://github.com/wasiuva/Privacy-Preserving-IR/blob/master/src/edu/virginia/cs/model/BuildTopicModel.java).
  * Dictionary and document record will be created using BBC dataset.
    + BBC dataset should be located at - **project_root_directory/data/bbc/**.
    + Dictionary ("dictionary.txt") and document record ("documentRecord.dat") will be placed at the project root directory.
  * This step also generated "dictionaryWithFrequency.txt" file and placed at the project root directory.

##### File description:
  * dictionary.txt: This file contains unigrams and bigrams found in the BBC dataset. Each line contains one unigram/bigram.
  * documentRecord.dat: This file contains one line per BBC document. Each line looks like the following.
  
    <pre>
    350 501:1 530:1 723:1 443:1 598:1 621:1 707:1 561:1 591:1 490:1 483:1 487:1 438:1 688:1 573:1 604:1 471:2
    413:1 410:1 3:1 632:1 569:1 488:1 499:1 599:1 439:1 401:7 595:2 713:1 526:1 648:1 179:1 626:1 518:3 655:1
    </pre>
    The first numeric value represents the total number of unique terms found in the document. Then all <code>x:y</code> value represents <code>term index in the dictionary:term frequency</code>. All the values are separated by space.
  * dictionaryWithFrequency.txt: This file contains unigrams and bigrams with their total term frequency over the entire BBC dataset.     Each line contains one unigram/bigram and corresponding total term frequency seperated by space.

#### Step. 2 - Generate the topic model using LDA-C

  * Double click the [run.bat](https://github.com/wasiuva/Privacy-Preserving-IR/blob/master/run-lda.bat) file (for windows environment), topic model will be generated and stored in **project_root_directory/topic_model/** folder.
  * Command written in [run.bat](https://github.com/wasiuva/Privacy-Preserving-IR/blob/master/run-lda.bat) file is **lda-win64 est 0.6 5 settings.txt documentRecord.dat seeded topic_model**.
  * Third parameter value ("5") in the command represents "number of topics" for the topic model.
  * [settings.txt](https://github.com/wasiuva/Privacy-Preserving-IR/blob/master/settings.txt) file should contain all required parameter values.

## Generating Pre-requisite Data

#### Step. 1 - Generate Topical Word Distribution

 * Probability distribution of all words for each topic can be generated by running [GenerateTopicWords.java](https://github.com/wasiuva/Privacy-Preserving-IR/blob/master/src/edu/virginia/cs/model/GenerateTopicWords.java) program.
 * This is required for cover query generation which is the core part of this privacy presercing IR model.

#### Step. 2 - Generate User Search Logs

 * User search logs are a list of user submitted query and their corresponding clicked document.
 * Top N user search logs are generated from AOL dataset to evaluate the model. Top users mean the users with maximum search history.
 * User search logs can be generated by running [SearchLogBuilder.java](https://github.com/wasiuva/Privacy-Preserving-IR/blob/master/src/edu/virginia/cs/searchlog/SearchLogBuilder.java) program. All search logs will be placed at **project_root_directory/data/user_search_logs/** directory.
 * User search logs need to be placed in the **project_root_directory/data/user_search_logs/** directory to run entire pipeline.

#### Step. 3 - Generate Reference Model

 * Reference model is required for smoothing purpose at different times.
 * To generate reference model, [ReferenceModel.java](https://github.com/wasiuva/Privacy-Preserving-IR/blob/master/src/edu/virginia/cs/user/ReferenceModel.java) program can be used.
 * Reference model is created over all user submitted queries, their corresponding clicked documents and BBC dataset.

## Evaluating Privacy-Preseving-IR Model

The entire evaluation procedure on a single user works as follows.
 * Loading the reference model and generate the judgements.
   + Judgements are required to measure search effectiveness (Mean Average Precision).
 * Generate **k** cover queries for each user query.
 * Each user query along with the **k** cover queries are submitted to the search engine. Search effectiveness is measured only for the true user query.
 * [Evaluate.java](https://github.com/wasiuva/Privacy-Preserving-IR/blob/master/src/edu/virginia/cs/eval/Evaluate.java) program follows the above mentioned steps.

### How to Run the Entire Pipeline
 * Run [MultiThread.java](https://github.com/wasiuva/Privacy-Preserving-IR/blob/master/src/edu/virginia/cs/eval/MultiThread.java)
 * Set the configuration of the model in the [parameters.txt](https://github.com/wasiuva/Privacy-Preserving-IR/blob/master/parameters.txt) file and place it in the project root directory. The file looks like following.
   <pre>
   number of cover queries = 2
   entropy range = 0.2
   client side re-ranking = off
   number of threads = 4
   </pre>
 * Final results will be placed at **project_root_directory/model-output-files/** directory.
 * Final result will look like following.
   <pre>
   ************* Parameter Settings *************
   Number of cover queries = 2
   Selected entropy range = 0.2
   **********************************************
   Total Number of users = 250
   Total Number of queries tested = 44033.0
   Averge MAP = 0.11552013550520247
   Average KL-Divergence = 1.2539138860702514
   Average Mutual Information = 1.0775569166010792
   </pre>

##### Additional Information

* The model is tested for both personalization and non-personalization settings.
* Privacy preservation is evaluated through KL-Divergence.
* All pre-generated data to run the entire pipeline can be found [here](https://github.com/wasiuva/Privacy-Preserving-IR/blob/master/docs/requirements.md#pre-generated-data).

