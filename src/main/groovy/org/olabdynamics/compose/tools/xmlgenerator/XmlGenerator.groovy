package org.olabdynamics.compose.tools.xmlgenerator

import groovy.xml.StreamingMarkupBuilder
import groovy.xml.XmlUtil
import org.olabdynamics.compose.Application
import org.olabdynamics.compose.DatabaseAdapter
import org.olabdynamics.compose.EventHandler
import org.olabdynamics.compose.HttpAdapter
import org.olabdynamics.compose.Input
import org.olabdynamics.compose.tools.visitor.Aggregator

class XmlGenerator {
	
	/*

def nodeAsText = XmlUtil.serialize(nodeToSerialize)


new File(baseDir,'haiku.txt').withWriter('utf-8') { writer ->
    writer.writeLine 'Into the ancient pond'
    writer.writeLine 'A frog jumps'
    writer.writeLine 'Water’s sound!'
}

*/
	
	def instructions
	def aggregators
	
	//File springApplicationContext;
	
	//BufferedWriter file = new File("essai.xml").newWriter() 
	//def fileName = 'localApplicationContext.xml'
	//def xml = new StreamingMarkupBuilder()
	
	
	def xmls = []
	
	void generate(){
		
		instructions.each {
			if(it instanceof Application){
				def xmlForCompute = generateApplication(it)
				xmls.add(xmlForCompute)
			} else if(it instanceof EventHandler){
				def xmlEvent = generateEventHandler(it)
				xmls.add(xmlEvent)
			} 
		}
		aggregators.each {
			def xmlForAggregator = generateAggregator(it)
			xmls.add(xmlForAggregator)
		}
		
		new File('essai.xml').withWriter('utf-8') { writer ->
			writer.writeLine '<beans xmlns="http://www.springframework.org/schema/beans" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:int="http://www.springframework.org/schema/integration" xmlns:int-http="http://www.springframework.org/schema/integration/http" xmlns:int-groovy="http://www.springframework.org/schema/integration/groovy" xmlns:context="http://www.springframework.org/schema/context" xmlns:task="http://www.springframework.org/schema/task" xmlns:jdbc="http://www.springframework.org/schema/jdbc" xmlns:int-jdbc="http://www.springframework.org/schema/integration/jdbc" xmlns:int-script="http://www.springframework.org/schema/integration/scripting" xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd http://www.springframework.org/schema/context http://www.springframework.org/schema/context/spring-context.xsd http://www.springframework.org/schema/task http://www.springframework.org/schema/task/spring-task.xsd http://www.springframework.org/schema/integration http://www.springframework.org/schema/integration/spring-integration-4.2.xsd http://www.springframework.org/schema/integration/jdbc http://www.springframework.org/schema/integration/jdbc/spring-integration-jdbc-4.2.xsd http://www.springframework.org/schema/jdbc http://www.springframework.org/schema/jdbc/spring-jdbc-4.2.xsd http://www.springframework.org/schema/integration/http http://www.springframework.org/schema/integration/http/spring-integration-http-4.2.xsd http://www.springframework.org/schema/integration/groovy http://www.springframework.org/schema/integration/groovy/spring-integration-groovy-4.2.xsd http://www.springframework.org/schema/integration/scripting http://www.springframework.org/schema/integration/scripting/spring-integration-scripting-4.2.xsd">' 
			xmls.each{
				writer.writeLine it.toString()
			}
			writer.writeLine '</beans>'
		}
		
	}
	
	def localApplicationContext = {
		String codeName, String expression ->
		//def inputFileChannel = 'inputFileChannel' + i
		def inputChannel = codeName + 'InputChannel'
		def outputServiceChannel = codeName + "Output"
		def outputChannel = codeName + "OutputChannel"
		def exp = expression
		def transformerBean = codeName + "TransformerBean"
		def transformerRef = codeName
		def clos = {
	
	/*		beans(xmlns:"http://www.springframework.org/schema/beans",
				"xmlns:xsi":"http://www.w3.org/2001/XMLSchema-instance",
				"xmlns:int":"http://www.springframework.org/schema/integration",
				"xmlns:int-file":"http://www.springframework.org/schema/integration/file",
				"xmlns:context":"http://www.springframework.org/schema/context",
				"xsi:schemaLocation":"http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd http://www.springframework.org/schema/context http://www.springframework.org/schema/context/spring-context.xsd http://www.springframework.org/schema/integration http://www.springframework.org/schema/integration/spring-integration-4.2.xsd http://www.springframework.org/schema/integration/file http://www.springframework.org/schema/integration/file/spring-integration-file-4.2.xsd"){*/
	
				"int:service-activator"("input-channel":inputChannel, "output-channel":outputServiceChannel, expression:exp){}
				
				"int:transformer"("input-channel":outputServiceChannel, "output-channel":outputChannel, ref:transformerBean, "method":"transform"){ }
				
				"bean"(id:transformerBean, class:"org.olabdynamics.compose.tools.code.ObjectToApplicationTransformer"){
					"property"(name:"application", ref:transformerRef){ }
				}
		//	}
		
		}
	}
	
