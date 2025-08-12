package com.opal;

import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

public interface TransactionAware {
	public void commitPhaseOne(Map<DataSource, TransactionParameter> argTPMap) throws PersistenceException;
	public void commitPhaseTwo(Map<DataSource, TransactionParameter> argTPMap) throws PersistenceException;
	public void ensureCommitStep(CommitStep argCommitStep);
	public CommitStep getCommitStep();
	public TransactionContext getTransactionContext();
	public void leaveTransactionContext();
	public void setCommitStep(CommitStep argCommitStep);
	public void setTransactionContext(TransactionContext argTransactionContext);
	public void rollback();
	public Set<TransactionAware> getRequiredPriorCommits();
	public Set<TransactionAware> getRequiredSubsequentCommits();
}
