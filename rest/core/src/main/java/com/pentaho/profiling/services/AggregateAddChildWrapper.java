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

package com.pentaho.profiling.services;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Created by bryan on 3/5/15.
 */
@XmlRootElement
public class AggregateAddChildWrapper {
  private String profileId;
  private String childProfileId;

  public AggregateAddChildWrapper() {
    this( null, null );
  }

  public AggregateAddChildWrapper( String profileId, String childProfileId ) {
    this.profileId = profileId;
    this.childProfileId = childProfileId;
  }

  public String getChildProfileId() {

    return childProfileId;
  }

  public void setChildProfileId( String childProfileId ) {
    this.childProfileId = childProfileId;
  }

  public String getProfileId() {
    return profileId;
  }

  public void setProfileId( String profileId ) {
    this.profileId = profileId;
  }

  //OperatorWrap isn't helpful for autogenerated methods
  //CHECKSTYLE:OperatorWrap:OFF
  @Override public String toString() {
    return "AggregateAddChildWrapper{" +
      "profileId='" + profileId + '\'' +
      ", childProfileId='" + childProfileId + '\'' +
      '}';
  }

  @Override
  public boolean equals( Object o ) {
    if ( this == o ) {
      return true;
    }
    if ( o == null || getClass() != o.getClass() ) {
      return false;
    }

    AggregateAddChildWrapper that = (AggregateAddChildWrapper) o;

    if ( childProfileId != null ? !childProfileId.equals( that.childProfileId ) : that.childProfileId != null ) {
      return false;
    }
    if ( profileId != null ? !profileId.equals( that.profileId ) : that.profileId != null ) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = profileId != null ? profileId.hashCode() : 0;
    result = 31 * result + ( childProfileId != null ? childProfileId.hashCode() : 0 );
    return result;
  }
  //CHECKSTYLE:OperatorWrap:ON
}