	def generateApplication(Application application){
		//def fileName = 'localApplicationContext.xml'
		def applicationName = application.name
		def methodName = application.input.adapter.method
		def expression = '@' + applicationName + '.' + methodName + '(payload.input.value)'
		def xml = new StreamingMarkupBuilder()
		xml.useDoubleQuotes = true
		//BufferedWriter file = springApplicationContext.newWriter()
		//xml.bind(localApplicationContext(applicationName,expression)).writeTo( file )
		return xml.bind(localApplicationContext(applicationName,expression))
	}
	
	def inputHttpAdapterContext = {
		String inputName ->
		def inputChannel = inputName + 'InputChannel'
		def gatewayReplyChannel = inputName + "ReplyChannel"
		def outputChannel = inputName + "OutputChannel"
		def serviceInterface = "myservice." + inputName + "Gateway"
		def id = "headers['id'].toString()"
		def clos = {
	
			"int:gateway"("default-request-channel":inputChannel, "default-reply-channel":gatewayReplyChannel, "service-interface":serviceInterface){ }
			
			"int:channel"(id:inputChannel){ }
			
			"int:header-enricher"("input-channel":inputChannel, "output-channel":outputChannel){
				"int:header"(name:"messageID", expression:id){ }
			}
				 
			"int:channel"(id:gatewayReplyChannel){ }
			
		}
	}
	
	def generateEventHandler(EventHandler eventHandler){
		def xml = new StreamingMarkupBuilder()
		xml.useDoubleQuotes = true
		if(eventHandler.input != null){
			if(eventHandler.input.adapter instanceof HttpAdapter){
				def inputName = eventHandler.name
				return xml.bind(inputHttpAdapterContext(inputName))
				//BufferedWriter file = springApplicationContext.newWriter()
				//xml.bind(inputHttpAdapterContext(inputName)).writeTo( file )
			}
		} 
		if(eventHandler.output != null){
			if(eventHandler.output.adapter instanceof DatabaseAdapter){
				return xml.bind(outputDatabaseAdapterContext(eventHandler))
				//BufferedWriter file = springApplicationContext.newWriter()
				//xml.bind(outputDatabaseAdapterContext(eventHandler)).writeTo( file )
			}
		}
		
	}

	def outputDatabaseAdapterContext = {
		EventHandler eventHandler ->
		def outputChannel = eventHandler.name + 'OutputChannel'
		def dataSourceBeanID = eventHandler.name + "DataSource"
		def dataSource = "org.springframework.jdbc.datasource.DriverManagerDataSource"
		def driver = eventHandler.output.adapter.dataSource.driver
		def url = eventHandler.output.adapter.dataSource.url
		def username = eventHandler.output.adapter.dataSource.username
		def password = eventHandler.output.adapter.dataSource.password
		def request = eventHandler.output.adapter.request
		def persitSpelSource = eventHandler.name + 'PersitSpelSource'
		def clos = {
	
			"int:channel"(id:outputChannel){ }
	
			"bean"(id:dataSourceBeanID, class:dataSource){
				"property"(name:"driverClassName", value:driver){ }
				"property"(name:"url", value:url){ }
				"property"(name:"username", value:username){ }
				"property"(name:"password", value:password){ }
			}
			    
			"int-jdbc:outbound-channel-adapter"("data-source":dataSourceBeanID, channel:outputChannel, query:request, "sql-parameter-source-factory":persitSpelSource){ }
			
			"bean"(id:persitSpelSource, class:"org.springframework.integration.jdbc.ExpressionEvaluatingSqlParameterSourceFactory"){
				"property"(name:"parameterExpressions"){
					"map"(){
						"entry"(key:"result", value:"payload"){  }	// ! result et payload à changer
					}
				}
			}			   			
		}
	}
	
	def generateAggregator(Aggregator aggregator){
		def xml = new StreamingMarkupBuilder()
		xml.useDoubleQuotes = true
		//BufferedWriter file = springApplicationContext.newWriter()
		//xml.bind(aggregatorContext(aggregator)).writeTo( file )
		return xml.bind(aggregatorContext(aggregator))
	}

	def aggregatorContext = {
		Aggregator aggregator ->
		def aggregatorName = ""
		aggregator.applications.each{
			aggregatorName = aggregatorName + it.name
		}
		def inputChannel = aggregatorName + "AggregatorInput"
		def outputChannel = aggregatorName + "AggregatorOutput"
		def outputTransformerChannel = aggregatorName + "AggregatorOutputTransformer"
		def size = "size()=="  + aggregator.applications.size()
		def releaseExpression = size + " AND " + "([0].payload instanceof T(org.olabdynamics.compose.Application) AND [0].payload.state==T(org.olabdynamics.compose.State).TERMINATED)"
		def transformerExpression = "(payload[0] instanceof T(org.olabdynamics.compose.Application) AND payload[0].name=='" + aggregatorName + "') ? payload[0] : payload[1]"
		def clos = {
	
			"int:aggregator"("input-channel":inputChannel, "output-channel": outputChannel, "correlation-strategy-expression":"headers['messageID']", "release-strategy-expression":releaseExpression){
			}
		    
			"int:transformer"("input-channel":outputChannel, "output-channel":outputTransformerChannel, expression:transformerExpression){	} 
			
		}
	}
}
