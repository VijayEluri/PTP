/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - Initial API and implementation
 *******************************************************************************/package org.eclipse.ptp.core.attributes;

import java.util.Arrays;
import java.util.List;

public final class ArrayAttribute extends AbstractAttribute implements IAttribute {

	private List<Object> value;

	public ArrayAttribute(ArrayAttributeDefinition definition, String initialValue) throws IllegalValueException {
		super(definition);
		setValue(initialValue);
	}

	public ArrayAttribute(ArrayAttributeDefinition definition, Object[] initialValue) throws IllegalValueException {
		super(definition);
		setValue(initialValue);
	}

	public Object[] getValue() {
		return this.value.toArray();
	}
	
	public void setValue(Object[] value) {
		this.value = Arrays.asList(value);
	}

	public void addAll(Object[] value) {
		this.value.addAll(Arrays.asList(value));
	}

	public String getValueAsString() {
		return Arrays.toString(value.toArray());
	}

	public boolean isValid(String string) {
		return true;
	}

	public void setValue(String string) throws IllegalValueException {
		String[] values = string.split("");
		setValue(values);
	}
}
