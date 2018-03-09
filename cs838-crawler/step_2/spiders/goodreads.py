import scrapy
import re
from datetime import datetime
from scrapy.exceptions import CloseSpider


class GoodReadsSpider(scrapy.Spider):
    counter = 0
    max_count = 3000
    name = "goodreads"

    def start_requests(self):
        urls = [
            'https://www.goodreads.com/search?page=1&q=fiction&tab=books'
        ]
        for url in urls:
            yield scrapy.Request(url=url, callback=self.parse)

    def parse(self, response):
        # visit each book page to scrape it's details
        books = response.xpath("//table[@class='tableList']/tr/td[1]/a/@href").extract()
        for book in books:
            yield scrapy.Request(url='https://www.goodreads.com' + book, callback=self.parse_book_page)

        # follow the pagination links to crawl all search results
        next_page = response.xpath("//a[@class='next_page']/@href").extract_first()
        if next_page is not None:
            yield response.follow(next_page, callback=self.parse)

    def parse_book_page(self, response):
        if self.counter > self.max_count:
            raise CloseSpider('Finished scraping %s pages' % self.max_count)
        else:
            book_id = re.split("^(.*)(show\/)([0-9]*)(.*)$", response.url)[3]
            book_name = response.xpath("//h1[@id='bookTitle']/text()").extract()[0].strip()
            book_name_alt = response.xpath(
                "//div[@id='bookDataBox']/div[@class='clearFloats']/div[@class='infoBoxRowItem']/text()").extract()[
                0].strip()
            author = response.xpath("//a[@class='authorName']/span/text()").extract()[0].strip()
            avg_rating = response.xpath("//span[@class='average']/text()").extract()[0].strip()
            book_format = response.xpath("//span[@itemprop='bookFormat']/text()").extract()[0].strip()
            count_pages = to_int(response.xpath("//span[@itemprop='numberOfPages']/text()").extract())
            publisher, date_published = extract_info(
                response.xpath("//div[@id='details']/div[@class='row'][2]/text()").extract()[0])
            self.counter += 1
            yield {
                'id': 'gr_%s' % book_id,
                'book_name': book_name,
                'book_name_alt': book_name_alt,
                'author': author,
                'avg_rating': avg_rating,
                'book_format': book_format,
                'count_pages': count_pages,
                'publisher': publisher,
                'date_published': date_published
            }


def to_int(param):
    if len(param):
        return int(param[0].strip().split(" ")[0])
    else:
        return -1


def extract_info(param):
    m = re.split("^(Published)(.*)( by )(.*)$", re.sub('\s+', ' ', param).strip())
    return m[4].strip(), to_date(m[2]) if len(m[2]) > 0 else None


def to_date(param):
    return datetime.strptime(re.sub(r'([0-9])+(th|nd|rd|st)', r'\1', param).strip(), "%B %d %Y")
