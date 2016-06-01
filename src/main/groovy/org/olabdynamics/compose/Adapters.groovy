package org.olabdynamics.compose

import groovy.transform.ToString;

enum UnidirectionalAdapter{
	Logging,
	File,
	FTP,
	SFTP,
	Feed,
	Stream,
	SQL,
	NoSQL,
	Mail,
	UDP,
	TCP,
	AMQP,
	MQTT,
	WebSocket
}

enum BidirectionalAdapter{
	HTTP,
	AMQP,
	WebService,
	JavaApplication
}

@ToString
class FileAdapter{
	def adapter = UnidirectionalAdapter.File
	def directory
}

@ToString
class WebServiceAdapter{
	def adapter = BidirectionalAdapter.WebService
	def wsdl
}

@ToString
class HttpAdapter{
	
	enum Method { GET, PUT, POST }
	
	def adapter = BidirectionalAdapter.HTTP
	def url
	def method
}

@ToString
class JavaServiceAdapter{
	def adapter = BidirectionalAdapter.JavaApplication
	def javaClass 
	def method
}

@ToString
class ScriptServiceAdapter{
	def adapter = BidirectionalAdapter.JavaApplication
	def file
}

class DataSource{
	def driver
	def url
	def username
	def password
}

@ToString
class DatabaseAdapter{	
	def adapter = UnidirectionalAdapter.SQL
	DataSource dataSource
	def request
}

@ToString
class LoggingAdapter{
	
	enum Level { INFO }
	
	def adapter = UnidirectionalAdapter.Logging
	def level = Level.INFO
}