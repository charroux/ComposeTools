package org.olabdynamics.compose.tools.xmlgenerator

import groovy.util.logging.Slf4j;
import groovy.xml.QName
import groovy.xml.StreamingMarkupBuilder
import groovy.xml.XmlUtil

import org.olabdynamics.compose.Application
import org.olabdynamics.compose.DatabaseAdapter
import org.olabdynamics.compose.EventHandler
import org.olabdynamics.compose.HttpAdapter
import org.olabdynamics.compose.Input
import org.olabdynamics.compose.JavaServiceAdapter
import org.olabdynamics.compose.tools.visitor.Aggregator
import org.olabdynamics.compose.tools.visitor.Instruction
import org.springframework.context.ApplicationContext;

@Slf4j
class XmlGenerator {
	
	def xmlSpringContent
	def instructions
	def aggregators

	//def xmls = []
	
	void generate(){

		BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(new File(xmlSpringContent)))
		bufferedWriter.writeLine('<beans xmlns="http://www.springframework.org/schema/beans" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:int="http://www.springframework.org/schema/integration" xmlns:int-http="http://www.springframework.org/schema/integration/http" xmlns:int-groovy="http://www.springframework.org/schema/integration/groovy" xmlns:context="http://www.springframework.org/schema/context" xmlns:task="http://www.springframework.org/schema/task" xmlns:jdbc="http://www.springframework.org/schema/jdbc" xmlns:int-jdbc="http://www.springframework.org/schema/integration/jdbc" xmlns:int-script="http://www.springframework.org/schema/integration/scripting" xmlns:int-event="http://www.springframework.org/schema/integration/event" xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd http://www.springframework.org/schema/context http://www.springframework.org/schema/context/spring-context.xsd http://www.springframework.org/schema/task http://www.springframework.org/schema/task/spring-task.xsd http://www.springframework.org/schema/integration http://www.springframework.org/schema/integration/spring-integration-4.2.xsd http://www.springframework.org/schema/integration/jdbc http://www.springframework.org/schema/integration/jdbc/spring-integration-jdbc-4.2.xsd http://www.springframework.org/schema/jdbc http://www.springframework.org/schema/jdbc/spring-jdbc-4.2.xsd http://www.springframework.org/schema/integration/http http://www.springframework.org/schema/integration/http/spring-integration-http-4.2.xsd http://www.springframework.org/schema/integration/groovy http://www.springframework.org/schema/integration/groovy/spring-integration-groovy-4.2.xsd http://www.springframework.org/schema/integration/scripting http://www.springframework.org/schema/integration/scripting/spring-integration-scripting-4.2.xsd http://www.springframework.org/schema/integration/event http://www.springframework.org/schema/integration/event/spring-integration-event-4.2.xsd">')
		
		def xmlForEvents = generateComposeEventManagement()
		
		bufferedWriter.writeLine(xmlForEvents.toString())
		
		//xmls.add(xmlForEvents)
		
		
		
		//def hashcodes = []
		
		def receiveEventHandlers = sortReceiveEventHandlers(instructions)
		
		receiveEventHandlers.each{
			
			def xmlEvent = generateReceiveEventHandler(it)	// input events
			
			bufferedWriter.writeLine(xmlEvent.toString())
			
			//xmls.add(xmlEvent)
			
		}
		
		def instructionsForReceiveEventHandlersRouter = sortInstructionsByReceiveEventHandler(instructions)	// 2 dimensions array, 1 event par dimension
		
		instructionsForReceiveEventHandlersRouter.each{		
			
			def xmlForEventHandlerRouter = generateRouterForEventHandlers(it)
			
			bufferedWriter.writeLine(xmlForEventHandlerRouter.toString())
			
			//xmls.add(xmlForEventHandlerRouter)
				
		}
			
			
			/*def routersForEventHandlersVariable = sortInstructionsByEventHandlerVariable(it)	// 2 dimensions array, 1 variable per dimension
			
			for(int i=0; i<routersForEventHandlersVariable.size(); i++){
				
				def xmlForEventHandlerRouter = generateRouterForEventHandlersVariable(routersForEventHandlersVariable[i])
				xmls.add(xmlForEventHandlerRouter)
				
			}	*/
		
		
		def applications = sortInstructionsByApplication(instructions)
		
		applications.each {
			def xmlForCompute = generateApplication(it, instructions, aggregators)
			bufferedWriter.writeLine(xmlForCompute.toString())	
			//xmls.add(xmlForCompute)
		}
				
