/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.apikit.api.console;

import static org.mule.apikit.ApiType.AMF;
import static org.mule.module.apikit.ApikitErrorTypes.throwErrorType;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.commons.io.IOUtils;
import org.mule.module.apikit.api.config.ConsoleConfig;
import org.mule.module.apikit.exception.NotFoundException;
import org.mule.apikit.model.ApiVendor;
import org.mule.runtime.api.exception.ErrorTypeRepository;

public class ConsoleResources {

  private static final String ROOT_CONSOLE_PATH = "/";
  private static final String INDEX_RESOURCE_RELATIVE_PATH = "/index.html";
  private static final String RAML_LOCATION_PLACEHOLDER_KEY = "RAML_LOCATION_PLACEHOLDER";

  private final String CONSOLE_RESOURCES_BASE;
  private ConsoleConfig config;
  private String listenerPath;
  private String requestPath;
  private String queryString;
  private String method;
  private String aceptHeader;
  private ErrorTypeRepository errorTypeRepository;

  public ConsoleResources(ConsoleConfig config, String listenerPath, String requestPath, String queryString, String method,
                          String aceptHeader, ErrorTypeRepository errorTypeRepository) {
    CONSOLE_RESOURCES_BASE = AMF.equals(config.getType()) ? "/console-resources-amf" : "/console-resources";

    this.config = config;
    this.listenerPath = listenerPath;
    this.requestPath = requestPath;
    this.queryString = queryString;
    this.method = method;
    this.aceptHeader = aceptHeader;
    this.errorTypeRepository = errorTypeRepository;
  }

  public Resource getConsoleResource(String resourceRelativePath) {

    // For getting RAML resources
    String raml = getApiResourceIfRequested(resourceRelativePath);
    if (raml != null) {
      return new RamlResource(raml);
    }

    String consoleResourcePath;
    InputStream resourceContent = null;
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    try {
      if (resourceRelativePath.equals(ROOT_CONSOLE_PATH)) {
        consoleResourcePath = CONSOLE_RESOURCES_BASE + INDEX_RESOURCE_RELATIVE_PATH;
      } else if (resourceRelativePath.endsWith(".map")) {
        throw throwErrorType(new NotFoundException(resourceRelativePath), errorTypeRepository);
      } else {
        consoleResourcePath = CONSOLE_RESOURCES_BASE + resourceRelativePath;
      }

      Path normalizedPath = Paths.get(consoleResourcePath).normalize();
      if (!normalizedPath.startsWith(CONSOLE_RESOURCES_BASE)) {
        throw throwErrorType(new NotFoundException(resourceRelativePath), errorTypeRepository);
      }
      resourceContent = getClass().getResourceAsStream(consoleResourcePath);

      if (resourceContent == null) {
        raml = config.getRamlHandler().getRamlV2(resourceRelativePath);
        if (raml == null) {
          throw throwErrorType(new NotFoundException(resourceRelativePath), errorTypeRepository);
        }

        return new RamlResource(raml);
      }

      if (consoleResourcePath.contains("index.html")) {
        resourceContent = updateIndexWithRamlLocation(resourceContent);
      }

      IOUtils.copyLarge(resourceContent, byteArrayOutputStream);
      return new ConsoleResource(byteArrayOutputStream.toByteArray(), consoleResourcePath);
    } catch (IOException e) {
      throw throwErrorType(new NotFoundException(resourceRelativePath), errorTypeRepository);
    } finally {
      IOUtils.closeQuietly(resourceContent);
      IOUtils.closeQuietly(byteArrayOutputStream);
    }


  }

  private InputStream updateIndexWithRamlLocation(InputStream inputStream) throws IOException {
    String ramlLocation;
    if (config.getRamlHandler().getApiVendor().equals(ApiVendor.RAML_10)) {
      ramlLocation = config.getRamlHandler().getRootRamlLocationForV2();
    } else {
      ramlLocation = config.getRamlHandler().getRootRamlLocationForV1();
    }


    String indexHtml = IOUtils.toString(inputStream);
    IOUtils.closeQuietly(inputStream);


    indexHtml = indexHtml.replaceFirst(RAML_LOCATION_PLACEHOLDER_KEY, ramlLocation);
    inputStream = new ByteArrayInputStream(indexHtml.getBytes());

    return inputStream;
  }

  /**
   * Validates if the path specified in the listener is a valid one. In order to this to be valid, path MUST end with "/*".
   * Example: path="/whatever/your/path/is/*"
   *
   * @param listenerPath Path specified in the listener element of the console
   */
  public void isValidPath(String listenerPath) {
    if (listenerPath != null && !listenerPath.endsWith("/*")) {
      throw new IllegalStateException("Console path in listener must end with /*");
    }
  }

  private String getApiResourceIfRequested(String resourceRelativePath) {
    if (queryString.equals("api")) {
      return config.getRamlHandler().dumpRaml();
    }

    if (config.getRamlHandler().isRequestingRamlV1ForConsole(listenerPath, requestPath, queryString, method, aceptHeader)) {
      return config.getRamlHandler().getRamlV1();
    }

    if (config.getRamlHandler().isRequestingRamlV2(listenerPath, requestPath, queryString, method)) {
      return config.getRamlHandler().getRamlV2(resourceRelativePath);
    }

    if (AMF == config.getType() && queryString.equals("amf")) {
      return config.getRamlHandler().getAMFModel();
    }

    return null;
  }
}
