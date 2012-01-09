/*
 * DomUI Java User Interface - shared code
 * Copyright (c) 2010 by Frits Jalvingh, Itris B.V.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 *
 * See the "sponsors" file for a list of supporters.
 *
 * The latest version of DomUI and related code, support and documentation
 * can be found at http://www.domui.org/
 * The contact for the project is Frits Jalvingh <jal@etc.to>.
 */
package to.etc.dbpool.info;

/**
 * Base class containing just count and overall time stuff.
 *
 * @author <a href="mailto:jal@etc.to">Frits Jalvingh</a>
 * Created on Nov 4, 2010
 */
public class InfoCollectorBase {
	private final String m_ident;

	/** The time that this request started. */
	private final long m_ts_started;

	/** Total duration, if known. */
	private long m_duration;

	/**
	 * The #of times a connection was allocated during this request.
	 */
	protected int m_nConnectionAllocations;

	/**
	 * The #of times a statement was PREPARED (preparedStatement)
	 */
	protected int m_nPrepares;

	/**
	 * The total time spent in preparing the statements above.
	 */
	protected long m_prepareDuration;

	/**
	 * #of queries issued using prepared statements.
	 */
	protected int m_nPreparedQueries;

	/**
	 * #of updates issued using prepared statements.
	 */
	protected int m_nPreparedUpdates;

	/**
	 * #of unprepared updates (using a statement).
	 */
	protected int m_nStatementUpdates;

	protected long m_nUpdatedRows;

	protected long m_preparedQueryDuration;

	protected long m_preparedUpdateDuration;

	protected long m_statementQueryDuration;

	protected long m_statementUpdateDuration;

	protected int m_nExecutes;

	protected long m_executeDuration;

	protected int m_nErrors;

	/** Number of rows retrieved, */
	protected int m_nRows;

	protected long m_totalFetchDuration;

	/** The #of statements passed */
	protected int m_nStatementQueries;

	public InfoCollectorBase(String ident) {
		m_ts_started = System.nanoTime();
		m_ident = ident;
	}

	/**
	 * Copy constructor.
	 * @param o
	 */
	public InfoCollectorBase(InfoCollectorBase o, long duration) {
		m_executeDuration = o.m_executeDuration;
		m_ident = o.m_ident;
		m_nConnectionAllocations = o.m_nConnectionAllocations;
		m_nErrors = o.m_nErrors;
		m_nExecutes = o.m_nExecutes;
		m_nPreparedQueries = o.m_nPreparedQueries;
		m_nPreparedUpdates = o.m_nPreparedUpdates;
		m_nPrepares = o.m_nPrepares;
		m_nRows = o.m_nRows;
		m_totalFetchDuration = o.m_totalFetchDuration;
		m_nStatementQueries = o.m_nStatementQueries;
		m_nStatementUpdates = o.m_nStatementUpdates;
		m_nUpdatedRows = o.m_nUpdatedRows;
		m_preparedQueryDuration = o.m_preparedQueryDuration;
		m_preparedUpdateDuration = o.m_preparedUpdateDuration;
		m_prepareDuration = o.m_prepareDuration;
		m_statementQueryDuration = o.m_statementQueryDuration;
		m_statementUpdateDuration = o.m_statementUpdateDuration;
		m_ts_started = o.m_ts_started;
		m_duration = duration;
	}

	public int getNConnectionAllocations() {
		return m_nConnectionAllocations;
	}

	public int getNPrepares() {
		return m_nPrepares;
	}

	public long getPrepareDuration() {
		return m_prepareDuration;
	}

	public int getNPreparedQueries() {
		return m_nPreparedQueries;
	}

	public int getNPreparedUpdates() {
		return m_nPreparedUpdates;
	}

	public int getNStatementUpdates() {
		return m_nStatementUpdates;
	}

	public long getNUpdatedRows() {
		return m_nUpdatedRows;
	}

	public long getPreparedQueryDuration() {
		return m_preparedQueryDuration;
	}

	public long getPreparedUpdateDuration() {
		return m_preparedUpdateDuration;
	}

	public long getStatementQueryDuration() {
		return m_statementQueryDuration;
	}

	public long getStatementUpdateDuration() {
		return m_statementUpdateDuration;
	}

	public int getNExecutes() {
		return m_nExecutes;
	}

	public long getExecuteDuration() {
		return m_executeDuration;
	}

	public int getNErrors() {
		return m_nErrors;
	}

	public int getNStatementQueries() {
		return m_nStatementQueries;
	}

	public int getTotalQueries() {
		return m_nExecutes + m_nStatementQueries + m_nPreparedQueries;
	}

	public int getTotalUpdates() {
		return m_nPreparedUpdates + m_nStatementUpdates;
	}

	public int getTotalDBRequests() {
		return getTotalQueries() + getTotalUpdates();
	}

	public long getTotalDBTime() {
		return m_executeDuration + m_preparedQueryDuration + m_preparedUpdateDuration + m_prepareDuration + m_statementQueryDuration + m_statementUpdateDuration + m_totalFetchDuration;
	}

	public int getNRows() {
		return m_nRows;
	}

	public long getTotalFetchDuration() {
		return m_totalFetchDuration;
	}

	public long getStartTS() {
		return m_ts_started;
	}

	public long getDuration() {
		return m_duration;
	}

	public String getIdent() {
		return m_ident;
	}

}
