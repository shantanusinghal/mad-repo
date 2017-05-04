### Requirements
Project requires the following python packages
* For the crawler
** scrapy
* For the plotter
** plotly
** numpy
** pandas

### Running the web crawler
The following command will scrape all the listings for category 'Breakfast' in the Madison, WI area and save it to disk at `breakfast_yelp.csv`

```bash
$ scrapy crawl yelp -o breakfast_yelp.csv -t csv
```