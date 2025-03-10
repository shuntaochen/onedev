package io.onedev.server.ee;

import io.onedev.commons.loader.AbstractPlugin;
import io.onedev.commons.loader.AbstractPluginModule;
import io.onedev.commons.loader.ImplementationProvider;
import io.onedev.commons.utils.ClassUtils;
import io.onedev.server.FeatureManager;
import io.onedev.server.OneDev;
import io.onedev.server.cluster.ClusterManager;
import io.onedev.server.ee.clustering.ClusterManagementPage;
import io.onedev.server.ee.clustering.EEClusterManager;
import io.onedev.server.ee.dashboard.DashboardPage;
import io.onedev.server.ee.dashboard.widgets.WidgetGroup;
import io.onedev.server.ee.subscription.DefaultSubscriptionManager;
import io.onedev.server.ee.subscription.SubscriptionManagementPage;
import io.onedev.server.ee.subscription.SubscriptionManager;
import io.onedev.server.ee.storage.EEStorageManager;
import io.onedev.server.ee.storage.StorageSetting;
import io.onedev.server.ee.terminal.BuildTerminalPage;
import io.onedev.server.ee.terminal.EETerminalManager;
import io.onedev.server.ee.xsearch.*;
import io.onedev.server.model.support.Widget;
import io.onedev.server.storage.StorageManager;
import io.onedev.server.terminal.TerminalManager;
import io.onedev.server.web.WebApplicationConfigurator;
import io.onedev.server.web.mapper.BasePageMapper;
import io.onedev.server.web.page.layout.*;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

/**
 * NOTE: Do not forget to rename moduleClass property defined in the pom if you've renamed this class.
 *
 */
public class EEModule extends AbstractPluginModule {

	@Override
	protected void configure() {
		super.configure();

		bind(TerminalManager.class).to(EETerminalManager.class);
		bind(ClusterManager.class).to(EEClusterManager.class);
		bind(StorageManager.class).to(EEStorageManager.class);
		bind(CodeSearchManager.class).to(DefaultCodeSearchManager.class);
		bind(CodeIndexManager.class).to(DefaultCodeIndexManager.class);
		bind(SubscriptionManager.class).to(DefaultSubscriptionManager.class);
		bind(FeatureManager.class).to(EEFeatureManager.class);
		bind(CodeIndexStatusChangedBroadcaster.class);
		
		bind(MainMenuCustomization.class).to(EEMainMenuCustomization.class);
		contribute(ImplementationProvider.class, new ImplementationProvider() {

			@Override
			public Collection<Class<?>> getImplementations() {
				Collection<Class<?>> implementations = new HashSet<>();
				for (Class<?> implementation: ClassUtils.findImplementations(Widget.class, WidgetGroup.class))
					implementations.add(implementation);
				return implementations;
			}

			@Override
			public Class<?> getAbstractClass() {
				return Widget.class;
			}

		});

		contribute(WebApplicationConfigurator.class, application -> {
			application.mount(new BasePageMapper("~dashboards", DashboardPage.class));
			application.mount(new BasePageMapper("~dashboards/${dashboard}", DashboardPage.class));
			application.mount(new BasePageMapper("~projects/${project}/builds/${build}/terminal", BuildTerminalPage.class));
			application.mount(new BasePageMapper("~code-search/text", TextSearchPage.class));
			application.mount(new BasePageMapper("~code-search/symbols", SymbolSearchPage.class));
			application.mount(new BasePageMapper("~code-search/files", FileSearchPage.class));
			application.mount(new BasePageMapper("~administration/cluster", ClusterManagementPage.class));
			application.mount(new BasePageMapper("~administration/subscription-management", SubscriptionManagementPage.class));
		});
		
		contribute(AdministrationSettingContribution.class, () -> {
			var settings = new ArrayList<Class<? extends ContributedAdministrationSetting>>();
			if (OneDev.getInstance(SubscriptionManager.class).isActive())
				settings.add(StorageSetting.class);
			return settings;
		});
		contribute(AdministrationMenuContribution.class, () -> {
			var menuItems = new ArrayList<SidebarMenuItem>();
			if (OneDev.getInstance(SubscriptionManager.class).isActive())
				menuItems.add(new SidebarMenuItem.Page(null, "High Availability & Scalability", ClusterManagementPage.class, new PageParameters()));
			menuItems.add(new SidebarMenuItem.Page(null, "Subscription Management", SubscriptionManagementPage.class, new PageParameters()));
			return menuItems;
		});
	}

	@Override
	protected Class<? extends AbstractPlugin> getPluginClass() {
		return EEPlugin.class;
	}
	
}