		aggregators.each {
			def xmlForAggregator = generateAggregator(it, instructions)
			
			bufferedWriter.writeLine(xmlForAggregator.toString())
			
			//xmls.add(xmlForAggregator)
		}
		
		def routersForSendEventHandlers = sortInstructionsBySendEventHandler(instructions)	// 2 dimensions array, 1 event par dimension
		
		routersForSendEventHandlers.each{
			
			def xmlEvent = generateSendEventHandler(it)	// output events
			
			bufferedWriter.writeLine(xmlEvent.toString())
			
			//xmls.add(xmlEvent)
			
		}
		
		/*BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(new File(xmlSpringContent)))
		bufferedWriter.writeLine('<beans xmlns="http://www.springframework.org/schema/beans" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:int="http://www.springframework.org/schema/integration" xmlns:int-http="http://www.springframework.org/schema/integration/http" xmlns:int-groovy="http://www.springframework.org/schema/integration/groovy" xmlns:context="http://www.springframework.org/schema/context" xmlns:task="http://www.springframework.org/schema/task" xmlns:jdbc="http://www.springframework.org/schema/jdbc" xmlns:int-jdbc="http://www.springframework.org/schema/integration/jdbc" xmlns:int-script="http://www.springframework.org/schema/integration/scripting" xmlns:int-event="http://www.springframework.org/schema/integration/event" xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd http://www.springframework.org/schema/context http://www.springframework.org/schema/context/spring-context.xsd http://www.springframework.org/schema/task http://www.springframework.org/schema/task/spring-task.xsd http://www.springframework.org/schema/integration http://www.springframework.org/schema/integration/spring-integration-4.2.xsd http://www.springframework.org/schema/integration/jdbc http://www.springframework.org/schema/integration/jdbc/spring-integration-jdbc-4.2.xsd http://www.springframework.org/schema/jdbc http://www.springframework.org/schema/jdbc/spring-jdbc-4.2.xsd http://www.springframework.org/schema/integration/http http://www.springframework.org/schema/integration/http/spring-integration-http-4.2.xsd http://www.springframework.org/schema/integration/groovy http://www.springframework.org/schema/integration/groovy/spring-integration-groovy-4.2.xsd http://www.springframework.org/schema/integration/scripting http://www.springframework.org/schema/integration/scripting/spring-integration-scripting-4.2.xsd http://www.springframework.org/schema/integration/event http://www.springframework.org/schema/integration/event/spring-integration-event-4.2.xsd">')
		xmls.each{
			bufferedWriter.writeLine(it.toString())
		}*/
		bufferedWriter.writeLine('</beans>')
		bufferedWriter.flush()
		bufferedWriter.close()
		
