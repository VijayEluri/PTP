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
 *         &lt;element ref="{}token" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="delim" use="required" type="{http://www.w3.org/2001/XMLSchema}anySimpleType" />
 *       &lt;attribute name="return" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = { "token" })
@XmlRootElement(name = "token")
public class Token {

	protected Token token;
	@XmlAttribute(required = true)
	protected String delim;
	@XmlAttribute(name = "return", required = true)
	protected String _return;

	/**
	 * Gets the value of the delim property.
	 * 
	 * @return possible object is {@link String }
	 * 
	 */
	public String getDelim() {
		return delim;
	}

	/**
	 * Gets the value of the return property.
	 * 
	 * @return possible object is {@link String }
	 * 
	 */
	public String getReturn() {
		return _return;
	}

	/**
	 * Gets the value of the token property.
	 * 
	 * @return possible object is {@link Token }
	 * 
	 */
	public Token getToken() {
		return token;
	}

	/**
	 * Sets the value of the delim property.
	 * 
	 * @param value
	 *            allowed object is {@link String }
	 * 
	 */
	public void setDelim(String value) {
		this.delim = value;
	}

	/**
	 * Sets the value of the return property.
	 * 
	 * @param value
	 *            allowed object is {@link String }
	 * 
	 */
	public void setReturn(String value) {
		this._return = value;
	}

	/**
	 * Sets the value of the token property.
	 * 
	 * @param value
	 *            allowed object is {@link Token }
	 * 
	 */
	public void setToken(Token value) {
		this.token = value;
	}

}
