package com.atomikos.recovery;

import java.util.Collection;

import com.atomikos.icatch.provider.ConfigProperties;

public interface CoordinatorLogEntryRepository {

	
	void init(ConfigProperties configProperties);
	/**
	 * @throws IllegalArgumentException If the same coordinatorLogEntry is already in the repository. 
	 */
	void put(String id,CoordinatorLogEntry coordinatorLogEntry) throws IllegalArgumentException,LogWriteException;
	
	CoordinatorLogEntry get(String coordinatorId) throws LogReadException;

	Collection<ParticipantLogEntry> findAllCommittingParticipants() throws LogReadException;
	
	Collection<CoordinatorLogEntry> getAllCoordinatorLogEntries();

	void writeCheckpoint(Collection<CoordinatorLogEntry> checkpointContent) throws LogWriteException;
	
	void close();
}
