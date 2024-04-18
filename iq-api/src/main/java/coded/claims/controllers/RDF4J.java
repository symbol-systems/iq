package systems.symbol.controllers;

import systems.symbol.io.StreamCopy;
import systems.symbol.iq.SPARQLInfer;
import systems.symbol.model.NamedMap;
import systems.symbol.rdf4j.store.Repositories;
import systems.symbol.render.HBSRenderer;
import io.cloudevents.spring.webflux.CloudEventHttpMessageReader;
import io.cloudevents.spring.webflux.CloudEventHttpMessageWriter;
import org.apache.tools.ant.filters.StringInputStream;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.codec.CodecCustomizer;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.CodecConfigurer;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.StringWriter;
import java.util.HashMap;

/*
 *  symbol.systems
 *  Copyright (c) 2009-2015, 2021-2023 Symbol Systems, All Rights Reserved.
 *  Licence: https://systems.symbol/about/license
 */
@SpringBootApplication
@Controller
@RequestMapping("/rdf")
//@Api(tags = { Swagger2SpringBoot.TAG_EVENT })
public class RDF4J {
	File home = new File(System.getProperty("IQ_HOME", "iq"));
	String serverURL = System.getProperty("IQ_RDF4J_SERVER", "");

	public static void main(String[] args) throws Exception {
		SpringApplication.run(RDF4J.class, args);
	}

	@PostMapping(value = "/learn/{repo}/{type}", produces={"text/turtle"}, consumes={"application/json","application/ld+json"})
	// Let Spring do the type conversion of request and response body
	public ResponseEntity<HashMap> learn(
			@RequestBody HashMap body,
			@RequestHeader HttpHeaders headers,
			@PathVariable(value="repo") String repo,
			@PathVariable(value="type") String type
	) {

		System.out.println("headers: "+headers);
		Repository repository = Repositories.getRemoteRepository(serverURL, repo);
		RepositoryConnection connection = repository.getConnection();
		File queryFile = new File(home, type + ".sparql");
		System.out.println("events.learn.ttl: "+queryFile.getAbsolutePath());
		if (!queryFile.exists())
			return ResponseEntity.status(404).body(body);

		try {
			String query = StreamCopy.load(queryFile);
			IRI scriptIRI = connection.getValueFactory().createIRI(queryFile.toURI().toASCIIString());
			System.out.println("events.learn.query: "+scriptIRI+" -> "+query);

			SPARQLInfer.infer(scriptIRI, connection, query, new NamedMap(scriptIRI));
			return ResponseEntity.ok().build();
		} catch (Exception e) {
			return ResponseEntity.status(420).body(body);
		}

	}

	@PostMapping(value = "/load/{repo}/{type}", produces={"text/turtle"}, consumes={"application/json","application/ld+json"})
	@ResponseBody
	public ResponseEntity<String> load(
			@RequestBody HashMap body,
			@RequestHeader HttpHeaders headers,
			@PathVariable(value="repo") String repo,
			@PathVariable(value="type") String type
	) {

		File rdfTemplate = new File(home, type + ".ttl");
		System.out.println("events.load.ttl: "+rdfTemplate.getAbsolutePath());
		if (!rdfTemplate.exists())
			return ResponseEntity.status(404).build();

		Repository repository = Repositories.getRemoteRepository(serverURL, repo);
		RepositoryConnection connection = repository.getConnection();
		try {
			String query = StreamCopy.load(rdfTemplate);
			StringWriter turtle = new StringWriter();
			HBSRenderer.writer(query, body, turtle);
			System.out.println("events.load.turtle: "+rdfTemplate.getAbsolutePath()+" -> "+turtle);
			connection.begin();
			connection.add( new StringInputStream(turtle.toString(), "utf-8"), RDFFormat.TURTLE);
			connection.commit();
			connection.close();
			return ResponseEntity.ok().body(turtle.toString());
		} catch (Exception e) {
			return ResponseEntity.status(500).build();
		}
	}

	@PostMapping(value = "/{type}", produces={"text/turtle"}, consumes={"application/json","application/ld+json"})
	@ResponseBody
	public ResponseEntity<String> transform(
			@RequestBody HashMap body,
			@PathVariable(value="type") String type
	) {

		System.out.println("events.transform.home: "+home.getAbsolutePath()+" --: "+System.getProperty("IQ_HOME"));
		File rdfTemplate = new File(home, type );
		System.out.println("events.transform.ttl: "+rdfTemplate.getAbsolutePath());
		if (!rdfTemplate.exists())
			return ResponseEntity.status(404).build();

		try {
			String query = StreamCopy.load(rdfTemplate);
			StringWriter turtle = new StringWriter();
			HBSRenderer.writer(query, body, turtle);
			System.out.println("events.transform.turtle: "+rdfTemplate.getAbsolutePath()+" -> "+turtle);
			return ResponseEntity.ok().body(turtle.toString());
		} catch (Exception e) {
			return ResponseEntity.status(500).build();
		}
	}

//	@PostMapping(value = "/etl/{repo}", produces={"text/turtle"}, consumes={"application/json","application/ld+json"})
//	// Use CloudEvent API and manual type conversion of request and response body
//	public Mono<CloudEvent> event(@RequestBody Mono<CloudEvent> body, @PathVariable(value="repo") String repo) {
//		String serverURL = System.getProperty("IQ_RDF4J_SERVER", "xxx");
//		Repository repository = Repositories.getRemoteRepository(serverURL, repo);
//		IRI scriptIRI = null;
//		RepositoryConnection connection = repository.getConnection();
//		Mono<CloudEvent> eventMono = body.map(event -> {
//			File queryFile = new File(event.getType() + ".sparql");
//			if (!queryFile.exists())
//				return null;
//			String query = StreamCopy.toString(queryFile);
//			try {
//				SPARQLInfer.infer(scriptIRI, connection, query, new Model(scriptIRI));
//				return CloudEventBuilder.from(event)
//						.withId(UUID.randomUUID().toString()) //
//						.withSource(URI.create("https://spring.io/foos")) //
//						.withType("io.spring.event.Foo") //
//						.withData(event.getData().toBytes()) //
//						.build();
//			} catch (IQException e) {
//				throw new RuntimeException(e);
//			} catch (IOException e) {
//				throw new RuntimeException(e);
//			}
//		});
//		return eventMono;
//	}

	@Configuration
	public static class CloudEventHandlerConfiguration implements CodecCustomizer {

		@Override
		public void customize(CodecConfigurer configurer) {
			configurer.customCodecs().register(new CloudEventHttpMessageReader());
			configurer.customCodecs().register(new CloudEventHttpMessageWriter());
		}

	}

}