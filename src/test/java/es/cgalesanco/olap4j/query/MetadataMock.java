package es.cgalesanco.olap4j.query;

import org.olap4j.metadata.MetadataElement;

public class MetadataMock implements MetadataElement {

	protected String name;
	protected String uniqueName;
	protected String caption;
	protected String description;
	protected boolean visible;

	public MetadataMock(String name, String uniqueName) {
		super();
		this.name = name;
		this.uniqueName = uniqueName;
		this.caption = name;
		this.description = null;
		this.visible = true;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public String getUniqueName() {
		return this.uniqueName;
	}

	@Override
	public String getCaption() {
		return this.caption;
	}

	@Override
	public String getDescription() {
		return this.description;
	}

	@Override
	public boolean isVisible() {
		return visible;
	}

	@Override
	public boolean equals(Object obj) {
		if ( obj == null )
			return false;
		if ( !(obj instanceof MetadataElement) )
			return false;
		
		return getUniqueName().equals(((MetadataMock)obj).getUniqueName());
	}
	
	@Override
	public int hashCode() {
		return getUniqueName().hashCode();
	}
	
	@Override
	public String toString() {
		return getUniqueName();
	}
}