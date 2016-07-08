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
		
		def visitor = new ComposeCodeVisitor(context: context)
		def myCL = new MyClassLoader(visitor: visitor)
		
		def uri = new URI("file:///C:/Users/Charroux_std/Documents/projet/ExecAndShare/Compose/ComposeTools/ComposeTools/src/main/compose/Code3.groovy")
		def script = myCL.parseClass(new GroovyCodeSource(uri))
		
		//def xmlSpringContent = "./src/main/resources/QoSSpringContext.xml"
		//def xmlSpringContent = "./src/main/resources/essai.xml"
		def xmlSpringContent = "./src/main/resources/xmlSpringContext.xml"
		
		def xmlGenerator = new XmlGenerator(xmlSpringContent: xmlSpringContent, instructions: visitor.instructions, aggregators: visitor.aggregators)
		xmlGenerator.generate()
		
		def javaCodeGenerator = new JavaCodeGenerator(context: context, instructions: visitor.instructions)
		javaCodeGenerator.generate()
	}

}