		// usefull to write line by line
		String xmlContext = new File(xmlSpringContent).text		
		String springContexteAsText = XmlUtil.serialize(xmlContext)
		new File(xmlSpringContent).withWriter('utf-8') { writer ->
			writer.writeLine springContexteAsText
		}
		
/*		new File(xmlSpringContent).withWriter('utf-8') { writer ->
			writer.writeLine '<beans xmlns="http://www.springframework.org/schema/beans" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:int="http://www.springframework.org/schema/integration" xmlns:int-http="http://www.springframework.org/schema/integration/http" xmlns:int-groovy="http://www.springframework.org/schema/integration/groovy" xmlns:context="http://www.springframework.org/schema/context" xmlns:task="http://www.springframework.org/schema/task" xmlns:jdbc="http://www.springframework.org/schema/jdbc" xmlns:int-jdbc="http://www.springframework.org/schema/integration/jdbc" xmlns:int-script="http://www.springframework.org/schema/integration/scripting" xmlns:int-event="http://www.springframework.org/schema/integration/event" xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd http://www.springframework.org/schema/context http://www.springframework.org/schema/context/spring-context.xsd http://www.springframework.org/schema/task http://www.springframework.org/schema/task/spring-task.xsd http://www.springframework.org/schema/integration http://www.springframework.org/schema/integration/spring-integration-4.2.xsd http://www.springframework.org/schema/integration/jdbc http://www.springframework.org/schema/integration/jdbc/spring-integration-jdbc-4.2.xsd http://www.springframework.org/schema/jdbc http://www.springframework.org/schema/jdbc/spring-jdbc-4.2.xsd http://www.springframework.org/schema/integration/http http://www.springframework.org/schema/integration/http/spring-integration-http-4.2.xsd http://www.springframework.org/schema/integration/groovy http://www.springframework.org/schema/integration/groovy/spring-integration-groovy-4.2.xsd http://www.springframework.org/schema/integration/scripting http://www.springframework.org/schema/integration/scripting/spring-integration-scripting-4.2.xsd http://www.springframework.org/schema/integration/event http://www.springframework.org/schema/integration/event/spring-integration-event-4.2.xsd">' 
			xmls.each{
				writer.writeLine it.toString()
			}
			writer.writeLine '</beans>'
		}*/
		
/*		String xmlContext = new File(xmlSpringContent).text
		
		String springContext = XmlUtil.serialize(xmlContext)
		
		def beans = new XmlParser().parseText(springContext)

		def aggregator = beans."*".find { node->
			node.name() == "int:aggregator"
		}

		aggregators.each {	
			
			it.applications.each {	
				
				log.info "application to aggregate: " + it
				
				// connect the aggregator input
				
				def applicationName = it.name + 'Output'	// it.name == code1 for compute code1
									
				log.info "application output channel: " + applicationName
				
				def transformer = beans."*".find { node->	// find a transformer after a compute
					node.name()=="int:transformer" && node.@"input-channel"==applicationName
				}
				
				log.info "transformer after the compute: " + transformer
				
				def transformerOutput = transformer.@"output-channel"
				
				def router = beans."*".find { node->	// find a router after a compute transformer: to route the result to the Compose Event handler
					node.name()=="int:recipient-list-router" && node.@"input-channel"==transformerOutput
				}
				
				log.info "router after the transformer (to route the result to the Compose Event handler): " + router
				
				def routeChannelName
				def route
				Iterator routes = router.iterator()
				while(routes.hasNext()){
					route = routes.next()
					routeChannelName = route.@"channel"
					if(routeChannelName.contains("ComposeEvent") == false){
						route.@"channel" = aggregator.@"input-channel"	// connect the route output to the aggregator input
					}
				}
				
				log.info "router output connected to the aggregator input: " + router
				
				def routeChannelAggregator = beans."*".find { node->	
					node.name()=="int:channel" && node.@"id"==aggregator.@"input-channel"
				}
				
				log.info "Channel with id = aggregator input(" + aggregator.@"input-channel" + "): " + routeChannelAggregator
				
				if(routeChannelAggregator == null){
				
					def routeChannel = beans."*".find { node->	// find a channel with the router's name
						node.name()=="int:channel" && node.@"id"==routeChannelName
					}
					
					log.info "Channel with id = aggregator input(" + routeChannelName + "): " + routeChannel
					
					routeChannel.@"id" = aggregator.@"input-channel"
					
					log.info "Channel changed with id = aggregator input(" + routeChannelName + "): " + routeChannel
	
				} else {
				
					log.info "Channel deleted: " + routeChannelAggregator
					
					def parent = routeChannelAggregator.parent()
					parent.remove(routeChannelAggregator)
				}
					
			
				// connect the aggregator output
				
				applicationName = applicationName + 'Channel'
				
				log.info "Aggregator output must be connected to: " + applicationName
				
				def applicationTransformerNode = beans."*".find { node->	// find the instruction using code1.result
					node.@"channel"==applicationName || (node.@"input-channel"==applicationName && node.name()!="int:recipient-list-router")
				}
				
				log.info "Channel to be connected: " + applicationTransformerNode				
				
				if(applicationTransformerNode != null){
							
					def aggregratorTransformer = beans."*".find { node->	// find the transformer after the aggregator
						node.name()=="int:transformer" && node.@"input-channel"==aggregator.@"output-channel"
					}
					
					log.info "Transformer after the aggregator: " + aggregratorTransformer
					
					applicationTransformerNode.@"input-channel" = aggregratorTransformer.@"output-channel"	// connect the aggregator output
					
					log.info "Aggregator changed: " + applicationTransformerNode
					
				}						
			}			
		}
		
		// adjust the service activator input to the routes:
		// - The route to the compose event related to the service call event
		// - the route to the services (in the case of an event routed  to many services)
		
		log.info "Adjust the service activator input according to the routes"
		
		def springContexteAsText = XmlUtil.serialize(beans)
		
		//new File('essai.xml').withWriter('utf-8') { writer ->
		new File(xmlSpringContent).withWriter('utf-8') { writer ->
			writer.writeLine springContexteAsText
		}
*/		
		
	}
	
	def Instruction[] sortReceiveEventHandlers(def instructions){
		
		def eventHandlers = []
		
		instructions.each {
			
			if(it.springBean instanceof EventHandler && it.instruction=="receive"){
				
				int i=0
				while(i<eventHandlers.size() && eventHandlers.get(i).springBean!=it.springBean){
					i++
				}
				
				if(i == eventHandlers.size()){
					eventHandlers.add(it)
				}
				
			}
			
		}
		
		return eventHandlers
	}
	
	def Instruction[][] sortInstructionsByReceiveEventHandler(def instructions){
		
		def routersForEventHandlers = [][]
		
		instructions.each {
			
			if(it.springBean instanceof EventHandler && it.instruction=="receive"){
				
				int i=0
				boolean newHandler = true
				
				while(i<routersForEventHandlers.size() && newHandler==true){
					if(routersForEventHandlers[i].size()>0 && it.springBean==routersForEventHandlers[i][0].springBean){
						routersForEventHandlers[i].add(it)
						newHandler = false
					}
					i++
				}
				
				if(newHandler == true){
					def instructs = []
					instructs.add(it)
					routersForEventHandlers.add(instructs)
				}
			}
		}
		
		return routersForEventHandlers
	}
	
	def Instruction[] sortInstructionsBySendEventHandler(def instructions){
	
		def sends = []
		
		instructions.each {
			if(it.instruction == "send"){
				sends.add(it)
			}
		}	
		return sends
	}
	
	def Instruction[] sortInstructionsByApplication(def instructions){
		
		def applications = []
		
		instructions.each {
			if(it.springBean instanceof Application){
				applications.add(it)
			}
		}
		
		return applications
		
	}
	
	def Instruction[][] sortInstructionsByEventHandlerVariable(def instructions){
		
		def sortedInstructions = instructions.toSorted { a, b -> a.variable <=> b.variable }
		
		def sortedInstructionsByVariable = [][]
		def instructs = []
		sortedInstructionsByVariable.add(instructs)
		
		int i = 0
		def variable = sortedInstructions[0].variable
		
		sortedInstructions.each {
			if(it.variable == variable){
				instructs = sortedInstructionsByVariable[i]
				instructs.add(it)
			} else {
				instructs = []
				instructs.add(it)
				sortedInstructionsByVariable.add(instructs)
				i++
			}
		} 
		
		return sortedInstructionsByVariable
	}
	
	def composeEventManagementContext = {
		def clos = {
			
			"int:publish-subscribe-channel"(id:"composeEventChannel"){  }
			
			"int-event:outbound-channel-adapter"(channel:"composeEventChannel"){  }
			
			"int:logging-channel-adapter"(channel:"composeEventChannel", level:"INFO"){  }
			
			"int-event:inbound-channel-adapter"(channel:"springContextEventChannel", "event-types":"org.springframework.context.event.ContextRefreshedEvent"){  }
					
			"int:transformer"("input-channel":"springContextEventChannel", "output-channel":"composeEventChannel"){
				"int-script:script"(lang:"groovy", location:"#{createReInitializedServiceEvent.input.adapter.file}"){  }
			}
				   
		}
	}
	
	def generateComposeEventManagement(){
		def xml = new StreamingMarkupBuilder()
		xml.useDoubleQuotes = true
		return xml.bind(composeEventManagementContext())
	}
	
	def localApplicationContext = {
		Instruction instruction, def instructions, def aggregators ->
		
		def inputName = instruction.withs[0].with
		//def Application application = instruction.springBean
		def applicationName = instruction.springBean.name
		def methodName = instruction.springBean.input.adapter.method
		JavaServiceAdapter javaServiceAdapter = (JavaServiceAdapter)instruction.springBean.input.adapter
		String className = javaServiceAdapter.javaClass
		className = className.substring(className.lastIndexOf('.')+1)
		className = className.substring(0,1).toLowerCase() + className.substring(1)
		def expression = '@' + className + '.' + methodName + '(payload)'
		
		// look for the index i of the current instruction into all the inctructions
		int i=0
		while(i<instructions.size() && instructions.get(i)!=instruction){
			i++
		}
		
		// search the previous instruction (decrease i) so its variable matches one of the withs of the current instruction
		
		while(i>=0 && instruction.containsWith(instructions.get(i).variable)==false){
			i--
		}
		/*boolean stop = false
		
		while(i>=0 && stop==false){
			int j=0
			while(j<instruction.withs.size() && instruction.withs.get(j).with!=instructions.get(i).variable){
				j++
			}
			if(j <  instruction.withs.size()){
				stop = true
			} else {
				i--
			}
			
		}*/
		
		def instructionToConnectToTheInput = instructions.get(i)
		
		i=0
		while(i<aggregators.size() && aggregators.get(i).releaseExpression.contains(instruction.springBean.name)==false){
			i++
		}
		
		def aggregatorName = ""
		def applicationNames = []
		
		aggregators.get(i).applications.each{
			applicationNames.add(it.name)
			aggregatorName = aggregatorName + it.name	
		}
		
		def outputRouterChannel = aggregatorName + "AggregatorInput"
		
		instruction.springIntegrationOutputChannel = outputRouterChannel
		instruction.springIntegrationOutputBeanId = outputRouterChannel
	
		aggregators.get(i).springIntegrationInputChannel = outputRouterChannel
		
		/*def aggregator
		while(i<aggregators.size()){
			aggregator
			
			i++
		}*/
		
		def id = applicationName + 'Channel'
		//def inputChannel = inputName + 'Channel'
		def inputChannel = instructionToConnectToTheInput.springIntegrationOutputChannel
		def outputServiceChannel = applicationName + "Output"
		def outputChannel = applicationName + 'OutputChannel'
		//def outputRouterChannel = applicationName + 'OutputRouteChannel'
		def exp = expression
		def transformerBean = applicationName + "TransformerBean"
		def transformerRef = applicationName
		def clos = {
	
				"int:service-activator"(id:"service-activator-"+id+"-id", "input-channel":inputChannel, "output-channel":outputServiceChannel, expression:exp){}
				
				"int:transformer"(id:"transformer-"+outputServiceChannel+"-id", "input-channel":outputServiceChannel, "output-channel":outputChannel, ref:transformerBean, "method":"transform"){ }
				
				"bean"(id:transformerBean, class:"org.olabdynamics.compose.tools.code.ObjectToApplicationTransformer"){
					"property"(name:"application", ref:transformerRef){ }
				}
				
				"int:recipient-list-router"(id:"router-"+ id + "-event-id", "input-channel":outputChannel){
					"int:recipient"(channel:outputRouterChannel){  }
					"int:recipient"(channel:applicationName + "ComposeEventRoute"){  }
				}
				
		/*		"int:channel"(id:outputRouterChannel){		already added by the aggregator
				}
		*/		
				"int:channel"(id:applicationName + "ComposeEventRoute"){
				}
				
				"int:transformer"("input-channel":applicationName + "ComposeEventRoute", "output-channel":"composeEventChannel"){
					"int-script:script"(lang:"groovy", location:"#{createServiceCallReturnEvent.input.adapter.file}"){  }
				}
		
		}
	}
	
	def generateApplication(Instruction instruction, def instructions, def aggregators){
		if(instruction.springBean.input.adapter instanceof JavaServiceAdapter){
			def xml = new StreamingMarkupBuilder()
			xml.useDoubleQuotes = true
			return xml.bind(localApplicationContext(instruction, instructions, aggregators))
		} else {
			def message = instruction.springBean.input.adapter + ' is not supported yet.'
			throw new CompilationException(message)
		}
		/*def inputName = instruction.with
		def Application application = instruction.springBean
		def applicationName = application.name
		def methodName = application.input.adapter.method
		if(application.input.adapter instanceof JavaServiceAdapter){
			JavaServiceAdapter javaServiceAdapter = (JavaServiceAdapter)application.input.adapter
			String className = javaServiceAdapter.javaClass
			className = className.substring(className.lastIndexOf('.')+1)
			className = className.substring(0,1).toLowerCase() + className.substring(1)
			def expression = '@' + className + '.' + methodName + '(payload)'
			def xml = new StreamingMarkupBuilder()
			xml.useDoubleQuotes = true
			return xml.bind(localApplicationContext(applicationName,inputName,expression))
		} else {
			def message = application.input.adapter + ' is not supported yet.'
			throw new CompilationException(message)
		}*/
	}
	
	def inputHttpAdapterContext = {
		Instruction instruction ->
		def inputName = instruction.springBean.name
		//def outputName = instruction.variable
		def inputChannel = inputName + 'InputChannel'
		def gatewayReplyChannel = inputName + "ReplyChannel"
		def outputChannel = inputName + "OutputChannel"
		instruction.springIntegrationOutputChannel = outputChannel
		instruction.springIntegrationOutputBeanId = "header-enricher-"+inputChannel+"-id"
		def serviceInterface = "myservice." + inputName.substring(0, 1).toUpperCase() + inputName.substring(1) + "Gateway"
		def id = "headers['id'].toString()"
		def clos = {
	
			//"int:gateway"(id:"gateway-"+inputChannel+"-id", "default-request-channel":inputChannel, "default-reply-channel":gatewayReplyChannel, "service-interface":serviceInterface){ }
			
			"int:gateway"(id:"gateway-"+inputChannel+"-id", "default-request-channel":inputChannel, "service-interface":serviceInterface){ }
			
			"int:channel"(id:inputChannel){ }
			
			"int:header-enricher"(id:instruction.springIntegrationOutputBeanId, "input-channel":inputChannel, "output-channel":instruction.springIntegrationOutputChannel){
				"int:header"(name:"messageID", expression:id){ }
			}
				
/*			"int:recipient-list-router"("input-channel":"inputRouterChannel"){
				"int:recipient"(channel:"executorChannel"){  }
				"int:recipient"(channel:"inputComposeEventChannel"){  }
			}
				
			"int:transformer"("input-channel":"inputComposeEventChannel", "output-channel":"composeEventChannel"){
				"int-script:script"(lang:"groovy", location:"#{createIncomingMessageEvent.input.adapter.file}"){  }
			}*/
		   		
		}
	}
	
	def generateReceiveEventHandler(Instruction instruction){
		def EventHandler eventHandler = instruction.springBean
		def xml = new StreamingMarkupBuilder()
		xml.useDoubleQuotes = true
		
		if(eventHandler.input.adapter instanceof HttpAdapter){
				
			return xml.bind(inputHttpAdapterContext(instruction))
				
		} 
	}

	def generateSendEventHandler(Instruction instruction){
		def EventHandler eventHandler = instruction.springBean
		def xml = new StreamingMarkupBuilder()
		xml.useDoubleQuotes = true

		if(eventHandler.output.adapter instanceof DatabaseAdapter){
				
			return xml.bind(outputDatabaseAdapterContext(instruction))
		
		}	
	}
	
	def outputDatabaseAdapterContext = {
		Instruction instruction ->
		def inputName = instruction.variable
		EventHandler eventHandler = instruction.springBean
		//String inputName, EventHandler eventHandler ->
		//instruction.springIntegrationInputChannel = inputName + 'OutputChannel'
		//def outputChannel = inputName + 'OutputChannel'
		def transformerBean = instruction.springIntegrationInputChannel + "TransformerBean"
		def databaseChannel = inputName + 'DatabaseChannel'
		def dataSourceBeanID = eventHandler.name + "DataSource"
		def dataSource = "org.springframework.jdbc.datasource.DriverManagerDataSource"
		def driver = eventHandler.output.adapter.dataSource.driver
		def url = eventHandler.output.adapter.dataSource.url
		def username = eventHandler.output.adapter.dataSource.username
		def password = eventHandler.output.adapter.dataSource.password
		def request = eventHandler.output.adapter.request
		def persitSpelSource = eventHandler.name + 'PersitSpelSource'
		instruction.springIntegrationInputBeanId = "transformer-"+instruction.springIntegrationInputChannel+"-id" 
		def clos = {
	
			"int:transformer"(id:instruction.springIntegrationInputBeanId, "input-channel":instruction.springIntegrationInputChannel, "output-channel":databaseChannel, ref:transformerBean, "method":"transform"){ }
			
			"bean"(id:transformerBean, class:"org.olabdynamics.compose.tools.code.ApplicationToObjectTransformer"){
			}
	
			"bean"(id:dataSourceBeanID, class:dataSource){
				"property"(name:"driverClassName", value:driver){ }
				"property"(name:"url", value:url){ }
				"property"(name:"username", value:username){ }
				"property"(name:"password", value:password){ }
			}
	
			"int:channel"(id:databaseChannel){ }
			
			"int-jdbc:outbound-channel-adapter"(id:"outbound-channel-adapter-"+instruction.springIntegrationInputChannel+"-id", "data-source":dataSourceBeanID, channel:databaseChannel, query:request, "sql-parameter-source-factory":persitSpelSource){ }
			
			"bean"(id:persitSpelSource, class:"org.springframework.integration.jdbc.ExpressionEvaluatingSqlParameterSourceFactory"){
				"property"(name:"parameterExpressions"){
					"map"(){
						"entry"(key:"result", value:"payload"){  }	// ! result et payload à changer
					}
				}
			}			   			
		}
	}
	
	def generateAggregator(Aggregator aggregator, def instructions){
		def xml = new StreamingMarkupBuilder()
		xml.useDoubleQuotes = true
		//BufferedWriter file = springApplicationContext.newWriter()
		//xml.bind(aggregatorContext(aggregator)).writeTo( file )
		return xml.bind(aggregatorContext(aggregator, instructions))
	}

	def releaseExpression(String expression, def applicationNames){
		int i=0
		def releaseExpression = ""
		Iterator names = applicationNames.iterator()
		def name
		int index
		while(names.hasNext()){
			name = names.next()
			index = expression.indexOf(name) + name.length() + 1
			expression = expression.substring(index)
			expression = expression.trim()
			if(expression.startsWith("terminates")){
				releaseExpression = releaseExpression + " ([" + i + "].payload instanceof T(org.olabdynamics.compose.Application) AND [" + i + "].payload.state==T(org.olabdynamics.compose.State).TERMINATED) "
				i++
				index = expression.indexOf("terminates") + "terminates".length() + 1
				if(index >= expression.length()){
					return releaseExpression
				}
				expression = expression.substring(index)
				expression = expression.trim()				
				if(expression.startsWith("and")){
					releaseExpression = releaseExpression + " and "
					index = expression.indexOf("and") + "and".length() + 1
					expression = expression.substring(index)
					expression = expression.trim()
				} else if(expression.startsWith("or")){
					releaseExpression = releaseExpression + " or "
					index = expression.indexOf("or") + "or".length() + 1
					expression = expression.substring(index)
					expression = expression.trim()
				}
				
			} 
			
		}
		return releaseExpression
	}
	
	def aggregatorContext = {
		Aggregator aggregator, def instructions ->
		
		def aggregatorName = ""
		def applicationNames = []
		aggregator.applications.each{
			applicationNames.add(it.name)
			aggregatorName = aggregatorName + it.name
		}
		
		//def inputChannel = aggregatorName + "AggregatorInput"
		
		def inputChannel = aggregator.springIntegrationInputChannel
		
		def outputChannel = aggregatorName + "AggregatorOutput"
		def outputTransformerChannel = aggregatorName + "AggregatorOutputTransformer"
		
		aggregator.springIntegrationOutputChannel = outputTransformerChannel
		
		int i=0;
		while(i<instructions.size() && instructions.get(i)!=aggregator.nextInstruction){
			i++
		}
		
		instructions.get(i).springIntegrationInputChannel = outputTransformerChannel
		
		// if the same event is used by the aggregator the release strategy should be based on the same message id :
		// look for the with clause of all the instructions leading to the aggregator
		 
		def with = ""
		boolean sameEvent = true
		
		aggregator.applications.each{
			i=0;
			while(i<instructions.size() && instructions.get(i).springBean.name!=it.name){
				i++
			}
			if(with == ""){
				with = instructions.get(i).withs.get(0)
			} else if(instructions.get(i).containsWith(with) == false){
				sameEvent = false 
			}
		}
		
		def releaseExpression = this.releaseExpression(aggregator.releaseExpression, applicationNames)
		def size = "size()=="  + aggregator.applications.size()
		releaseExpression = size + " and " + "(" + releaseExpression + ")"
		//def releaseExpression = size + " AND " + "([0].paload instanceof T(org.olabdynamics.compose.Application) AND [0].payload.state==T(org.olabdynamics.compose.State).TERMINATED)"
		def nextIntruction = aggregator.nextInstruction.variable
		def transformerExpression = "(payload[0] instanceof T(org.olabdynamics.compose.Application) AND payload[0].name=='" + nextIntruction + "') ? payload[0] : payload[1]"
		def clos = {
	
			if(sameEvent == true){
				"int:aggregator"(id:"aggregator-"+inputChannel+"-id", "input-channel":inputChannel, "output-channel": outputChannel, "correlation-strategy-expression":"headers['messageID']", "release-strategy-expression":releaseExpression){
				}
			} else {
				"int:aggregator"(id:"aggregator-"+inputChannel+"-id", "input-channel":inputChannel, "output-channel": outputChannel, "correlation-strategy-expression":"0", "release-strategy-expression":releaseExpression){
				}
			}
			
		    
			"int:transformer"(id:"transformer-"+outputChannel+"-id", "input-channel":outputChannel, "output-channel":outputTransformerChannel, expression:transformerExpression){	} 
		
			"int:channel"(id:outputTransformerChannel){ }
		}
	}
	
	/*def generateRouter(Instruction instruction){
		def EventHandler eventHandler = instruction.springBean
		def xml = new StreamingMarkupBuilder()
		xml.useDoubleQuotes = true
		if(eventHandler.input != null){
			if(eventHandler.input.adapter instanceof HttpAdapter){
				def inputName = instruction.variable
				return xml.bind(routerContext(inputName))
			}
		} 
	}*/
	
	def generateRouterForEventHandlers(def instructions){
		def xml = new StreamingMarkupBuilder()
		xml.useDoubleQuotes = true
		return xml.bind(routerForEventHandlersVariable(instructions))
	}
	
	def routerForEventHandlersVariable = {
		def instructions ->
		def firstInstruction = instructions[0]
		def inputName = firstInstruction.variable	// all instructions should have the same variable
		//def inputChannel = inputName + 'Channel'
		def id = "router-"+firstInstruction.springBean.name+"Channel-id"
		def inputChannel = firstInstruction.springIntegrationOutputChannel
		
		//Instruction instruction
		//def instructs
		
		def clos = {
			
			int i=0
			
			"int:recipient-list-router"(id:id, "input-channel":inputChannel){
				
				boolean  defaultChannel = true
				
				instructions.each{
					
					//it.springIntegrationInputChannel = inputChannel
					//it.springIntegrationInputBeanId = id
					
					if(it.whose != null){
						String selectorExpression = it.whose.replaceFirst(it.variable,"payload")
						"int:recipient"(channel:inputChannel + "Route" + (i+1), "selector-expression":selectorExpression){ }
					} else {
						defaultChannel = false
						"int:recipient"(channel:inputChannel + "Route" + (i+1)){ }
					}	
					i++
				}
				
				if(defaultChannel == true){
					"int:recipient"(channel:"errorChannel"){ }
				}
				
			}
			
			//instructs = instructions.iterator()
			i=0
			instructions.each{
						
				"int:channel"(id:inputChannel + "Route" + (i+1)){
				}
				
				def outputChannel = inputChannel + "Route" + (i+1) + "Output"
				it.springIntegrationOutputChannel = outputChannel
				it.springIntegrationOutputBeanId = "router-"+inputChannel + "Route" + (i+1) + "-id"
						
				"int:recipient-list-router"(id:it.springIntegrationOutputBeanId, "input-channel":inputChannel + "Route" + (i+1)){
					"int:recipient"(channel:outputChannel){ }
					"int:recipient"(channel:inputChannel + "Route" + (i+1) + "ComposeEventRoute"){ }
				}
						
				"int:channel"(id:inputChannel + "Route" + (i+1) + "ComposeEventRoute"){
				}
						
				"int:transformer"("input-channel":inputChannel + "Route" + (i+1) + "ComposeEventRoute", "output-channel":"composeEventChannel"){
					"int-script:script"(lang:"groovy", location:"#{createIncomingMessageEvent.input.adapter.file}"){  }
				}
					
				i++
			}
				
		}
	}
	
/*	def routerContext = {
		String inputName ->
		def inputChannel = inputName + 'Channel'
		def clos = {
	
			"int:recipient-list-router"(id:"router-"+inputChannel+"-id", "input-channel":inputChannel){
				"int:recipient"(channel:inputChannel + "Route1"){ }
				"int:recipient"(channel:inputChannel + "Route2"){ }
				"int:recipient"(channel:inputChannel + "ComposeEventRoute"){ }
			}
			
			"int:channel"(id:inputChannel + "Route1"){
			}
			
			"int:channel"(id:inputChannel + "Route2"){
			}
			
			"int:channel"(id:inputChannel + "ComposeEventRoute"){
			}
			
			"int:transformer"("input-channel":inputChannel + "ComposeEventRoute", "output-channel":"composeEventChannel"){
				"int-script:script"(lang:"groovy", location:"#{createIncomingMessageEvent.input.adapter.file}"){  }
			}
		}
	}*/
}
