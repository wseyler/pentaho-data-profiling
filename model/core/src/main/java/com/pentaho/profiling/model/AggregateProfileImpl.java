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

package com.pentaho.profiling.model;

import com.pentaho.profiling.api.AggregateProfile;
import com.pentaho.profiling.api.MessageUtils;
import com.pentaho.profiling.api.MutableProfileField;
import com.pentaho.profiling.api.MutableProfileFieldValueType;
import com.pentaho.profiling.api.MutableProfileStatus;
import com.pentaho.profiling.api.Profile;
import com.pentaho.profiling.api.ProfileField;
import com.pentaho.profiling.api.ProfileFieldProperty;
import com.pentaho.profiling.api.ProfileState;
import com.pentaho.profiling.api.ProfileStatus;
import com.pentaho.profiling.api.ProfileStatusManager;
import com.pentaho.profiling.api.ProfileStatusMessage;
import com.pentaho.profiling.api.ProfileStatusReadOperation;
import com.pentaho.profiling.api.ProfileStatusReader;
import com.pentaho.profiling.api.ProfileStatusWriteOperation;
import com.pentaho.profiling.api.action.ProfileActionException;
import com.pentaho.profiling.api.commit.CommitAction;
import com.pentaho.profiling.api.commit.CommitStrategy;
import com.pentaho.profiling.api.commit.strategies.LinearTimeCommitStrategy;
import com.pentaho.profiling.api.metrics.MetricContributor;
import com.pentaho.profiling.api.metrics.MetricContributors;
import com.pentaho.profiling.api.metrics.MetricContributorsFactory;
import com.pentaho.profiling.api.metrics.MetricMergeException;
import com.pentaho.profiling.api.metrics.ProfileFieldProperties;
import org.pentaho.osgi.notification.api.NotificationListener;
import org.pentaho.osgi.notification.api.NotificationObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by bryan on 3/5/15.
 */
public class AggregateProfileImpl implements AggregateProfile {
  private static final Logger LOGGER = LoggerFactory.getLogger( AggregateProfileImpl.class );
  private static String KEY_PATH = MessageUtils.getId( "data-profiling-model", AggregateProfileImpl.class );
  private final ProfileStatusManager profileStatusManager;
  private final ProfilingServiceImpl profilingService;
  private final ReadWriteLock readWriteLock;
  private final List<String> childProfileIdList;
  private final Set<String> childProfileIdSet;
  private final List<MetricContributor> metricContributorList;
  private final NotificationListener notificationListener;
  private final AtomicBoolean running;
  private final CommitStrategy commitStrategy;
  private final CommitAction commitAction = new CommitAction() {
    @Override public void perform() {
      commit();
    }
  };
  private ExecutorService executorService;

  public AggregateProfileImpl( ProfileStatusManager profileStatusManager,
                               ProfilingServiceImpl profilingService,
                               MetricContributorsFactory metricContributorsFactory,
                               MetricContributors metricContributors ) {
    this.profileStatusManager = profileStatusManager;
    this.profilingService = profilingService;
    this.metricContributorList = metricContributorsFactory.construct( metricContributors );
    this.childProfileIdList = new ArrayList<String>();
    this.childProfileIdSet = new HashSet<String>();
    this.readWriteLock = new ReentrantReadWriteLock();
    this.running = new AtomicBoolean( false );
    this.commitStrategy = new LinearTimeCommitStrategy( 1000 );
    profileStatusManager.write( new ProfileStatusWriteOperation<Void>() {
      @Override public Void write( MutableProfileStatus profileStatus ) {
        List<ProfileFieldProperty> intrinsicProperties = Arrays.asList( ProfileFieldProperties.LOGICAL_NAME,
          ProfileFieldProperties.PHYSICAL_NAME, ProfileFieldProperties.FIELD_TYPE,
          ProfileFieldProperties.COUNT_FIELD );
        List<ProfileFieldProperty> profileFieldProperties = new ArrayList<ProfileFieldProperty>( intrinsicProperties );
        for ( MetricContributor metricContributor : metricContributorList ) {
          for ( ProfileFieldProperty profileFieldProperty : metricContributor.getProfileFieldProperties() ) {
            profileFieldProperties.add( profileFieldProperty );
          }
        }
        profileStatus.setProfileFieldProperties( profileFieldProperties );
        return null;
      }
    } );
    notificationListener = new NotificationListener() {
      @Override public void notify( NotificationObject notificationObject ) {
        Lock readLock = readWriteLock.readLock();
        readLock.lock();
        try {
          if ( childProfileIdSet.contains( notificationObject.getId() ) ) {
            commitStrategy.eventProcessed();
          }
        } finally {
          readLock.unlock();
        }
      }
    };
  }

  @Override public String getId() {
    return profileStatusManager.getId();
  }

  @Override public String getName() {
    return profileStatusManager.getName();
  }

