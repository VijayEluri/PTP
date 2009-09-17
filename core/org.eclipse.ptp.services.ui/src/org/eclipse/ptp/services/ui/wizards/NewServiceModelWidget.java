/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    IBM Corporation - initial API and implementation
 *******************************************************************************/ 
package org.eclipse.ptp.services.ui.wizards;


import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.eclipse.ptp.services.core.IService;
import org.eclipse.ptp.services.core.IServiceCategory;
import org.eclipse.ptp.services.core.IServiceConfiguration;
import org.eclipse.ptp.services.core.IServiceProvider;
import org.eclipse.ptp.services.core.IServiceProviderDescriptor;
import org.eclipse.ptp.services.core.ServiceModelManager;
import org.eclipse.ptp.services.internal.core.ServiceConfiguration;
import org.eclipse.ptp.services.ui.IServiceProviderContributor;
import org.eclipse.ptp.services.ui.ServiceModelUIManager;
import org.eclipse.ptp.services.ui.messages.Messages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

public class NewServiceModelWidget extends Composite {
	
	// Keys for data attached to TreeItems that represent services
	private static final String SERVICE_KEY = "service";   // IService //$NON-NLS-1$
	private static final String DISABLED_KEY = "disabled"; // Boolean //$NON-NLS-1$
	private static final String PROVIDER_KEY = "provider"; // IServiceProvider //$NON-NLS-1$
	

	private IServiceConfiguration configuration;

	private Tree servicesTree;
	private Button enabledCheckbox;
	private Combo providerCombo;
	private Composite configurationComposite;
	private Composite providerComposite;
	private StackLayout stackLayout;
	
	private Image enabledIcon;
	private Image disabledIcon;
	private Image configIcon;
	
