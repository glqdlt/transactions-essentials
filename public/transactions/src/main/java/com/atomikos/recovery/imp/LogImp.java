package com.atomikos.recovery.imp;

import java.util.Collection;

import com.atomikos.icatch.TxState;
import com.atomikos.logging.Logger;
import com.atomikos.logging.LoggerFactory;
import com.atomikos.recovery.CoordinatorLogEntry;
import com.atomikos.recovery.CoordinatorLogEntryRepository;
import com.atomikos.recovery.LogException;
import com.atomikos.recovery.LogReadException;
import com.atomikos.recovery.OltpLog;
import com.atomikos.recovery.ParticipantLogEntry;
import com.atomikos.recovery.RecoveryLog;

public class LogImp implements OltpLog, RecoveryLog {

	private static final Logger LOGGER = LoggerFactory.createLogger(LogImp.class);
	
	private CoordinatorLogEntryRepository repository;

	public void setRepository(CoordinatorLogEntryRepository repository) {
		this.repository = repository;
	}

	@Override
	public void write(CoordinatorLogEntry coordinatorLogEntry)
			throws IllegalStateException, LogException {
		if (!entryAllowed(coordinatorLogEntry)) {
			throw new IllegalStateException();
		}
		repository.put(coordinatorLogEntry.coordinatorId, coordinatorLogEntry);
	}

	private boolean entryAllowed(CoordinatorLogEntry coordinatorLogEntry) throws LogReadException {
		CoordinatorLogEntry existing = repository
				.get(coordinatorLogEntry.coordinatorId);
		return coordinatorLogEntry.transitionAllowedFrom(existing);
	}

	

	@Override
	public void terminated(ParticipantLogEntry entry)  {
		try {
			CoordinatorLogEntry coordinatorLogEntry =null;
				coordinatorLogEntry = repository.get(entry.coordinatorId);
			if (coordinatorLogEntry == null) {
				LOGGER.logWarning("termination called on non existent Coordinator "
						+ entry.coordinatorId + " " + entry.participantUri);
			} else {
				CoordinatorLogEntry updated = coordinatorLogEntry.terminated(entry);
				repository.put(updated.coordinatorId, updated);
			}
		} catch (LogException e) {
			//TODO coordinator will remain committing in log - clean up by admin tools?
			LOGGER.logWarning("Unable to write to repository "+entry+" ignoring");
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}

	

	@Override
	public void terminatedWithHeuristicRollback(ParticipantLogEntry entry) throws LogException {

		CoordinatorLogEntry coordinatorLogEntry = repository.get(entry.coordinatorId);
		if (coordinatorLogEntry == null) {
			LOGGER.logWarning("terminatedWithHeuristicRollback called on non existent Coordinator "
					+ entry.coordinatorId + " " + entry.participantUri);
		} else {
			CoordinatorLogEntry updated = coordinatorLogEntry.terminatedWithHeuristicRollback(entry);
			repository.put(updated.coordinatorId, updated);
		}
	}

	@Override
	public Collection<ParticipantLogEntry> getCommittingParticipants()
			throws LogReadException {

		return repository.findAllCommittingParticipants();
	}

	@Override
	public void presumedAborting(ParticipantLogEntry entry)
			throws IllegalStateException, LogException {
		if (entry == null || entry.state != TxState.IN_DOUBT) {
			throw new IllegalArgumentException();
		}
		
		CoordinatorLogEntry coordinatorLogEntry = repository.get(entry.coordinatorId);
		if (coordinatorLogEntry == null) {
			coordinatorLogEntry = createCoordinatorLogEntry(entry);
			write(coordinatorLogEntry);
			throw new IllegalStateException();
		} else {
			CoordinatorLogEntry updated = coordinatorLogEntry.presumedAborting(entry);
			write(updated);
		}
	}

	private CoordinatorLogEntry createCoordinatorLogEntry(
			ParticipantLogEntry entry) {
		CoordinatorLogEntry coordinatorLogEntry;
		ParticipantLogEntry[] participantDetails = new ParticipantLogEntry[1];
		participantDetails[0] = entry;
		coordinatorLogEntry = new CoordinatorLogEntry(entry.coordinatorId,
				participantDetails);
		return coordinatorLogEntry;
	}

	@Override
	public void terminatedWithHeuristicCommit(ParticipantLogEntry entry) throws LogException {
		CoordinatorLogEntry coordinatorLogEntry = repository
				.get(entry.coordinatorId);
		if (coordinatorLogEntry == null) {
			LOGGER.logWarning("terminatedWithHeuristicCommit called on non existent Coordinator "
					+ entry.coordinatorId + " " + entry.participantUri);
		} else {
			CoordinatorLogEntry updated = coordinatorLogEntry.terminatedWithHeuristicCommit(entry);
			repository.put(updated.coordinatorId, updated);
			
		}

	}

	@Override
	public void terminatedWithHeuristicHazard(ParticipantLogEntry entry) {
		LOGGER.logWarning("terminatedWithHeuristicHazard " + entry);
	}

	@Override
	public void terminatedWithHeuristicMixed(ParticipantLogEntry entry) throws LogException {
		CoordinatorLogEntry coordinatorLogEntry = repository
				.get(entry.coordinatorId);
		if (coordinatorLogEntry == null) {
			LOGGER.logWarning("terminatedWithHeuristicMixed called on non existent Coordinator "
					+ entry.coordinatorId + " " + entry.participantUri);
		} else {

			CoordinatorLogEntry updated = coordinatorLogEntry.terminatedWithHeuristicMixed(entry);
			repository.put(updated.coordinatorId, updated);
			
		}
	}

}
