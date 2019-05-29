/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.apikit.validation.body.form;

import org.junit.Test;
import org.mule.module.apikit.AbstractMultiParserFunctionalTestCase;
import org.mule.runtime.core.api.util.IOUtils;

import java.io.InputStream;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.startsWith;

public abstract class MultipartFormFunctionalTestCase extends AbstractMultiParserFunctionalTestCase {

  @Test
  public void answer400WhenRequiredFormParameterIsNotProvided() throws Exception {
    given().multiPart("Unnecessary", "Form Parameter")
        .expect()
        .response()
        .statusCode(400)
        .body(is("{message: 'Bad Request'}"))
        .when().post("/api/users");
  }

  @Test
  public void answer400WhenRequiredFormParameterIsInvalid() throws Exception {
    given().multiPart("userId", "I am not an integer HAHAHA")
        .expect()
        .response()
        .statusCode(400)
        .body(is("{message: 'Bad Request'}"))
        .when().post("/api/users");
  }

  @Test
  public void answer200WhenRequiredFormParameterIsValid() throws Exception {
    given().multiPart("userId", 5101)
        .expect()
        .response()
        .statusCode(200)
        .body(is("5101"))
        .when().post("/api/users");
  }

  @Test
  public void answer200WhenMultipleRequiredFormParameterAreProvided() throws Exception {
    given().multiPart("userId", 5101)
        .multiPart("second", "segundo")
        .multiPart("third", true)
        .expect()
        .response()
        .statusCode(201)
        .body(is("[\n" +
            "  \"userId\",\n" +
            "  \"second\",\n" +
            "  \"third\"\n" +
            "]"))
        .when().post("/api/multiple-required-multipart");
  }

  @Test
  public void answer200WhenMultipleOptionalFormParameterAreNotProvidedAndAdded() throws Exception {
    given().multiPart("userId", 5101)
        .expect()
        .response()
        .statusCode(201)
        .body(is("[\n" +
            "  \"userId\",\n" +
            "  \"second\",\n" +
            "  \"third\"\n" +
            "]"))
        .when().post("/api/multiple-optional-multipart");
  }

  @Test
  public void answer200WhenOptionalFormParameterIsNotProvided() throws Exception {
    given().multiPart("Unnecessary", "Form Parameter")
        .expect()
        .response()
        .statusCode(200)
        .body(is("content-not-provided"))
        .when().post("/api/announcements");
  }

  @Test
  public void answer400WhenOptionalFormParameterIsInvalid() throws Exception {
    given().multiPart("content", "More than 10 characters")
        .expect()
        .response()
        .statusCode(400)
        .body(is("{message: 'Bad Request'}"))
        .when().post("/api/announcements");
  }

  @Test
  public void answer200WhenOptionalFormParameterIsValid() throws Exception {
    given().multiPart("content", "Is Valid")
        .expect()
        .response()
        .statusCode(200)
        .body(is("Is Valid"))
        .when().post("/api/announcements");
  }

  @Test
  public void setDefaultFormParameterForMultipartRequest() throws Exception {
    given().multiPart("first", "primero")
        .multiPart("payload", "3.4")
        .expect().response()
        .body(is("{\n" +
            "  \"first\": \"primero\",\n" +
            "  \"payload\": \"3.4\",\n" +
            "  \"second\": \"segundo\",\n" +
            "  \"third\": \"true\"\n" +
            "}"))
        .statusCode(201)
        .when().post("/api/multipart");
  }
  @Test
  public void postTextFileResourceIntoMultiPartFormData() throws Exception {
    given().multiPart("document", "lorem.txt", this.getClass().getClassLoader()
        .getResourceAsStream("org/mule/module/apikit/validation/formParameters/lorem.txt"), "text/plain")
        .expect()
        .response()
        .statusCode(200)
        .body(startsWith("Lorem ipsum dolor sit amet"))
        .when().post("/api/uploadFile");
  }


  @Test
  public void postJsonFileResourceIntoMultiPartFormData() throws Exception {
    final String jsonFilePath = "org/mule/module/apikit/validation/formParameters/example.json";

    final String jsonAsString = IOUtils.toString(getResourceAsStream(jsonFilePath));

    given().multiPart("document", "example.json", getResourceAsStream(jsonFilePath), "application/json")
            .expect()
            .response()
            .statusCode(200)
            .body(is(jsonAsString))
            .when().post("/api/uploadJsonFile");
  }

  @Test
  public void postXMLFileResourceIntoMultiPartFormData() throws Exception {
    final String xmlFilePath = "org/mule/module/apikit/validation/formParameters/example.xml";

    final String xmlAsString = IOUtils.toString(getResourceAsStream(xmlFilePath));

    given().multiPart("document", "example.xml", getResourceAsStream(xmlFilePath), "application/xml")
        .expect()
        .response()
        .statusCode(200)
        .body(is(xmlAsString))
        .when().post("/api/uploadXmlFile");
  }

  @Test
  public void postImageResourceIntoMultiPartFormData() throws Exception {
    InputStream resourceAsStream = getResourceAsStream("org/mule/module/apikit/validation/formParameters/bbva.jpg");
    given().multiPart("image", "bbva.jpg", resourceAsStream)
        .expect()
        .response()
        .statusCode(200)
        .body(is("true"))
        .when().post("/api/uploadImage");
  }

  @Test
  public void answer201WhenOptionalFormParameterIsProvidedAsEmpty() throws Exception {
    given().multiPart("first", "required")
        .multiPart("third", "false")
        .multiPart("fourth", "")
        .expect()
        .response()
        .statusCode(201)
        .body(is("{\n" +
            "  \"first\": \"required\",\n" +
            "  \"third\": \"false\",\n" +
            "  \"fourth\": \"\",\n" +
            "  \"second\": \"segundo\"\n" +
            "}"))
        .when().post("/api/multipart");
  }

  private InputStream getResourceAsStream(String resource) {
    return this.getClass().getClassLoader().getResourceAsStream(resource);
  }
}