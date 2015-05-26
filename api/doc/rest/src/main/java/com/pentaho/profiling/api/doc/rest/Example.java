/*******************************************************************************
 *
 * Pentaho Data Profiling
 *
 * Copyright (C) 2002-2015 by Pentaho : http://www.pentaho.com
 *
 *******************************************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ******************************************************************************/

package com.pentaho.profiling.api.doc.rest;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by bryan on 4/8/15.
 */
public class Example {
  private Map<String, String> queryParameters;
  private Map<String, String> pathParameters;
  private Object body;
  private Object response;

  public Example() {
    this( new HashMap<String, String>(), new HashMap<String, String>(), null, null );
  }

  public Example( Map<String, String> queryParameters, Map<String, String> pathParameters, Object body,
                  Object response ) {
    this.queryParameters = queryParameters;
    this.pathParameters = pathParameters;
    this.body = body;
    this.response = response;
  }

  public Map<String, String> getQueryParameters() {
    return queryParameters;
  }

  public void setQueryParameters( Map<String, String> queryParameters ) {
    this.queryParameters = queryParameters;
  }

  public Map<String, String> getPathParameters() {
    return pathParameters;
  }

  public void setPathParameters( Map<String, String> pathParameters ) {
    this.pathParameters = pathParameters;
  }

  public Object getBody() {
    return body;
  }

  public void setBody( Object body ) {
    this.body = body;
  }

  public Object getResponse() {
    return response;
  }

  public void setResponse( Object response ) {
    this.response = response;
  }

  //OperatorWrap isn't helpful for autogenerated methods
  //CHECKSTYLE:OperatorWrap:OFF
  @Override public boolean equals( Object o ) {
    if ( this == o ) {
      return true;
    }
    if ( o == null || getClass() != o.getClass() ) {
      return false;
    }

    Example example = (Example) o;

    if ( queryParameters != null ? !queryParameters.equals( example.queryParameters ) :
      example.queryParameters != null ) {
      return false;
    }
    if ( pathParameters != null ? !pathParameters.equals( example.pathParameters ) : example.pathParameters != null ) {
      return false;
    }
    if ( body != null ? !body.equals( example.body ) : example.body != null ) {
      return false;
    }
    return !( response != null ? !response.equals( example.response ) : example.response != null );

  }

  @Override public int hashCode() {
    int result = queryParameters != null ? queryParameters.hashCode() : 0;
    result = 31 * result + ( pathParameters != null ? pathParameters.hashCode() : 0 );
    result = 31 * result + ( body != null ? body.hashCode() : 0 );
    result = 31 * result + ( response != null ? response.hashCode() : 0 );
    return result;
  }

  @Override public String toString() {
    return "Example{" +
      "queryParameters=" + queryParameters +
      ", pathParameters=" + pathParameters +
      ", body=" + body +
      ", response=" + response +
      '}';
  }
  //CHECKSTYLE:OperatorWrap:ON
}
