//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.0.5-b02-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2011.02.14 at 04:05:44 PM CST 
//

package org.eclipse.ptp.rm.jaxb.core.data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>
 * Java class for anonymous complex type.
 * 
 * <p>
 * The following schema fragment specifies the expected content contained within
 * this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="contents" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="deleteAfterUse" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *       &lt;attribute name="name" use="required" type="{http://www.w3.org/2001/XMLSchema}anySimpleType" />
 *       &lt;attribute name="uniqueIdSuffix" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = { "contents" })
@XmlRootElement(name = "managed-file")
public class ManagedFile {

	protected String contents;
	@XmlAttribute
	protected Boolean deleteAfterUse;
	@XmlAttribute(required = true)
	protected String name;
	@XmlAttribute
	protected Boolean uniqueIdSuffix;

	/**
	 * Gets the value of the contents property.
	 * 
	 * @return possible object is {@link String }
	 * 
	 */
	public String getContents() {
		return contents;
	}

	/**
	 * Gets the value of the name property.
	 * 
	 * @return possible object is {@link String }
	 * 
	 */
	public String getName() {
		return name;
	}

	/**
	 * Gets the value of the deleteAfterUse property.
	 * 
	 * @return possible object is {@link Boolean }
	 * 
	 */
	public Boolean isDeleteAfterUse() {
		return deleteAfterUse;
	}

	/**
	 * Gets the value of the uniqueIdSuffix property.
	 * 
	 * @return possible object is {@link Boolean }
	 * 
	 */
	public Boolean isUniqueIdSuffix() {
		return uniqueIdSuffix;
	}

	/**
	 * Sets the value of the contents property.
	 * 
	 * @param value
	 *            allowed object is {@link String }
	 * 
	 */
	public void setContents(String value) {
		this.contents = value;
	}

	/**
	 * Sets the value of the deleteAfterUse property.
	 * 
	 * @param value
	 *            allowed object is {@link Boolean }
	 * 
	 */
	public void setDeleteAfterUse(Boolean value) {
		this.deleteAfterUse = value;
	}

	/**
	 * Sets the value of the name property.
	 * 
	 * @param value
	 *            allowed object is {@link String }
	 * 
	 */
	public void setName(String value) {
		this.name = value;
	}

	/**
	 * Sets the value of the uniqueIdSuffix property.
	 * 
	 * @param value
	 *            allowed object is {@link Boolean }
	 * 
	 */
	public void setUniqueIdSuffix(Boolean value) {
		this.uniqueIdSuffix = value;
	}

}