	private Map<String,IServiceProvider> providerMap = new HashMap<String,IServiceProvider>();
	
	
	public NewServiceModelWidget(Composite parent, int style) {
		super(parent, style);
		setLayout(new GridLayout(2, false));

		Label label = new Label(this, SWT.NONE);
		label.setLayoutData(new GridData());
		label.setText(Messages.NewServiceModelWidget_0);
		
		Label filler = new Label(this, SWT.NONE); 
		filler.setLayoutData(new GridData());
		filler.setText(""); //$NON-NLS-1$
		
		Composite left = new Composite(this, SWT.NONE);
		GridData data = new GridData(GridData.FILL_VERTICAL);
		data.widthHint = 200;
		left.setLayoutData(data);
		GridLayout layout = new GridLayout(1, false);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		left.setLayout(layout);
		
		servicesTree = new Tree(left, SWT.BORDER  | SWT.SINGLE);
		servicesTree.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		servicesTree.addSelectionListener(new SelectionAdapter() {
			@Override public void widgetSelected(SelectionEvent e) {
				displayService(servicesTree.getSelection()[0]);
			}
		});
		
		providerComposite = new Composite(this, SWT.NONE);
		providerComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
		providerComposite.setLayout(new GridLayout(1, false));
		
		enabledCheckbox = new Button(providerComposite, SWT.CHECK);
		enabledCheckbox.setText(Messages.NewServiceModelWidget_1);
		enabledCheckbox.setLayoutData(new GridData());
		enabledCheckbox.addSelectionListener(new SelectionAdapter() {
			@Override public void widgetSelected(SelectionEvent e) {
				boolean checked = enabledCheckbox.getSelection();
				changeServiceState(!checked);
			}
		});
		enabledCheckbox.setEnabled(false);
		
		Label provider = new Label(providerComposite, SWT.NONE);
		provider.setText(Messages.NewServiceModelWidget_2);
		provider.setLayoutData(new GridData());
		provider.setEnabled(false);
		
		providerCombo = new Combo(providerComposite, SWT.DROP_DOWN | SWT.READ_ONLY);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = 250;
		providerCombo.setLayoutData(data);
		providerCombo.addSelectionListener(new SelectionAdapter() {
			@Override public void widgetSelected(SelectionEvent e) {
				IServiceProviderDescriptor[] descriptors = (IServiceProviderDescriptor[]) providerCombo.getData();
				selectProvider(descriptors[providerCombo.getSelectionIndex()]);
			}
		});
		providerCombo.setEnabled(false);
		
		Label separator = new Label(providerComposite, SWT.SEPARATOR | SWT.SHADOW_OUT | SWT.HORIZONTAL);
		separator.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		configurationComposite = new Composite(providerComposite, SWT.NONE);
		data = new GridData(GridData.FILL_BOTH);
		data.horizontalSpan = 2;
		configurationComposite.setLayoutData(data);
		stackLayout = new StackLayout();
		stackLayout.marginHeight = 0;
		stackLayout.marginWidth = 0;
		configurationComposite.setLayout(stackLayout);

		// TODO make this better using an ImageRegistry
		enabledIcon  = new Image(getDisplay(), getClass().getResourceAsStream("/icons/etool16/service.gif")); //$NON-NLS-1$
		disabledIcon = new Image(getDisplay(), getClass().getResourceAsStream("/icons/etool16/service-disabled.gif")); //$NON-NLS-1$
		configIcon   = new Image(getDisplay(), getClass().getResourceAsStream("/icons/etool16/service-category.gif")); //$NON-NLS-1$
		
		addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				enabledIcon.dispose();
				disabledIcon.dispose();
				configIcon.dispose();
			}
		});
	}
	
	
	
	/**
	 * Causes the tree to display all the services that are available
	 * in the system. Services that are not part of the given service
	 * configuration will be shown as disabled. Services that are part
	 * of the given configuration will show as enabled.
	 * 
	 * Any changes made by the user will only be applied to the given configuration 
	 * when the applyChangesToConfiguration() method is called.
	 */
	public void setServiceConfiguration(IServiceConfiguration conf) {
		setServiceConfiguration(conf, null);
	}


	/**
	 * Causes the tree to display all the services that are available
	 * in the system. Services that are not part of the given service
	 * configuration will be shown as disabled. Services that are part
	 * of the given configuration will show as enabled.
	 * 
	 * Any changes made by the user will only be applied to the given configuration 
	 * when the applyChangesToConfiguration() method is called.
	 * 
	 * Additionally the services tree will be filtered to exclude services
	 * that do not apply to the given set of project nature IDs. This is
	 * useful when the widget is used as part of a project properties page
	 * as only the services that apply to the project will be shown.
	 */
	public void setServiceConfiguration(IServiceConfiguration configuration, Set<String> natureIds) {
		this.configuration = configuration;
		createTreeContent(natureIds);
	}
	
	
	/**
	 * Returns the service configuration object that is being displayed
	 * by this widget. In order for the changes made by the user to be 
	 * reflected in the configuration the applyChangesToConfiguration() method
	 * must be called first.
	 */
	public IServiceConfiguration getServiceConfiguration() {
		return configuration;
	}
	
	
	protected void displayService(TreeItem serviceTreeItem) {
		// Each tree item represents a service
		IService service = (IService) serviceTreeItem.getData(SERVICE_KEY);
		
		// clear everything out
		providerCombo.removeAll();
		for(Control child : providerComposite.getChildren())
			child.setEnabled(service != null);
		
		// if the user selected a category node then clear out the composite too and quit
		if(service == null) {
			stackLayout.topControl = null;
			configurationComposite.layout();
			return;
		}
		
		// get the service provider that has been selected
		IServiceProvider provider = (IServiceProvider) serviceTreeItem.getData(PROVIDER_KEY);
		if(provider == null && !configuration.isDisabled(service)) {
			provider = configuration.getServiceProvider(service);
		}
		
		// populate the provider combo
		Set<IServiceProviderDescriptor> providers = service.getProviders();
		// it's possible there are no providers
		if (providers.size() == 0) {
			return;
		}
		IServiceProviderDescriptor[] descriptors = providers.toArray(new IServiceProviderDescriptor[0]);
		Arrays.sort(descriptors, PROVIDER_COMPARATOR);
		
		int selection = 0;
		for(int i = 0; i < descriptors.length; i++) {
			providerCombo.add(descriptors[i].getName());
			if(provider != null && provider.getId().equals(descriptors[i].getId()))
				selection = i;
		}
		providerCombo.setData(descriptors);
		providerCombo.select(selection);
		
		// set the enabled/disabled state appropriately
		boolean disabled = Boolean.TRUE.equals(serviceTreeItem.getData(DISABLED_KEY));
		providerCombo.setEnabled(!disabled);
		enabledCheckbox.setSelection(!disabled);
		
		if(disabled) {
			// this is easier than disabling the provider composite
			stackLayout.topControl = null;
			configurationComposite.layout();
		}
		else {
			selectProvider(descriptors[selection]);
		}
	}
	
	
	private void selectProvider(IServiceProviderDescriptor descriptor) {
		TreeItem serviceTreeItem = servicesTree.getSelection()[0];
		IServiceProvider newProvider = providerMap.get(descriptor.getId());
		if(newProvider == null) {
			newProvider = ServiceModelManager.getInstance().getServiceProvider(descriptor);
			providerMap.put(newProvider.getId(), newProvider);
			
			IService service = (IService) serviceTreeItem.getData(SERVICE_KEY);
			IServiceProvider existingProvider = getExistingProvider(newProvider.getId(), service);
			if(existingProvider != null) {
				for(String key : existingProvider.keySet()) {
					newProvider.putString(key, existingProvider.getString(key, null));
				}
			}
		}
		
		serviceTreeItem.setData(PROVIDER_KEY, newProvider);
		
		Composite comp = new Composite(configurationComposite, SWT.NONE);
		GridLayout layout = new GridLayout(1,false);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		comp.setLayout(layout);
		
		ServiceModelUIManager uim = ServiceModelUIManager.getInstance();
		IServiceProviderContributor contributor = uim.getServiceProviderContributor(descriptor);
		
		if(contributor != null) {
			contributor.configureServiceProvider(newProvider, comp);
		}
		
		stackLayout.topControl = comp;
		configurationComposite.layout();
	}

	
	/**
	 * Returns the active provider if there is one or returns
	 * one of the former providers if possible.
	 */
	private IServiceProvider getExistingProvider(String providerId, IService service) {
		IServiceProvider setProvider = configuration.getServiceProvider(service);
		if(setProvider != null && providerId.equals(setProvider.getId()))
			return setProvider;
		
		if(configuration instanceof ServiceConfiguration) {
			for(IServiceProvider formerProvider : ((ServiceConfiguration)configuration).getFormerServiceProviders(service)) {
				if(providerId.equals(formerProvider.getId())) {
					return formerProvider;
				}
			}
		}
		return null;
	}
	
	
	protected void changeServiceState(boolean disabled) {
		TreeItem serviceTreeItem = servicesTree.getSelection()[0];
		serviceTreeItem.setData(DISABLED_KEY, disabled);		
		serviceTreeItem.setImage(disabled ? disabledIcon : enabledIcon);
		displayService(serviceTreeItem);
	}
	
	
	private void createTreeContent(Set<String> filterNatureIds) {
		servicesTree.removeAll();
		if(configuration == null)
			return;
		
		if(filterNatureIds == null)
			filterNatureIds = Collections.emptySet();
		
		SortedSet<IService> defaultCategoryServices = new TreeSet<IService>(SERVICE_COMPARATOR);
		SortedMap<IServiceCategory, SortedSet<IService>> categoryServices = new TreeMap<IServiceCategory, SortedSet<IService>>(CATEGORY_COMPARATOR);
		
		for(IService service : ServiceModelManager.getInstance().getServices()) {
			if(filterOut(service.getNatures(), filterNatureIds))
				continue;
			
			IServiceCategory category = service.getCategory();
			if(category == null) {
				defaultCategoryServices.add(service);
			}
			else {
				SortedSet<IService> services = categoryServices.get(category);
				if(services == null) {
					services = new TreeSet<IService>(SERVICE_COMPARATOR);
					categoryServices.put(category, services);
				}
				services.add(service);
			}
		}
		
		for(Map.Entry<IServiceCategory,SortedSet<IService>> entry : categoryServices.entrySet()) {
			TreeItem parent = createTreeCategory(servicesTree, entry.getKey());
			for(IService service : entry.getValue()) {
				createTreeService(parent, service);
			}
			parent.setExpanded(true);
		}
		
		if(!defaultCategoryServices.isEmpty()) {
			TreeItem parent = createTreeCategory(servicesTree, null);
			for(IService service : defaultCategoryServices) {
				createTreeService(parent, service);
			}
			parent.setExpanded(true);
		}
	}
	
	private static boolean filterOut(Set<String> serviceIds, Set<String> filterIds) {
		if(serviceIds.isEmpty() || filterIds.isEmpty())
			return false;

		for(String id : serviceIds) {
			if(filterIds.contains(id)) {
				return false;
			}
		}
		return true;
	}
	
	private TreeItem createTreeCategory(Tree parent, IServiceCategory category) {
		TreeItem item = new TreeItem(servicesTree, SWT.NONE);
		item.setText(category == null ? Messages.NewServiceModelWidget_3 : category.getName());
		item.setImage(configIcon);
		return item;
	}
	
	private void createTreeService(TreeItem parent, IService service) {
		boolean disabled = configuration.isDisabled(service);
		TreeItem child = new TreeItem(parent, SWT.NONE);
		child.setText(service.getName());
		child.setData(SERVICE_KEY, service);
		child.setData(DISABLED_KEY, disabled);
		child.setImage(disabled ? disabledIcon : enabledIcon);
	}
	
	
	public void applyChangesToConfiguration() {
		for(TreeItem categoryTreeItem : servicesTree.getItems()) {
			for(TreeItem serviceTreeItem : categoryTreeItem.getItems()) {
				IService service = (IService) serviceTreeItem.getData(SERVICE_KEY);
				boolean disabled = Boolean.TRUE.equals(serviceTreeItem.getData(DISABLED_KEY));
				
				if(disabled) {
					configuration.disable(service);
				}
				else {
					// TODO check if the state of the new service provider is actually different from the current one
					// no need to fire bogus change events
					IServiceProvider serviceProvider = (IServiceProvider) serviceTreeItem.getData(PROVIDER_KEY);
					if(serviceProvider != null)
						configuration.setServiceProvider(service, serviceProvider);
				}
			}
		}
	}
	
	
	
	private static Comparator<IServiceCategory> CATEGORY_COMPARATOR = new Comparator<IServiceCategory>() {
		public int compare(IServiceCategory x, IServiceCategory y) {
			return x.getName().compareTo(y.getName());
		}
	};
	
	private static Comparator<IService> SERVICE_COMPARATOR = new Comparator<IService>() {
		public int compare(IService x, IService y) {
			return comparePriorities(x.getPriority(), y.getPriority(), x.getName(), y.getName());
		}
	};
	
	private static Comparator<IServiceProviderDescriptor> PROVIDER_COMPARATOR = new Comparator<IServiceProviderDescriptor>() {
		public int compare(IServiceProviderDescriptor x, IServiceProviderDescriptor y) {
			return comparePriorities(x.getPriority(), y.getPriority(), x.getName(), y.getName());
		}
	};
	
	private static int comparePriorities(Integer p1, Integer p2, String name1, String name2) {
		// sort by priority but fall back on sorting alphabetically
		if(p1 == null && p2 == null)
			return name1.compareTo(name2);
		if(p1 == null)
			return -1;
		if(p2 == null)
			return 1;
		if (p1.equals(p2)) {
			return name1.compareTo(name2);
		}
		return p1.compareTo(p2);
	}
}
