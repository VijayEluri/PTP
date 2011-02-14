//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.0.5-b02-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2011.02.14 at 04:05:44 PM CST 
//

package org.eclipse.ptp.rm.jaxb.core.data;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementRefs;
import javax.xml.bind.annotation.XmlMixed;
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
 *         &lt;element ref="{}arglist" maxOccurs="unbounded"/>
 *         &lt;element ref="{}parser-ref" maxOccurs="2" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="displayStderr" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *       &lt;attribute name="displayStdout" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *       &lt;attribute name="name" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = { "content" })
@XmlRootElement(name = "command")
public class Command {

	@XmlElementRefs({ @XmlElementRef(name = "arglist", type = Arglist.class),
			@XmlElementRef(name = "parser-ref", type = JAXBElement.class) })
	@XmlMixed
	protected List<Object> content;
	@XmlAttribute
	protected Boolean displayStderr;
	@XmlAttribute
	protected Boolean displayStdout;
	@XmlAttribute
	protected String name;

	/**
	 * Gets the value of the content property.
	 * 
	 * <p>
	 * This accessor method returns a reference to the live list, not a
	 * snapshot. Therefore any modification you make to the returned list will
	 * be present inside the JAXB object. This is why there is not a
	 * <CODE>set</CODE> method for the content property.
	 * 
	 * <p>
	 * For example, to add a new item, do as follows:
	 * 
	 * <pre>
	 * getContent().add(newItem);
	 * </pre>
	 * 
	 * 
	 * <p>
	 * Objects of the following type(s) are allowed in the list {@link Arglist }
	 * {@link String } {@link JAXBElement }{@code <}{@link String }{@code >}
	 * 
	 * 
	 */
	public List<Object> getContent() {
		if (content == null)
			content = new ArrayList<Object>();
		return this.content;
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
	 * Gets the value of the displayStderr property.
	 * 
	 * @return possible object is {@link Boolean }
	 * 
	 */
	public Boolean isDisplayStderr() {
		return displayStderr;
	}

	/**
	 * Gets the value of the displayStdout property.
	 * 
	 * @return possible object is {@link Boolean }
	 * 
	 */
	public Boolean isDisplayStdout() {
		return displayStdout;
	}

	/**
	 * Sets the value of the displayStderr property.
	 * 
	 * @param value
	 *            allowed object is {@link Boolean }
	 * 
	 */
	public void setDisplayStderr(Boolean value) {
		this.displayStderr = value;
	}

	/**
	 * Sets the value of the displayStdout property.
	 * 
	 * @param value
	 *            allowed object is {@link Boolean }
	 * 
	 */
	public void setDisplayStdout(Boolean value) {
		this.displayStdout = value;
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

}
