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
	WebSocket,
	ComposeEvent
}

enum BidirectionalAdapter{
	HTTP,
	AMQP,
	WebService,
	JavaApplication
}

@ToString
class InputFileAdapter{
	def adapter = UnidirectionalAdapter.File
	def directory
	def filenamePattern
}

enum WritingMode{
	REPLACE,
	APPEND
}

@ToString
class OutputFileAdapter{
	def adapter = UnidirectionalAdapter.File
	def directory
	boolean createDirectory
	def filename
	boolean appendNewLine
	WritingMode writingMode
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

@ToString
class ComposeEventAdapter{
	
	def adapter = UnidirectionalAdapter.ComposeEvent
	
}
