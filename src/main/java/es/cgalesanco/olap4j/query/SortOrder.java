package es.cgalesanco.olap4j.query;

/**
 * Defines in what order to perform sort operations.
 * 
 * @author César García
 * 
 */
public enum SortOrder {
	/**
	 * Ascending sort order.
	 */
	ASC,
	/**
	 * Sorts in ascending order, but does not maintain members of a same
	 * hierarchy together.
	 */
	BASC,
	/**
	 * Descending sort order.
	 */
	DESC,
	/**
	 * Sorts in descending order, but does not maintain members of a same
	 * hierarchy together.
	 */
	BDESC
};
