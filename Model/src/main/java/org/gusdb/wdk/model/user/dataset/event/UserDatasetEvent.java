package org.gusdb.wdk.model.user.dataset.event;

import java.util.Collections;
import java.util.Set;

import org.gusdb.wdk.model.user.dataset.UserDatasetType;

public class UserDatasetEvent {
  private Set<String> projectsFilter; // null if no filter
  private Integer userDatasetId;
  private UserDatasetType userDatasetType;
  private Integer eventId;
  
  public UserDatasetEvent(Integer eventId, Set<String> projectsFilter, Integer userDatasetId, UserDatasetType userDatasetType) {
    this.eventId = eventId;
    this.projectsFilter = projectsFilter;
    this.userDatasetId = userDatasetId;
    this.userDatasetType = userDatasetType;
  }
  
  public Set<String> getProjectsFilter() { return Collections.unmodifiableSet(projectsFilter); }
  public Integer getUserDatasetId() { return userDatasetId; }
  public UserDatasetType getUserDatasetType() { return userDatasetType; }
  public Integer getEventId() { return eventId; }
}
