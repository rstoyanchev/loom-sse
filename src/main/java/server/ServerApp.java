package server;

import java.io.File;

import org.apache.catalina.Context;
import org.apache.catalina.Wrapper;
import org.apache.catalina.startup.Tomcat;

import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

public class ServerApp {

	public static void main(String[] args) throws Exception {

		File base = new File(System.getProperty("java.io.tmpdir"));

		Tomcat server = new Tomcat();
		server.setBaseDir(base.getAbsolutePath());
		server.setHostname("0.0.0.0");
		server.setPort(8080);

		AnnotationConfigWebApplicationContext webAppContext = new AnnotationConfigWebApplicationContext();
		webAppContext.register(WebConfig.class);

		DispatcherServlet dispatcherServlet = new DispatcherServlet();
		dispatcherServlet.setApplicationContext(webAppContext);

		Context context = server.addContext("", null);
		Wrapper wrapper = Tomcat.addServlet(context, "dispatcherServlet", dispatcherServlet);
		wrapper.setLoadOnStartup(1);
		wrapper.setAsyncSupported(true);
		wrapper.addMapping("/");

		server.start();
		System.out.println("Tomcat server started " + server.getConnector().getLocalPort());
		server.getServer().await();
	}

}