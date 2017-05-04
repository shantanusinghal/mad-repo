import re

NUMBER = re.compile("[0-9]+")
STARS = re.compile("^[0-9]\.[0-9]")

yelp_keys = {
    'title': '//div[contains(@class, \'biz-page-header\')]//h1[contains(@class, \'biz-page-title\')]//text()',
    'rating': '//div[contains(@class, \'rating-info\')]//div[contains(@class, \'i-star\')]/@title',
    'reviews': '//div[contains(@class, \'rating-info\')]//span[contains(@class, \'review-count\')]/text()',
    'address': '//div[contains(@class, \'map-box-address\')]//address/text()',
    'phone_number': '//span[contains(@class, \'biz-phone\')]/text()',
    'price_range': '//div[contains(@class, \'island summary\')]//dd[contains(@class, \'price-description\')]/text()',
    'categories': '//div[contains(@class, \'biz-page-header\')]//div[contains(@class, \'price-category\')]'
                  '//span[contains(@class, \'category-str-list\')]//a/text()'
}


class YelpListing(object):
    def __init__(self, response):
        self.price_range = get_range(response)
        self.title = str(response.xpath(yelp_keys['title']).extract()[0].encode("utf8")).strip()
        self.rating = STARS.findall(str(response.xpath(yelp_keys['rating']).extract()[0]))[0]
        self.num_reviews = NUMBER.findall(response.xpath(yelp_keys['reviews']).extract()[0])[0]
        self.phone_number = str(response.xpath(yelp_keys['phone_number']).extract()[0]).strip()
        self.categories = [str(cat).strip() for cat in response.xpath(yelp_keys['categories']).extract()]
        self.address = ' '.join([str(line).strip() for line in response.xpath(yelp_keys['address']).extract()]),


def get_range(response):
    price_str = str(response.xpath(yelp_keys['price_range']).extract()[0]).strip()
    prices = NUMBER.findall(price_str)
    if len(prices) == 1:
        # assume the ceiling to be twice the minimum
        if 'above' in price_str.lower():
            return prices[0], prices[0] * 2
        # cap the minimum value at zero
        elif 'under' in price_str.lower():
            return 0, prices[0]
    elif len(prices) == 2:
        return prices[0], prices[1]

