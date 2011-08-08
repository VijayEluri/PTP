//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.10 in JDK 6 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2011.05.10 at 09:53:19 AM CEST 
//


package org.eclipse.ptp.rm.lml.internal.core.elements;

import java.io.Serializable;


import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;


/**
 * 
 *     			Defines several jobs, that are now running on the
 *     			system. CpuCount can be computed by adding all
 *     			cpuCount-attributes of the defined job-tags.
 *     		
 * 
 * <p>Java class for usagebar_type complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="usagebar_type">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.llview.de}gobject_type">
 *       &lt;sequence>
 *         &lt;element name="job" type="{http://www.llview.de}job_type" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="cpucount" use="required" type="{http://www.w3.org/2001/XMLSchema}nonNegativeInteger" />
 *       &lt;attribute name="cpupernode" type="{http://www.w3.org/2001/XMLSchema}positiveInteger" default="1" />
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "usagebar_type", propOrder = {
    "job"
})
public class UsagebarType
    extends GobjectType
 implements Serializable {

    protected List<JobType> job;
    @XmlAttribute(required = true)
    @XmlSchemaType(name = "nonNegativeInteger")
    protected BigInteger cpucount;
    @XmlAttribute
    @XmlSchemaType(name = "positiveInteger")
    protected BigInteger cpupernode;

    /**
     * Gets the value of the job property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the job property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getJob().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link JobType }
     * 
     * 
     */
    public List<JobType> getJob() {
        if (job == null) {
            job = new ArrayList<JobType>();
        }
        return this.job;
    }

    /**
     * Gets the value of the cpucount property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getCpucount() {
        return cpucount;
    }

    /**
     * Sets the value of the cpucount property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setCpucount(BigInteger value) {
        this.cpucount = value;
    }

    /**
     * Gets the value of the cpupernode property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getCpupernode() {
        if (cpupernode == null) {
            return new BigInteger("1"); //$NON-NLS-1$
        } else {
            return cpupernode;
        }
    }

    /**
     * Sets the value of the cpupernode property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setCpupernode(BigInteger value) {
        this.cpupernode = value;
    }

}