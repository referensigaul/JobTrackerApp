from google.appengine.ext import ndb
import webapp2
import json
import re
import logging

import Jobs

#Define Employee model
class Employee(ndb.Model):	
	first_name = ndb.StringProperty(required=True)
	last_name = ndb.StringProperty(required=True)
	hourly_wage = ndb.FloatProperty()
	skills = ndb.StringProperty(repeated=True)
	jobs_assigned = ndb.StringProperty(repeated=True)
	id = ndb.StringProperty()
	self = ndb.StringProperty()

class  EmployeesHandler(webapp2.RequestHandler):
	def get(self, employee_id=None):
		if employee_id:
			employee = ndb.Key(urlsafe=employee_id).get()
			employee_dict = employee.to_dict()
			self.response.status = 200
			self.response.write(json.dumps(employee_dict))
		#if no id included return all jobs_assigned or possibly filtered results 
		#if a query field included in the request
		else:					
			#Get query parameters from request
			filter = self.request.GET
			#if parameters included search based on them or do general query
			if filter:
				if 'first_name' in filter:					
					option = filter['first_name']
					employee_query = Employee.query(Employee.first_name == option)								
				elif 'last_name' in filter:
					option = filter['last_name']
					employee_query	= Employee.query(Employee.last_name == option)
				elif 'hourly_wage' in filter:					
					option = filter['hourly_wage']
					employee_query = Employee.query(Employee.hourly_wage == option)
				elif 'skills' in filter:					
					option = filter['skills']
					employee_query = Employee.query(Employee.skills == option)				
				elif 'jobs_assigned' in filter:
					option = filter['jobs_assigned']
					employee_query = Employee.query(Employee.jobs_assigned == option)				
			#if no filter included print all jobs_assigned
			else:				
				employee_query = Employee.query()
				
			#loop through each employee in the query object and create a
			#dict version and add to results list, then return list as JSON
			results = []
			for entry in employee_query:
				e_dict = entry.to_dict()
				#e_dict['self'] = '/employees/' + entry.key.urlsafe()
				results.append(e_dict)
			self.response.status=200
			self.response.write(json.dumps(results))

	#create new Employee and add to db
	def post(self):
		jobsArray = []
		#create parent key so all jobs_assigned in same ancestor groups
		parent_key_employee = ndb.Key(Employee, "parent_employee")
		#get employee details from the request body
		employee_data = json.loads(self.request.body)
		
		#ensure first_name and last_name included, if not return bad request
		if employee_data.get('first_name') and employee_data.get('last_name'):			
			new_employee = Employee(first_name=employee_data['first_name'], last_name=employee_data['last_name'], parent=parent_key_employee)			
			if employee_data.get('hourly_wage'):
				new_employee.hourly_wage= float(employee_data['hourly_wage'])
			if employee_data.get('skills'):
				new_employee.skills=employee_data['skills']
			if employee_data.get('jobs_assigned'):
				new_employee.jobs_assigned=employee_data['jobs_assigned']
				jobsArray = employee_data.get('jobs_assigned')				
			
			#add new_employee to the database
			new_employee.put()
			new_employee.id = new_employee.key.urlsafe()
			new_employee.self = '/employees/' + new_employee.key.urlsafe()
			new_employee.put()

			#if jobs were assigned, get job and add this employee to employee list
			if jobsArray:
				for x in range(0, len(jobsArray)):													
					job_query = Jobs.Job.query(Jobs.Job.self == jobsArray[x])
					if job_query.get():
						for job in job_query:							
							if(job.employees.count(new_employee.self) == 0):
								job.employees.append(new_employee.self)
								job.put()
			
			employee_dict = new_employee.to_dict()			
			self.response.status = 201
			self.response.headers.add('location', str(employee_dict['self']))			
			self.response.write(json.dumps(employee_dict))
		else:
			self.response.status = 400
			self.response.write("first_name and last_name are required")

	#delete a employee
	def delete(self, id=None):
		if id:
			employee = ndb.Key(urlsafe=id).get()
			#if the employee is assigned to any jobs, find the jobs
			#and remove from list	
			url = str("/employees/" + id)
			logging.info("url = " + url)
			#self.response.write(url)
			job_query = Jobs.Job.query(Jobs.Job.employees == url)
			for job in job_query:
				job.employees.remove(url)
				job.put()

			employee.key.delete()
			self.response.status = 200

	#update a employee, any fields left out are unchanged
	def patch(self, id=None):
		if id:
			jobsArray = []
			#get employee and request info
			employee = ndb.Key(urlsafe=id).get()
			employee_data = json.loads(self.request.body)
			
			prevJobsArray = employee.jobs_assigned
			#if fields included in the request body, update
			if employee_data.get('first_name'):
				employee.first_name=employee_data['first_name']
			if employee_data.get('last_name'):
				employee.last_name=employee_data['last_name']
			if employee_data.get('hourly_wage'):
				employee.hourly_wage= float(employee_data['hourly_wage'])
			if employee_data.get('skills'):
				employee.skills=employee_data['skills']
			if employee_data.get('jobs_assigned'):
				employee.jobs_assigned=employee_data['jobs_assigned']
				jobsArray = employee_data.get('jobs_assigned')
			employee.put()

			#if a job was previously listed as assigned job but no longer is after update
			#grab that job and remove employee from the jobs employees list
			for prevJob in prevJobsArray:
				found = False
				if jobsArray:
					for y in jobsArray:
						if prevJob == y:
							found == True
				if found == False:
					query = Jobs.Job.query(Jobs.Job.self == str(prevJob))
					if query.get():
						for j in query:
							j.employees.remove(employee.self)
							j.put()

			#add employee to assigned jobs employees list
			if jobsArray:
				for x in range(0, len(jobsArray)):													
					job_query = Jobs.Job.query(Jobs.Job.self == jobsArray[x])
					if job_query.get():
						for job in job_query:
							#if not already in list append to list							
							if(job.employees.count(employee.self) == 0):
								job.employees.append(employee.self)
								job.put()	

			#send response with new employee details
			employee_dict = employee.to_dict()			
			self.response.status = 200
			self.response.write(json.dumps(employee_dict))

	###The put command will overwrite an existing entry and keep the same key, 
	###requires at least a first_name and a last_name
	###any fields left blank will be cleared or set to default value
	def put(self, employee_id=None):
		if employee_id:
			jobsArray = []
			
			employee = ndb.Key(urlsafe=employee_id).get()
			employee_data = json.loads(self.request.body)
						
			prevJobsArray = employee.jobs_assigned

			if employee_data.get('first_name') and employee_data.get('last_name'):					
				employee.first_name=employee_data['first_name']
				employee.last_name=employee_data['last_name']
				
				if employee_data.get('hourly_wage'):
					employee.hourly_wage= float(employee_data['hourly_wage'])
				else:					
					employee.hourly_wage= None
				if employee_data.get('skills'):
					employee.skills=employee_data['skills']
				else:
					employee.skills=[]
				if employee_data.get('jobs_assigned'):
					employee.jobs_assigned=employee_data['jobs_assigned']
					jobsArray = employee_data.get('jobs_assigned')
				else:
					employee.jobs_assigned=[]
				employee.put()
					
			#if a job was previously listed as assigned job but no longer is after update
			#grab that job and remove employee from the jobs employees list
			for prevJob in prevJobsArray:
				found = False
				if jobsArray:					
					for y in jobsArray:
						if prevJob == y:
							found == True
				if found == False:
					query = Jobs.Job.query(Jobs.Job.self == prevJob)
					if query.get():
						for j in query:
							j.employees.remove(employee.self)
							j.put()

			#add employee to assigned jobs employees list
			if jobsArray:
				for x in range(0, len(jobsArray)):													
					job_query = Jobs.Job.query(Jobs.Job.self == jobsArray[x])
					if job_query.get():
						for job in job_query:
							#if not already in list append to list							
							if(job.employees.count(employee.self) == 0):
								job.employees.append(employee.self)
								job.put()

				employee_dict = employee.to_dict()				
				self.response.status = 200
				self.response.write(json.dumps(employee_dict))
		else:
			self.response.status = 400
			self.response.write("first_name and last_name are required")

allowed_methods = webapp2.WSGIApplication.allowed_methods
new_allowed_methods = allowed_methods.union(('PATCH',))
webapp2.WSGIApplication.allowed_methods = new_allowed_methods

app = webapp2.WSGIApplication([    
    ('/employees/(.*)', EmployeesHandler),
    ('/employees', EmployeesHandler)
], debug=True)