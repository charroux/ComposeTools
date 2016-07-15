package org.olabdynamics.compose.tools

import org.olabdynamics.compose.tools.javacodegeneration.JavaCodeGenerator
import org.olabdynamics.compose.tools.visitor.ComposeCodeVisitor
import org.olabdynamics.compose.tools.visitor.MyClassLoader
import org.olabdynamics.compose.tools.xmlgenerator.XmlGenerator
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component

@Component
class Compile implements CommandLineRunner{
	
	@Autowired
	ApplicationContext context

	@Override
	public void run(String... args) throws Exception {
		
		String[] arguments = args[0].split("\\s+")
		
		if(arguments.length != 2){
			throw new Exception("A compose source file (.groovy extension) should be in ./compose/source, put the file name (with no path) as the first argument, true or false for the Quality Of Service as a second argumeent")
		}
		
		def visitor = new ComposeCodeVisitor(context: context)
		def myCL = new MyClassLoader(visitor: visitor)
		
		String pathToCode = "." + File.separator + "compose" + File.separator + "source" + File.separator + arguments[0]
		def script = myCL.parseClass(new GroovyCodeSource(new File(pathToCode)))

		boolean QoS = Boolean.parseBoolean(arguments[1])
			
		if(QoS == true){
	
			def xmlSpringContent = "." + File.separator + "src" + File.separator + "main" + File.separator + "resources" + File.separator + "QoSSpringContext.xml"
			def xmlGenerator = new XmlGenerator(xmlSpringContent: xmlSpringContent, instructions: visitor.instructions, aggregators: visitor.aggregators, composeEvents: false)
			xmlGenerator.generate()
	
		} else {

			def xmlSpringContent = "." + File.separator + "src" + File.separator + "main" + File.separator + "resources" + File.separator + "xmlSpringContext.xml"
			def xmlGenerator = new XmlGenerator(xmlSpringContent: xmlSpringContent, instructions: visitor.instructions, aggregators: visitor.aggregators, composeEvents: true)
			xmlGenerator.generate()
			
		} 
		
		def javaCodeGenerator = new JavaCodeGenerator(context: context, instructions: visitor.instructions)
		javaCodeGenerator.generate()
		
		
	}

}
