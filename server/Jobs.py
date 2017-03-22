from google.appengine.ext import ndb
import webapp2
import json
import datetime
import logging

import Employees

#Define job model
class Job(ndb.Model):	
	customer = ndb.StringProperty()
	bid = ndb.FloatProperty()
	start_date = ndb.StringProperty()
	completion_date = ndb.StringProperty()
	employees = ndb.StringProperty(repeated=True)
	id = ndb.StringProperty()
	self = ndb.StringProperty()

class  JobsHandler(webapp2.RequestHandler):
	def get(self, job_id=None):
		if job_id:
			job = ndb.Key(urlsafe=job_id).get()
			job_dict = job.to_dict()
			self.response.status = 200
			self.response.write(json.dumps(job_dict))
		#if no id included return all jobs or possibly filtered results 
		#if a query field included in the request
		else:					
			#Get query parameters from request
			filter = self.request.GET
			#if parameters included search based on them or do general query
			if filter:
				if 'customer' in filter:					
					option = filter['customer']
					job_query = Job.query(Job.customer == option)								
				elif 'bid' in filter:
					option = filter['bid']
					job_query	= Job.query(Job.bid == option)
				elif 'start_date' in filter:					
					option = filter['start_date']
					job_query = Job.query(Job.start_date == option)
				elif 'completion_date' in filter:					
					option = filter['completion_date']
					job_query = Job.query(Job.completion_date == option)				
				elif 'employees' in filter:
					option = filter['employees']
					job_query = Job.query(Job.employees == option)

			#if no filter included print all jobs
			else:				
				job_query = Job.query()
				
			#loop through each job in the query object and create a
			#dict version and add to results list, then return list as JSON
			results = []
			for entry in job_query:
				e_dict = entry.to_dict()
				results.append(e_dict)
			self.response.status=200
			self.response.write(json.dumps(results))

	#create new Job and add to db
	def post(self):
		empArray = []
		#create parent key so all jobs in same ancestor groups
		parent_key_job = ndb.Key(Job, "parent_job")
		#get job details from the request body
		job_data = json.loads(self.request.body)
		
		#ensure customer included, if not return bad request
		if job_data.get('customer'):			
			new_job = Job(customer=job_data['customer'], parent=parent_key_job)
			if job_data.get('bid'):
				new_job.bid=float(job_data['bid'])
			else:
				new_job.bid=float(0.00)
			if job_data.get('start_date'):
				new_job.start_date= job_data['start_date']
			if job_data.get('completion_date'):
				new_job.completion_date=job_data['completion_date']
			if job_data.get('employees'):
				new_job.employees=job_data['employees']
				empArray = job_data.get('employees')
			
			#add new_job to the database
			new_job.put()
			new_job.id = new_job.key.urlsafe()
			new_job.self = '/jobs/' + new_job.key.urlsafe()
			new_job.put()

			if empArray:
				for x in range(0, len(empArray)):													
					emp_query = Employees.Employee.query(Employees.Employee.self == empArray[x])
					if emp_query.get():
						for emp in emp_query:							
							if(emp.jobs_assigned.count(new_job.self) == 0):
								emp.jobs_assigned.append(new_job.self)
								emp.put()	
			
			job_dict = new_job.to_dict()					
			self.response.status = 201
			self.response.headers.add('location', str(job_dict['self']))
			self.response.write(json.dumps(job_dict))
		else:
			self.response.status = 400

	#delete a job
	def delete(self, id=None):
		if id:
			job = ndb.Key(urlsafe=id).get()	
			url = job.self
			emp_query = Employees.Employee.query(Employees.Employee.jobs_assigned == url)
			for emp in emp_query:
				emp.jobs_assigned.remove(url)
				emp.put()
			job.key.delete()
			self.response.status = 200

	#update a job, any fields left out are unchanged
	def patch(self, id=None):
		if id:
			empArray = []
			#get job and request info
			job = ndb.Key(urlsafe=id).get()
			job_data = json.loads(self.request.body)

			prevEmpsArray = job.employees
			#if fields included in the request body, update
			if job_data.get('customer'):
				job.customer=job_data['customer']
			if job_data.get('bid'):
				job.bid=float(job_data['bid'])
			if job_data.get('start_date'):
				job.start_date= job_data['start_date']
			if job_data.get('completion_date'):
				job.completion_date=job_data['completion_date']
			if job_data.get('employees'):
				job.employees=job_data['employees']
				empArray = job_data.get('employees')
			job.put()

			if prevEmpsArray:
				for prevEmp in prevEmpsArray:
					#logging.info("prevEmp = " + prevEmp)
					found = False
					if empArray:
						for y in empArray:
							if prevEmp == y:
								found == True
					if found == False:
						query = Employees.Employee.query(Employees.Employee.self == str(prevEmp))
						if query.get():
							for e in query:
								e.jobs_assigned.remove(job.self)
								e.put()

			#add employee to assigned jobs employees list
			if empArray:
				for x in range(0, len(empArray)):													
					emp_query = Employees.Employee.query(Employees.Employee.self == empArray[x])
					if emp_query.get():
						for emp in emp_query:							
							if(emp.jobs_assigned.count(job.self) == 0):
								emp.jobs_assigned.append(job.self)
								emp.put()	
			#send response with new job details
			job_dict = job.to_dict()
			self.response.status = 200
			self.response.write(json.dumps(job_dict))

	###The put command will overwrite an existing entry and keep the same key, requires at least a customer
	###any fields left blank will be cleared or set to default value
	def put(self, job_id=None):
		if job_id:
			empArray = []
			
			job = ndb.Key(urlsafe=job_id).get()
			job_data = json.loads(self.request.body)

			prevEmpsArray = job.employees

			if job_data.get('customer'):					
				job.customer=job_data['customer']

				if job_data.get('bid'):
					job.bid=int(job_data['bid'])
				else:
					job.bid = None
				if job_data.get('start_date'):
					job.start_date= job_data['start_date']
				else:					
					job.start_date= None
				if job_data.get('completion_date'):
					job.completion_date=job_data['completion_date']
				else:
					job.completion_date=None
				if job_data.get('employees'):
					job.employees=job_data['employees']
					empArray = job_data.get('employees')
				else:
					job.employees=[]
				job.put()
				
				for prevEmp in prevEmpsArray:
					found = False
					for y in empArray:
						if prevEmp == y:
							found == True
					if found == False:
						query = Employees.Employee.query(Employees.Employee.self == prevEmp)
						if query.get():
							for e in query:
								e.jobs_assigned.remove(job.self)
								e.put()

				#add employee to assigned jobs employees list
				if empArray:
					for x in range(0, len(empArray)):													
						emp_query = Employees.Employee.query(Employees.Employee.self == empArray[x])
						if emp_query.get():
							for emp in emp_query:							
								if(emp.jobs_assigned.count(job.self) == 0):
									emp.jobs_assigned.append(job.self)
									emp.put()

				#send response with new job details
				job_dict = job.to_dict()
				self.response.status = 200
				self.response.write(json.dumps(job_dict))
		else:
			self.response.status = 400
			self.response.write("customer is required")

allowed_methods = webapp2.WSGIApplication.allowed_methods
new_allowed_methods = allowed_methods.union(('PATCH',))
webapp2.WSGIApplication.allowed_methods = new_allowed_methods

app = webapp2.WSGIApplication([   
    ('/jobs/(.*)', JobsHandler),
	('/jobs', JobsHandler)
], debug=True)