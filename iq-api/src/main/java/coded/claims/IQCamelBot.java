package systems.symbol;

import com.google.common.base.Stopwatch;
import systems.symbol.gql.GQLServlet;
import systems.symbol.rdf4j.NS;
import systems.symbol.rdf4j.io.loader.BulkAssetLoader;

import systems.symbol.rdf4j.iq.KBMS;
import systems.symbol.rdf4j.store.Repositories;
import org.apache.camel.CamelContext;
import org.apache.camel.component.servlet.CamelHttpTransportServlet;
import org.apache.camel.spring.SpringCamelContext;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;

import java.io.File;
import java.io.IOException;

/**
 * Defines entrypoint for a standalone application launcher for Springboot / Camel / Servlets.
 *
 * @author 	Symbol Systems
 *
 */

/*
 *  symbol.systems
 *  Copyright (c) 2009-2015, 2021-2023 Symbol Systems, All Rights Reserved.
 *  Licence: https://systems.symbol/about/license
 */
@SpringBootApplication
public class IQCamelBot extends SpringBootServletInitializer {
	private static final Logger log = LoggerFactory.getLogger(IQCamelBot.class);

	@Value("${iq.home}") File homePath;
	@Value("${camel.springboot.name}") String appName;
	@Value("${iq.context}") String ns = NS.GG_DEMO;
	private Repositories repositories = null;
	private CamelContext camelContext = null;

	public IQCamelBot() throws IOException {
		if (homePath==null) homePath = new File("iq.hub");
		assert homePath.mkdirs();
		init(homePath);
	}

	public IQCamelBot(File homePath) throws IOException {
		init(homePath);
	}

	public void init(File homePath) throws IOException {
		hooks();
		log.info("iq.server.ns: {} -> {}", appName, ns);
		log.info("iq.server.camel: "+camelContext);
		log.info("iq.server.home: "+homePath.getAbsolutePath());
		repositories = new Repositories(homePath);
		camelContext = new SpringCamelContext();

//		try {
//			installResources();
//		} catch (IOException e) {
//			log.error("iq.server.deploy.failed: "+e.getMessage());
//		}
		log.info("iq.server.init");
	}

	private void installResources() throws IOException {
		RepositoryConnection connection = getRepository().getConnection();
		BulkAssetLoader loader = new BulkAssetLoader(ns, connection);
//		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
//		URL resource = classLoader.getResource("iq.bootstrap.ttl");
//		loader.deploy(resource);
		File resourceFolder = new File("src/main/resources/assets/");
		log.info("iq.server.install.folder: "+resourceFolder.getAbsolutePath()+" -> "+resourceFolder.exists());
		loader.deploy(resourceFolder);
		connection.close();
	}

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
		return application.sources(getClass());
	}

	public static void main(String[] args) {
		SpringApplication.run(IQCamelBot.class, args);
	}

	@Bean
	public Repositories getRepositories() {
		return repositories;
	}

	@Bean
	public Repository getRepository() {
		return repositories.getCurrentRepository();
	}

	@Bean
	public IQ newIQ() {
		log.info("iq.server.iq: "+NS.GG_DEMO);
		return new KBMS(NS.GG_DEMO, getRepository().getConnection());
	}
	@Bean
	public CamelContext getCamelContext() {
		log.info("iq.server.camel: "+camelContext);
		return camelContext;
	}

	@Bean
	public ServletRegistrationBean<CamelHttpTransportServlet> newCamelServlet() {
		ServletRegistrationBean<CamelHttpTransportServlet> registrationBean = new ServletRegistrationBean();
		registrationBean.setServlet(new CamelHttpTransportServlet());
		registrationBean.addUrlMappings("/iq/*");
		registrationBean.setName("CamelServlet");
		log.info("iq.server.servlet.camel: "+registrationBean);
		return registrationBean;
	}

	@Bean
	public ServletRegistrationBean<GQLServlet> newGQLServlet() {
		ServletRegistrationBean<GQLServlet> registrationBean = new ServletRegistrationBean();
		registrationBean.setServlet(new GQLServlet(ns, getRepository()));
		registrationBean.addUrlMappings("/graphql/*");
		return registrationBean;
	}

	void hooks() {
		Thread repositoryShutdownHook = new Thread(() -> {
			Stopwatch stopwatch = Stopwatch.createStarted();
			log.info("KBMS graceful shutdown");
			getRepository().shutDown();
			log.info("KBMS saved: "+stopwatch.elapsed());
		});
//		Runtime.getRuntime().addShutdownHook( repositoryShutdownHook );
	}
}
