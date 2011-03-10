//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.0.5-b02-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2011.03.09 at 09:35:06 PM CST 
//

package org.eclipse.ptp.rm.jaxb.core.data;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>
 * Java class for command complex type.
 * 
 * <p>
 * The following schema fragment specifies the expected content contained within
 * this class.
 * 
 * <pre>
 * &lt;complexType name="command">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="args" type="{http://org.eclipse.ptp/rm}arglist"/>
 *         &lt;element name="stdout-parser" type="{http://org.eclipse.ptp/rm}tokenizer" minOccurs="0"/>
 *         &lt;element name="stderr-parser" type="{http://org.eclipse.ptp/rm}tokenizer" minOccurs="0"/>
 *         &lt;element name="environment" type="{http://org.eclipse.ptp/rm}environment-variable" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="directory" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="displayStderr" type="{http://www.w3.org/2001/XMLSchema}boolean" default="false" />
 *       &lt;attribute name="displayStdout" type="{http://www.w3.org/2001/XMLSchema}boolean" default="false" />
 *       &lt;attribute name="redirectStderr" type="{http://www.w3.org/2001/XMLSchema}boolean" default="false" />
 *       &lt;attribute name="replaceEnvironment" type="{http://www.w3.org/2001/XMLSchema}boolean" default="false" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "command", propOrder = { "args", "stdoutParser", "stderrParser", "environment" })
public class Command {

	@XmlElement(required = true)
	protected Arglist args;
	@XmlElement(name = "stdout-parser")
	protected Tokenizer stdoutParser;
	@XmlElement(name = "stderr-parser")
	protected Tokenizer stderrParser;
	protected List<EnvironmentVariable> environment;
	@XmlAttribute
	protected String directory;
	@XmlAttribute
	protected Boolean displayStderr;
	@XmlAttribute
	protected Boolean displayStdout;
	@XmlAttribute
	protected Boolean redirectStderr;
	@XmlAttribute
	protected Boolean replaceEnvironment;

	/**
	 * Gets the value of the args property.
	 * 
	 * @return possible object is {@link Arglist }
	 * 
	 */
	public Arglist getArgs() {
		return args;
	}

	/**
	 * Gets the value of the directory property.
	 * 
	 * @return possible object is {@link String }
	 * 
	 */
	public String getDirectory() {
		return directory;
	}

	/**
	 * Gets the value of the environment property.
	 * 
	 * <p>
	 * This accessor method returns a reference to the live list, not a
	 * snapshot. Therefore any modification you make to the returned list will
	 * be present inside the JAXB object. This is why there is not a
	 * <CODE>set</CODE> method for the environment property.
	 * 
	 * <p>
	 * For example, to add a new item, do as follows:
	 * 
	 * <pre>
	 * getEnvironment().add(newItem);
	 * </pre>
	 * 
	 * 
	 * <p>
	 * Objects of the following type(s) are allowed in the list
	 * {@link EnvironmentVariable }
	 * 
	 * 
	 */
	public List<EnvironmentVariable> getEnvironment() {
		if (environment == null) {
			environment = new ArrayList<EnvironmentVariable>();
		}
		return this.environment;
	}

	/**
	 * Gets the value of the stderrParser property.
	 * 
	 * @return possible object is {@link Tokenizer }
	 * 
	 */
	public Tokenizer getStderrParser() {
		return stderrParser;
	}

	/**
	 * Gets the value of the stdoutParser property.
	 * 
	 * @return possible object is {@link Tokenizer }
	 * 
	 */
	public Tokenizer getStdoutParser() {
		return stdoutParser;
	}

	/**
	 * Gets the value of the displayStderr property.
	 * 
	 * @return possible object is {@link Boolean }
	 * 
	 */
	public boolean isDisplayStderr() {
		if (displayStderr == null) {
			return false;
		} else {
			return displayStderr;
		}
	}

	/**
	 * Gets the value of the displayStdout property.
	 * 
	 * @return possible object is {@link Boolean }
	 * 
	 */
	public boolean isDisplayStdout() {
		if (displayStdout == null) {
			return false;
		} else {
			return displayStdout;
		}
	}

	/**
	 * Gets the value of the redirectStderr property.
	 * 
	 * @return possible object is {@link Boolean }
	 * 
	 */
	public boolean isRedirectStderr() {
		if (redirectStderr == null) {
			return false;
		} else {
			return redirectStderr;
		}
	}

	/**
	 * Gets the value of the replaceEnvironment property.
	 * 
	 * @return possible object is {@link Boolean }
	 * 
	 */
	public boolean isReplaceEnvironment() {
		if (replaceEnvironment == null) {
			return false;
		} else {
			return replaceEnvironment;
		}
	}

	/**
	 * Sets the value of the args property.
	 * 
	 * @param value
	 *            allowed object is {@link Arglist }
	 * 
	 */
	public void setArgs(Arglist value) {
		this.args = value;
	}

	/**
	 * Sets the value of the directory property.
	 * 
	 * @param value
	 *            allowed object is {@link String }
	 * 
	 */
	public void setDirectory(String value) {
		this.directory = value;
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
	 * Sets the value of the redirectStderr property.
	 * 
	 * @param value
	 *            allowed object is {@link Boolean }
	 * 
	 */
	public void setRedirectStderr(Boolean value) {
		this.redirectStderr = value;
	}

	/**
	 * Sets the value of the replaceEnvironment property.
	 * 
	 * @param value
	 *            allowed object is {@link Boolean }
	 * 
	 */
	public void setReplaceEnvironment(Boolean value) {
		this.replaceEnvironment = value;
	}

	/**
	 * Sets the value of the stderrParser property.
	 * 
	 * @param value
	 *            allowed object is {@link Tokenizer }
	 * 
	 */
	public void setStderrParser(Tokenizer value) {
		this.stderrParser = value;
	}

	/**
	 * Sets the value of the stdoutParser property.
	 * 
	 * @param value
	 *            allowed object is {@link Tokenizer }
	 * 
	 */
	public void setStdoutParser(Tokenizer value) {
		this.stdoutParser = value;
	}

}
