package systems.symbol.controllers;

import io.cloudevents.spring.webflux.CloudEventHttpMessageReader;
import io.cloudevents.spring.webflux.CloudEventHttpMessageWriter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.codec.CodecCustomizer;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.CodecConfigurer;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.File;
import java.util.Map;

/*
 *  systems.symbol - Proprietary License
 *  Copyright (c) 2009-2015, 2021-2023 Symbol Systems, All Rights Reserved.
 *  Licence: https://systems.symbol/about/license
 */
@SpringBootApplication
@Controller
@RequestMapping("/rdf")
//@Api(tags = { Swagger2SpringBoot.TAG_EVENT })
public class SpringNotaryAPI {
	File home = new File(System.getProperty("IQ_HOME", "iq"));
	String serverURL = System.getProperty("IQ_RDF4J_SERVER", "");

	public static void main(String[] args) throws Exception {
		SpringApplication.run(SpringNotaryAPI.class, args);
	}

	@PostMapping(value = "/notary", produces={"application/json"}, consumes={"application/json","application/ld+json"})
	// Let Spring do the type conversion of request and response body
	public ResponseEntity<Map> learn(
			@RequestBody Map body,
			@RequestHeader HttpHeaders headers
	) {
		System.out.println("headers: "+headers);
		String type = (String)body.get("type");
		String sub = (String)body.get("subject");
		System.out.println("ce.notarize: "+type+", sub: "+sub);
		return ResponseEntity.status(200).body(body);
	}

	@Configuration
	public static class CloudEventHandlerConfiguration implements CodecCustomizer {

		@Override
		public void customize(CodecConfigurer configurer) {
			configurer.customCodecs().register(new CloudEventHttpMessageReader());
			configurer.customCodecs().register(new CloudEventHttpMessageWriter());
		}

	}

}