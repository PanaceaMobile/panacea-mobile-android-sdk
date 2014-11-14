package com.panacea.sdk.model;

/**
 * Object representation of the Pagination details returned in Panacea List
 * calls.
 * 
 * @author Cobi Interactive
 */
public class PMPagination
{
	private int total;
	private int page;
	private int limit;
	private int pages;

	public PMPagination()
	{
	}

	public PMPagination(PMDictionary pagination)
	{
		super();

		if (pagination == null)
			return;

		total = pagination.getInt("total", -1);
		page = pagination.getInt("page", -1);
		limit = pagination.getInt("limit", -1);
		pages = pagination.getInt("pages", -1);
	}

	public int getLimit()
	{
		return limit;
	}

	public int getPage()
	{
		return page;
	}

	public boolean hasMorePages()
	{
		if (pages < 1 || total < 1)
			return false;


		if (page < pages)
			return true;
		else
			return false;
	}
}