  @Override public void start( ExecutorService executorService ) {
    if ( !running.getAndSet( true ) ) {
      this.executorService = executorService;
      commitStrategy.init( commitAction, executorService );
      profilingService.register( notificationListener );
      commitStrategy.eventProcessed();
    } else {
      LOGGER.warn( "Tried to start an already running aggregate profile: " + getId() );
    }
  }

  @Override public void stop() {
    running.set( false );
    for ( Profile profile : getChildProfiles() ) {
      profile.stop();
    }
    profileStatusManager.write( new ProfileStatusWriteOperation<Void>() {
      @Override public Void write( MutableProfileStatus profileStatus ) {
        profileStatus.setProfileState( ProfileState.STOPPED );
        profileStatus.setStatusMessages( new ArrayList<ProfileStatusMessage>() );
        return null;
      }
    } );
    profilingService.unregister( notificationListener );
  }

  @Override public boolean isRunning() {
    return running.get();
  }

  @Override public List<Profile> getChildProfiles() {
    List<Profile> result = new ArrayList<Profile>();
    Lock readLock = readWriteLock.readLock();
    readLock.lock();
    try {
      for ( String profileId : childProfileIdList ) {
        result.add( profilingService.getProfile( profileId ) );
      }
    } finally {
      readLock.unlock();
    }
    return result;
  }

  private void merge( MutableProfileStatus into, ProfileStatus from ) {
    for ( MutableProfileField intoField : into.getMutableFieldMap().values() ) {
      if ( LOGGER.isDebugEnabled() ) {
        LOGGER.debug( "Merging field " + intoField.getLogicalName() );
      }
      ProfileField fromField = from.getField( intoField.getPhysicalName() );
      if ( fromField != null ) {
        if ( LOGGER.isDebugEnabled() ) {
          LOGGER.debug( "Field exists in both into and from" );
        }
        Set<String> overlappingTypes = new HashSet<String>( intoField.typeKeys() );
        overlappingTypes.retainAll( fromField.typeKeys() );
        for ( String type : overlappingTypes ) {
          MutableProfileFieldValueType intoType = intoField.getValueTypeMetrics( type );
          intoType.setCount( intoType.getCount() + fromField.getType( type ).getCount() );
        }
      }
    }
    for ( MetricContributor metricContributor : metricContributorList ) {
      try {
        metricContributor.merge( into, from );
      } catch ( MetricMergeException e ) {
        LOGGER.error( e.getMessage(), e );
      }
    }
    for ( MetricContributor metricContributor : metricContributorList ) {
      try {
        metricContributor.setDerived( into );
      } catch ( ProfileActionException e ) {
        LOGGER.error( e.getMessage(), e );
      }
    }
  }

  @Override public synchronized void commit() {
    final List<ProfileStatus> childStatuses = new ArrayList<ProfileStatus>();
    final List<ProfileStatusMessage> newStatusMessages = new ArrayList<ProfileStatusMessage>();
    int num = 1;
    for ( String profileId : childProfileIdList ) {
      ProfileStatusReader profileStatusReader = profilingService.getProfileUpdate( profileId );
      final int finalNum = num;
      profileStatusReader.read( new ProfileStatusReadOperation<Void>() {
        @Override public Void read( ProfileStatus profileStatus ) {
          List<ProfileStatusMessage> statusMessages = profileStatus.getStatusMessages();
          if ( statusMessages != null && statusMessages.size() > 0 ) {
            newStatusMessages
              .add( new ProfileStatusMessage( KEY_PATH, "ChildProfile", Arrays.asList( "" + finalNum ) ) );
            newStatusMessages.addAll( statusMessages );
          }
          childStatuses.add( profileStatus );
          return null;
        }
      } );
      num++;
    }
    profileStatusManager.write( new ProfileStatusWriteOperation<Void>() {
      @Override public Void write( MutableProfileStatus profileStatus ) {
        profileStatus.getMutableFieldMap().clear();
        for ( ProfileStatus childStatus : childStatuses ) {
          merge( profileStatus, childStatus );
        }
        if ( profileStatus.getProfileState() == ProfileState.STOPPED ) {
          profileStatus.setStatusMessages( new ArrayList<ProfileStatusMessage>() );
        } else {
          profileStatus.setStatusMessages( newStatusMessages );
        }
        return null;
      }
    } );
  }

  @Override public void addChildProfile( String profileId ) {
    Profile childProfile = profilingService.getProfile( profileId );
    if ( childProfile != null ) {
      Lock writeLock = readWriteLock.writeLock();
      writeLock.lock();
      try {
        if ( childProfileIdSet.add( profileId ) ) {
          childProfileIdList.add( profileId );
          commitStrategy.eventProcessed();
        } else {
          LOGGER.warn( "Tried to add same child profile id more than once: " + profileId );
        }
      } finally {
        writeLock.unlock();
      }
    } else {
      LOGGER.warn( "Tried to add nonexistent child profile with id: " + profileId );
    }
  }
}
