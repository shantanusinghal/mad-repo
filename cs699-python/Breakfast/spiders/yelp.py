from scrapy import Request
from scrapy.spiders import CrawlSpider
from Breakfast.items import YelpListing


class YelpSpider(CrawlSpider):
    name = "yelp"
    base_uri = 'https://www.yelp.com'

    allowed_domains = ['yelp.com']

    def start_requests(self):
        for starting_index in xrange(0, 100, 10):
            yield Request('https://www.yelp.com/search?find_desc=Restaurants&find_loc=Madison,+WI&start=%d'
                          '&cflt=breakfast_brunch' % starting_index, callback=self.parse_results_page)

    # This will be called automatically by Scrapy
    def parse_results_page(self, response):
        biz_paths = response.xpath("//ul[contains(@class, 'search-results')][2]/li[contains(@class, "
                                   "'regular-search-result')]//a[contains(@class, 'biz-name')]/@href").extract()
        for path in biz_paths:
            yield Request(url=self.base_uri + path, callback=self.parse_listing_page)

    @staticmethod
    def parse_listing_page(response):
        listing = YelpListing(response)
        yield {
            'title': listing.title,
            'link': response.url,
            'rating': listing.rating,
            'reviews': listing.num_reviews,
            'phone_number': listing.phone_number,
            'price_from': listing.price_range[0],
            'price_to': listing.price_range[1],
            'address': listing.address,
            'categories': listing.categories
        }
