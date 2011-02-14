package org.eclipse.ptp.rm.jaxb.core.variables;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.variables.IDynamicVariable;
import org.eclipse.core.variables.IDynamicVariableResolver;
import org.eclipse.ptp.rm.jaxb.core.JAXBCorePlugin;

public class RMVariableResolver implements IDynamicVariableResolver {

	public String resolveValue(IDynamicVariable variable, String argument) throws CoreException {
		Map<String, Object> variables = RMVariableMap.getInstance().getVariables();
		String[] parts = argument.split("[#]");
		System.out.println("LOOKING FOR " + parts[0] + ": ");
		System.out.println("MAP HAS KEYS: " + variables.keySet());
		Object value = variables.get(parts[0]);
		if (value != null)
			if (parts.length == 2)
				try {
					return invokeGetter(value, parts[1]);
				} catch (Throwable t) {
					t.printStackTrace();
					Status status = new Status(Status.ERROR, JAXBCorePlugin.getUniqueIdentifier(),
							"RMVariable dereferencing error", t);
					throw new CoreException(status);
				}
			else
				return value.toString();
		return null;
	}

	/*
	 * We should provide an interface to make sure these are all contained on
	 * this classloader ...
	 */
	private String invokeGetter(Object value, String string) throws SecurityException, NoSuchMethodException,
			IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		String name = "get" + string.substring(0, 1).toUpperCase() + string.substring(1);
		System.out.println("APPLYING " + name + " to " + value);
		Method getter = value.getClass().getDeclaredMethod(name, (Class[]) null);
		Object result = getter.invoke(value, (Object[]) null);
		if (result == null)
			return null;
		return result.toString();
	}
}
