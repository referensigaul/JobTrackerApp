from google.appengine.ext import ndb
import webapp2
import json

class HomeHandler(webapp2.RequestHandler):
	def get(self):
		self.response.write("Welcome Home")

app = webapp2.WSGIApplication([    
    ('/', HomeHandler)
], debug=True)
